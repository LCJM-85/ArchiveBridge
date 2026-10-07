import json
import sys
import unittest
from pathlib import Path
from unittest.mock import patch, MagicMock

sys.path.insert(0, str(Path(__file__).resolve().parents[2] / 'main/python/ai_assistant'))
import tools
import db
import agent


class FactToolsTest(unittest.TestCase):
    def test_prompt_does_not_confuse_truncated_list_with_unknown_total(self):
        self.assertIn('截断只影响名单展示，不影响已返回的范围内总数', agent.SYSTEM_PROMPT)

    def test_prompt_prohibits_inventing_missing_record_causes(self):
        self.assertIn('不要猜测档案缺失的原因', agent.SYSTEM_PROMPT)

    def test_active_count_excludes_graduated_and_unknown(self):
        def query(sql, *args, **kwargs):
            return (114 if 'graduated IS FALSE' in sql else 301,)
        with patch.object(tools, '_query', side_effect=query):
            self.assertEqual(114, json.loads(tools.get_student_count.invoke({}))['student_count'])

    def test_degree_counts_all_use_active_filter(self):
        for degree in [None, '本科', '研究生']:
            with patch.object(tools, '_query', return_value=[('学士', 3)] if degree is None else (3,)) as query:
                tools.get_student_count_by_degree.invoke({'degree': degree})
                for call in query.call_args_list:
                    self.assertIn('graduated IS FALSE', call.args[0])

    def test_detail_preserves_name_without_admission(self):
        with patch.object(tools, '_query', side_effect=[None, None, ('2011-06-30', '法学学士', '毕业', '李剑安')]):
            result = json.loads(tools.get_student_detail.invoke({'student_no': '200630820110'}))
            self.assertEqual('李剑安', result['name'])

    def test_detail_explicitly_labels_missing_record_reason_unknown(self):
        with patch.object(tools, '_query', side_effect=[None, None, ('2011-06-30', '法学学士', '毕业', '李剑安')]):
            result = json.loads(tools.get_student_detail.invoke({'student_no': '200630820110'}))
            self.assertEqual('unknown', result.get('missing_record_reason'))
            self.assertEqual({'admission': False, 'student': False, 'graduation': True}, result.get('record_sources'))

    def test_null_student_status_is_not_active(self):
        with patch.object(tools, '_query', side_effect=[None, (None, None, '', '', '', '王同学'), None]):
            result = json.loads(tools.get_student_detail.invoke({'student_no': '1'}))
            self.assertEqual('状态未知', result['student_status'])

    def test_search_reports_total_and_preserves_name_and_status(self):
        with patch.object(tools, '_query', side_effect=[
            [('1', '王同学', 12)], None, (True,), (1,)
        ]):
            result = json.loads(tools.search_student.invoke({'keyword': '王'}))
            self.assertIsInstance(result, dict)
            self.assertEqual(12, result['total_matches'])
            self.assertTrue(result['truncated'])
            self.assertEqual('王同学', result['students'][0]['name'])
            self.assertFalse(result['students'][0]['in_school'])

    def test_prediction_tool_labels_actuals(self):
        with patch.object(tools, '_query', return_value=(0,)):
            result = json.loads(tools.get_prediction_data.invoke({'year': 2027}))
            self.assertIs(result.get('is_prediction'), False)
            self.assertEqual('historical_actual', result.get('data_kind'))

    def test_search_unknown_status_is_not_false(self):
        with patch.object(tools, '_query', side_effect=[ [('1', '王同学', 1)], None, (None,), None ]):
            result = json.loads(tools.search_student.invoke({'keyword': '王'}))
            self.assertIsNone(result['students'][0]['in_school'])

    def test_pool_is_thread_safe_for_parallel_tools(self):
        with patch.object(db, '_pool', None), patch.object(db.pool, 'ThreadedConnectionPool') as constructor:
            db._get_pool()
            constructor.assert_called_once()

    def test_query_returns_connection_after_rollback_on_error(self):
        conn = MagicMock()
        conn.cursor.return_value.__enter__.return_value.execute.side_effect = ValueError('bad query')
        with patch.object(tools, 'get_conn', return_value=conn), patch.object(tools, 'put_conn'):
            with self.assertRaises(ValueError):
                tools._query('bad SQL')
        conn.rollback.assert_called_once()


if __name__ == '__main__':
    unittest.main()
