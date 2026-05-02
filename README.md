# SCAU 档案洞察系统

华南农业大学毕业生与招生可视化名册管理系统。支持学生档案的批量导入、OCR 智能识别、元数据驱动的数据清洗、多维度可视化分析及报表生成。

## 技术栈

### 后端
- **框架**: Spring Boot 3.5.13 + MyBatis-Plus 3.5.13
- **语言**: Java 17
- **数据库**: PostgreSQL + PostGIS
- **连接池**: Druid（初始 5，最小 10，最大 20）
- **安全**: Spring Security + JWT（BCrypt 加密）+ Hutool 图形验证码
- **构建**: Maven Wrapper

### 前端
- **框架**: Vue 3（Vite 8）
- **UI**: Element Plus
- **状态管理**: Pinia（localStorage 持久化）
- **可视化**: ECharts
- **HTTP**: Axios

### 数据处理
- **OCR**: PaddleOCR（Python，基于列位置的表格识别）
- **PDF 转图**: PyMuPDF / fitz（Python，200dpi PNG）
- **图像增强**: OpenCV（Python，灰度 → 高斯模糊 → 自适应阈值 → 锐化）

## 项目结构

```
scau-archive-insight/                   # 后端 Spring Boot
├── src/main/java/com/scau/archive/
│   ├── controller/                     # REST 控制器
│   │   ├── ArchiveUploadController     # /api/upload — 文件上传
│   │   ├── LoginController             # /api/login, /api/captcha
│   │   ├── ChangePasswordController    # /api/change-password
│   │   ├── MetaDataController          # /metadata/** — 元数据 CRUD
│   │   ├── StorageController           # /storage/status — 存储监控
│   │   └── OCRLogController            # /ocr/log/** — OCR 日志
│   ├── service/                        # 业务逻辑层
│   │   ├── UserService                 # 登录 / 密码修改
│   │   ├── StorageService              # 文件存储与归档管理
│   │   ├── MetaDataService             # 元数据标准 CRUD
│   │   ├── MetaDataMappingService      # 字段映射与数据校验
│   │   ├── OCRService / OCRLogService  # OCR 识别与日志同步
│   │   ├── PdfToImageService           # PDF 转图片
│   │   ├── OpenCVService               # 图像增强
│   │   └── DataPersistenceService      # 数据持久化接口（需自行实现）
│   ├── processor/                      # 文件解析器
│   │   ├── CSVProcessor                # CSV 解析（含引号处理）
│   │   ├── ExcelProcessor              # Excel 解析（Apache POI）
│   │   ├── PDFProcessor                # PDF → 图片 → OCR → 入库
│   │   └── WaxProcessor                # 蜡纸图片 → OpenCV增强 → OCR
│   ├── mapper/                         # MyBatis-Plus 数据访问接口
│   ├── pojo/                           # 实体类（维度/事实/元数据等）
│   ├── config/                         # 安全配置 / CORS / 异常处理
│   ├── filter/                         # JWT 认证过滤器
│   └── util/                           # JWT 工具类
└── src/main/python/
    ├── ocr/ocr.py                      # PaddleOCR 表格识别
    ├── pdf2image/pdf2image.py          # PDF → PNG
    └── openCV/opencv.py                # 图像预处理增强

scau_archive-frontend/                  # 前端 Vue 3
├── src/
│   ├── views/                          # 页面
│   │   ├── dashboard                   # 数据看板
│   │   ├── archive/ArchiveUpload       # 档案上传
│   │   ├── analysis                    # 数据分析
│   │   ├── charts                      # 图表可视化
│   │   ├── data                        # 数据管理
│   │   ├── governance                  # 数据治理
│   │   ├── ocr/OCRProcess              # OCR 处理
│   │   ├── prediction                  # AI 预测
│   │   ├── report                      # 报表
│   │   └── system/MetaDataManage       # 系统设置 / 元数据管理
│   ├── components/                     # 组件
│   │   ├── common/ (TableView, UploadPanel, Loading, Empty)
│   │   └── layout/ (AppLayout, Header, Sidebar, Content)
│   ├── api/                            # Axios API 接口
│   ├── store/                          # Pinia 状态管理（user, archive, menu, metadata）
│   └── router/                         # 路由配置（JWT 守卫）
└── package.json
```

## 快速开始

### 后端

> **注意**: 需 JDK 17+，构建时需指定 JAVA_HOME。

```bash
cd scau-archive-insight
JAVA_HOME="D:/java/jdk-21.0.5" ./mvnw clean package     # 构建
JAVA_HOME="D:/java/jdk-21.0.5" ./mvnw spring-boot:run   # 启动（端口 8080）
JAVA_HOME="D:/java/jdk-21.0.5" ./mvnw test              # 运行测试
```

### 前端

```bash
cd scau_archive-frontend
npm install                   # 安装依赖
npm run dev                   # 启动开发服务器（端口 5173）
npm run build                 # 生产构建
npm run preview               # 预览生产构建
```

### Python 脚本（Windows venv）

```bash
cd scau-archive-insight
.venv/Scripts/python.exe src/main/python/ocr/ocr.py <图片路径>
.venv/Scripts/python.exe src/main/python/pdf2image/pdf2image.py <PDF路径>
.venv/Scripts/python.exe src/main/python/opencv/opencv.py <图片路径>
```

## 核心功能

- **档案上传**: 支持 CSV、Excel、PDF 及图片（OCR/蜡纸）批量上传，按类型（入学/毕业）自动分流处理
- **OCR 识别**: 基于 PaddleOCR 的列对齐表格识别，输出结构化的 JSON 数据（字段编码为键）
- **元数据治理**: 自定义字段映射、类型校验、清洗规则，驱动全流程数据标准化
- **数据看板**: 招生趋势、地理分布、专业流向等多维度 ECharts 可视化
- **AI 预测**: 基于历史数据的招生与就业趋势预测
- **存储监控**: 实时监控临时 / 归档 / 失败目录状态
- **用户管理**: JWT 认证 + 图形验证码 + BCrypt 密码 + 密码修改
- **安全防护**: IP+用户登录频率限制（8 次/10 分钟）、验证码请求限流（30 次/分钟）

## 文件上传处理流程

1. 接收 multipart 文件 + 类型参数（pdf / wax / ocr / excel / csv）+ 档案类型（admission / graduation）
2. 文件保存至 `storage/temp/{yyyyMMdd}/{type}/`
3. 根据格式分发至对应处理器：
   - **CSV / Excel** → 解析为行记录 → `MetaDataMappingService` 字段映射 + 校验 → `DataPersistenceService` 持久化 → 归档
   - **PDF** → PyMuPDF 转图片 → PaddleOCR 逐页识别 → 结构化数据 → 持久化 → 归档
   - **图片（wax / ocr）** → OpenCV 增强 → PaddleOCR 识别 → 持久化 → 归档
4. 处理失败：移至 `storage/failed/` 并生成 `.error.json` 错误日志
5. OCR 日志每日自动同步归档与失败目录数据至 `ocr_log_dim` 表

## 元数据驱动数据清洗

- `metadata_standard` 表定义字段编码、名称、类型、来源字段、转换规则、是否必填
- 字段匹配优先级：`fieldCode` > `fieldName` > `sourceField`
- 支持类型自动转换：int / decimal / boolean / date
- OCR 处理时，元数据规则以临时 JSON 文件传递给 Python 脚本，输出以 `fieldCode` 为键

## 数据库

- **维度表**: student_dim, college_dim, major_dim, class_dim, province_dim, nation_dim, political_dim, degree_dim, destination_dim, source_type_dim, archive_file_dim, ocr_log_dim, quality_score_dim
- **事实表**: student_fact, admission_fact, graduation_fact
- **元数据表**: metadata_standard（fieldCode 主键）
- 日期字段使用 `date` 类型直接存储（已移除 date_dim 表及 FK 关联）
