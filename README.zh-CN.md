# S3 File Nexus

<div align="center">

[English](README.md) | 简体中文

[![GitHub](https://img.shields.io/badge/GitHub-s3--file--nexus-blue?logo=github)](https://github.com/jQiango/s3-file-nexus)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.8-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Vue.js](https://img.shields.io/badge/Vue.js-3.4-42b883.svg)](https://vuejs.org/)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

**现代化 S3 对象存储管理系统**

一个基于 Spring Boot 和 Vue.js 构建的 Web 文件管理器，支持所有 S3 兼容的对象存储服务

[🚀 快速开始](#-快速开始) • [🔧 配置说明](#-配置说明) • [🛠️ 技术栈](#️-技术栈)

</div>

---

## 📖 项目简介

S3 File Nexus 是一个轻量级的 Web 文件管理系统，提供了类似操作系统文件管理器的用户体验。通过标准的 S3 协议与对象存储服务通信，无需额外的存储代理或中间层，可以直接管理您的云端文件。



## 🚀 快速开始

### 环境要求

- **Java** 17+
- **Maven** 3.6+
- **S3 兼容存储服务** (AWS S3, MinIO, 阿里云 OSS, 腾讯云 COS 等)

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

### 首次使用

1. 访问配置页面 `http://localhost:8081/config.html`
2. 填写 S3 存储配置信息：
   - 存储后端名称（自定义）
   - Endpoint（S3 服务地址）
   - Access Key
   - Secret Key
   - Region
   - Bucket 名称
3. 点击"测试连接"验证配置
4. 保存配置并返回首页


### 支持的存储服务

本系统基于标准 S3 协议开发，理论上支持所有兼容 S3 协议的对象存储服务。

**已测试：**

- ✅ **自有 S3 协议的 OSS** - 已验证可用

**理论支持（S3 兼容）：**

- 📦 **AWS S3** - Amazon Simple Storage Service
- 📦 **MinIO** - 开源对象存储服务
- 📦 **阿里云 OSS** - Alibaba Cloud Object Storage Service（S3 兼容模式）
- 📦 **腾讯云 COS** - Tencent Cloud Object Storage（S3 兼容模式）
- 📦 **华为云 OBS** - Huawei Cloud Object Storage Service（S3 兼容模式）
- 📦 **七牛云 Kodo** - Qiniu Cloud Object Storage（S3 兼容模式）

> 💡 **提示**：只要您的对象存储服务支持标准 S3 API，就可以使用本系统进行管理。如遇到兼容性问题，欢迎提 Issue 反馈。
>



## 📄 开源协议

本项目采用 [MIT License](LICENSE) 开源协议

## 🤝 贡献

欢迎提交 Issue 和 Pull Request！

## 💖 支持

如果这个项目对你有帮助，请给个 ⭐ Star 支持一下！

---

<div align="center">

Made with ❤️ by [jQiango](https://github.com/jQiango)

</div>
