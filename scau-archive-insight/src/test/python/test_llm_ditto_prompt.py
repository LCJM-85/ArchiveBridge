"""提取提示词契约测试；不调用模型，不代表实际图片识别准确率。"""
import importlib.util
import unittest
from pathlib import Path

MODULE_PATH = Path(__file__).resolve().parents[2] / 'main/python/ppstructure/llm_extractor.py'
spec = importlib.util.spec_from_file_location('llm_ditto_extractor', MODULE_PATH)
extractor = importlib.util.module_from_spec(spec)
spec.loader.exec_module(extractor)


class DittoPromptTest(unittest.TestCase):
    def setUp(self):
        self.prompt = extractor.build_prompt([
            {'fieldCode': 'gender', 'fieldName': '性别'},
            {'fieldCode': 'province_name', 'fieldName': '省份'},
        ])

    def test_whole_cell_ditto_and_chains(self):
        for text in ['〃', '同上符号', '同列上方最近', '连续同上', '实际值，不输出同上符号']:
            self.assertIn(text, self.prompt)

    def test_partial_ditto_only_inherits_corresponding_part(self):
        for text in ['部分同上', '只继承符号对应的部分', '广东台山', '广东梅县', '不能复制整格']:
            self.assertIn(text, self.prompt)

    def test_ambiguous_marks_must_not_fill_down(self):
        for text in ['空白不等于同上', '横线、污点', '不要跨表、跨页', '无法确定继承来源', '设为 null']:
            self.assertIn(text, self.prompt)


if __name__ == '__main__':
    unittest.main()
