# ========== 构建阶段：编译 Spring Boot JAR ==========
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /build
# 阿里云 Maven 镜像（国内加速）
RUN mkdir -p /root/.m2 && \
    echo '<settings><mirrors><mirror><id>aliyun</id><mirrorOf>central</mirrorOf><name>Aliyun Mirror</name><url>https://maven.aliyun.com/repository/central</url></mirror></mirrors></settings>' \
    > /root/.m2/settings.xml
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn package -DskipTests -B

# ========== 运行阶段 ==========
# 用 eclipse-temurin 提供 Java 21 运行时
FROM eclipse-temurin:21-jre AS java-provider
FROM python:3.12-slim
WORKDIR /app

# 1. 从 java-provider 复制 Java 21 运行时
COPY --from=java-provider /opt/java/openjdk /opt/java/openjdk
ENV JAVA_HOME=/opt/java/openjdk
ENV PATH=$JAVA_HOME/bin:$PATH

# 2. 安装字体（验证码需要）—— 使用阿里云国内镜像，并移除 deb822 格式源避免重复配置
RUN rm -f /etc/apt/sources.list.d/debian.sources && \
    echo 'deb https://mirrors.aliyun.com/debian/ trixie main' > /etc/apt/sources.list && \
    echo 'deb https://mirrors.aliyun.com/debian/ trixie-updates main' >> /etc/apt/sources.list && \
    echo 'deb https://mirrors.aliyun.com/debian-security/ trixie-security main' >> /etc/apt/sources.list && \
    apt-get update && apt-get install -y --no-install-recommends \
    fonts-dejavu-core fontconfig \
    && rm -rf /var/lib/apt/lists/*

# 3. 先复制 Python 脚本（.dockerignore 已排除 .venv，避免覆盖软链接）
COPY src/main/python ./src/main/python

# 4. 创建 Python 虚拟环境（直接在 Java 代码预期的路径，避免软链接 sys.path 问题）
RUN python3 -m venv /app/src/main/python/.venv && \
    mkdir -p /app/src/main/python/.venv/Scripts && \
    ln -sf /app/src/main/python/.venv/bin/python3 /app/src/main/python/.venv/Scripts/python.exe && \
    ln -sf /app/src/main/python/.venv/bin/pip3 /app/src/main/python/.venv/Scripts/pip3.exe

# 5. 安装 Python 依赖 —— pip 使用阿里云镜像源（paddlepaddle 官方 CPU 站保留为 extra-index）
COPY src/main/python/requirements.txt /tmp/
RUN /app/src/main/python/.venv/bin/pip3 install --no-cache-dir -i https://mirrors.aliyun.com/pypi/simple/ --upgrade pip && \
    /app/src/main/python/.venv/bin/pip3 install --no-cache-dir -i https://mirrors.aliyun.com/pypi/simple/ \
    --extra-index-url https://www.paddlepaddle.org.cn/packages/stable/cpu/ \
    paddlepaddle==3.0.0 && \
    /app/src/main/python/.venv/bin/pip3 install --no-cache-dir -i https://mirrors.aliyun.com/pypi/simple/ -r /tmp/requirements.txt

# 6. 创建 models 目录（PaddlePaddle 模型缓存）
RUN mkdir -p models/.paddlex
ENV HOME=/app/models
ENV USERPROFILE=/app/models

# 7. 复制 JAR
COPY --from=builder /build/target/*.jar ./app.jar

# 8. 创建存储目录
RUN mkdir -p storage/temp storage/archive storage/failed storage/enhance

# 9. 启动
CMD ["java", "-jar", "app.jar"]
