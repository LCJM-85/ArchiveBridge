# -*- coding: utf-8 -*-
import os
import re
import json
from datetime import datetime, date, timezone, timedelta
from langchain_openai import ChatOpenAI
from langchain.agents import create_agent
from langchain.agents.middleware import AgentMiddleware
from langchain_core.messages import HumanMessage, AIMessage, SystemMessage, ToolMessage
from tools import tools

SYSTEM_PROMPT = """你是华南农业大学档案管理系统的 AI 数据分析助手，负责帮用户分析招生、学籍、毕业数据。

工作方式：
1. **知识库资料** — 系统会在消息前附上知识库中检索到的相关文档（政策文件、学校介绍、招生简章等），优先引用它们来支撑分析，回答时注明信息来源文档标题
2. **数据查询** — 按需调用工具查数据库拿具体数据
3. **联网搜索** — 需要最新信息、政策、新闻时，调用 web_search 搜索互联网，再用 web_fetch 查看具体网页
4. **综合分析** — 把知识库资料、联网信息和数据库数据结合起来，给用户有深度的回答

说话自然一点，像在跟同事聊天一样，不用太正经。数据就摆数据，分析就讲分析，别绕弯子。

规则：
- 数据库中的人数、姓名、状态、分数必须调用相应数据库工具后回答；知识库、历史对话和联网信息不能代替当前数据库查询。
- 只回答工具实际支持的筛选口径；多个独立统计不能拼成联合条件结果。工具不支持或执行失败时明确说明，不能猜数字。
- 在籍仅指学籍记录 graduated=false；有学籍记录不等于在籍，null 表示未知。没有招生记录也不等于姓名未知。
- 搜索必须说明 total_matches、returned_count 与 truncated；截断时不能声称列出了全部学生。遵守 scope，无学号档案未纳入搜索。
- 截断只影响名单展示，不影响已返回的范围内总数。工具返回 total_matches 时，可以确定 scope 内总数；不要同时说“无法确定总人数”。“姓名含王”不等于“姓王”。
- 不要猜测档案缺失的原因，也不要凭空推断学生通过其他途径毕业。仅陈述已找到和未找到哪些记录。
- 历史实际记录不是预测。get_prediction_data 不计算预测，缺少未来年份记录不代表未来录取人数为0。
- 所有统计仅代表本系统已入库档案，不代表学校现实全量人数；缺失值不等于0。
- 用户问具体年份 → 调工具时带上 year 参数
- “今年/去年/前年”以本次请求日期为准，不能用数据库最新年份替代今年。年份缺失的录取档案不能按上传年份计算。
- 最新用户消息优先于历史回答和知识库。用户纠正年份或质疑上一轮时，先回应纠正，再按新口径重新查询；不得复制上一轮回答或沿用旧统计数字。
- 若上一轮把旧年份说成今年，先承认上一轮口径有误（例如“你说得对，今年是2026年，我刚才误用了2024年的数据”），再给重新查询结果。不要不回应纠正就重说统计。
- 回答中非当前年份不得称为“今年”；指定2025年就说“2025年”，不能说“今年”。
- 用户只纠正时间时，结合上一轮用户问题理解要重新查的指标，不能把自己的旧回答当作事实依据。
- 查询为空时说“系统未找到该年份相关档案，无法判断”，不能改查旧年份冒充今年，也不能断言现实录取人数为0。
- 没有档案只能陈述查询范围和未找到记录，不得猜测未录入、正在整理或其他缺失原因，也不需要建议用户等待。简短回应纠正和查询结果即可。
- 多年比较分别查询各年。未指定年份时遵守各工具说明，全部年份合计不能当成某一年。
- 有数据就说数据，不确定就说"数据里没找到相关信息"
- 数字带单位（人、分、%）
- 数据多就用表格展示，看着清楚
- 事实查询简洁回答；只有用户要求分析时才展开，分析必须区分证据与推测"""


def _build_llm(temperature=0.1):
    return ChatOpenAI(
        model="glm-4-plus",
        openai_api_key=os.getenv("GLM_API_KEY"),
        openai_api_base=os.getenv("LLM_BASE_URL", "https://open.bigmodel.cn/api/paas/v4"),
        temperature=temperature,
    )


def create_agent_executor():
    llm = _build_llm()
    return create_agent(model=llm, tools=tools, system_prompt=SYSTEM_PROMPT,
                        middleware=[QueryYearGuard()])


def current_date():
    return datetime.now(timezone(timedelta(hours=8))).date()


def raw_user_question(question):
    # RAG 文档里的历史年份不能变成用户的查询条件。
    return question.rsplit('【用户问题】', 1)[-1].strip()


def resolve_query_year(question, today):
    """仅约束明确的单年请求；多年份/范围/否定年份留给模型澄清或分别查询。"""
    question = raw_user_question(question)
    if re.search(r'近.{0,3}年|过去.{0,3}年|历年|同比|环比|(?:\d{4}年?|今年|去年|前年)\s*(?:至|到|—|~|-)\s*(?:\d{4}|今年|去年|前年)', question):
        return None
    years = {int(y) for y in re.findall(r'(?<!\d)((?:19|20)\d{2})(?!\d)', question)}
    relative = {today.year + offset for word, offset in [('今年', 0), ('去年', -1), ('前年', -2)] if word in question}
    # “今年不是2026吗”是对当前年份的追问；“不是2024年”则不能强制查2024。
    if not relative and re.search(r'(?:不是|不要|不查|非)\s*(?:19|20)\d{2}', question):
        return None
    years |= relative
    return next(iter(years)) if len(years) == 1 else None


def is_year_correction(question, year):
    return year is not None and bool(re.search(r'不对|不是|错误|有误|纠正|重新查|问的是', raw_user_question(question)))


def format_query_answer(question, answer, today):
    year = resolve_query_year(question, today)
    if is_year_correction(question, year):
        return f'按你的纠正，本轮以{year}年为准。\n\n{answer.strip()}'
    return answer


def build_query_messages(question, history):
    today = current_date()
    year = resolve_query_year(question, today)
    context = (f'[请求日期] {today.isoformat()}（北京时间）；今年={today.year}，去年={today.year - 1}。'
               '按最新用户问题回答；纠正上一轮时必须重新查询，不得复用历史回答的数字。')
    if year is not None:
        context += f'本轮明确的单年查询目标为{year}年；查询为空也不得切换其他年份。'
    correction = is_year_correction(question, year)
    messages = [SystemMessage(content=context)]
    recent = history[-20:]
    disputed_index = next((i for i in range(len(recent) - 1, -1, -1) if recent[i].get('role') == 'assistant'), None)
    for i, msg in enumerate(recent):
        if msg.get('role') == 'user':
            messages.append(HumanMessage(content=msg.get('content', '')))
        elif msg.get('role') == 'assistant':
            # 只隔离本轮明确纠正的上一条模型回答；保留之前用户问的指标。
            content = '(上一轮回答的年份口径已被用户质疑，需重新查询。)' if correction and i == disputed_index else msg.get('content', '')
            messages.append(AIMessage(content=content))
    if correction:
        messages.append(SystemMessage(content=f'用户正在纠正上一轮年份。回应用户纠正，再重新查询上一轮用户所问指标的{year}年数据。结果明确写“{year}年”，不要沿用“今年”；不得推测档案为什么缺失。'))
    messages.append(HumanMessage(content=question))
    return messages


class QueryYearGuard(AgentMiddleware):
    """拦截单年错查，并给工具结果附真实查询年份；状态仅来自本次请求。"""
    def _prepare(self, request):
        fields = getattr(getattr(request.tool, 'args_schema', None), 'model_fields', {})
        if 'year' not in fields:
            return request, None
        messages = request.state.get('messages', [])
        question = next((m.content for m in reversed(messages) if isinstance(m, HumanMessage)), '')
        today = current_date()
        for message in messages:
            if isinstance(message, SystemMessage):
                match = re.search(r'\[请求日期\] (\d{4}-\d{2}-\d{2})', message.content)
                if match:
                    today = date.fromisoformat(match.group(1))
        expected = resolve_query_year(question, today)
        args = dict(request.tool_call['args'])
        supplied = args.get('year')
        if expected is not None and supplied is not None and str(supplied) != str(expected):
            return request, ToolMessage(content=f'查询被拦截：本轮用户要求{expected}年，不是{supplied}年。请重新以year={expected}调用此工具；不得使用旧年份结果。',
                tool_call_id=request.tool_call['id'], status='error')
        if expected is not None and supplied is None:
            args['year'] = expected
            request = request.override(tool_call={**request.tool_call, 'args': args})
        return request, None

    def _annotate(self, request, result):
        if isinstance(result, ToolMessage) and result.status != 'error' and request.tool_call['args'].get('year') is not None:
            try:
                data = json.loads(result.content)
            except (ValueError, TypeError):
                return result
            return result.model_copy(update={'content': json.dumps({
                'queried_year': request.tool_call['args']['year'], 'data': data,
                'scope': '仅代表该年份系统已入库档案，不能用其他年份替代。',
                'record_note': ('该年份查询未找到记录，无法判断所问结果；不能证明现实人数为0。' if data == [] else '请依据返回的统计直接回答。')
                    + '本工具不提供档案完整性或缺失原因信息，不能推断未录入、整理中、暂无录取等原因。'
            }, ensure_ascii=False)})
        return result

    def wrap_tool_call(self, request, handler):
        request, error = self._prepare(request)
        return error if error is not None else self._annotate(request, handler(request))

    async def awrap_tool_call(self, request, handler):
        request, error = self._prepare(request)
        return error if error is not None else self._annotate(request, await handler(request))


async def run_agent(agent, question: str, history: list):
    messages = build_query_messages(question, history)

    result = await agent.ainvoke({"messages": messages})

    for m in reversed(result.get("messages", [])):
        if isinstance(m, AIMessage) and m.content and not m.tool_calls:
            return format_query_answer(question, m.content, date.fromisoformat(messages[0].content[7:17]))
    return "抱歉，无法获取回答"


async def run_agent_stream(agent, question: str, history: list):
    """流式运行 agent，逐步 yield 状态事件和最终结果。"""
    messages = build_query_messages(question, history)

    yield {"type": "status", "content": "正在分析问题..."}

    final_answer = None

    async for state in agent.astream({"messages": messages}):
        # state 是 {node_name: {"messages": [...]}} 结构
        all_msgs = []
        if isinstance(state, dict):
            for val in state.values():
                if isinstance(val, dict) and "messages" in val:
                    all_msgs = val["messages"]
                    break
        if not all_msgs:
            continue

        last = all_msgs[-1]

        # 工具调用阶段 → 发状态提示
        if hasattr(last, "tool_calls") and last.tool_calls:
            for tc in last.tool_calls:
                name = (tc.get("name", "数据") if isinstance(tc, dict)
                        else getattr(tc, "name", "数据"))
                yield {"type": "status", "content": f"正在查询{name}..."}

        # 记录最终 AI 回复
        if isinstance(last, AIMessage) and last.content and not last.tool_calls:
            final_answer = last.content

    if final_answer:
        yield {"type": "token", "content": format_query_answer(question, final_answer, date.fromisoformat(messages[0].content[7:17]))}
    else:
        yield {"type": "token", "content": "抱歉，无法获取回答"}


async def run_report_chain(report_chain, report_data: dict) -> str:
    # 精简数据，只保留关键指标
    summary = {
        "year": report_data.get("year"),
        "overview": report_data.get("overview"),
        "score": report_data.get("score"),
        "destination": report_data.get("destination"),
        "majorCount": len(report_data.get("majorDistribution", [])),
        "provinceCount": len(report_data.get("provinceDistribution", [])),
        "topMajor": (report_data.get("majorDistribution") or [{}])[0:3],
        "topProvince": (report_data.get("provinceDistribution") or [{}])[0:3],
    }
    import json
    data_str = json.dumps(summary, ensure_ascii=False, default=str)
    result = await report_chain.ainvoke({"report_data": data_str})
    return result.content if hasattr(result, "content") else str(result)


def create_report_chain():
    llm = _build_llm(temperature=0.3)

    from langchain_core.prompts import PromptTemplate

    prompt = PromptTemplate.from_template(
        """你是一个数据分析专家。以下是一份招生报告数据，请根据数据写一段分析结论。

数据字段说明：
- overview: 招生总览。其中"总录取人数 total"和"覆盖省份数"**包含硕士/博士全部层次**；只有其中的"平均分 avg_score"是仅统计学士/本科生群体
- score: 录取分数统计（仅统计学士/本科生群体，不含硕士/博士）
- destination: 毕业去向分布（如就业、升学等），与招生无关
- topMajor: 录取人数最多的几个专业
- topProvince: 生源最多的几个省份

数据：
{report_data}

请按以下结构输出（使用中文）：
1. **总体概况**：一句话总结今年的招生情况
2. **关键发现**：列出 2-3 个突出的数据点（增长/下降/异常）
3. **趋势分析**：基于数据的简短判断

注意：destination 是毕业去向数据，不属于招生录取信息。
"总录取人数"和"覆盖省份数"是全部层次（含硕士/博士），描述时直接说"录取 X 人"，不要加"本科生"限定词。
只有涉及分数（平均分/最高分/最低分）时才用"本科生"限定。
不要编造数据，只基于给出的数据说话。"""
    )
    return prompt | llm
