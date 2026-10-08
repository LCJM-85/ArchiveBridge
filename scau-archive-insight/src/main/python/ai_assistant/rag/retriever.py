import json
import psycopg2.extras
from .embedding import get_embedding
from db import get_conn, put_conn


def merge_context_chunks(primary, neighbors, max_chars=12000):
    """原始命中优先，补充完整相邻段落；去重且不截断条款。"""
    results, seen, size = [], set(), 0
    for row in primary + neighbors:
        if row['id'] in seen:
            continue
        length = len(row.get('content', ''))
        if size + length > max_chars:
            continue
        results.append(row)
        seen.add(row['id'])
        size += length
    return results


def search_knowledge_context(query: str, top_k: int = 3) -> list[dict]:
    """top_k向量命中加前后各一段，补全分块边界上的限制条件。"""
    primary = search_knowledge(query, top_k)
    if not primary:
        return []
    conn = get_conn()
    try:
        neighbors = []
        with conn.cursor(cursor_factory=psycopg2.extras.RealDictCursor) as cur:
            for row in primary:
                cur.execute('''
                    SELECT c.id, c.kb_id, c.chunk_index, c.content, c.metadata,
                           kb.title AS source_title
                    FROM knowledge_chunks c JOIN knowledge_base kb ON c.kb_id = kb.id
                    WHERE kb.status = 'ready' AND c.kb_id = %s
                      AND c.chunk_index BETWEEN %s AND %s
                    ORDER BY c.chunk_index
                ''', (row['kb_id'], max(0, row['chunk_index'] - 1), row['chunk_index'] + 1))
                neighbors.extend(dict(r) for r in cur.fetchall())
        return merge_context_chunks(primary, neighbors)
    finally:
        try:
            conn.rollback()
        finally:
            put_conn(conn)


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
