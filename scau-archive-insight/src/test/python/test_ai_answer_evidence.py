import asyncio
import json
import sqlite3
import sys
import unittest
from datetime import date
from pathlib import Path
from unittest.mock import patch

sys.path.insert(0, str(Path(__file__).resolve().parents[2] / 'main/python/ai_assistant'))
import agent
import tools
from langchain_core.messages import AIMessage, ToolMessage


class AnswerEvidenceTest(unittest.TestCase):
    def test_unknown_major_records_are_not_dropped(self):
        conn = sqlite3.connect(':memory:')
        self.addCleanup(conn.close)
        conn.executescript('CREATE TABLE major_dim(major_id INTEGER, major_name TEXT);'
                           'CREATE TABLE admission_fact(major_id INTEGER);'
                           "INSERT INTO major_dim VALUES(1,'农学');"
                           'INSERT INTO admission_fact VALUES(NULL),(NULL),(1);')
        def execute(sql, params=None, **kwargs):
            return conn.execute(sql.replace('COUNT(*)::int', 'COUNT(*)'), params or ()).fetchall()
        with patch.object(tools, '_query', side_effect=execute):
            rows = json.loads(tools.get_major_distribution.invoke({}))
        self.assertEqual({'未知': 2, '农学': 1}, {r['name']: r['count'] for r in rows})

    def test_unqueried_followup_cannot_invent_count_in_both_paths(self):
        class NoQuery:
            async def ainvoke(self, payload):
                return {'messages': [AIMessage(content='2026年录取45人。')]}
            async def astream(self, payload):
                yield {'model': {'messages': [AIMessage(content='2026年录取45人。')]}}
        history = [{'role': 'user', 'content': '2001年数据库记录的录取人数是多少？'},
                   {'role': 'assistant', 'content': '8人。'}]
        async def run():
            answer = await agent.run_agent(NoQuery(), '那今年呢？', history)
            events = [e async for e in agent.run_agent_stream(NoQuery(), '那今年呢？', history)]
            return answer, events[-1]['content']
        for answer in asyncio.run(run()):
            self.assertNotIn('45', answer)
            self.assertIn('无法', answer)

    def test_count_is_rendered_from_current_tool_not_model_text(self):
        evidence = ToolMessage(name='get_admission_stats', tool_call_id='c1', content=json.dumps({
            'queried_year': 2026, 'data': {'total_admissions': 0, 'major_count': 0,
                                         'avg_score': None, 'province_count': 0}}))
        answer = agent.grounded_database_answer('今年数据库录取人数是多少？', '45人', [evidence], [], date(2026, 10, 8))
        self.assertNotIn('45', answer)
        self.assertIn('未找到', answer)

    def test_status_answer_does_not_claim_archive_completion(self):
        data = {'student_no': '209910070003', 'name': '实验丙', 'student_status': '已毕业',
                'record_sources': {'admission': True, 'student': True, 'graduation': True},
                'graduation_date': '2005-06-30', 'degree': '学士', 'destination': '就业'}
        evidence = ToolMessage(name='get_student_detail', tool_call_id='c1', content=json.dumps(data))
        answer = agent.grounded_database_answer('学号209910070003现在是否在籍、是否毕业？',
                    '三类档案完整，已接收完成。', [evidence], [], date(2026, 10, 8))
        self.assertIn('不在籍', answer)
        self.assertIn('已毕业', answer)
        self.assertNotIn('已接收完成', answer)
        self.assertNotIn('三类档案完整', answer)

    def test_wrong_year_evidence_is_not_rendered(self):
        evidence = ToolMessage(name='get_major_distribution', tool_call_id='c1',
                              content='{"queried_year":2024,"data":[{"name":"农学","count":8}]}')
        answer = agent.grounded_database_answer('今年各专业录取人数？', '农学8人', [evidence], [], date(2026, 10, 8))
        self.assertNotIn('农学8人', answer)
        self.assertIn('无法', answer)

    def test_knowledge_question_is_not_mistaken_for_database_query(self):
        self.assertTrue(hasattr(agent, 'requires_database_query'))
        self.assertFalse(agent.requires_database_query('依据知识库，历史人数少于预测值能否证明预测错误？', []))
        self.assertTrue(agent.requires_database_query('2001年各专业录取人数分别是多少？', []))


if __name__ == '__main__':
    unittest.main()
