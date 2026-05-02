# noinspection PyUnresolvedReferences
from paddleocr import PaddleOCR
import sys
import re
import json
import os
import math

import logging
logging.getLogger("ppocr").setLevel(logging.ERROR)

ocr = PaddleOCR(
    use_textline_orientation=True,
    lang='ch',
    det_model_dir="D:/models/det",
    rec_model_dir="D:/models/rec",
    cls_model_dir="D:/models/cls",
    show_log=False
)

def run_ocr(img_path):
    result = ocr.ocr(img_path)
    boxes = []
    # ===================== 过滤干扰文本 =====================
    ignore_keywords = ["学院", "专业", "第", "页", "制表", "名册", "学习年限", "延长"]
    # =========================================================
    for line in result:
        for word in line:
            text = word[1][0].strip()
            if not text:
                continue
            # 跳过干扰行
            if any(k in text for k in ignore_keywords):
                continue
            x1 = word[0][0][0]
            y1 = word[0][0][1]
            x2 = word[0][2][0]
            y2 = word[0][2][1]
            cx = (x1 + x2) / 2
            cy = (y1 + y2) / 2
            boxes.append({
                "text": text,
                "cx": cx,
                "cy": cy,
                "x1": x1, "y1": y1, "x2": x2, "y2": y2
            })
    return boxes

def group_by_columns(boxes):
    if not boxes:
        return []
    boxes_sorted = sorted(boxes, key=lambda x: x["cx"])
    groups = []
    current_group = [boxes_sorted[0]]
    # ===================== 关键：阈值从 25 → 70 =====================
    threshold = 70
    # ===============================================================
    for b in boxes_sorted[1:]:
        last_cx = current_group[-1]["cx"]
        if b["cx"] - last_cx < threshold:
            current_group.append(b)
        else:
            groups.append(current_group)
            current_group = [b]
    if current_group:
        groups.append(current_group)
    cols = []
    for g in groups:
        g_sorted = sorted(g, key=lambda x: x["cy"])
        col = [x["text"] for x in g_sorted]
        if col:
            cols.append(col)
    return cols

def extract_table_data(columns, rules):
    rows = []
    errors = []

    # ================= 字段映射 =================
    field_map = {}

    for r in rules:
        fn = r.get("fieldName", "").strip()

        field_map[fn] = {
            "field": r.get("fieldCode") or r.get("sourceField"),
            "required": r.get("isRequired", False)
        }

    # ================= 提取列 =================
    table_columns = {}

    max_row_count = 0

    for col in columns:

        if not col:
            continue

        header = col[0].strip()

        if header not in field_map:
            continue

        values = [v.strip() for v in col[1:] if v.strip()]

        table_columns[header] = values

        max_row_count = max(max_row_count, len(values))

    # ================= 转为行数据 =================

    for i in range(max_row_count):

        row_data = {}

        for header, values in table_columns.items():

            info = field_map[header]

            value = values[i] if i < len(values) else ""

            row_data[info["field"]] = value

            # 必填校验
            if info["required"] and not value:
                errors.append({
                    "row": i + 1,
                    "field": info["field"],
                    "message": f"第{i+1}行字段 '{header}' 为空"
                })

        # 空行过滤
        if any(v.strip() for v in row_data.values()):
            rows.append(row_data)

    return {
        "data": rows,
        "errors": errors
    }

if __name__ == "__main__":
    img_path = sys.argv[1]
    boxes = run_ocr(img_path)
    columns = group_by_columns(boxes)
    rules_file = sys.argv[2] if len(sys.argv) >= 3 else None
    rules = []
    if rules_file and os.path.isfile(rules_file):
        with open(rules_file, "r", encoding="utf-8") as f:
            rules = json.load(f)
    result = extract_table_data(columns, rules)
    print(json.dumps(result, ensure_ascii=False, indent=2))