# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

SCAU Archive Insight is a full-stack student archive management system for South China Agricultural University.

- **Backend**: Spring Boot 3.5.13 (Java 17) + MyBatis-Plus 3.5.13 + PostgreSQL/PostGIS + Druid
- **Frontend**: Vue 3 SPA (Vite 8, Element Plus, Pinia, ECharts, Axios)
- **Python scripts**: PPStructureV3 (PaddleOCR table recognition), PDF-to-image (PyMuPDF), image enhancement (OpenCV)
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
2. Dispatched by extension to processors (CSV/Excel/Image/PDF), each follows: extract → map → persist → archive/failed
3. Quality score saved to `quality_score_dim` after successful processing
4. OCR log created via `addLog()` during processing OR `syncTodayLogs()` scans archive/failed dirs
5. On failure: `storage/failed/` + `.error.json` sidecar

### Upload Type Mapping (from frontend)
- `csv` → `CSVProcessor`, `excel` → `ExcelProcessor`
- `pdf` → `PDFProcessor` (PyMuPDF → pages → OCR each page)
- `ocr` / `wax` → `ImageProcessor` (OpenCV enhance → OCR)

### Security
- Spring Security + JWT (BCrypt), stateless, no session
- Rate limiting: 8 login attempts per 10 min per IP+username; 30 captcha requests per min
- JWT stored in localStorage, sent via `Authorization: Bearer <token>` header
- Login flow: GET `/api/captcha` → POST `/api/login` (captchaKey + captchaCode required)

### PPStructureV3 Notes
- Uses PaddleX models auto-cached at `models/.paddlex/official_models/` (~1.8 GB)
- Must set `HOME` and `USERPROFILE` env vars to `models/` directory (avoids C++ inference crash with Chinese-chars in Windows username)
- Table output is `pred_html` (HTML), parsed via regex; NOT the old `cells[row][col]` format
- Python script must NOT be named `ppstructure.py` (circular import with paddleocr module)

### Frontend Routing
- `router/index.js` uses `localStorage.getItem('token')` as auth guard — redirects to `/login` if missing
- Pinia stores backed by localStorage for persistence across refreshes
- Axios interceptors: attach token header, handle 401 → redirect login

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
│   ├── AdmissionController         /api/admission/**
│   ├── GraduationController        /api/graduation/**
│   ├── StudentController           /api/student/**
│   ├── MetaDataController          /metadata/**
│   ├── StorageController           /storage/status
│   ├── OCRLogController            /ocr/log/**
│   └── QualityScoreController      /api/quality-score/list
├── service/        — Business logic
│   ├── *Processor.java             — CSVProcessor, ExcelProcessor, ImageProcessor, PDFProcessor
│   ├── DataPersistenceService      — save + dedup + fuzzy dimension matching
│   ├── MetaDataMappingService      — CSV/Excel field mapping + validation
│   ├── FieldCorrectionService      — post-mapping Levenshtein distance correction
│   ├── PPStructureService          — Calls Python ocr_table.py via ProcessBuilder
│   ├── PdfToImageService           — Calls Python pdf2image.py
│   ├── OpenCVService               — Calls Python opencv.py
│   ├── OCRLogService               — syncTodayLogs, getTodayLogs, addLog, delete
│   ├── QualityScoreService         — completeness/accuracy/consistency/timeliness scoring
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
│   ├── charts/      — AdmissionTrend, Geographic, MajorTrainingPath, AIPrediction
│   ├── dashboard/   — Data overview dashboard
│   ├── analysis/    — Data analysis views
│   ├── governance/  — Data governance
│   ├── prediction/  — AI prediction views
│   ├── report/      — Report generation
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
├── ppstructure/ocr_table.py    — PPStructureV3 → HTML table → field mapping → JSON
├── pdf2image/pdf2image.py      — PyMuPDF → 200dpi PNG per page
├── openCV/opencv.py            — Grayscale → blur → threshold → sharpen
├── data_fill/                  — Data filling/completion scripts
└── predict/                    — Prediction scripts
```

### Storage
```
storage/
├── temp/{yyyyMMdd}/{type}/     — Upload staging
├── archive/{yyyyMMdd}/{type}/  — Successfully processed files
├── failed/{yyyyMMdd}/{type}/   — Failed files + .error.json sidecar
└── ocr_log_dim table           — Log entries created by addLog() or syncTodayLogs()
```

### Database Schema
- **Dimension tables**: student_dim, college_dim, major_dim, class_dim, province_dim, nation_dim, political_dim, degree_dim, destination_dim, source_type_dim, archive_file_dim, ocr_log_dim, quality_score_dim
- **Fact tables**: student_fact, admission_fact, graduation_fact
- **Metadata**: metadata_standard (fieldCode PK), sys_user
