import os
import time
import requests


EMBEDDING_URL = "https://open.bigmodel.cn/api/paas/v4/embeddings"
EMBEDDING_MODEL = "embedding-3"
EMBEDDING_DIMENSIONS = 1024
MAX_RETRIES = 3


def _call_embedding(payload, timeout=60):
    """带重试的 embedding API 调用，遇到 429 自动等待"""
    api_key = os.getenv("GLM_API_KEY", "")
    if not api_key:
        raise ValueError("GLM_API_KEY 未设置")

    headers = {
        "Authorization": f"Bearer {api_key}",
        "Content-Type": "application/json",
    }

    for attempt in range(MAX_RETRIES):
        resp = requests.post(
            EMBEDDING_URL, headers=headers, json=payload, timeout=timeout
        )
        if resp.status_code == 429:
            wait = (attempt + 1) * 3
            print(f"[embedding] 触发限流，{wait}秒后重试（第{attempt+1}次）")
            time.sleep(wait)
            continue
        resp.raise_for_status()
        data = resp.json()
        return data

    raise Exception(f"embedding API 请求失败，已重试{MAX_RETRIES}次")


def get_embedding(text: str) -> list[float]:
    data = _call_embedding({
        "model": EMBEDDING_MODEL,
        "input": text,
        "dimensions": EMBEDDING_DIMENSIONS,
    }, timeout=30)
    return data["data"][0]["embedding"]


def get_embeddings_batch(texts: list[str]) -> list[list[float]]:
    data = _call_embedding({
        "model": EMBEDDING_MODEL,
        "input": texts,
        "dimensions": EMBEDDING_DIMENSIONS,
    }, timeout=60)
    results = sorted(data["data"], key=lambda x: x["index"])
    return [r["embedding"] for r in results]
