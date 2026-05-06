# ppstructure_service.py
# 纯 PP-Structure 独立版本，不复用任何外部代码！

import sys
import json
import os
import logging
from paddleocr import PPStructure

# 关闭日志
logging.getLogger("ppocr").setLevel(logging.ERROR)

# ====================== 初始化 PP-Structure（独立、干净）
table_engine = PPStructure(
    lang="ch",
    table=True,
    show_log=False,
    det_model_dir="models/server/ch_PP-OCRv4_det_server_infer",
    rec_model_dir="models/server/ch_PP-OCRv4_rec_server_infer",
    cls_model_dir="models/server/ch_ppocr_mobile_v2.0_cls_infer",
)

# ====================== 运行表格识别（纯 PP-Structure）
def parse_table(img_path):
    result = table_engine(img_path)
    if not result:
        return []

    table = result[0]["res"]
    cells = table["cells"]

    # 构建行列网格
    max_row = max(c["row"] for c in cells) + 1
    max_col = max(c["col"] for c in cells) + 1
    grid = [["" for _ in range(max_col)] for _ in range(max_row)]

    for c in cells:
        grid[c["row"]][c["col"]] = c["text"].strip()

    return grid

# ====================== 字段映射（独立实现，不复用旧代码）
def extract_metadata(grid, rules):
    if not grid:
        return {"data": [], "errors": []}

    headers = grid[0]
    rows = grid[1:]
    field_map = {}

    for rule in rules:
        name = rule.get("fieldName", "").strip()
        code = rule.get("fieldCode") or rule.get("sourceField")
        required = rule.get("isRequired", False)
        field_map[name] = {"code": code, "required": required}

    data = []
    errors = []

    for idx, row in enumerate(rows):
        item = {}
        for i, header in enumerate(headers):
            if header not in field_map:
                continue
            val = row[i] if i < len(row) else ""
            item[field_map[header]["code"]] = val

        data.append(item)

    return {"data": data, "errors": errors}

# ====================== 主入口
if __name__ == "__main__":
    img_path = sys.argv[1]
    rule_path = sys.argv[2] if len(sys.argv) >= 3 else None

    rules = []
    if rule_path and os.path.exists(rule_path):
        with open(rule_path, "r", encoding="utf-8") as f:
            rules = json.load(f)

    grid = parse_table(img_path)
    result = extract_metadata(grid, rules)

    print(json.dumps(result, ensure_ascii=False, indent=2))