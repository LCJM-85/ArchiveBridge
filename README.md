# SCAU 档案洞察系统

华南农业大学毕业与招生的可视化名册管理系统。支持学生档案的批量导入、OCR 识别、数据可视化分析及报表生成。

## 技术栈

### 后端
- **框架**: Spring Boot 3.5 + MyBatis-Plus
- **语言**: Java 17
- **数据库**: PostgreSQL + PostGIS
- **连接池**: Druid
- **安全**: Spring Security + JWT + 图形验证码

### 前端
- **框架**: Vue 3 (Vite 8)
- **UI**: Element Plus
- **状态管理**: Pinia
- **可视化**: ECharts
- **HTTP**: Axios

### 数据处理
- **OCR**: PaddleOCR (Python)
- **PDF 转图**: PyMuPDF (Python)
- **图像增强**: OpenCV (Python)

## 项目结构

```
scau-archive-insight/          # 后端 Spring Boot
├── src/main/java/
│   ├── controller/            # REST 控制器
│   ├── service/               # 业务逻辑层
│   ├── processor/             # 文件解析器 (CSV/Excel/PDF/图像)
│   ├── mapper/                # MyBatis-Plus 数据访问
│   ├── pojo/                  # 实体类
│   ├── config/                # 安全配置、异常处理
│   ├── filter/                # JWT 过滤器
│   └── util/                  # 工具类 (JWT)
├── src/main/python/
│   ├── ocr/ocr.py             # PaddleOCR 识别脚本
│   ├── pdf2image/pdf2image.py # PDF 转图片脚本
│   └── openCV/opencv.py       # 图像增强脚本
└── src/main/resources/
    └── application.yaml       # 应用配置

scau_archive-frontend/         # 前端 Vue 3
├── src/
│   ├── views/                 # 页面 (登录/档案/分析/图表/OCR/系统等)
│   ├── components/            # 组件 (布局/通用)
│   ├── api/                   # API 接口
│   ├── store/                 # Pinia 状态管理
│   └── router/                # 路由配置
└── package.json
```

## 快速开始

### 后端

```bash
cd scau-archive-insight
./mvnw clean package          # 构建
./mvnw spring-boot:run        # 启动 (默认端口 8080)
./mvnw test                   # 运行测试
```

### 前端

```bash
cd scau_archive-frontend
npm install                   # 安装依赖
npm run dev                   # 启动开发服务器 (默认端口 5173)
npm run build                 # 生产构建
npm run preview               # 预览生产构建
```

### Python 脚本 (需 venv 环境)

```bash
cd scau-archive-insight
.venv/Scripts/python.exe src/main/python/ocr/ocr.py <图片路径>
.venv/Scripts/python.exe src/main/python/pdf2image/pdf2image.py <PDF路径>
.venv/Scripts/python.exe src/main/python/opencv/opencv.py <图片路径>
```

## 核心功能

- **档案上传**: 支持 CSV、Excel、PDF 及图片格式的批量上传
- **OCR 识别**: 基于 PaddleOCR 自动提取学号、姓名、院系、专业、毕业证号等信息
- **数据看板**: 招生趋势、地理分布、专业流向等多维度 ECharts 可视化
- **报表生成**: 支持自定义报表导出
- **数据治理**: 元数据管理、数据质量评估与清洗
- **AI 预测**: 基于历史数据的招生与就业趋势预测
- **用户管理**: JWT 认证 + 图形验证码 + 密码管理
