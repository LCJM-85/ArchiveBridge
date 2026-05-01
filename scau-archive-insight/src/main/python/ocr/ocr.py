# noinspection PyUnresolvedReferences
from paddleocr import PaddleOCR
import sys
import re
import json

# 你原来的模型配置（完全保留，不动）
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

# 你原来的 OCR 逻辑（完全保留）
def run_ocr(img_path):
    result = ocr.ocr(img_path)
    texts = []

    for line in result:
        for word in line:
            texts.append(word[1][0])

    return "\n".join(texts)

# ======================
# 新增：清洗 + 结构化（针对你的毕业生表格）
# ======================
def parse_student_info(text):
    # 1. 空白规范化
    text = re.sub(r'\s+', ' ', text.strip())

    # 2. 提取字段（精准匹配你的表格）
    data = {}

    # 学号
    match_xuehao = re.search(r'学号\s*[:：\s]?([\dA-Za-z]+)', text)
    if match_xuehao:
        data['xuehao'] = match_xuehao.group(1).strip()

    # 姓名
    match_xingming = re.search(r'姓名\s*[:：\s]?([\u4e00-\u9fa5]+)', text)
    if match_xingming:
        data['xingming'] = match_xingming.group(1).strip()

    # 院系
    match_yuanxi = re.search(r'院系\s*[:：\s]?([\u4e00-\u9fa5]+)', text)
    if match_yuanxi:
        data['yuanxi'] = match_yuanxi.group(1).strip()

    # 学生类别
    match_leibie = re.search(r'学生类别\s*[:：\s]?([\u4e00-\u9fa5]+)', text)
    if match_leibie:
        data['leibie'] = match_leibie.group(1).strip()

    # 专业名称
    match_zhuanye = re.search(r'专业名称\s*[:：\s]?([\u4e00-\u9fa5]+)', text)
    if match_zhuanye:
        data['zhuanye'] = match_zhuanye.group(1).strip()

    # 毕业证号
    match_biye = re.search(r'毕业证号\s*[:：\s]?([\dA-Za-z]+)', text)
    if match_biye:
        data['biyezheng'] = match_biye.group(1).strip()

    return data

# ======================
# 主入口（Java调用）
# ======================
if __name__ == "__main__":
    # 获取Java传入的图片路径
    img_path = sys.argv[1]

    # 1. OCR识别（你原来的逻辑）
    raw_text = run_ocr(img_path)

    # 2. 解析成结构化数据
    student_data = parse_student_info(raw_text)

    # 3. 输出JSON给Java（重点！Java直接接收）
    print(json.dumps(student_data, ensure_ascii=False))