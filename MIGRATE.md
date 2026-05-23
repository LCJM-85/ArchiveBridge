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
| **.git/** | `.git/` | Git 版本历史，新机器不需要 |
| **docs/** | `docs/` | Claude Code 工具文件，与项目无关 |
| **.claude/** | `.claude/` | Claude Code 配置文件，不影响运行 |

## 最终拷贝清单

```
SCAU/
├── docker-compose.yml
├── README.md
├── CLAUDE.md
├── scau-archive-insight/
│   ├── Dockerfile
│   ├── pom.xml
│   ├── .mvn/
│   └── src/           ← Java + Python 源码 + 配置文件
├── scau_archive-frontend/
│   ├── Dockerfile
│   ├── nginx.conf
│   ├── package.json
│   ├── vite.config.js
│   └── src/           ← Vue 源码
├── .gitignore
└── backup.sql         ← 数据库导出文件（自行准备）
```

## 新机器部署步骤

```bash
# 1. 安装 Docker
# 2. 拷贝上述清单的文件到新机器
# 3. 在 SCAU 目录下执行
docker compose up -d
# 4. 导入数据库
docker cp backup.sql scau-db:/tmp/
docker exec scau-db psql -U postgres -d scau_archive -f /tmp/backup.sql
# 5. 访问 http://localhost
```
