# Playwright 部署依赖修复记录

## 问题现象

项目的知识库 URL 导入和 AI 助手 `web_fetch` 已经调用 Playwright 渲染网页，但 Python 依赖清单没有声明 Playwright，后端 Docker 镜像也没有安装 Chromium 及其系统运行库。

在原状态下，调用上述功能会先因缺少 Python 包而报错；即使只安装 Python 包，仍会因缺少匹配的 Chromium 浏览器而无法启动。

## 原因

- `document_loader.py` 使用 `playwright.async_api` 并启动 Chromium。
- `requirements.txt` 未声明 `playwright`。
- 后端 `Dockerfile` 只安装了通用 Python 依赖，没有执行 Playwright 浏览器安装。
- Dockerfile 后续会修改 `HOME`，若沿用默认缓存目录，构建期与运行期可能查找不同的浏览器目录。

## 修复方案

1. 在 `requirements.txt` 中固定 `playwright==1.63.0`，使 Python 包与浏览器版本保持一致。
2. 在后端镜像中设置 `PLAYWRIGHT_BROWSERS_PATH=/ms-playwright`，固定构建期和运行期的浏览器位置。
3. 安装依赖后执行 `python -m playwright install --with-deps chromium`，同时安装 Chromium 和 Linux 系统依赖。
4. 在镜像构建阶段启动一次无头 Chromium，并通过本地 HTML 页面标题断言验证浏览器确实可运行。

安装方式参考 Playwright Python 官方文档：

- https://playwright.dev/python/docs/docker
- https://playwright.dev/python/docs/browsers

## 修改文件

- `scau-archive-insight/src/main/python/requirements.txt`
- `scau-archive-insight/Dockerfile`
- `README.md`

本次没有数据库结构或数据变更。

## 验证结果

- 修复前检查按预期失败：`requirements.txt` 中没有 Playwright 声明。
- 修复后静态部署检查通过：依赖版本、浏览器安装命令、固定浏览器目录和构建期启动检查均已存在。
- `docker compose config --quiet` 通过，Compose 配置可以正常解析。
- Python 测试共运行 15 项，全部通过，另有 1 项按原条件跳过。
- 本次未实际构建 Docker 镜像：本机 Docker 引擎启动耗时较长，按用户要求停止等待。Dockerfile 已包含 Chromium 启动断言，下一次正常构建镜像时会自动完成该项验证；在镜像成功构建前，不将容器运行验证记录为已通过。

## 使用说明

Docker Compose 构建后会自动安装 Playwright 和 Chromium，无需在宿主机单独安装浏览器。

本地直接运行后端时，安装 Python 依赖后还需在相同虚拟环境执行：

```powershell
python -m playwright install chromium
```
