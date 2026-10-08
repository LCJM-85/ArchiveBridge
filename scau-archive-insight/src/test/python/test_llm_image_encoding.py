"""LLM 图片尺寸回归测试；不调用模型接口。"""
import base64
import importlib.util
import tempfile
import unittest
from pathlib import Path

import cv2
import numpy as np

MODULE_PATH = Path(__file__).resolve().parents[2] / 'main/python/ppstructure/llm_extractor.py'
spec = importlib.util.spec_from_file_location('image_test_extractor', MODULE_PATH)
extractor = importlib.util.module_from_spec(spec)
spec.loader.exec_module(extractor)


class ImageEncodingTest(unittest.TestCase):
    def check_dimensions(self, width, height, expected_width, expected_height):
        source = np.zeros((height, width, 3), dtype=np.uint8)
        success, encoded = cv2.imencode('.png', source)
        self.assertTrue(success)
        original_bytes = encoded.tobytes()
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / 'image.png'
            path.write_bytes(original_bytes)
            content, mime = extractor.encode_image(str(path))
            result = cv2.imdecode(np.frombuffer(base64.b64decode(content), np.uint8), cv2.IMREAD_COLOR)
            self.assertEqual((expected_height, expected_width), result.shape[:2])
            self.assertEqual('image/jpeg', mime)
            self.assertEqual(original_bytes, path.read_bytes())

    def test_dense_roster_size_is_limited_to_1200(self):
        self.check_dimensions(1556, 980, 1200, 755)

    def test_large_landscape_is_limited_to_1200(self):
        self.check_dimensions(3200, 2000, 1200, 750)

    def test_large_portrait_preserves_aspect_ratio(self):
        self.check_dimensions(2000, 3200, 750, 1200)

    def test_limit_image_is_not_resized(self):
        self.check_dimensions(1200, 600, 1200, 600)


if __name__ == '__main__':
    unittest.main()
