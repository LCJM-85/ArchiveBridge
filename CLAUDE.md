# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

SCAU Archive Insight is a full-stack student archive management system for South China Agricultural University.

- **Backend**: Spring Boot 3.5.13 (Java 17/21) + MyBatis-Plus 3.5.13 + PostgreSQL/PostGIS + Druid
- **Frontend**: Vue 3 SPA (Vite 8, Element Plus, Pinia, ECharts, Axios)
- **Python scripts**: PPStructureV3 (PaddleOCR table recognition), PDF-to-image (PyMuPDF), image enhancement (OpenCV), ARIMA+XGBoost prediction, LLM Vision extraction
- **Analysis modules**: Trend analysis (5 charts), Geographic distribution (China map via PostGIS), Training path (Sankey), AI prediction (ARIMA+XGBoost), Report generation (Word/A3 poster), LLM-based archive extraction
- **Database**: PostgreSQL on localhost:5432, database `scau_archive`, user `postgres` / `123456`
- **Docker**: docker-compose.yml orchestrates db + backend + frontend; backend Dockerfile includes Java 21, Python 3, PaddlePaddle (CPU); frontend Dockerfile builds Vite SPA and serves via Nginx

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

### Docker (deployment, any machine with Docker)
```bash
cd D:/Ideaworkplace/SCAU
docker compose build   # Build all images
docker compose up -d   # Start services (db:5432, backend:8080, frontend:80)
# First-time setup: restore DB dump into db container
docker cp backup.sql scau-db:/tmp/
docker exec scau-db psql -U postgres -d scau_archive -f /tmp/backup.sql
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
PGPASSWORD=123456 D:/postgresql/bin/psql.exe -h localhost -U postgres -d scau_archive
```

## Key Architecture Rules

### Field Matching Priority (all pipelines)
```
fieldName > sourceField > fieldCode
```
- **OCR pipeline** (Python `ocr_table.py`): 4-level matching: exact → remove-whitespace → contains → Levenshtein distance (≤1 char for ≤3-char text, ≤30% for longer)
- **CSV/Excel pipeline** (Java `MetaDataMappingService`): exact key match → contains match
- **LLM pipeline** (Python `llm_extractor.py`): prompt includes fieldCode（fieldName）+ sourceField 别名，LLM 自行映射
- **FieldCorrectionService**: Post-mapping distance correction — if a mapped value doesn't match any dimension table entry, uses Levenshtein distance to find the closest match and auto-corrects the value
- Results always use `fieldCode` as output key

### Dimension Table Auto-Creation
When `fuzzyLookupXxx()` fails to find a name in dimension tables (province/major/class/degree/destination), it **auto-inserts** a new record with that name.

### Data Deduplication
- **admission**: match by `student_no` → `id_card` → `exam_no`, UPDATE existing or INSERT
- **graduation**: match by `student_no` → `id_card`, UPDATE existing or INSERT; marks `student_fact.graduated = true`

### File Upload Pipeline
1. `ArchiveUploadController` → `StorageService.saveFiles()` → saves to `storage/temp/{yyyyMMdd}/{type}/`
2. Dispatched by extension to processors (CSV/Excel/Image/PDF/LLM), each follows: extract → map → persist → archive/failed
3. Upload form has an "LLM 智能提取" toggle for image/PDF types — when enabled, uses `LLMProcessor` instead of OCR
4. Quality score saved to `quality_score_dim` after successful processing
5. OCR log: `addLog()` during processing OR `syncTodayLogs()` scans archive/failed dirs
6. On failure: `storage/failed/` + `.error.json` sidecar

### Upload Type Mapping
- `csv` → `CSVProcessor`, `excel` → `ExcelProcessor`
- `pdf` → `PDFProcessor` (PyMuPDF → pages → OCR) or `LLMProcessor.processPdfPages()` when LLM toggle on
- `ocr` / `wax` → `ImageProcessor` (OpenCV enhance → OCR) or `LLMProcessor` when LLM toggle on
- Direct images (jpg/png) → `ImageProcessor` or `LLMProcessor`

### PDF + LLM Processing (processPdfPages)
When PDF uses LLM mode: pages are NOT processed individually. `LLMProcessor.processPdfPages()` extracts all pages, creates ONE archive entry, ONE quality score, ONE OCR log for the PDF. Temporary page images are deleted after processing. Processing counter only increments/decrements once for the PDF, not per page.

### LLM Image Compression
`llm_extractor.py` `encode_image()` automatically resizes images to longest edge 1200px and encodes as JPEG quality 85 before sending to API. Falls back to raw encoding if OpenCV fails (e.g., corrupt file).

### Security
- Spring Security + JWT (BCrypt), stateless, no session
- Rate limiting: 8 login attempts per 10 min per IP+username; 30 captcha requests per min
- JWT stored in localStorage, sent via `Authorization: Bearer <token>` header
- Login flow: GET `/api/captcha` → POST `/api/login`

### Data Desensitization
Annotation-driven backend desensitization with toggle. Uses Jackson `@Sensitive(DesensitizeUtil.SensitiveType.ID_CARD)` on VO fields, `DesensitizeSerializer` (ContextualSerializer) intercepts JSON serialization, checks global flag via `DesensitizeContext.isEnabled()`. Toggle endpoint: POST `/api/desensitize/toggle`. Frontend switch in Header.vue.

### Swagger API Docs
Available at `/swagger-ui.html` (SpringDoc OpenAPI 2.8.16). All controllers annotated with `@Tag` and `@Operation`. JWT Bearer auth supported via "Authorize" button.

### ProvinceDim GeoJSON
`province_dim.geom` stores province boundaries (`geometry(MultiPolygon,4326)`). Frontend imports static snapshot at `scau_archive-frontend/src/assets/geo/china.json`. To refresh: query `ST_AsGeoJSON(ST_Simplify(geom,0.05),4)` from the database.

### OCR Model Configuration
Current `ocr_table.py` uses lightweight models for speed: `PP-OCRv4_mobile_det` + `PP-OCRv4_mobile_rec` with batch size 6. Cached models in `models/.paddlex/official_models/` include GPU/CPU variants.

### Processing Counter (AtomicInteger)
`StorageService.processingCount` tracks files in `storage/temp/` that are being or waiting to be processed. Incremented on `saveFiles()`, decremented on `moveArchiveFile()` / `failedFile()`. Critical for PDF+LLM: `processPdfPages()` only archives the PDF once, not per page, keeping the counter balanced.

## Project Structure

### Backend (scau-archive-insight)
```
src/main/java/edu/scau/scauarchiveinsight/
├── controller/     — 14 REST controllers (Admission, Graduation, Student, Login,
│                     Dashboard, Report, ArchiveUpload, MetaData, OCRLog,
│                     QualityScore, Storage, ChangePassword, LLM, Desensitize)
├── service/        — Business logic
│   ├── processor/   — CSVProcessor, ExcelProcessor, ImageProcessor, PDFProcessor, LLMProcessor
│   └── ...Service  — LLMExtraction, DataPersistence, MetaDataMapping, FieldCorrection,
│                      PPStructure, PdfToImage, OpenCV, OCRLog, QualityScore,
│                      TrendAnalysis, Geographic, TrainingPath, Prediction, Report, Dashboard
├── config/         — SecurityConfig, MyBatisPlusConfig, GlobalExceptionHandler,
│                      JsonAuthenticationEntryPoint, SpringDocConfig, DesensitizeConfig
├── util/           — JwtUtils, DateUtil, TextUtil, DesensitizeUtil, Sensitive (annotation),
│                      DesensitizeSerializer, DesensitizeContext
└── dto/vo/mapper/pojo/filter
```

### Frontend (scau_archive-frontend)
```
src/
├── api/modules/    — auth, archive, metadata, ocr, admission, graduation, student, desensitize
├── views/
│   ├── archive/     — ArchiveUpload (with LLM toggle)
│   ├── data/        — AdmissionData, GraduationData, StudentStatusData
│   ├── charts/      — AddmissionTrend, Geographic, MajorTrainingPath, AIPrediction
│   ├── dashboard/   — Dashboard (stats cards, trend, province pie)
│   ├── report/      — ReportGenerate (Word/A3 poster)
│   └── login/       — Login page
└── layouts/         — AppLayout, Header (with desensitize switch), Sidebar, Content
```

### Python Scripts
```
src/main/python/
├── ppstructure/ocr_table.py        — PPStructureV3 → HTML table → field mapping → JSON
├── ppstructure/llm_extractor.py    — LLM Vision API → field extraction → JSON (with auto-compress)
├── pdf2image/pdf2image.py          — PyMuPDF → 200dpi PNG per page
├── openCV/opencv.py                — Grayscale → blur → threshold → sharpen
├── predict/predict_admission.py    — ARIMA + XGBoost ensemble → 3-year forecast
├── seed/seed_fake_data.py          — Generate 2020-2025 fake data for demo
└── requirements.txt                — Python deps for Docker/clean install
```

### Storage
```
storage/
├── temp/{yyyyMMdd}/{type}/     — Upload staging
├── archive/{yyyyMMdd}/{type}/  — Successfully processed files
├── failed/{yyyyMMdd}/{type}/   — Failed files + .error.json sidecar
└── enhance/                    — OpenCV enhanced temp images (auto-cleaned)
```

### Database Schema
- **Dimension tables**: student_dim, college_dim, major_dim, class_dim, province_dim, nation_dim, political_dim, degree_dim, destination_dim, source_type_dim, archive_file_dim, ocr_log_dim, quality_score_dim
- **Fact tables**: student_fact, admission_fact, graduation_fact
- **Metadata**: metadata_standard (fieldCode PK), sys_user
