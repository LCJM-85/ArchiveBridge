import asyncio
import json
import sys
import unittest
from datetime import date
from pathlib import Path
from unittest.mock import patch

sys.path.insert(0, str(Path(__file__).resolve().parents[2] / 'main/python/ai_assistant'))
import agent
import tools
from langchain_core.messages import HumanMessage, AIMessage, ToolMessage, SystemMessage
from langgraph.prebuilt.tool_node import ToolCallRequest


class YearContextTest(unittest.TestCase):
    def test_correction_prompt_distinguishes_target_year_from_current_year(self):
        self.assertIn('非当前年份不得称为“今年”', agent.SYSTEM_PROMPT)
        self.assertIn('先承认上一轮口径有误', agent.SYSTEM_PROMPT)
        self.assertIn('不得猜测未录入、正在整理', agent.SYSTEM_PROMPT)

    def resolve(self, question):
        self.assertTrue(hasattr(agent, 'resolve_query_year'), '缺少请求年份解析')
        return agent.resolve_query_year(question, date(2026, 10, 7))

    def test_relative_year(self):
        for question, expected in [('今年录取最多的专业', 2026), ('去年招生人数', 2025), ('前年招生人数', 2024)]:
            self.assertEqual(expected, self.resolve(question))
        self.assertEqual(2026, self.resolve('今年查不到录取数据吗？'))

    def test_correction_year(self):
        self.assertEqual(2026, self.resolve('今年不是2026吗？'))
        self.assertEqual(2025, self.resolve('不对，我问的是2025年'))

    def test_does_not_force_multiyear_or_negative_year(self):
        for question in ['比较今年和去年', '2024和2026年对比', '近三年趋势', '不是2024年', '今年同比去年', '2024年的学生今年毕业人数']:
            self.assertIsNone(self.resolve(question), question)

    def test_request_date_is_fresh_and_raw_question_ignores_rag_year(self):
        self.assertTrue(hasattr(agent, 'build_query_messages'))
        with patch.object(agent, 'current_date', return_value=date(2027, 1, 1)):
            messages = agent.build_query_messages('【知识库资料】2024年招生简章\n\n【用户问题】\n今年招生人数', [])
        self.assertIsInstance(messages[0], SystemMessage)
        self.assertIn('2027-01-01', messages[0].content)
        self.assertIn('2027', messages[0].content)
        self.assertEqual('human', messages[-1].type)

    def request(self, question, year):
        return ToolCallRequest(tool_call={'name': 'get_major_distribution', 'args': {'year': year}, 'id': 'call1', 'type': 'tool_call'},
            tool=tools.get_major_distribution, state={'messages': [HumanMessage(content=question)]}, runtime=None)

    def guard(self):
        self.assertTrue(hasattr(agent, 'QueryYearGuard'), '缺少查询年份拦截')
        return agent.QueryYearGuard()

    def test_wrong_year_is_blocked_before_database_query(self):
        guard = self.guard()
        with patch.object(agent, 'current_date', return_value=date(2026, 10, 7)):
            result = guard.wrap_tool_call(self.request('今年录取最多的专业', 2024), lambda _: self.fail('错误年份不得查询'))
        self.assertEqual('error', result.status)
        self.assertIn('2026', result.content)

    def test_missing_year_filled_and_empty_result_has_scope(self):
        guard = self.guard()
        def execute(request):
            self.assertEqual(2026, request.tool_call['args']['year'])
            return ToolMessage(content='[]', tool_call_id='call1')
        with patch.object(agent, 'current_date', return_value=date(2026, 10, 7)):
            result = guard.wrap_tool_call(self.request('今年录取最多的专业', None), execute)
        data = json.loads(result.content)
        self.assertEqual(2026, data['queried_year'])
        self.assertEqual([], data['data'])
        self.assertIn('不能', data['scope'])

    def test_async_guard_and_multiyear_query(self):
        guard = self.guard()
        async def execute(request):
            self.assertEqual(2024, request.tool_call['args']['year'])
            return ToolMessage(content='[]', tool_call_id='call1')
        result = asyncio.run(guard.awrap_tool_call(self.request('比较2024和2026年', 2024), execute))
        self.assertEqual(2024, json.loads(result.content)['queried_year'])

    def test_stream_and_nonstream_include_correction_context(self):
        history = [{'role': 'user', 'content': '今年录取最多的专业'}, {'role': 'assistant', 'content': '根据2024年数据金融学8人'}]
        captured = []
        class Probe:
            async def ainvoke(self, payload):
                captured.append(payload['messages'])
                return {'messages': [AIMessage(content='probe')]}
            async def astream(self, payload):
                captured.append(payload['messages'])
                yield {'model': {'messages': [AIMessage(content='probe')]}}
        async def run():
            await agent.run_agent(Probe(), '今年不是2026吗？', history)
            return [event async for event in agent.run_agent_stream(Probe(), '今年不是2026吗？', history)]
        asyncio.run(run())
        for messages in captured:
            self.assertIsInstance(messages[0], SystemMessage)
            self.assertIn('重新查询', messages[0].content)
            self.assertEqual('今年不是2026吗？', messages[-1].content)

    def test_disputed_answer_does_not_reseed_wrong_wording(self):
        self.assertTrue(hasattr(agent, 'build_query_messages'))
        messages = agent.build_query_messages('不对，我问的是2025年，请重新查。', [
            {'role': 'user', 'content': '今年录取人数最多的专业是哪个？'},
            {'role': 'assistant', 'content': '2024年，今年金融学8人。'}])
        self.assertFalse(any(isinstance(m, AIMessage) and '今年金融学8人' in m.content for m in messages))
        self.assertTrue(any(isinstance(m, SystemMessage) and '不要沿用“今年”' in m.content for m in messages))

    def test_correction_reply_always_acknowledges_latest_request(self):
        class Probe:
            async def ainvoke(self, payload):
                return {'messages': [AIMessage(content='2025年测试工程专业5人。')]}
            async def astream(self, payload):
                yield {'model': {'messages': [AIMessage(content='2025年测试工程专业5人。')]}}
        async def run():
            answer = await agent.run_agent(Probe(), '不对，我问的是2025年', [])
            events = [e async for e in agent.run_agent_stream(Probe(), '不对，我问的是2025年', [])]
            return answer, events[-1]['content']
        for answer in asyncio.run(run()):
            self.assertIn('按你的纠正', answer)
            self.assertIn('2025年为准', answer)

    def test_admission_year_does_not_use_upload_year(self):
        with patch.object(tools, '_query', return_value=[]) as query:
            tools.get_major_distribution.invoke({'year': 2026})
        self.assertNotIn('create_time', query.call_args.args[0])


if __name__ == '__main__':
    unittest.main()
