"""部署契约检查：不连接 Docker、数据库或外部 AI 服务。"""
import re
import unittest
from pathlib import Path
from xml.etree import ElementTree

ROOT = Path(__file__).resolve().parents[4]


class DeploymentConfigTest(unittest.TestCase):
    def test_directory_long_labels_fit_their_group_boxes(self):
        graph = ElementTree.parse(ROOT / 'docs/images/项目目录结构图-审查版.xml')
        for label_id, box_id in [('j-svc-d', 'be-box'), ('fv5d', 'fe-box')]:
            label = graph.find(f'.//mxCell[@id="{label_id}"]/mxGeometry').attrib
            box = graph.find(f'.//mxCell[@id="{box_id}"]/mxGeometry').attrib
            self.assertLessEqual(float(label['x']) + float(label['width']),
                                 float(box['x']) + float(box['width']) - 8,
                                 f'{label_id} 文字区域不能越过虚线框')

    def test_ai_stream_has_unbuffered_long_lived_proxy(self):
        nginx = (ROOT / 'scau_archive-frontend/nginx.conf').read_text(encoding='utf-8')
        match = re.search(r'location = /api/ai/chat/stream\s*\{([^}]+)\}', nginx)
        self.assertIsNotNone(match, 'AI SSE 需要独立的代理配置')
        block = match.group(1)
        for setting in ['proxy_buffering off;', 'proxy_cache off;', 'proxy_read_timeout 300s;', 'proxy_pass http://backend:8080;']:
            self.assertIn(setting, block)

    def test_agent_dependencies_match_verified_api_versions(self):
        requirements = (ROOT / 'scau-archive-insight/src/main/python/requirements-common.txt').read_text(encoding='utf-8')
        for requirement in ['langchain==1.3.13', 'langchain-core==1.4.9', 'langchain-openai==1.3.5', 'langgraph==1.2.9']:
            self.assertIn(requirement, requirements)

    def test_backend_image_checks_agent_import_without_api_key(self):
        dockerfile = (ROOT / 'scau-archive-insight/Dockerfile').read_text(encoding='utf-8')
        self.assertIn('from agent import QueryYearGuard', dockerfile)
        self.assertIn('PYTHON_VENV_PATH', (ROOT / 'docker-compose.yml').read_text(encoding='utf-8'))

    def test_readme_explains_review_and_existing_volume_upgrade(self):
        readme = (ROOT / 'README.md').read_text(encoding='utf-8')
        for text in ['archive_review_draft', '已有数据库升级', '不是 OCR 识别准确率', 'docker compose up -d --build',
                     '设置 JWT_SECRET', '招生、毕业两类档案', '单文件上限为 200MB',
                     '原图直接提取', '原文件归档失败', '*-审查版.xml']:
            self.assertIn(text, readme)

    def test_readme_uses_valid_review_diagrams_and_keeps_originals(self):
        readme = (ROOT / 'README.md').read_text(encoding='utf-8')
        for name in ['系统架构图', '项目目录结构图']:
            path = f'docs/images/{name}-审查版.svg'
            self.assertIn(path, readme)
            document = ElementTree.parse(ROOT / path)
            self.assertEqual('{http://www.w3.org/2000/svg}svg', document.getroot().tag)
            self.assertTrue((ROOT / f'docs/images/{name}.svg').is_file())
            self.assertTrue((ROOT / f'docs/images/{name}.xml').is_file())
            embedded = ElementTree.fromstring(document.getroot().attrib['content'])
            editable = ElementTree.parse(ROOT / f'docs/images/{name}-审查版.xml').getroot()
            self.assertEqual(ElementTree.tostring(embedded), ElementTree.tostring(editable))
            original = ElementTree.parse(ROOT / f'docs/images/{name}.svg').getroot()
            original_graph = ElementTree.fromstring(original.attrib['content'])
            original_ids = {cell.attrib['id'] for cell in original_graph.iter('mxCell')}
            updated_ids = {cell.attrib['id'] for cell in embedded.iter('mxCell')}
            self.assertTrue(original_ids <= updated_ids, '原图节点和连线应完整保留')


if __name__ == '__main__':
    unittest.main()
