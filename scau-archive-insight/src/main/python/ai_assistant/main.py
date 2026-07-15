# -*- coding: utf-8 -*-
import os
import sys
import uvicorn
from fastapi import FastAPI
from contextlib import asynccontextmanager
from models import ChatRequest, ChatResponse, ReportRequest, ReportResponse
from agent import create_agent_executor, create_report_chain, run_agent, run_report_chain

agent = None
report_chain = None


@asynccontextmanager
async def lifespan(app: FastAPI):
    global agent, report_chain
    agent = create_agent_executor()
    report_chain = create_report_chain()
    yield


app = FastAPI(title="AI 助手服务", lifespan=lifespan)


@app.post("/chat", response_model=ChatResponse)
async def chat(req: ChatRequest):
    answer = await run_agent(agent, req.question, req.history)
    return ChatResponse(answer=answer)


@app.post("/analyze-report", response_model=ReportResponse)
async def analyze_report(req: ReportRequest):
    analysis = await run_report_chain(report_chain, req.report_data)
    return ReportResponse(analysis=analysis)


if __name__ == "__main__":
    uvicorn.run(app, host="127.0.0.1", port=8765)
