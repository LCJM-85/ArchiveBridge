"""真实模型 + 模拟数据库的年份纠正验收；不读写真实学生数据。"""
import asyncio
import json
import os
import re
import sys
from pathlib import Path
from unittest.mock import patch

import yaml
sys.path.insert(0, str(Path(__file__).resolve().parents[2] / 'main/python/ai_assistant'))
import agent
import tools


def configure():
    cfg = yaml.safe_load((Path(__file__).resolve().parents[2] / 'main/resources/application.yaml').read_text(encoding='utf-8'))
    def resolve(value):
        match = re.fullmatch(r'\$\{([^:}]+)(?::(.*))?\}', value or '')
        return os.getenv(match[1], match[2] or '') if match else value
    os.environ['GLM_API_KEY'] = resolve(cfg['llm']['api-key'])
    os.environ['LLM_BASE_URL'] = resolve(cfg['llm']['base-url'])


async def main():
    configure()
    year = agent.current_date().year
    history = [{'role': 'user', 'content': '今年录取人数最多的专业是哪个？'},
               {'role': 'assistant', 'content': '根据2024年数据，今年金融学和英语并列第一，各8人。'}]
    cases = [
        ('relative_year', '今年录取人数最多的专业是哪个？', [], year, False),
        ('correct_previous', f'今年不是{year}吗？', history, year, False),
        ('explicit_correction', '不对，我问的是2025年，请重新查。', history, 2025, False),
        ('empty_current_year', f'今年不是{year}吗？', history, year, True),
    ]
    failures = 0
    with patch.object(agent, 'tools', [tools.get_major_distribution]):
        executor = agent.create_agent_executor()
    for label, question, past, expected, empty in cases:
        calls = []
        def query(sql, params=None, **kwargs):
            calls.append(params[0] if params else None)
            if empty and params == (year,):
                return []
            return [('测试农业专业', 3)] if params == (year,) else [('测试工程专业', 5)] if params == (2025,) else [('金融学', 8), ('英语', 8)]
        try:
            with patch.object(tools, '_query', side_effect=query):
                answer = await asyncio.wait_for(agent.run_agent(executor, question, past), timeout=90)
            good = bool(calls) and all(y == expected for y in calls) and str(expected) in answer
            if past:
                good = good and bool(re.search(r'有误|错误|抱歉|说得对|确实|纠正|不该|不应|弄错', answer))
            if expected != year:
                good = good and not re.search(r'今年.{0,12}最多', answer)
            good = good and not re.search(r'可能是因为|正在整理|这可能意味着', answer)
            if empty:
                good = good and bool(re.search(r'未|没有|无|缺', answer)) and '金融学' not in answer
            else:
                good = good and ('测试农业专业' if expected == year else '测试工程专业') in answer
            failures += not good
            print(json.dumps({'case': label, 'actual_query_years': calls, 'passed': good, 'answer': answer}, ensure_ascii=False), flush=True)
        except Exception as exc:
            failures += 1
            print(json.dumps({'case': label, 'error_type': type(exc).__name__}, ensure_ascii=False), flush=True)
    return failures


if __name__ == '__main__':
    sys.exit(asyncio.run(main()))
