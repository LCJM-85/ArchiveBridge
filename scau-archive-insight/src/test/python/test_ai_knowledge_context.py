import sys
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[2] / 'main/python/ai_assistant'))
from rag import retriever
import agent


class KnowledgeContextTest(unittest.TestCase):
    def test_adjacent_clause_is_kept_without_duplicate_primary(self):
        self.assertTrue(hasattr(retriever, 'merge_context_chunks'))
        primary = [{'id': 85, 'kb_id': 7, 'content': '[A03] 保留原图', 'source_title': '接收规范'}]
        neighbors = [primary[0], {'id': 86, 'kb_id': 7, 'content': '[A04] 改名另记原文件名', 'source_title': '接收规范'}]
        rows = retriever.merge_context_chunks(primary, neighbors, max_chars=100)
        self.assertEqual([85, 86], [r['id'] for r in rows])

    def test_budget_does_not_cut_a_clause_in_half(self):
        self.assertTrue(hasattr(retriever, 'merge_context_chunks'))
        primary = [{'id': 1, 'content': '原始命中'}]
        neighbors = [{'id': 2, 'content': '原件和提供副本的界限不能截断'}]
        self.assertEqual(primary, retriever.merge_context_chunks(primary, neighbors, max_chars=8))

    def test_unquoted_reason_cannot_be_presented_as_knowledge_fact(self):
        self.assertTrue(hasattr(agent, 'render_knowledge_evidence'))
        question = '【知识库资料】\n[规则]: 历史人数不等于现实招生总数。不能据此判断缺少记录的原因。\n\n【用户问题】\n人数不同能说明什么？'
        answer = agent.render_knowledge_evidence(question, {'quotes': [{'source': '规则', 'quote': '只能说明档案尚未入库。'}]})
        self.assertNotIn('尚未入库', answer)
        self.assertIn('无法', answer)

    def test_exact_quote_and_source_are_preserved(self):
        self.assertTrue(hasattr(agent, 'render_knowledge_evidence'))
        quote = '工作副本改名不能改变原始图像的内容，原文件名另记在接收登记中。'
        question = f'【知识库资料】\n[接收规范]: {quote}\n\n【用户问题】\n改名怎么处理？'
        answer = agent.render_knowledge_evidence(question, {'quotes': [{'source': '接收规范', 'quote': quote}]})
        self.assertIn(quote, answer)
        self.assertIn('接收规范', answer)

    def test_real_quote_with_wrong_source_is_rejected(self):
        self.assertTrue(hasattr(agent, 'render_knowledge_evidence'))
        quote = '工作副本改名不能改变原始图像的内容。'
        question = f'【知识库资料】\n[接收规范]: {quote}\n\n【用户问题】\n改名怎么处理？'
        answer = agent.render_knowledge_evidence(question, {'quotes': [{'source': '其他规范', 'quote': quote}]})
        self.assertNotIn('其他规范', answer)
        self.assertIn('无法', answer)



if __name__ == '__main__':
    unittest.main()
