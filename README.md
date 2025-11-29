# S3 File Nexus

<div align="center">

English | [简体中文](README.zh-CN.md)

[![GitHub](https://img.shields.io/badge/GitHub-s3--file--nexus-blue?logo=github)](https://github.com/jQiango/s3-file-nexus)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.8-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Vue.js](https://img.shields.io/badge/Vue.js-3.4-42b883.svg)](https://vuejs.org/)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

**Modern S3 Object Storage Management System**

A web-based file manager built with Spring Boot and Vue.js, supporting all S3-compatible object storage services

[🚀 Quick Start](#-quick-start) • [🔧 Configuration](#-configuration) • [🛠️ Tech Stack](#️-tech-stack)

</div>

---

## 📖 About

S3 File Nexus is a lightweight web-based file management system that provides an OS-like file manager experience. It communicates with object storage services through standard S3 protocol without requiring any additional storage proxies or middleware layers, allowing you to directly manage your cloud files.



## 🚀 Quick Start

### Requirements

- **Java** 17+
- **Maven** 3.6+
- **S3-compatible Storage Service** (AWS S3, MinIO, Alibaba Cloud OSS, Tencent Cloud COS, etc.)

### Launch Application

```bash
# Windows
start.bat

# Linux/Mac
./start.sh

# Or manually start
mvn spring-boot:run -Dspring-boot.run.profiles=storage
```

After startup, visit: **http://localhost:8081**

### First-time Setup

1. Visit the configuration page `http://localhost:8081/config.html`
2. Fill in S3 storage configuration:
   - Storage backend name (custom)
   - Endpoint (S3 service address)
   - Access Key
   - Secret Key
   - Region
   - Bucket name
3. Click "Test Connection" to verify configuration
4. Save configuration and return to homepage


### Supported Storage Services

This system is developed based on standard S3 protocol and theoretically supports all S3-compatible object storage services.

**Tested:**

- ✅ **Self-hosted S3 Protocol OSS** - Verified

**Theoretically Supported (S3-compatible):**

- 📦 **AWS S3** - Amazon Simple Storage Service
- 📦 **MinIO** - Open-source object storage service
- 📦 **Alibaba Cloud OSS** - Alibaba Cloud Object Storage Service (S3-compatible mode)
- 📦 **Tencent Cloud COS** - Tencent Cloud Object Storage (S3-compatible mode)
- 📦 **Huawei Cloud OBS** - Huawei Cloud Object Storage Service (S3-compatible mode)
- 📦 **Qiniu Cloud Kodo** - Qiniu Cloud Object Storage (S3-compatible mode)

> 💡 **Tip**: As long as your object storage service supports standard S3 API, you can use this system to manage it. If you encounter compatibility issues, please submit an Issue.
>



## 📄 License

This project is licensed under the [MIT License](LICENSE)

## 🤝 Contributing

Issues and Pull Requests are welcome!

## 💖 Support

If this project helps you, please give it a ⭐ Star!

---

<div align="center">

Made with ❤️ by [jQiango](https://github.com/jQiango)

</div>
