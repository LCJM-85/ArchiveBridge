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
    # 过滤无关杂文本
    ignore_keywords = ["学院", "专业", "第", "页", "制表日期", "名册"]
    for line in result:
        for word in line:
            text = word[1][0].strip()
            if not text:
                continue
            # 跳过无关行
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
    # 关键：调大阈值，适配你宽间距表格
    threshold = 70
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

if __name__ == "__main__":
    # 改成你的图片路径
    img_path = "D:\Ideaworkplace\SCAU\c1d967a7-fdf6-4975-b72e-557a58d0a66c.jpg"

    print("===== 开始OCR识别 =====")
    boxes = run_ocr(img_path)

    columns = group_by_columns(boxes)
    print("\n===== 按列分组结果（已优化阈值+过滤杂行） =====")
    for idx, col in enumerate(columns):
        print(f"第{idx+1}列：{col}")