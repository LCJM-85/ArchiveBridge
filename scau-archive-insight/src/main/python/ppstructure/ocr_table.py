# PP-Structure V3 表格识别 + 字段映射
import sys, json, os, logging, re
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
table_engine = PPStructureV3(lang="ch", use_table_recognition=True)

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

# ====================== 主入口
if __name__ == "__main__":
    img_path = sys.argv[1]
    rule_path = sys.argv[2] if len(sys.argv) >= 3 else None

    rules = []
    if rule_path and os.path.exists(rule_path):
        with open(rule_path, "r", encoding="utf-8") as f:
            rules = json.load(f)

    try:
        results = table_engine.predict(img_path)
        all_data = []
        all_errors = []

        for page_res in results:
            tables = page_res.get("table_res_list", [])
            for tbl in tables:
                html = tbl.get("pred_html", "")
                grid = parse_html_table(html)
                if grid:
                    mapped = extract_metadata(grid, rules)
                    all_data.extend(mapped.get("data", []))
                    all_errors.extend(mapped.get("errors", []))

        output = {"data": all_data, "errors": all_errors}
        print(json.dumps(output, ensure_ascii=False))

    except Exception as e:
        print(json.dumps({"data": [], "errors": [{"msg": str(e)}]}))
