# S3 File Nexus - Docker Image
# 使用 Ubuntu 基础镜像（国内可访问）

FROM ubuntu:22.04

LABEL maintainer="S3 File Nexus Team"
LABEL description="Enterprise-grade S3 Object Storage Management System"
LABEL version="1.0.0"

# 使用阿里云 Ubuntu 镜像源
RUN sed -i 's/archive.ubuntu.com/mirrors.aliyun.com/g' /etc/apt/sources.list && \
    sed -i 's/security.ubuntu.com/mirrors.aliyun.com/g' /etc/apt/sources.list && \
    apt-get update && \
    apt-get install -y openjdk-17-jre-headless curl && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# Create app user
RUN groupadd -r appgroup && useradd -r -g appgroup appuser

WORKDIR /app

# Copy pre-built JAR file
COPY target/*.jar app.jar

# Create data and cache directories
RUN mkdir -p /app/data /app/cache /app/logs && \
    chown -R appuser:appgroup /app

# Switch to non-root user
USER appuser

# Expose port
EXPOSE 8081

# Environment variables
ENV SPRING_PROFILES_ACTIVE=storage
ENV SERVER_PORT=8081
ENV JAVA_OPTS="-Xms512m -Xmx1024m"

# Run application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar --spring.profiles.active=$SPRING_PROFILES_ACTIVE"]
