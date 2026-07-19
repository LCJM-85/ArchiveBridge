# RAG 知识库设计文档

## 概述

在现有 AI 助手基础上，增加 RAG 知识库功能。用户可上传招生资料（PDF/Word/Excel/TXT/网页链接），系统自动解析、分块、向量化后存入 pgvector，AI 助手在回答问题时自动检索相关知识增强回答能力。

## 整体架构

```
前端上传 → Java Controller → 临时存储 → 调 Python RAG 服务 → 解析/分块/向量化
                                                                ↓
                                                           pgvector
                                                                ↓
用户提问 → Python AI 助手 → 向量搜索 pgvector → 拼 context → LLM 回答
```

- **扩展现有 Python FastAPI 服务（8765 端口）**，不新增独立服务
- 上传文件不长期保留，解析完成后删除临时文件

## 数据库设计

### knowledge_base — 文档记录

```sql
CREATE TABLE knowledge_base (
    id SERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    file_type VARCHAR(20) NOT NULL,        -- pdf/docx/xlsx/txt/html
    source VARCHAR(50) DEFAULT 'upload',   -- upload / web_link
    url TEXT,                               -- 网页来源链接
    chunk_count INTEGER DEFAULT 0,
    status VARCHAR(20) DEFAULT 'parsing',  -- parsing / ready / failed
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### knowledge_chunks — 文本块 + 向量

```sql
CREATE TABLE knowledge_chunks (
    id SERIAL PRIMARY KEY,
    kb_id INTEGER NOT NULL REFERENCES knowledge_base(id) ON DELETE CASCADE,
    chunk_index INTEGER NOT NULL,
    content TEXT NOT NULL,
    metadata JSONB DEFAULT '{}',            -- {page: 3, source_title: "..."}
    embedding VECTOR(1024)
);

CREATE INDEX idx_chunks_kb_id ON knowledge_chunks(kb_id);
CREATE INDEX idx_chunks_embedding ON knowledge_chunks
    USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100);
```

## 文档处理流程

### 文件解析
| 类型 | Python 库 | 方式 |
|------|-----------|------|
| PDF | PyMuPDF（已有） | 逐页提取文本 |
| DOCX | python-docx | 提取段落 |
| XLSX | openpyxl | 每行转文本 |
| TXT | 内置 | 直接读取 |
| 网页/推文 | requests + html2text | 抓取 → 转 markdown |

### 文本分块
使用 LangChain `RecursiveCharacterTextSplitter`：
- chunk_size=500, chunk_overlap=50
- 分隔符优先级：`\n\n` → `\n` → `。` → `！` → `？` → `，`

### 向量化
调智谱 `text_embedding-v3` API，输出 1024 维向量存入 pgvector。

## RAG 查询流程

```
用户问题
  ├── 向量化 → pgvector 相似搜索 (cosine distance, top-5)
  └── 已有 Agent Tools → 数据库查询（录取人数/分数等）
      ↓
两路结果合并 → LLM 综合回答
```

AI 助手 system prompt 新增规则：
- 涉及招生政策、专业介绍等文本资料优先检索知识库
- 回答时标注信息来源文档标题

## API 接口

| 方法 | 路径 | 功能 |
|------|------|------|
| POST | `/api/knowledge/upload` | 上传文件 |
| POST | `/api/knowledge/url` | 添加网页链接 |
| GET | `/api/knowledge/list` | 文档列表 |
| DELETE | `/api/knowledge/{id}` | 删除文档 |

## 前端页面

侧边栏新增「知识库」菜单项，页面包含：
1. 拖拽上传区域（支持 pdf/docx/xlsx/txt）
2. 网页链接粘贴输入框
3. 文档列表（显示标题、类型、状态、操作按钮）

## Python 服务扩展

在 `ai_assistant/` 目录下新增模块：
- `rag/document_loader.py` — 文档解析器
- `rag/text_splitter.py` — 文本分块
- `rag/embedding.py` — 向量化
- `rag/retriever.py` — pgvector 检索

FastAPI 新增端点：
| 方法 | 路径 | 功能 |
|------|------|------|
| POST | `/kb/process` | 处理上传文件 |
| POST | `/kb/process-url` | 处理网页链接 |
| GET | `/kb/search` | 搜索知识库 |

Java 端新增：
- `KnowledgeController` — 上传/列表/删除
- `KnowledgeService` — 调用 Python RAG 服务
