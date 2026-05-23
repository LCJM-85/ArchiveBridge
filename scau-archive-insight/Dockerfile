# ========== 构建阶段：编译 Spring Boot JAR ==========
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /build
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn package -DskipTests -B

# ========== 运行阶段 ==========
FROM eclipse-temurin:21-jre
WORKDIR /app

# 1. 安装 Python 3 + pip + 基础依赖
RUN apt-get update && apt-get install -y --no-install-recommends \
    python3 python3-pip python3-venv \
    && rm -rf /var/lib/apt/lists/*

# 2. 创建 Python 虚拟环境（与 Java 代码中的路径一致）
RUN mkdir -p src/main/python/.venv/Scripts
RUN python3 -m venv /app/.venv && \
    ln -sf /app/.venv/bin/python3 src/main/python/.venv/Scripts/python.exe && \
    ln -sf /app/.venv/bin/pip3 src/main/python/.venv/Scripts/pip3.exe

# 3. 安装 Python 依赖
COPY src/main/python/requirements.txt /tmp/
RUN /app/.venv/bin/pip3 install --no-cache-dir -r /tmp/requirements.txt

# 4. 复制 Python 脚本
COPY src/main/python ./src/main/python

# 5. 创建 models 目录（PaddlePaddle 模型缓存）
RUN mkdir -p models/.paddlex
ENV HOME=/app/models
ENV USERPROFILE=/app/models

# 6. 复制 JAR
COPY --from=builder /build/target/*.jar ./app.jar

# 7. 创建存储目录
RUN mkdir -p storage/temp storage/archive storage/failed storage/enhance

# 8. 启动
CMD ["java", "-jar", "app.jar"]
