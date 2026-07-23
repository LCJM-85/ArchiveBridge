# 环境迁移清理清单

拷贝项目到新机器前，删除以下文件/目录：

## 必须删除（迁移后重新生成）

| 目录 | 位置 | 大小 | 说明 |
|------|------|------|------|
| **models/** | `scau-archive-insight/models/` | ~1.8GB | PaddlePaddle 模型缓存，Docker 首次启动自动下载 |
| **.venv/** | `scau-archive-insight/src/main/python/.venv/` | ~500MB | Python 虚拟环境，到新机器重新 `pip install -r requirements.txt` |
| **node_modules/** | `scau_archive-frontend/node_modules/` | ~300MB | npm 包，到新机器重新 `npm install` |
| **target/** | `scau-archive-insight/target/` | ~200MB | Maven 编译产物，到新机器重新 `mvn package` |
| **__pycache__/** | `scau-archive-insight/src/main/python/**/` | 几MB | Python 字节码缓存 |

## 可选删除

| 目录 | 位置 | 说明 |
|------|------|------|
| **storage/** | `storage/` | 运行时文件存储，新机器自动创建空目录 |
| **.env** | `.env` | 环境变量（含 API Key），新机器需从 `.env.example` 重新创建 |
| **.git/** | `.git/` | Git 版本历史，新机器不需要 |

## 最终拷贝清单

```
SCAU/
├── .env.example          ← 到新机器后复制为 .env 并填入 API Key
├── .gitignore
├── docker-compose.yml
├── Dockerfile.db
├── README.md
├── CLAUDE.md
├── scau-archive-insight/
│   ├── Dockerfile
│   ├── pom.xml
│   ├── .mvn/
│   └── src/              ← Java + Python 源码 + 配置文件
├── scau_archive-frontend/
│   ├── Dockerfile
│   ├── nginx.conf
│   ├── package.json
│   ├── vite.config.js
│   └── src/              ← Vue 源码
```

## 新机器部署步骤（Docker）

```bash
# 1. 安装 Docker
# 2. 拷贝上述清单的文件到新机器
# 3. 创建环境变量配置
cp .env.example .env
# 编辑 .env，填入 GLM_API_KEY（AI 助手 + 智能提取需要）
# 4. 启动所有服务（数据库自动初始化表结构 + 种子数据）
docker compose up -d
# 5. 访问 http://localhost
```

## 本地开发部署（非 Docker，Windows）

```bash
# 后端
cd scau-archive-insight
JAVA_HOME="D:/java/jdk-21.0.5" ./mvnw spring-boot:run

# 前端（新开终端）
cd scau_archive-frontend
npm install
npm run dev

# AI 助手（可选，新开终端）
HOME="D:/Ideaworkplace/SCAU/scau-archive-insight/models" \
scau-archive-insight/src/main/python/.venv/Scripts/python.exe \
  scau-archive-insight/src/main/python/ai_assistant/main.py
```
