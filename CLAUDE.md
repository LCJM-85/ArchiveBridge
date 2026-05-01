# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

SCAU Archive Insight is a full-stack student archive management system for South China Agricultural University. It consists of:

- **Backend**: Spring Boot 3.5 application (`scau-archive-insight/`)
- **Frontend**: Vue 3 SPA (`scau_archive-frontend/`)

## Commands

### Backend (Spring Boot)
```bash
cd scau-archive-insight

# Build
./mvnw clean package

# Run (default port 8080)
./mvnw spring-boot:run

# Run tests
./mvnw test
```

### Frontend (Vue 3)
```bash
cd scau_archive-frontend

# Install dependencies
npm install

# Dev server (default port 5173)
npm run dev

# Build for production
npm run build

# Preview production build
npm run preview
```

## Architecture

### Backend Structure
- `config/` - Security configuration, CORS, exception handlers
- `controller/` - REST API endpoints
- `service/` - Business logic layer
- `mapper/` - MyBatis-Plus data access interfaces
- `pojo/` - Entity classes (dimension and fact tables)
- `dto/` - Data transfer objects
- `filter/` - JWT authentication filter
- `util/` - Utilities (JWT, etc.)

### Frontend Structure
- `src/views/` - Page components organized by feature (dashboard, login, analysis, archive, charts, data, governance, ocr, prediction, report, system)
- `src/components/` - Reusable UI components (common, layout)
- `src/api/` - API modules for backend communication
- `src/store/` - Pinia state management
- `src/router/` - Vue Router with auth guards
- `src/utils/` - Request wrapper, auth helpers

### Data Model
The system uses a dimensional data model with:
- **Dimension tables**: `student_dim`, `college_dim`, `major_dim`, `class_dim`, `province_dim`, `nation_dim`, `political_dim`, `date_dim`, etc.
- **Fact tables**: `student_fact`, `admission_fact`, `graduation_fact`, etc.

### Authentication Flow
1. Frontend requests captcha from `GET /api/captcha` (session-based)
2. User submits login with captcha to `POST /api/login`
3. Backend validates credentials and returns JWT token
4. Frontend stores token in localStorage and sends it in `Authorization: Bearer <token>` header
5. `JwtAuthenticationFilter` validates token on protected routes

### Key Dependencies
- Backend: Spring Security, MyBatis-Plus, PostgreSQL with PostGIS, JWT (jjwt), Apache POI/PDFBox, Hutool captcha
- Frontend: Vue 3, Vite, Element Plus, Pinia, Vue Router, Axios, ECharts
