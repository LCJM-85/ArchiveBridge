# -*- coding: utf-8 -*-
import os
import sys
import json
import uvicorn
import traceback
from fastapi import FastAPI, HTTPException
from fastapi.responses import StreamingResponse
from contextlib import asynccontextmanager
from models import (
    ChatRequest, ChatResponse, ReportRequest, ReportResponse,
    KbProcessRequest, KbProcessUrlRequest, KbSearchRequest,
)
from agent import create_agent_executor, create_report_chain, run_agent, run_report_chain, run_agent_stream
from rag.document_loader import load_document, load_document_async
from rag.text_splitter import split_text
from rag.embedding import get_embeddings_batch
from rag.retriever import search_knowledge

agent = None
report_chain = None


@asynccontextmanager
async def lifespan(app: FastAPI):
    global agent, report_chain
    agent = create_agent_executor()
    report_chain = create_report_chain()
    yield


app = FastAPI(title="AI 助手服务", lifespan=lifespan)


# ─── 聊天 ───


def _build_knowledge_context(question: str) -> str:
    """搜索知识库，返回知识上下文文本"""
    try:
        results = search_knowledge(question, top_k=3)
        if not results:
            return ""
        context_parts = []
        for r in results:
            source = r.get("source_title", "未知来源")
            content = r.get("content", "")
            if content:
                context_parts.append(f"[{source}]: {content}")
        return "\n\n".join(context_parts)
    except Exception as e:
        print(f"[RAG] 知识库检索失败: {e}", file=sys.stderr)
        return ""


@app.post("/chat/stream")
async def chat_stream(req: ChatRequest):
    async def event_stream():
        # 检索知识库，拼接到问题前面
        kb_context = _build_knowledge_context(req.question)
        enriched_question = req.question
        if kb_context:
            yield f"data: {json.dumps({'type': 'status', 'content': '已检索知识库资料'}, ensure_ascii=False)}\n\n"
            enriched_question = f"【知识库资料】\n{kb_context}\n\n【用户问题】\n{req.question}"

        async for event in run_agent_stream(agent, enriched_question, req.history):
            yield f"data: {json.dumps(event, ensure_ascii=False)}\n\n"
        yield "data: {\"type\": \"done\"}\n\n"

    return StreamingResponse(event_stream(), media_type="text/event-stream")


@app.post("/chat", response_model=ChatResponse)
async def chat(req: ChatRequest):
    # 检索知识库，拼接到问题前面
    kb_context = _build_knowledge_context(req.question)
    enriched_question = req.question
    if kb_context:
        enriched_question = f"【知识库资料】\n{kb_context}\n\n【用户问题】\n{req.question}"

    answer = await run_agent(agent, enriched_question, req.history)
    return ChatResponse(answer=answer)


@app.post("/analyze-report", response_model=ReportResponse)
async def analyze_report(req: ReportRequest):
    analysis = await run_report_chain(report_chain, req.report_data)
    return ReportResponse(analysis=analysis)


# ─── 知识库 ───


def _save_embedding(kb_id: int, chunks: list[str], metadata_list: list[dict] = None):
    """分块写入 knowledge_chunks，并批量生成向量"""
    from db import get_conn, put_conn

    conn = get_conn()
    try:
        with conn.cursor() as cur:
            for i, chunk in enumerate(chunks):
                meta = json.dumps(metadata_list[i] if metadata_list and i < len(metadata_list) else {}, ensure_ascii=False)
                cur.execute(
                    "INSERT INTO knowledge_chunks (kb_id, chunk_index, content, metadata) VALUES (%s, %s, %s, %s::jsonb)",
                    (kb_id, i, chunk, meta),
                )
        conn.commit()
    finally:
        put_conn(conn)

    # 批量生成向量
    try:
        embeddings = get_embeddings_batch(chunks)
        conn = get_conn()
        try:
            with conn.cursor() as cur:
                for i, emb in enumerate(embeddings):
                    cur.execute(
                        "UPDATE knowledge_chunks SET embedding = %s::vector WHERE kb_id = %s AND chunk_index = %s",
                        (json.dumps(emb), kb_id, i),
                    )
            conn.commit()
        finally:
            put_conn(conn)
    except Exception as e:
        print(f"[RAG] 向量生成失败: {e}", file=sys.stderr)
        raise


def _update_kb_status(kb_id: int, status: str, chunk_count: int = 0, error_msg: str = ""):
    from db import get_conn, put_conn
    conn = get_conn()
    try:
        with conn.cursor() as cur:
            if status == "ready":
                cur.execute(
                    "UPDATE knowledge_base SET status = %s, chunk_count = %s, error_msg = NULL WHERE id = %s",
                    (status, chunk_count, kb_id),
                )
            else:
                cur.execute(
                    "UPDATE knowledge_base SET status = %s, error_msg = %s WHERE id = %s",
                    (status, error_msg[:500], kb_id),
                )
        conn.commit()
    finally:
        put_conn(conn)


@app.post("/kb/process")
async def kb_process(req: KbProcessRequest):
    """处理上传的文件：解析 → 分块 → 向量化 → 存储"""
    if not os.path.exists(req.file_path):
        raise HTTPException(400, f"文件不存在: {req.file_path}")

    # 1. 创建知识库记录（保存原文件路径用于删除时清理）
    from db import get_conn, put_conn
    conn = get_conn()
    try:
        with conn.cursor() as cur:
            store_path = req.store_path or req.file_path
            cur.execute(
                "INSERT INTO knowledge_base (title, file_type, file_path, status) VALUES (%s, %s, %s, 'parsing') RETURNING id",
                (req.title, req.file_type, store_path),
            )
            kb_id = cur.fetchone()[0]
        conn.commit()
    finally:
        put_conn(conn)

    try:
        # 2. 解析文档
        pages = load_document(req.file_path, req.file_type)
        full_text = "\n".join(pages)

        # 3. 分块
        chunks = split_text(full_text)
        if not chunks:
            raise ValueError("文档解析后为空")

        metadata_list = [{"source_title": req.title, "chunk": i} for i in range(len(chunks))]

        # 4. 保存文本块和向量
        _save_embedding(kb_id, chunks, metadata_list)

        # 5. 更新状态
        _update_kb_status(kb_id, "ready", len(chunks))

        return {
            "kb_id": kb_id,
            "title": req.title,
            "chunk_count": len(chunks),
            "status": "ready",
        }
    except Exception as e:
        err_msg = str(e)
        _update_kb_status(kb_id, "failed", error_msg=err_msg)
        traceback.print_exc()
        raise HTTPException(500, f"处理失败: {err_msg}")


@app.post("/kb/process-url")
async def kb_process_url(req: KbProcessUrlRequest):
    """处理网页链接：抓取 → 分块 → 向量化 → 存储"""
    title = req.title or req.url

    from db import get_conn, put_conn
    conn = get_conn()
    try:
        with conn.cursor() as cur:
            cur.execute(
                "INSERT INTO knowledge_base (title, file_type, source, url, status) VALUES (%s, 'html', 'web_link', %s, 'parsing') RETURNING id",
                (title, req.url),
            )
            kb_id = cur.fetchone()[0]
        conn.commit()
    finally:
        put_conn(conn)

    try:
        # 1. 抓取网页（Playwright 渲染）
        pages = await load_document_async(req.url, "html")
        full_text = "\n".join(pages)

        # 2. 分块
        chunks = split_text(full_text)
        if not chunks:
            raise ValueError("网页内容为空")

        metadata_list = [{"source_title": title, "url": req.url, "chunk": i} for i in range(len(chunks))]

        # 3. 保存
        _save_embedding(kb_id, chunks, metadata_list)
        _update_kb_status(kb_id, "ready", len(chunks))

        return {
            "kb_id": kb_id,
            "title": title,
            "url": req.url,
            "chunk_count": len(chunks),
            "status": "ready",
        }
    except Exception as e:
        err_msg = str(e)
        _update_kb_status(kb_id, "failed", error_msg=err_msg)
        traceback.print_exc()
        raise HTTPException(500, f"处理失败: {err_msg}")


@app.post("/kb/search")
async def kb_search(req: KbSearchRequest):
    """搜索知识库"""
    try:
        results = search_knowledge(req.query, req.top_k)
        return {"results": results}
    except Exception as e:
        traceback.print_exc()
        raise HTTPException(500, f"搜索失败: {str(e)}")


if __name__ == "__main__":
    uvicorn.run(app, host="127.0.0.1", port=8765)
