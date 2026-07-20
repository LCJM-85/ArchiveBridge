# -*- coding: utf-8 -*-
import os
from langchain_openai import ChatOpenAI
from langchain.agents import create_agent
from langchain_core.messages import HumanMessage, AIMessage
from tools import tools

SYSTEM_PROMPT = """你是华南农业大学档案管理系统的 AI 数据分析助手，负责帮用户分析招生、学籍、毕业数据。

工作方式：
1. **知识库资料** — 系统会在消息前附上知识库中检索到的相关文档（政策文件、学校介绍、招生简章等），优先引用它们来支撑分析，回答时注明信息来源文档标题
2. **数据查询** — 按需调用工具查数据库拿具体数据
3. **联网搜索** — 需要最新信息、政策、新闻时，调用 web_search 搜索互联网，再用 web_fetch 查看具体网页
4. **综合分析** — 把知识库资料、联网信息和数据库数据结合起来，给用户有深度的回答

说话自然一点，像在跟同事聊天一样，不用太正经。数据就摆数据，分析就讲分析，别绕弯子。

规则：
- 用户问具体年份 → 调工具时带上 year 参数
- 不传 year 返回的是全部年份合计，别当成某一年数据
- 有数据就说数据，不确定就说"数据里没找到相关信息"
- 数字带单位（人、分、%）
- 数据多就用表格展示，看着清楚
- 多展开讲讲数据背后的意思，别只报数字"""


def _build_llm(temperature=0.7):
    return ChatOpenAI(
        model="glm-4-plus",
        openai_api_key=os.getenv("GLM_API_KEY"),
        openai_api_base=os.getenv("LLM_BASE_URL", "https://open.bigmodel.cn/api/paas/v4"),
        temperature=temperature,
    )


def create_agent_executor():
    llm = _build_llm()
    return create_agent(model=llm, tools=tools, system_prompt=SYSTEM_PROMPT)


async def run_agent(agent, question: str, history: list):
    messages = []
    for msg in history[-20:]:
        if msg.get("role") == "user":
            messages.append(HumanMessage(content=msg.get("content", "")))
        elif msg.get("role") == "assistant":
            messages.append(AIMessage(content=msg.get("content", "")))
    messages.append(("human", question))

    result = await agent.ainvoke({"messages": messages})

    for m in reversed(result.get("messages", [])):
        if isinstance(m, AIMessage) and m.content:
            return m.content
    return "抱歉，无法获取回答"


async def run_agent_stream(agent, question: str, history: list):
    """流式运行 agent，逐步 yield 状态事件和最终结果。"""
    messages = []
    for msg in history[-20:]:
        if msg.get("role") == "user":
            messages.append(HumanMessage(content=msg.get("content", "")))
        elif msg.get("role") == "assistant":
            messages.append(AIMessage(content=msg.get("content", "")))
    messages.append(("human", question))

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
        if isinstance(last, AIMessage) and last.content:
            final_answer = last.content

    if final_answer:
        yield {"type": "token", "content": final_answer}
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
- overview: 招生总览（总录取人数、平均分、覆盖省份数等）
- score: 录取分数统计
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
不要编造数据，只基于给出的数据说话。"""
    )
    return prompt | llm
