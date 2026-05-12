import sys
import os
import fitz

def pdf_to_images(pdf_path):
    doc = fitz.open(pdf_path)

    # 用时间戳生成纯 ASCII 输出路径（避免中文路径编码问题）
    ts = str(int(__import__('time').time() * 1000))
    out_dir = os.path.dirname(pdf_path)

    for page_num in range(len(doc)):
        page = doc[page_num]
        pix = page.get_pixmap(dpi=300)
        img_path = os.path.join(out_dir, f"{ts}_page_{page_num + 1}.png")
        pix.save(img_path)
        print(img_path, flush=True)

    doc.close()

if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("请传入PDF路径")
        sys.exit(1)

    pdf_to_images(sys.argv[1])
