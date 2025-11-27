# S3 File Nexus

<div align="center">

[![GitHub release](https://img.shields.io/github/release/yourusername/s3-file-nexus.svg)](https://github.com/yourusername/s3-file-nexus/releases)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.8-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Vue.js](https://img.shields.io/badge/Vue.js-3.x-42b883.svg)](https://vuejs.org/)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

**现代化 S3 对象存储管理系统**

一个美观、强大的 Web 文件管理器，支持所有 S3 兼容存储
</div>

## 🚀 快速开始

### 启动应用

```bash
# Windows
start.bat

# Linux/Mac
./start.sh

# 或手动启动
mvn spring-boot:run -Dspring-boot.run.profiles=storage
```
启动后访问: **http://localhost:8081**

## 🔐 支持的存储服务 
✅ 支持所有 S3 协议 

---

## 🔧 配置说明

### 应用配置

编辑 `src/main/resources/application-storage.yml`:

```yaml
server:
  port: 8081

spring:
  servlet:
    multipart:
      max-file-size: 100MB

storage:
  upload:
    max-file-size: 104857600  # 100MB
    temp-dir: /tmp/storage

  preview:
    enabled: true
    url-expiration: 3600  # 1小时

  cache:
    enabled: true
    expiration: 300  # 5分钟
    max-entries: 1000
```

### 存储配置 (MinIO 示例)

```yaml
type: S3
endpoint: http://localhost:9000
region: us-east-1
access-key-id: minioadmin
access-key-secret: minioadmin
default-bucket: test-bucket
```


## 📚 文档

- **[CLAUDE.md](CLAUDE.md)** - 开发指南和架构说明
- **[快速开始](QUICKSTART.md)** - 3 分钟快速上手
- **[Docker 部署](DOCKER_DEPLOY.md)** - Docker 部署指南
- **[发布指南](执行手册.md)** - 发布流程说明

---


**Windows 路径问题？**
```yaml
storage:
  upload:
    temp-dir: C:/Temp/storage
  cache:
    cache-dir: C:/Temp/storage-cache
```

---



## 📄 开源协议

本项目采用 [MIT License](LICENSE) 开源协议

---


**⭐ 如果这个项目对你有帮助，请给个 Star 支持一下！⭐**
