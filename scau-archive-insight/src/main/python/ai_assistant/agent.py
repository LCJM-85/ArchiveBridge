# -*- coding: utf-8 -*-
import os
from langchain_openai import ChatOpenAI
from langchain.agents import create_agent
from langchain_core.messages import HumanMessage, AIMessage
from tools import tools

SYSTEM_PROMPT = """你是一个华南农业大学档案管理系统的 AI 数据分析助手。
你可以查询招生、学籍、毕业等数据，并用中文回答用户的问题。

重要规则：
- 用户问特定年份时，调用工具时务必传入 year 参数
- 不指定年份的工具返回的是全部年份合计，不能当作某一年数据使用
- 基于数据回答，不要猜测，不确定时说"数据中未找到相关信息"
- 涉及数字时注明单位（人、分、%等）
- 数据较多时用表格展示
- 对数据做详细一点的分析"""


def _build_llm(temperature=0.7):
    return ChatOpenAI(
        model="glm-4-flash",
        openai_api_key=os.getenv("LLM_API_KEY"),
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
