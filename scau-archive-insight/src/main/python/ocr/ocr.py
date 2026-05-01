# noinspection PyUnresolvedReferences
from paddleocr import PaddleOCR
# ... existing code ...

ocr = PaddleOCR(
    use_textline_orientation=True,
    lang='ch',
    det_model_dir="D:/models/det",
    rec_model_dir="D:/models/rec",
    cls_model_dir="D:/models/cls"
)

def run_ocr(img_path):
    result = ocr.ocr(img_path)
    texts = []

    for line in result:
        for word in line:
            texts.append(word[1][0])

    return "\n".join(texts)


if __name__ == "__main__":

    img_path = "D:\ETest\ETest.png"


    print(run_ocr(img_path))