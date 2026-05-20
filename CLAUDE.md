# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

SCAU Archive Insight is a full-stack student archive management system for South China Agricultural University.

- **Backend**: Spring Boot 3.5.13 (Java 17) + MyBatis-Plus 3.5.13 + PostgreSQL/PostGIS + Druid
- **Frontend**: Vue 3 SPA (Vite 8, Element Plus, Pinia, ECharts, Axios)
- **Python scripts**: PPStructureV3 (PaddleOCR table recognition), PDF-to-image (PyMuPDF), image enhancement (OpenCV), ARIMA+XGBoost prediction, LLM Vision extraction
- **Analysis modules**: Trend analysis (5 charts), Geographic distribution (China map via PostGIS), Training path (Sankey), AI prediction (ARIMA+XGBoost), Report generation (Word/A3 poster), LLM-based archive extraction
- **Database**: PostgreSQL on localhost:5432, database `scau_archive`, user `postgres` / `123456`

## Commands

### Backend (Spring Boot, port 8080)
```bash
cd scau-archive-insight
JAVA_HOME="D:/java/jdk-21.0.5" ./mvnw clean package   # Build
JAVA_HOME="D:/java/jdk-21.0.5" ./mvnw spring-boot:run  # Run
JAVA_HOME="D:/java/jdk-21.0.5" ./mvnw test -Dtest=TestClass#method  # Single test
```

### Frontend (Vue 3, port 5173)
```bash
cd scau_archive-frontend
npm install        # Install dependencies
npm run dev        # Dev server (hot reload)
npm run build      # Production build
npm run preview    # Preview production build
```

### Python (Windows venv, PaddlePaddle-GPU 3.2.2 + paddleocr 3.5.0)
```bash
# HOME must point to models/ (contains .paddlex cache, avoids Chinese-username path issue)
HOME="D:/Ideaworkplace/SCAU/scau-archive-insight/models" \
USERPROFILE="D:/Ideaworkplace/SCAU/scau-archive-insight/models" \
scau-archive-insight/src/main/python/.venv/Scripts/python.exe \
  scau-archive-insight/src/main/python/ppstructure/ocr_table.py <image_path> [rules_path]
```

### Database Access
```bash
# psql at D:/postgresql/bin/psql.exe
PGPASSWORD=123456 D:/postgresql/bin/psql.exe -h localhost -U postgres -d scau_archive
```

## Key Architecture Rules

### Field Matching Priority (all pipelines)
```
fieldName > sourceField > fieldCode
```
- **OCR pipeline** (Python `ocr_table.py`): 4-level matching: exact → remove-whitespace → contains → Levenshtein distance (≤1 char for ≤3-char text, ≤30% for longer)
- **CSV/Excel pipeline** (Java `MetaDataMappingService`): exact key match → contains match
- **FieldCorrectionService**: Post-mapping distance correction — if a mapped value doesn't match any dimension table entry, uses Levenshtein distance to find the closest match and auto-corrects the value
- Results always use `fieldCode` as output key

### Dimension Table Auto-Creation
When `fuzzyLookupXxx()` fails to find a name in dimension tables (province/major/class/degree/destination), it **auto-inserts** a new record with that name. This prevents FK nulls when processing data with unrecognized dimension values.

### Data Deduplication
- **admission**: match by `student_no` → `id_card` → `exam_no`, UPDATE existing or INSERT
- **graduation**: match by `student_no` → `id_card`, UPDATE existing or INSERT; marks `student_fact.graduated = true`

### File Upload Pipeline
1. `ArchiveUploadController` → `StorageService.saveFiles()` → saves to `storage/temp/{yyyyMMdd}/{type}/`
2. Dispatched by extension to processors (CSV/Excel/Image/PDF/LLM), each follows: extract → map → persist → archive/failed
3. Upload form has an "LLM 智能提取" toggle for image/PDF types — when enabled, uses `LLMProcessor` instead of OCR
4. Quality score saved to `quality_score_dim` after successful processing
5. OCR log created via `addLog()` during processing OR `syncTodayLogs()` scans archive/failed dirs
6. On failure: `storage/failed/` + `.error.json` sidecar

### Upload Type Mapping (from frontend)
- `csv` → `CSVProcessor`, `excel` → `ExcelProcessor`
- `pdf` → `PDFProcessor` (PyMuPDF → pages → OCR each page) or `LLMProcessor` when LLM toggle on
- `ocr` / `wax` → `ImageProcessor` (OpenCV enhance → OCR) or `LLMProcessor` when LLM toggle on

### Security
- Spring Security + JWT (BCrypt), stateless, no session
- Rate limiting: 8 login attempts per 10 min per IP+username; 30 captcha requests per min
- JWT stored in localStorage, sent via `Authorization: Bearer <token>` header
- Login flow: GET `/api/captcha` → POST `/api/login` (captchaKey + captchaCode required)

### ProvinceDim GeoJSON
`province_dim.geom` stores province boundaries (`geometry(MultiPolygon,4326)`). Data sourced from the `china-geojson` npm package. The frontend imports a static snapshot at `scau_archive-frontend/src/assets/geo/china.json` (simplified, ~240KB). To refresh: query `ST_AsGeoJSON(ST_Simplify(geom,0.05),4)` from the database.

### LLM 智能提取配置
`application.yaml` 中配置 LLM Vision API（必须使用支持图片的多模态模型）:
```yaml
llm:
  api-key: "sk-your-key"
  base-url: https://dashscope.aliyuncs.com/compatible-mode/v1  # 通义千问示例
  model: qwen-vl-plus
```
推荐搭配: 通义千问 qwen-vl-plus（¥0.003/千token）、DeepSeek-VL2（第三方平台）、GPT-4o-mini
上传页面勾选「LLM 智能提取」开关后，图片/PDF 走 LLMProcessor 路径，跳过 OCR。
LLM 路径包含完整的存库、质量评分、OCR 日志、归档流程，与 OCR 路径一致。

### LLM Processing Flow (useLlm=true)
```
Image/PDF → LLMProcessor.process()
  ├─ LLMExtractionService.extract()  → 调 Python → LLM Vision API
  ├─ DataPersistenceService.saveExtractedData()  → 写入事实表
  ├─ QualityScoreService.scoreFile()  → 质量评分
  ├─ OCRLogService.addLog()  → 处理日志
  └─ StorageService  → 归档/失败处理
```

### PPStructureV3 Notes
- Uses PaddleX models auto-cached at `models/.paddlex/official_models/` (~1.8 GB)
- Must set `HOME` and `USERPROFILE` env vars to `models/` directory (avoids C++ inference crash with Chinese-chars in Windows username)
- Table output is `pred_html` (HTML), parsed via regex; NOT the old `cells[row][col]` format
- Python script must NOT be named `ppstructure.py` (circular import with paddleocr module)

### Frontend Routing
- `router/index.js` uses `localStorage.getItem('token')` as auth guard — redirects to `/login` if missing
- Pinia stores backed by localStorage for persistence across refreshes
- Axios interceptors: attach token header, handle 401 → redirect login

### AdmissionController Trend/GEO Endpoints
All under `/api/admission/`:
- `GET /api/admission/trend/yearly|major|province|score|gender` — yearly aggregation queries with optional `startYear`/`endYear`
- `GET /api/admission/geo/province-stats` — province admission counts
- `GET /api/admission/geo/map-data` — Full GeoJSON FeatureCollection from `province_dim.geom`
- `GET /api/admission/training-path/sankey` — Sankey data (major→degree→destination)
- `GET /api/admission/predict/next-years?years=3` — Runs Python ARIMA+XGBoost prediction

### Dashboard/Report Endpoints
- `GET /api/dashboard/stats` — Homepage aggregate stats + trend + major distribution
- `GET /api/report/data?year=2024` — Annual report data (overview, distribution, scores, destination)

### Python Prediction Script
```bash
# Requires statsmodels + xgboost (installed in project venv via Tsinghua mirror)
HOME="D:/Ideaworkplace/SCAU/scau-archive-insight/models" \
USERPROFILE="D:/Ideaworkplace/SCAU/scau-archive-insight/models" \
scau-archive-insight/src/main/python/.venv/Scripts/python.exe \
  scau-archive-insight/src/main/python/predict/predict_admission.py \
  --yearly "2020,2021,2022,2023,2024,2025" --counts "100,100,100,100,100,50" --years_ahead 3
```

### JSON Response Wrapper
All API responses use `R<T>` (`dto/R.java`):
```json
{"code": 200, "msg": "success", "data": {...}}
```
Error responses: `{"code": 500, "msg": "error message", "data": null}`

## Project Structure

### Backend (scau-archive-insight)
```
src/main/java/edu/scau/scauarchiveinsight/
├── controller/     — REST endpoints
│   ├── ArchiveUploadController     POST /api/upload
│   ├── LoginController             GET /api/captcha, POST /api/login
│   ├── ChangePasswordController    POST /api/change-password
│   ├── AdmissionController         /api/admission/** (also hosts trend/geo/training-path/predict endpoints)
│   ├── GraduationController        /api/graduation/**
│   ├── StudentController           /api/student/**
│   ├── ReportController            /api/report/**
│   ├── DashboardController         /api/dashboard/**
│   ├── MetaDataController          /metadata/**
│   ├── StorageController           /storage/status
│   ├── LLMController               GET /api/llm/status (check if LLM is configured)
│   ├── OCRLogController            /ocr/log/**
│   └── QualityScoreController      /api/quality-score/list
├── service/        — Business logic
│   ├── processor/   — CSVProcessor, ExcelProcessor, ImageProcessor, PDFProcessor, LLMProcessor
│   ├── LLMExtractionService        — Calls Python llm_extractor.py (LLM Vision API)
│   ├── DataPersistenceService      — save + dedup + fuzzy dimension matching
│   ├── MetaDataMappingService      — CSV/Excel field mapping + validation
│   ├── FieldCorrectionService      — post-mapping Levenshtein distance correction
│   ├── PPStructureService          — Calls Python ocr_table.py via ProcessBuilder
│   ├── PdfToImageService           — Calls Python pdf2image.py
│   ├── OpenCVService               — Calls Python opencv.py
│   ├── OCRLogService               — syncTodayLogs, getTodayLogs, addLog, delete
│   ├── QualityScoreService         — completeness/accuracy/consistency/timeliness scoring
│   ├── TrendAnalysisService        — 5 aggregation queries for trend charts
│   ├── GeographicService           — province stats + GeoJSON from PostGIS
│   ├── TrainingPathService         — Sankey diagram data (major→degree→destination)
│   ├── PredictionService           — Calls Python predict_admission.py (ARIMA+XGBoost)
│   ├── ReportService               — Annual report data aggregation
│   ├── DashboardService            — Homepage dashboard stats
│   └── *Service.java               — CRUD: AdmissionService, GraduationService, StudentService, UserService, StorageService, MetaDataService
├── dto/            — AdmissionDTO, GraduationDTO, StudentDTO, LoginDTO, ChangePasswordDTO, MetaDataDTO, R<T>
├── vo/             — AdmissionVO, GraduationVO, StudentVO
├── mapper/         — MyBatis-Plus BaseMapper<T> interfaces
├── pojo/           — Entities: AdmissionFact, GraduationFact, StudentFact, StudentDim + dimension dims (CollegeDim, MajorDim, ClassDim, ProvinceDim, etc.) + SysUser, MetaDataStandard
├── config/         — SecurityConfig, MyBatisPlusConfig, GlobalExceptionHandler, JsonAuthenticationEntryPoint
├── filter/         — JwtAuthenticationFilter
└── util/           — JwtUtils, DateUtil, TextUtil (Levenshtein)
```

### Frontend (scau_archive-frontend)
```
src/
├── api/modules/    — auth, archive, metadata, ocr, admission, graduation, student
├── store/          — Pinia (localStorage persist): user, menu, archive, metadata, admission, graduation, student
├── views/
│   ├── archive/     — ArchiveUpload
│   ├── data/        — AdmissionData, GraduationData, StudentStatusData
│   ├── ocr/         — OCRProcess (today logs, quality scores, history, delete)
│   ├── charts/      — AddmissionTrend (5 ECharts: yearly/major/province/score/gender), Geographic (China map + province ranking), MajorTrainingPath (Sankey: major→degree→destination), AIPrediction (ARIMA+XGBoost forecast with confidence bands)
│   ├── dashboard/   — Dashboard (greeting, stat cards, trend chart, province pie, quick links)
│   ├── report/      — ReportGenerate (annual report + A3 printable poster with print-to-PDF)
│   ├── system/      — MetaDataManage, UserManage
│   └── login/       — Login page
├── layouts/         — AppLayout, Header, Sidebar, Content
├── router/          — Auth guard via localStorage JWT
├── components/      — common/ (TableView, UploadPanel, Loading, Empty, etc.)
└── utils/           — auth, format
```

### Python Scripts
```
src/main/python/
├── ppstructure/ocr_table.py        — PPStructureV3 → HTML table → field mapping → JSON
├── ppstructure/llm_extractor.py    — LLM Vision API → field extraction → JSON
├── pdf2image/pdf2image.py          — PyMuPDF → 200dpi PNG per page
├── openCV/opencv.py                — Grayscale → blur → threshold → sharpen
└── predict/predict_admission.py    — ARIMA + XGBoost ensemble → 3-year forecast
```

### Storage
```
storage/
├── temp/{yyyyMMdd}/{type}/     — Upload staging
├── archive/{yyyyMMdd}/{type}/  — Successfully processed files
├── failed/{yyyyMMdd}/{type}/   — Failed files + .error.json sidecar
├── enhance/                    — OpenCV enhanced temp images (auto-cleaned)
└── ocr_log_dim table           — Log entries created by addLog() or syncTodayLogs()
```

### Database Schema
- **Dimension tables**: student_dim, college_dim, major_dim, class_dim, province_dim, nation_dim, political_dim, degree_dim, destination_dim, source_type_dim, archive_file_dim, ocr_log_dim, quality_score_dim
- **Fact tables**: student_fact, admission_fact, graduation_fact
- **Metadata**: metadata_standard (fieldCode PK), sys_user
