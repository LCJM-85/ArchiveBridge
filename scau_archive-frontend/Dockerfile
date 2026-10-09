# 基础镜像源：默认走国内加速源（DaoCloud 公共镜像）。
# 换源方式（任选其一，无需改动本文件）：
#   1) 根目录 .env 中设置 DOCKER_REGISTRY=<加速器域名>，docker compose 会自动传入；
#   2) 手动构建时 --build-arg REGISTRY=<加速器域名>；官方源用 REGISTRY=docker.io。
# 已验证可用：docker.m.daocloud.io、dockerproxy.net、hub.rat.dev
ARG REGISTRY=docker.m.daocloud.io

# ========== 构建阶段 ==========
FROM ${REGISTRY}/library/node:20-alpine AS builder
WORKDIR /build
COPY package*.json ./
# npm 使用 npmmirror 国内镜像源；同时改写 lockfile 中的 registry 主机，
# 避免不同 npm 版本的 replace-registry-host 行为差异导致回源 registry.npmjs.org
RUN npm config set registry https://registry.npmmirror.com && \
    sed -i 's|https://registry.npmjs.org|https://registry.npmmirror.com|g' package-lock.json && \
    npm ci
COPY . .
RUN npm run build

# ========== 运行阶段 ==========
FROM ${REGISTRY}/library/nginx:alpine
COPY --from=builder /build/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
