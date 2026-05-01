import sys
import cv2
import os
import numpy as np

def enhance_image(input_path, output_path=None):
    # 读取图片（用 imdecode 支持中文路径）
    with open(input_path, 'rb') as f:
        raw = f.read()
    img = cv2.imdecode(np.frombuffer(raw, np.uint8), cv2.IMREAD_COLOR)
    if img is None:
        print("ERROR: 无法读取图片")
        return

    # 1. 转灰度
    gray = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)

    # 2. 高斯去噪
    blur = cv2.GaussianBlur(gray, (3, 3), 0)

    # 3. 自适应二值化
    binary = cv2.adaptiveThreshold(
        blur, 255,
        cv2.ADAPTIVE_THRESH_GAUSSIAN_C,
        cv2.THRESH_BINARY,
        blockSize=15,
        C=3
    )

    # 4. 锐化增强文字边缘
    kernel = np.array([[0, -1, 0],
                       [-1, 5,-1],
                       [0, -1, 0]])
    sharp = cv2.filter2D(binary, -1, kernel)

    # ======================
    # 在这里修改输出路径！
    # ======================
    if not output_path:
        # 用时间戳生成纯 ASCII 文件名（避免中文路径编码问题）
        output_dir = "storage/enhance/"
        if not os.path.exists(output_dir):
            os.makedirs(output_dir)
        output_path = os.path.join(output_dir, f"{int(__import__('time').time() * 1000)}_enhance.jpg")

    cv2.imwrite(output_path, sharp)
    print(output_path)  # 返回给Java

if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("请传入图片路径")
    else:
        in_path = sys.argv[1]
        enhance_image(in_path)