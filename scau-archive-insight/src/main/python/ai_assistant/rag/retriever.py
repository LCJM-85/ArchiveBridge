import json
import psycopg2.extras
from .embedding import get_embedding
from db import get_conn, put_conn


def search_knowledge(query: str, top_k: int = 5) -> list[dict]:
    """向量搜索知识库，返回 top_k 个匹配的文本块"""
    query_vec = get_embedding(query)

    conn = get_conn()
    try:
        with conn.cursor(cursor_factory=psycopg2.extras.RealDictCursor) as cur:
            cur.execute(
                """
                SELECT
                    c.id, c.kb_id, c.chunk_index, c.content, c.metadata,
                    kb.title AS kb_title,
                    1 - (c.embedding <=> %s::vector) AS similarity
                FROM knowledge_chunks c
                JOIN knowledge_base kb ON c.kb_id = kb.id
                WHERE kb.status = 'ready'
                ORDER BY c.embedding <=> %s::vector
                LIMIT %s
                """,
                (query_vec, query_vec, top_k),
            )
            rows = cur.fetchall()
            results = []
            for row in rows:
                results.append({
                    "id": row["id"],
                    "kb_id": row["kb_id"],
                    "chunk_index": row["chunk_index"],
                    "content": row["content"],
                    "metadata": row["metadata"] if isinstance(row["metadata"], dict) else json.loads(row["metadata"] or "{}"),
                    "source_title": row["kb_title"],
                    "similarity": round(float(row["similarity"]), 4),
                })
            return results
    finally:
        put_conn(conn)


def search_knowledge_by_kb_id(kb_id: int) -> list[dict]:
    """按知识库 ID 检索所有文本块（用于展示/删除）"""
    conn = get_conn()
    try:
        with conn.cursor(cursor_factory=psycopg2.extras.RealDictCursor) as cur:
            cur.execute(
                "SELECT id, kb_id, chunk_index, content, metadata FROM knowledge_chunks WHERE kb_id = %s ORDER BY chunk_index",
                (kb_id,),
            )
            return [dict(r) for r in cur.fetchall()]
    finally:
        put_conn(conn)
