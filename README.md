# SCAU Archive Insight

华南农业大学 招生学籍档案数字化与可视化分析平台

> 把纸面名册变成会说话的图表，让招生与毕业数据一键洞察。

![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?logo=docker)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.5-6DB33F?logo=springboot)
![Vue](https://img.shields.io/badge/Vue-3-4FC08D?logo=vuedotjs)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-4169E1?logo=postgresql)
![License](https://img.shields.io/badge/License-MIT-yellow)

---

## 快速开始

### 方式一：Docker 一键部署（推荐）

```bash
# 1. 克隆项目
git clone https://github.com/your/scau-archive-insight.git
cd scau-archive-insight

# 2. 配置 AI 密钥和DB_PASS(数据库密码)
cp .env.example .env
# 编辑 .env，填入 GLM_API_KEY=your_api_key
# 填入DB_PASS(必填！！)

# 3. 启动（首次构建需下载 PaddlePaddle ~1.8GB，约 10-20 分钟）
docker compose up -d

# 4. 打开 http://localhost
# 登录：admin / 123456
```

> 首次启动时，数据库会自动初始化表结构、维度数据及演示业务数据（280 条录取、215 条毕业、280 条学籍），无需手动导入。

### 方式二：本地开发

**1. 数据库**
```bash
# 需要 PostgreSQL 15+ + PostGIS
PGPASSWORD=123456 psql -h localhost -U postgres -f scau-archive-insight/sql/init.sql
```

> 数据库初始化 SQL 文件位于 `sql/` 目录，`init.sql` 为主入口，通过 `\ir` 依次加载 `parts/` 下的表结构、维度数据、地理数据和演示业务数据。

**2. 后端**（端口 8080）
```bash
cd scau-archive-insight
./mvnw spring-boot:run
# API 文档: http://localhost:8080/swagger-ui.html
```

**3. 前端**（端口 5173，新终端）
```bash
cd scau_archive-frontend
npm install
npm run dev
# 访问 http://localhost:5173
```

**4. AI 助手**（端口 8765，可选，需要 AI 对话 & 知识库时启动）
```bash
cd scau-archive-insight
HOME="./models" src/main/python/.venv/Scripts/python \
  src/main/python/ai_assistant/main.py
```

> 开发环境前端通过 Vite proxy 将 `/api` 请求转发至 `localhost:8080`，无需额外配置。

---

## 功能一览

| 模块 | 说明 |
|------|------|
| **档案智能采集** | 蜡纸/扫描件/PDF/Excel/CSV 五类输入，自动 OCR 表格识别与字段映射，可选 LLM 智能提取 |
| **OCR 识别监控** | 实时查看处理进度、质量评分与失败原因 |
| **招生数据管理** | 录取名单查看、筛选、编辑 |
| **学籍数据管理** | 在校生学籍信息管理 |
| **毕业数据管理** | 毕业生信息、学位、去向管理 |
| **可视化分析大屏** | 招生趋势、地理热力、学科培养桑基图、AI 招生预测 |
| **智能报告生成** | 一键生成年度招生质量报告（Word + A3 海报），含 AI 智能分析 |
| **AI 助手** | SSE 流式对话，自动检索知识库，支持联网搜索 + 19 种数据库查询 |
| **知识库 (RAG)** | 上传文件或网页链接，自动分块向量化，增强 AI 回答 |
| **元数据管理** | 自定义字段编码与映射规则 |
| **数据脱敏** | 身份证号、姓名等敏感信息一键遮挡，不修改原始数据 |
| **API 文档** | Swagger UI 在线接口文档与调试 |

---

## 技术栈

| 层 | 技术 |
|----|------|
| 后端 | Spring Boot 3.5.13, MyBatis-Plus 3.5.13, PostgreSQL 15 + pgvector + PostGIS, Druid |
| 前端 | Vue 3, Vite 8, Element Plus, ECharts 5, Pinia, Axios |
| Python | FastAPI, LangChain, PaddleOCR 3.5 (PPStructureV3), PaddlePaddle 3.2.2 (CPU), PyMuPDF, OpenCV, Playwright |
| LLM | 智谱 GLM-4-Plus (聊天) / GLM-4V-Plus-0111 (视觉) / embedding-3 (向量), 通义千问 Qwen-VL-Plus |
| AI | SSE 流式对话、RAG 知识库（pgvector 向量检索）、Bing 联网搜索 + Playwright 网页抓取 |
| 预测 | ARIMA + XGBoost 集成预测 |
| 部署 | Docker Compose, Nginx |

---

## 配置

### LLM 配置

| 变量 | 默认值 | 说明 |
|------|--------|------|
| `GLM_API_KEY` | — | API 密钥（必填） |
| `LLM_BASE_URL` | `https://open.bigmodel.cn/api/paas/v4` | API 地址 |
| `LLM_MODEL` | `glm-4v-plus-0111` | 模型名称 |

推荐模型：智谱 GLM-4V-Plus-0111（付费，支持 Base64 图片）、通义千问 Qwen-VL-Plus。

### 数据脱敏

Header 右上角「脱敏/原始」开关 — 后端 Jackson 注解驱动，不修改数据库原始数据。

---

## 项目结构

```
SCAU/
├── docker-compose.yml              # Docker 编排（db + backend + frontend）
├── .env.example                    # 环境变量模板
├── sql/                            # 数据库初始化
│   ├── init.sql                    #   主入口，Docker 首次启动自动执行
│   └── parts/
│       ├── schema.sql              #   表结构（DDL）
│       ├── seed-data.sql           #   维度表 + 系统用户
│       ├── province-geo.sql        #   省份地理边界（PostGIS）
│       └── demo-data.sql           #   演示业务数据
├── scau-archive-insight/           # Spring Boot 后端
│   ├── Dockerfile
│   └── src/main/
│       ├── java/edu/scau/scauarchiveinsight/
│       │   ├── controller/         # 14 个 REST 控制器
│       │   ├── service/            # 业务逻辑 + 文件处理器
│       │   ├── config/             # Security, Swagger, 脱敏等配置
│       │   ├── processor/          # CSV/Excel/PDF/图片/LLM 处理器
│       │   ├── util/               # JWT, 脱敏工具
│       │   ├── dto/vo/mapper/pojo/ # 数据传输、视图、映射、实体
│       │   └── filter/             # JWT 认证过滤器
│       └── python/
│           ├── ai_assistant/       # AI 助手（FastAPI + LangChain + RAG）
│           ├── ppstructure/        # OCR 表格识别 + LLM 提取
│           ├── pdf2image/          # PDF → PNG
│           ├── openCV/             # 图像增强
│           ├── predict/            # ARIMA+XGBoost 预测
│           └── seed/               # 假数据生成
├── scau_archive-frontend/          # Vue 3 前端
│   ├── Dockerfile
│   ├── nginx.conf
│   └── src/
│       ├── views/                  # 页面组件
│       ├── api/                    # API 封装
│       ├── store/                  # Pinia（menu, tab, user）
│       └── layouts/                # 布局组件
├── storage/                        # 文件存储（运行时，.gitignore）
└── models/                         # PaddlePaddle 缓存（运行时，.gitignore）
```

---

## 核心流程

### 文件上传处理

```
上传 → StorageService.saveFiles() → storage/temp/{date}/{type}/
  ├─ CSV/Excel → 字段映射 → 数据校验 → 持久化 → 归档
  ├─ PDF → 转图片 → 逐页 OCR/LLM → 结构化数据 → 持久化 → 归档
  ├─ 图片 → OpenCV 增强 → OCR/LLM → 结构化数据 → 持久化 → 归档
  └─ 失败 → storage/failed/ + .error.json
```

### AI 助手 + 知识库

```
用户提问 → 检索知识库（pgvector 余弦相似度）→ 拼入上下文 → LLM 生成
  ├─ SSE 流式：Python agent.astream_events() → Java SseEmitter → 前端 ReadableStream
  ├─ 工具调用：19 种数据库查询 + Bing 搜索 + Playwright 抓取
  └─ 知识库：上传文件/URL → 解析 → 分块 → 向量化 → 存入 pgvector
```

### 字段匹配

```
fieldName > sourceField > fieldCode
```

OCR 管道：精确 → 去空白 → 包含 → Levenshtein 距离（≤3 字符容差 1，长文本容差 30%）

---

## 数据库

- **事实表**: `admission_fact`, `student_fact`, `graduation_fact`
- **维度表**: `province_dim`, `major_dim`, `college_dim`, `degree_dim`, `destination_dim`, `nation_dim`, `political_dim`, `class_dim`, `archive_file_dim`, `ocr_log_dim`, `quality_score_dim`
- **系统表**: `sys_user`, `metadata_standard`
- **知识库表**: `knowledge_base`, `knowledge_chunks`（pgvector 向量字段）
