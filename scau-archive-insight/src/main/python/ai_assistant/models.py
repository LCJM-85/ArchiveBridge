from pydantic import BaseModel
from typing import Optional, Any


class ChatRequest(BaseModel):
    question: str
    history: list = []


class ChatResponse(BaseModel):
    answer: str


class ReportRequest(BaseModel):
    report_data: dict


class ReportResponse(BaseModel):
    analysis: str


class KbProcessRequest(BaseModel):
    file_path: str
    title: str
    file_type: str
    store_path: str = ""


class KbProcessUrlRequest(BaseModel):
    url: str
    title: str = ""


class KbSearchRequest(BaseModel):
    query: str
    top_k: int = 5


class KbSearchResponse(BaseModel):
    results: list[Any]
