# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

SCAU Archive Insight is a full-stack student archive management system for South China Agricultural University.

- **Backend**: Spring Boot 3.5 (Java 17) + MyBatis-Plus + PostgreSQL/PostGIS
- **Frontend**: Vue 3 SPA (Vite 8, Element Plus, Pinia, ECharts, Axios)
- **Python scripts**: OCR (PaddleOCR), PDF-to-image (PyMuPDF), image enhancement (OpenCV)

## Commands

### Backend (Spring Boot, port 8080)
```bash
cd scau-archive-insight
./mvnw clean package          # Build
./mvnw spring-boot:run        # Run
./mvnw test                   # Run tests
```

### Frontend (Vue 3, port 5173)
```bash
cd scau_archive-frontend
npm install                   # Install dependencies
npm run dev                   # Dev server
npm run build                 # Production build
npm run preview               # Preview production build
```

### Python (Windows venv)
```bash
cd scau-archive-insight
.venv/Scripts/python.exe src/main/python/ocr/ocr.py <image_path>
.venv/Scripts/python.exe src/main/python/pdf2image/pdf2image.py <pdf_path>
.venv/Scripts/python.exe src/main/python/opencv/opencv.py <image_path>
```

## Architecture

### Backend Layers
- **controller/** — REST endpoints (`/api/upload`, `/api/login`, `/api/captcha`, `/api/change-password`)
- **service/** — Business logic: `UserService`, `StorageService`, `OCRService`, `PdfToImageService`, `OpenCVService`
- **processor/** — File parsing: `CSVProcessor`, `ExcelProcessor`, `PDFProcessor`, `WaxProcessor` (image)
- **mapper/** — MyBatis-Plus data access interfaces
- **pojo/** — Entity classes (dimension/fact tables + `SysUser`)
- **config/** — `SecurityConfig` (Spring Security + CORS), `GlobalExceptionHandler`, `JsonAuthenticationEntryPoint`
- **filter/** — `JwtAuthenticationFilter` (OncePerRequestFilter)
- **util/** — `JwtUtils`

### File Upload Pipeline
1. `ArchiveUploadController` receives multipart files + type param
2. `StorageService.saveFiles()` saves to `storage/temp/{yyyyMMdd}/{type}/`
3. Based on file extension, dispatches to processor:
   - **CSV/Excel** → parsed into `List<Map>` rows → auto-archived to `storage/archive/`
   - **PDF** → `PdfToImageService` (Python PyMuPDF) → `OCRService` (Python PaddleOCR) → structured JSON
   - **Images** → `OpenCVService` (Python enhance) → `OCRService` → structured JSON

### Authentication
- `GET /api/captcha` → session-based captcha (Hutool LineCaptcha, 2min TTL)
- `POST /api/login` → validates captcha + BCrypt password → returns JWT
- Subsequent requests: `Authorization: Bearer <token>` header
- Rate limiting: 8 attempts/10min per IP+user, 30 captcha requests/min per IP
- SessionCreationPolicy.ALWAYS (captcha stored in HttpSession)

### Python Scripts
All called via `ProcessBuilder` from Java services, Python venv at `.venv/Scripts/python.exe`:
- **ocr.py**: PaddleOCR → regex extraction of student fields (学号, 姓名, 院系, 专业, 毕业证号) → JSON
- **pdf2image.py**: PyMuPDF (fitz) → 200dpi PNG per page → prints paths to stdout
- **opencv.py**: OpenCV → grayscale → Gaussian blur → adaptive threshold → sharpen

### Database (PostgreSQL + PostGIS)
- **Dimension tables**: student_dim, college_dim, major_dim, class_dim, province_dim, nation_dim, political_dim, date_dim, degree_dim, destination_dim, source_type_dim, archive_file_dim, ocr_log_dim
- **Fact tables**: student_fact, admission_fact, graduation_fact
- Connection pool: Druid (initial 5, min 10, max 20)

### Frontend Layout
- **views/** — dashboard, login, archive, analysis, charts, data, governance, ocr, prediction, report, system
- **components/common/** — TableView, UploadPanel, Loading, Empty
- **components/layout/** — AppLayout, Header, Sidebar, Content
- **api/** — auth.js, archive.js, analysis.js, report.js
- **store/** — user.js (Pinia), archive.js, menu.js
- **router/** — Auth guard via localStorage JWT + client-side expiry check
