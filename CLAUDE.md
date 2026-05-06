# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

SCAU Archive Insight is a full-stack student archive management system for South China Agricultural University.

- **Backend**: Spring Boot 3.5.13 (Java 17) + MyBatis-Plus 3.5.13 + PostgreSQL/PostGIS + Druid
- **Frontend**: Vue 3 SPA (Vite 8, Element Plus, Pinia, ECharts, Axios)
- **Python scripts**: OCR (PaddleOCR), PDF-to-image (PyMuPDF), image enhancement (OpenCV)

## Commands

### Backend (Spring Boot, port 8080)
```bash
# Must override JAVA_HOME to JDK 17+
cd scau-archive-insight
JAVA_HOME="D:/java/jdk-21.0.5" ./mvnw clean package   # Build
JAVA_HOME="D:/java/jdk-21.0.5" ./mvnw spring-boot:run  # Run
```
Note: No automated tests exist in the project.

### Frontend (Vue 3, port 5173)
```bash
cd scau_archive-frontend
npm install        # Install dependencies
npm run dev        # Dev server
npm run build      # Production build
npm run preview    # Preview production build
```

### Python (Windows venv)
```bash
cd scau-archive-insight
.venv/Scripts/python.exe src/main/python/ocr/ocr.py <image_path>
.venv/Scripts/python.exe src/main/python/pdf2image/pdf2image.py <pdf_path>
.venv/Scripts/python.exe src/main/python/openCV/opencv.py <image_path>
```

## Architecture

### Backend Layers
Base package: `edu.scau.scauarchiveinsight`
- **controller/** — REST endpoints:
  - `ArchiveUploadController` (`/api/upload`) — multipart file upload + type/archiveType params
  - `LoginController` (`/api/login`, `/api/captcha`) — captcha + JWT auth
  - `ChangePasswordController` (`/api/change-password`)
  - `MetaDataController` (`/metadata/**`) — CRUD + page query for metadata_standard table
  - `StorageController` (`/storage/status`) — scans temp/archive/failed dirs for monitoring
  - `OCRLogController` (`/ocr/log/**`) — sync today's logs, query history, delete
- **dto/** — `LoginDTO`, `ChangePasswordDTO`
- **service/** — Business logic:
  - `UserService` — login, password change
  - `StorageService` — `saveFiles()`, `moveArchiveFile()`, `failedFile()` with `.error.json` sidecar
  - `MetaDataService` — CRUD + keyword search page for metadata_standard
  - `MetaDataMappingService` — field mapping + validation for CSV/Excel (uses `metadata_standard` rules)
  - `OCRService` — calls Python PaddleOCR via ProcessBuilder, passes metadata rules as temp JSON
  - `OCRLogService` — syncTodayLogs (scans archive/failed dirs), getTodayLogs, getHistory, delete
  - `PdfToImageService` — calls pdf2image.py via ProcessBuilder
  - `OpenCVService` — calls opencv.py via ProcessBuilder
  - `DataPersistenceService` — **stub** (`saveExtractedData(archiveType, data)`) — user implements custom persistence
- **processor/** — File parsing (each follows same pattern: extract → map → persist → archive/failed):
  - `CSVProcessor` — standard CSV parsing with quote handling
  - `ExcelProcessor` — Apache POI (both .xls and .xlsx)
  - `PDFProcessor` — pdf2image → OCR per page → collect errors → persist
  - `WaxProcessor` — image (蜡纸) → OpenCV enhance → OCR → persist
- **mapper/** — MyBatis-Plus interfaces for all entities: `StudentDimMapper`, `StudentFactMapper`, `AdmissionFactMapper`, `GraduationFactMapper`, `CollegeDimMapper`, `MajorDimMapper`, `ClassDimMapper`, `ProvinceDimMapper`, `NationDimMapper`, `PoliticalDimMapper`, `DegreeDimMapper`, `DestinationDimMapper`, `SourceTypeDimMapper`, `ArchiveFileDimMapper`, `OCRLogDimMapper`, `QualityScoreDimMapper`, `MetaDataStandardMapper`, `UserMapper`, `DateDimMapper`
- **pojo/** — Entity classes: dimension tables, fact tables (`StudentFact`, `AdmissionFact`, `GraduationFact`), `MetaDataStandard`, `OCRLogDim`, `SysUser`
- **config/** — `SecurityConfig` (Spring Security + CORS), `MyBatisPlusConfig` (PaginationInnerInterceptor), `GlobalExceptionHandler`, `JsonAuthenticationEntryPoint`
- **filter/** — `JwtAuthenticationFilter` (OncePerRequestFilter)
- **util/** — `JwtUtils`, `DateUtil`

### File Upload Pipeline
1. `ArchiveUploadController` receives multipart files + `type` (pdf/wax/ocr/excel/csv) + `archiveType` (admission/graduation)
2. `StorageService.saveFiles()` saves to `storage/temp/{yyyyMMdd}/{type}/`
3. Based on file extension, dispatches to processor:
   - **CSV/Excel** → parsed into `List<Map>` → `MetaDataMappingService` maps fields + validates → `DataPersistenceService.saveExtractedData()` per record → archive to `storage/archive/`
   - **PDF** → `PdfToImageService` (Python PyMuPDF) → `OCRService` (Python PaddleOCR) → structured JSON → persist → archive
   - **Images (wax/ocr)** → `OpenCVService` (Python enhance) → `OCRService` (Python PaddleOCR) → persist → archive
4. On failure: `StorageService.failedFile()` moves to `storage/failed/` + writes `.error.json` sidecar
5. `OCRLogService.syncTodayLogs()` scans `archive/` and `failed/` dirs daily to populate `ocr_log_dim` table

### Metadata-Driven Data Cleaning
- All processors use `metadata_standard` table rules for field mapping and validation
- `MetaDataMappingService`: maps source fields → `fieldCode`, validates types (int/decimal/boolean/date), checks required fields
- For OCR pipelines: `OCRService` passes metadata rules as a temp JSON file to Python; OCR returns JSON with `fieldCode` as keys
- Field matching priority: `fieldCode` > `fieldName` > `sourceField`

### Authentication
- `GET /api/captcha` → session-based captcha (Hutool LineCaptcha, 2min TTL)
- `POST /api/login` → validates captcha + BCrypt password → returns JWT
- Subsequent requests: `Authorization: Bearer <token>` header
- Rate limiting: 8 attempts/10min per IP+user, 30 captcha requests/min per IP
- `SessionCreationPolicy.ALWAYS` (captcha stored in HttpSession)
- Frontend axios instance (`utils/request.js`) auto-attaches JWT and handles 401 redirects

### Python Scripts
All called via `ProcessBuilder` from Java services, Python venv at `.venv/Scripts/python.exe`:
- **ocr.py**: PaddleOCR → column-based table extraction with position grouping → uses `fieldCode` as output key → returns `{"data": [{...}], "errors": [...]}`
- **pdf2image.py**: PyMuPDF (fitz) → 200dpi PNG per page → prints paths to stdout
- **opencv.py**: OpenCV → grayscale → Gaussian blur → adaptive threshold → sharpen

### Database (PostgreSQL + PostGIS)
- **Dimension tables**: student_dim, college_dim, major_dim, class_dim, province_dim, nation_dim, political_dim, degree_dim, destination_dim, source_type_dim, archive_file_dim, ocr_log_dim, quality_score_dim
- **Fact tables**: student_fact, admission_fact, graduation_fact
- **Metadata table**: metadata_standard (fieldCode PK, fieldName, fieldType, sourceField, transformType, transformRule, isRequired)
- `date_dim` table was removed; all `xxx_date_id` FK columns replaced with direct `xxx_date date` columns
- Connection pool: Druid (initial 5, min 10, max 20)
- All entity fields use `@TableField("snake_case_name")` for explicit column mapping
- Config: `src/main/resources/application.yaml`

### Frontend Structure
- **api/** — Axios instance + modular API layer:
  - `request.js` — axios instance with JWT interceptor and 401 handling
  - `modules/` — API modules per domain: `auth.js`, `archive.js`, `metadata.js`, `ocr.js`, `analysis.js`, `report.js`
- **layouts/** — `AppLayout.vue`, `Header.vue`, `Sidebar.vue`, `Content.vue`
- **components/common/** — `TableView`, `UploadPanel`, `Loading`, `Empty`
- **composables/** — `useTheme.js`, `useFullscreen.js`, `usePasswordChange.js`
- **config/** — `index.js` (APP_CONFIG, JWT_CONFIG, PAGE_CONFIG)
- **styles/** — `index.css` (full CSS variable theme system: light + dark mode)
- **store/** — Pinia stores (auto-sync with localStorage): `user.js`, `menu.js`, `archive.js`, `metadata.js`
- **utils/** — `auth.js` (token/remember-me helpers), `format.js`
- **router/index.js** — Auth guard via localStorage JWT + client-side expiry check
- **views/** — All page views:
  - `login/Login.vue`, `dashboard/Dashboard.vue`
  - `archive/ArchiveUpload.vue`
  - `data/AdmissionData.vue`, `GraduationData.vue`, `StudentStatusData.vue`
  - `charts/AddmissionTrend.vue`, `Geographic.vue`, `MajorTrainingPath.vue`, `AIPrediction.vue`
  - `analysis/ReportGenerate.vue`, `report/ReportGenerate.vue`
  - `ocr/OCRProcess.vue`
  - `governance/DataClean.vue`, `DataQuality.vue`
  - `prediction/PredictionView.vue`
  - `system/MetaDataManage.vue`, `UserManage.vue`
- All imports use `@/` path alias (e.g., `@/store/user`, `@/api/modules/auth`)
