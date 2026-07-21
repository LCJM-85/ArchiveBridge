# SCAU Archive Insight

华南农业大学 招生学籍档案数字化与可视化分析平台

把纸面名册变成会说话的图表，让招生与毕业数据一键洞察。

## 功能模块

| 模块 | 说明 |
|------|------|
| **档案智能采集** | 支持蜡纸/扫描PDF/OCR/Excel/CSV五类输入，自动表格识别与字段映射，可选 LLM 智能提取 |
| **OCR 识别监控** | 实时查看处理进度、质量评分、处理日志、失败原因 |
| **招生数据管理** | 录取名单查看、筛选、编辑 |
| **学籍数据管理** | 在校生学籍信息管理 |
| **毕业数据管理** | 毕业生信息、学位、去向管理 |
| **可视化分析大屏** | 招生趋势分析、地理热力分布、学科培养桑基图、AI招生预测 |
| **智能报告生成** | 一键生成年度招生质量报告（Word + A3 海报打印），含 AI 智能分析 |
| **AI 助手** | 流式 SSE 对话，自动检索知识库，支持联网搜索和数据库查询 |
| **知识库 (RAG)** | 上传文件（PDF/DOCX/XLSX/TXT）或网页链接，自动分块向量化，AI 助手中增强回答 |
| **元数据管理** | 自定义字段编码、字段名、来源字段映射规则 |
| **数据脱敏** | 身份证号、姓名等敏感信息一键脱敏（后端注解驱动 + 前端开关） |
| **API 文档** | Swagger UI 在线接口文档与测试 |

## 技术栈

| 层 | 技术 |
|----|------|
| 后端 | Spring Boot 3.5.13, MyBatis-Plus 3.5.13, PostgreSQL 15 + pgvector, PostGIS, Druid |
| 前端 | Vue 3, Vite 8, Element Plus, ECharts 5, Pinia, Axios |
| Python | FastAPI, LangChain, PaddleOCR 3.5 (PPStructureV3), PaddlePaddle 3.2.2 (CPU), PyMuPDF, OpenCV, Playwright |
| LLM | 智谱 GLM-4-Plus (聊天) / GLM-4V-Plus-0111 (视觉) / embedding-3 (向量), 通义千问 Qwen-VL-Plus |
| AI | SSE 流式对话、RAG 知识库（向量检索）、联网搜索（Bing scraping + Playwright） |
| 预测 | ARIMA + XGBoost 集成预测 |
| 部署 | Docker, Docker Compose, Nginx |

## 快速启动

### 本地开发

**1. 数据库**
```bash
# 需要 PostgreSQL 15+ 和 PostGIS 扩展
PGPASSWORD=123456 psql -h localhost -U postgres -d scau_archive < backup.sql
```

**2. 后端**（端口 8080）
```bash
cd scau-archive-insight
./mvnw spring-boot:run
# API 文档: http://localhost:8080/swagger-ui.html
```

**3. Python AI 助手**（端口 8765，AI 对话 + 知识库需要）
```bash
# 需先配置 Python 虚拟环境，参考 src/main/python/requirements.txt
cd scau-archive-insight
HOME="./models" src/main/python/.venv/Scripts/python \
  src/main/python/ai_assistant/main.py
```

**4. 前端**（端口 5173）
```bash
cd scau_archive-frontend
npm install
npm run dev
```

### Docker 部署

```bash
# 1. 配置环境变量（AI 助手需要 GLM_API_KEY）
cp .env.example .env
# 编辑 .env，填入 GLM_API_KEY=your_api_key

# 2. 导出数据库
PGPASSWORD=123456 pg_dump -h localhost -U postgres -d scau_archive > backup.sql

# 3. 构建并启动（首次构建需下载 PaddlePaddle ~1.8GB，可能 10-20 分钟）
docker compose up -d

# 4. 导入数据
docker cp backup.sql scau-db:/tmp/
docker exec scau-db psql -U postgres -d scau_archive -f /tmp/backup.sql

# 5. 访问 http://localhost
```

## 核心流程

### 文件上传处理

```
上传文件 → StorageService.saveFiles() → storage/temp/{date}/{type}/
  ├─ CSV/Excel → 字段映射 → 数据校验 → 持久化 → 归档
  ├─ PDF → 转图片 → 逐页OCR/LLM → 结构化数据 → 持久化 → 归档
  ├─ 图片 → OpenCV增强 → OCR/LLM → 结构化数据 → 持久化 → 归档
  └─ 失败 → storage/failed/ + .error.json 错误日志
```

### LLM 智能提取

启用「LLM 智能提取」开关后，图片/PDF走 LLM 路径：
- 直接调用多模态大模型提取结构化数据
- 图片自动压缩（最长边1200px，JPEG质量85）
- PDF多页合并归档（一条日志，一次计数）
- 完整的存库、质量评分、日志流程

### AI 助手 + 知识库

```
用户提问 → 检索知识库（pgvector 余弦相似度）→ 拼入上下文 → LLM 生成回答
  ├─ 流式 SSE：Python agent.astream_events() → Java SseEmitter → 前端 ReadableStream
  ├─ 工具调用：19 种数据库查询 + 联网搜索（Bing）+ 网页抓取（Playwright）
  └─ 知识库：上传文件/URL → 解析 → 分块 → 向量化（Zhipu embedding-3）→ 存入 pgvector
```

### 字段匹配

```
fieldName > sourceField > fieldCode
```
OCR管道：精确匹配 → 去空白匹配 → 包含匹配 → Levenshtein距离修正

## API 文档

启动后端后访问：`http://localhost:8080/swagger-ui.html`
- 所有接口带中文描述
- 支持 JWT Bearer Token 在线测试

## LLM 配置

通过环境变量或 `.env` 文件配置：

```bash
# 复制环境变量模板并编辑
cp .env.example .env
# 填入 GLM_API_KEY=your_api_key
```

或直接设置环境变量启动后端：

```bash
GLM_API_KEY=your_api_key ./mvnw spring-boot:run
```

可使用环境变量切换模型：

| 变量 | 默认值 | 说明 |
|------|--------|------|
| `GLM_API_KEY` | — | API 密钥（必填） |
| `LLM_BASE_URL` | `https://open.bigmodel.cn/api/paas/v4` | API 地址 |
| `LLM_MODEL` | `glm-4v-plus-0111` | 模型名称 |

推荐模型：智谱 GLM-4V-Plus-0111（付费，支持Base64）、通义千问 Qwen-VL-Plus

## 数据脱敏

Header 右上角「脱敏/原始」开关控制。开启后身份证号、姓名等敏感信息在 API 返回时自动遮挡，不修改数据库原始数据。

## 项目结构

```
SCAU/
├── docker-compose.yml              # Docker 编排（db + backend + frontend）
├── .env.example                    # 环境变量模板（复制为 .env 使用）
├── scau-archive-insight/           # Spring Boot 后端
│   ├── .dockerignore               # 构建上下文排除清单
│   ├── Dockerfile                  # 后端镜像（Java 21 + Python 3 + PaddlePaddle）
│   └── src/
│       ├── main/java/edu/scau/scauarchiveinsight/
│       │   ├── controller/         # REST 控制器（含 AI 助手、知识库）
│       │   ├── service/            # 业务逻辑 + processor 文件处理器
│       │   ├── config/             # Security, Swagger, 脱敏, MyBatis-Plus 配置
│       │   ├── util/               # JWT, 脱敏工具, 文本工具
│       │   ├── dto/vo/mapper/pojo/ # 数据传输、视图、映射、实体
│       │   └── filter/             # JWT 认证过滤器
│       └── main/python/
│           ├── ai_assistant/       # AI 助手（FastAPI + LangChain + RAG）
│           ├── ppstructure/        # OCR 表格识别 + LLM 提取
│           ├── pdf2image/          # PDF → PNG
│           ├── openCV/             # 图像增强
│           ├── predict/            # ARIMA+XGBoost 预测
│           └── seed/               # 假数据生成
├── scau_archive-frontend/          # Vue 3 前端
│   ├── .dockerignore               # 构建上下文排除清单
│   ├── Dockerfile                  # 前端镜像
│   ├── nginx.conf                  # Nginx 反向代理配置
│   └── src/
│       ├── views/                  # 页面组件
│       ├── api/                    # API 接口封装
│       ├── store/                  # Pinia 状态管理（menu, tab, user）
│       └── layouts/                # 布局组件
└── scau-archive-insight/
    ├── storage/                    # 文件存储（运行时）
    └── models/                     # PaddlePaddle 模型缓存（运行时）
```

## 数据库

- **事实表**: admission_fact, student_fact, graduation_fact
- **维度表**: province_dim, major_dim, college_dim, degree_dim, destination_dim, nation_dim, political_dim, class_dim, archive_file_dim, ocr_log_dim, quality_score_dim
- **系统表**: sys_user, metadata_standard
- **知识库表**: knowledge_base, knowledge_chunks（含 pgvector 向量字段）
