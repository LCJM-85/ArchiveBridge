# PP-Structure V3 表格识别 + 字段映射
import sys, json, os, logging, re
import paddle
from paddleocr import PPStructureV3

logging.getLogger("ppocr").setLevel(logging.ERROR)

# ====================== Levenshtein 距离
def levenshtein(a, b):
    m, n = len(a), len(b)
    if m < n:
        a, b = b, a
        m, n = n, m
    prev = list(range(n + 1))
    for i, ca in enumerate(a):
        curr = [i + 1]
        for j, cb in enumerate(b):
            cost = 0 if ca == cb else 1
            curr.append(min(curr[-1] + 1, prev[j + 1] + 1, prev[j] + cost))
        prev = curr
    return prev[n]

# ====================== 初始化
def gpu_runtime_healthy():
    if not paddle.device.is_compiled_with_cuda():
        return False
    if paddle.device.cuda.device_count() <= 0:
        return False
    try:
        paddle.device.set_device("gpu")
        image = paddle.ones([1, 1, 3, 3])
        kernel = paddle.ones([1, 1, 1, 1])
        paddle.nn.functional.conv2d(image, kernel)
        paddle.device.cuda.synchronize()
        return True
    except Exception as exc:
        logging.getLogger(__name__).warning("GPU OCR 不可用，回退 CPU: %s", exc)
        return False


requested_device = os.getenv("OCR_DEVICE", "auto").strip().lower()
if requested_device not in {"auto", "cpu", "gpu"}:
    raise ValueError("OCR_DEVICE 仅支持 auto、cpu 或 gpu")

if requested_device == "cpu":
    ocr_device = "cpu"
    paddle.device.set_device("cpu")
elif gpu_runtime_healthy():
    ocr_device = "gpu"
else:
    if requested_device == "gpu":
        raise RuntimeError("OCR_DEVICE=gpu，但 CUDA/cuDNN 健康检查失败")
    ocr_device = "cpu"
    paddle.device.set_device("cpu")

table_engine = PPStructureV3(
    lang="ch",
    device=ocr_device,
    use_table_recognition=True,
    text_detection_model_name="PP-OCRv4_mobile_det",
    text_recognition_model_name="PP-OCRv4_mobile_rec",
    text_recognition_batch_size=6,
)

# ====================== 从 HTML 解析表格网格
def parse_html_table(html):
    if not html:
        return []
    rows = re.findall(r"<tr>(.*?)</tr>", html, re.DOTALL)
    grid = []
    for row in rows:
        cells = re.findall(r"<td>(.*?)</td>", row, re.DOTALL)
        grid.append([c.strip() for c in cells])
    return grid

# ====================== 字段映射
def extract_metadata(grid, rules):
    if not grid or len(grid) < 1:
        return {"data": [], "errors": []}

    raw_headers = grid[0]
    rows = grid[1:]

    # 构建匹配映射：每个规则注册多个可匹配的键，按优先级匹配
    # 匹配优先级: fieldName > sourceField > fieldCode
    field_entries = []
    for rule in rules:
        code = rule.get("fieldCode")
        name = rule.get("fieldName", "").strip() or None
        source = rule.get("sourceField", "").strip() or None
        required = rule.get("isRequired", False)

        # 收集此规则的候选匹配键（去重、去空）
        candidates = []
        seen = set()
        for key in [name, source, code]:
            if key and key not in seen:
                candidates.append(key)
                seen.add(key)

        if candidates:
            field_entries.append({
                "code": code,
                "required": required,
                "candidates": candidates,
                "primary": candidates[0]
            })

    def match_header(raw):
        h = raw.strip()
        if not h:
            return None
        # 先尝试精确匹配
        for entry in field_entries:
            if h in entry["candidates"]:
                return entry
        # 去所有空白后匹配
        compact = "".join(h.split())
        for entry in field_entries:
            for key in entry["candidates"]:
                if "".join(key.split()) == compact:
                    return entry
        # 包含匹配
        for entry in field_entries:
            for key in entry["candidates"]:
                if key in h or h in key:
                    return entry
        # Levenshtein 距离修正（处理 OCR 识别错字，如"性別"→"性别"）
        best = None
        best_dist = float("inf")
        for entry in field_entries:
            for key in entry["candidates"]:
                d = levenshtein(h, key)
                if d < best_dist:
                    best_dist = d
                    best = entry
        if best:
            max_len = max(len(h), len(best["candidates"][0]))
            # 相似度 >= 70% 或短文本（<=3字）误差 ≤1 字时接受
            if best_dist == 0:
                pass  # 已被精确匹配捕获
            elif max_len <= 3 and best_dist <= 1:
                return best
            elif best_dist / max_len <= 0.3:
                return best
        return None

    header_map = {}
    for h in raw_headers:
        matched = match_header(h)
        header_map[h] = matched

    data = []
    unmatched_headers = [h for h, m in header_map.items() if m is None]

    for row in rows:
        item = {}
        for i, header in enumerate(raw_headers):
            entry = header_map.get(header)
            if entry is None:
                continue
            val = row[i] if i < len(row) else ""
            item[entry["code"]] = val
        data.append(item)

    errors = []
    if unmatched_headers:
        errors.append({"field": "", "message": f"未匹配的列: {', '.join(unmatched_headers)}"})

    return {"data": data, "errors": errors}

def recognize(input_path):
    try:
        results = table_engine.predict(input_path)

        all_grids = []
        all_errors = []

        for page_res in results:
            tables = page_res.get("table_res_list", [])
            for tbl in tables:
                html = tbl.get("pred_html", "")
                grid = parse_html_table(html)
                if grid and len(grid) >= 2:
                    all_grids.append({
                        "headers": grid[0],
                        "rows": grid[1:]
                    })

        return {"grids": all_grids, "errors": all_errors}
    except Exception as e:
        return {"grids": [], "errors": [{"msg": str(e)}]}


# 保留单次命令行入口，方便独立排查脚本问题。
if __name__ == "__main__":
    print(json.dumps(recognize(sys.argv[1]), ensure_ascii=False))
