"""
pdf2image.py — 将 PDF 每页转为 PNG 图片

Usage: python pdf2image.py <pdf_path> <output_dir>

Output (stdout): JSON array of page image paths
"""

import json
import os
import sys
import fitz

def main():
    if len(sys.argv) < 3:
        print(json.dumps({"error": "Usage: python pdf2image.py <pdf_path> <output_dir>"}))
        sys.exit(1)

    pdf_path = sys.argv[1]
    output_dir = sys.argv[2]

    if not os.path.exists(pdf_path):
        print(json.dumps({"error": f"PDF file not found: {pdf_path}"}))
        sys.exit(1)

    os.makedirs(output_dir, exist_ok=True)

    doc = fitz.open(pdf_path)
    pages = []

    for page_num in range(len(doc)):
        page = doc[page_num]
        pix = page.get_pixmap(dpi=200)
        base_name = os.path.splitext(os.path.basename(pdf_path))[0]
        img_name = f"{base_name}_p{page_num + 1}.png"
        img_path = os.path.join(output_dir, img_name)
        pix.save(img_path)
        pages.append(img_path)

    doc.close()
    print(json.dumps({"pages": pages}))

if __name__ == '__main__':
    main()
