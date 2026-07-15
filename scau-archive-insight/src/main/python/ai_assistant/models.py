from pydantic import BaseModel
from typing import Optional


class ChatRequest(BaseModel):
    question: str
    history: list = []


class ChatResponse(BaseModel):
    answer: str


class ReportRequest(BaseModel):
    report_data: dict


class ReportResponse(BaseModel):
    analysis: str
