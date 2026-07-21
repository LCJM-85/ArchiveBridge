  -- 启用 pgvector 扩展（用于向量相似度搜索）
  CREATE EXTENSION IF NOT EXISTS vector;

  -- 知识库文档表
  CREATE TABLE IF NOT EXISTS public.knowledge_base (
      id           SERIAL PRIMARY KEY,
      title        VARCHAR(255) NOT NULL,
      file_type    VARCHAR(20),
      file_path    TEXT,
      source       VARCHAR(50),           -- 'web_link' 或其他来源标识
      url          TEXT,
      chunk_count  INTEGER DEFAULT 0,
      status       VARCHAR(20) DEFAULT 'parsing',  -- parsing → ready / failed
      error_msg    TEXT,
      create_time  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
  );

  -- 知识库文本块表（含向量）
  CREATE TABLE IF NOT EXISTS public.knowledge_chunks (
      id           SERIAL PRIMARY KEY,
      kb_id        INTEGER NOT NULL REFERENCES public.knowledge_base(id) ON DELETE CASCADE,
      chunk_index  INTEGER NOT NULL,
      content      TEXT NOT NULL,
      metadata     JSONB DEFAULT '{}',
      embedding    vector(1024)            -- 智谱 embedding-3，1024 维
  );

  -- 索引
  CREATE INDEX IF NOT EXISTS idx_kb_chunks_kb_id ON public.knowledge_chunks (kb_id);
  CREATE INDEX IF NOT EXISTS idx_kb_status ON public.knowledge_base (status);