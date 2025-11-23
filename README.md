# S3 文件管理系统

> 基于 AWS S3 协议的现代化对象存储文件管理系统

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.8-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Vue.js](https://img.shields.io/badge/Vue.js-3.3-brightgreen.svg)](https://vuejs.org/)
[![Element Plus](https://img.shields.io/badge/Element%20Plus-2.4.4-brightgreen.svg)](https://element-plus.org/)

## 📖 项目简介

这是一个简洁易用的S3对象存储文件管理系统，提供现代化的Web界面，方便团队内部快速管理存储在S3兼容存储服务上的文件。

### ✨ 核心特性

- 🎨 **现代化UI** - Vue 3 + Element Plus，蓝白配色，简洁美观
- 📁 **文件管理** - 上传、下载、删除、搜索、创建文件夹
- 📄 **智能排序** - 文件夹永远在最上面，便于导航
- 📊 **分页加载** - 默认每页100条，避免大量文件时卡顿
- 🔄 **Bucket切换** - 轻松切换不同的存储桶
- 🔍 **文件搜索** - 快速查找文件
- 📦 **批量操作** - 支持批量上传和批量删除
- 🚀 **拖拽上传** - 直接拖拽文件到页面即可上传
- 🧭 **面包屑导航** - 快速返回上级目录
- 💾 **无数据库** - 纯配置文件管理，部署简单

---

## 🚀 快速开始

### 环境要求

- JDK 17+
- Maven 3.6+
- 浏览器（Chrome、Firefox、Edge等现代浏览器）

### 5分钟快速启动

#### 1. 配置S3存储信息

编辑 `src/main/resources/application-storage.yml`：

```yaml
storage:
  default-backend: lianjia-s3
  backends:
    lianjia-s3:
      name:
      type: S3
      endpoint:   # 你的S3服务地址
      access-key-id: YOUR_ACCESS_KEY         # 替换为你的Access Key
      access-key-secret: YOUR_SECRET_KEY     # 替换为你的Secret Key
      region: ap-beijing
      default-bucket: your-bucket-name       # 默认存储桶
      enabled: true
```

#### 2. 启动应用

**Windows:**
```bash
start.bat
```

**Linux/Mac:**
```bash
./start.sh
```

#### 3. 访问界面

打开浏览器访问：`http://localhost:8081`

详细使用说明请查看 **[快速开始指南](QUICKSTART.md)**

---

## 📸 界面预览

```
┌─────────────────────────────────────────────────────────────┐
│  🗂 S3 文件管理系统                [选择Bucket ▼] [刷新 🔄]  │
├─────────────────────────────────────────────────────────────┤
│  [🔍 搜索]  [📁 新建文件夹]  [⬆ 上传]  [🗑 删除]           │
├─────────────────────────────────────────────────────────────┤
│  📁 首页 > 📁 文档 > 📁 2024                                │
├─────────────────────────────────────────────────────────────┤
│  ☐  类型  名称                大小      修改时间      操作   │
│  ☐  📁   项目文档             -        2024-11-24   [→]    │
│  ☐  📁   图片资源             -        2024-11-24   [→]    │
│  ☐  📄   需求文档.pdf         2.5MB    2024-11-24   [↓][✕] │
│  ☐  🖼   设计稿.png           150KB    2024-11-24   [↓][✕] │
└─────────────────────────────────────────────────────────────┘
```

---

## 📋 核心功能

| 功能 | 说明 |
|------|------|
| **文件浏览** | 文件夹优先显示，支持分页（默认100条/页） |
| **文件上传** | 点击上传或拖拽，支持批量上传 |
| **文件下载** | 单击下载按钮即可 |
| **文件删除** | 单个删除或批量删除（二次确认） |
| **文件搜索** | 关键词搜索，支持子目录 |
| **Bucket切换** | 顶部下拉框快速切换 |
| **创建文件夹** | 支持多级目录结构 |
| **面包屑导航** | 快速跳转到任意上级目录 |

详细功能说明请查看 **[功能文档](FEATURES.md)**

---

## 🔧 技术架构

### 后端
- **Spring Boot 3.4.8** - 应用框架
- **AWS SDK v2** - S3客户端
- **Java 17** - 编程语言

### 前端
- **Vue 3** - 渐进式框架
- **Element Plus** - UI组件库
- **Axios** - HTTP客户端

### API接口

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/storage/buckets` | GET | 获取存储桶列表 |
| `/api/storage/files/list` | POST | 获取文件列表 |
| `/api/storage/upload` | POST | 上传文件 |
| `/api/storage/download` | GET | 下载文件 |
| `/api/storage/files` | DELETE | 删除文件 |
| `/api/storage/files/batch` | DELETE | 批量删除 |
| `/api/storage/folder` | POST | 创建文件夹 |
| `/api/storage/search` | GET | 搜索文件 |

---

## 🔐 S3兼容性

支持所有兼容S3协议的对象存储服务：

- ✅ AWS S3
- ✅ MinIO
- ✅ 阿里云 OSS
- ✅ 腾讯云 COS
- ✅ 华为云 OBS
- ✅ Cloudflare R2
- ✅ 自建S3服务

配置示例请查看 **[S3兼容性文档](S3-COMPATIBLE.md)**

---

## ⚙️ 配置说明

### 修改端口

```yaml
server:
  port: 8081  # 修改为你想要的端口
```

### 修改文件大小限制

```yaml
storage:
  upload:
    max-file-size: 104857600  # 100MB

spring:
  servlet:
    multipart:
      max-file-size: 100MB
```

### 修改每页显示数量

前端 `app.js` 中修改：

```javascript
const pagination = ref({
    pageSize: 100,  // 改为你想要的值
    // ...
});
```

---

## 🐛 常见问题

| 问题 | 解决方案 |
|------|----------|
| 端口被占用 | 修改 `application-storage.yml` 中的端口 |
| 无法连接S3 | 检查 endpoint、access-key、网络连接 |
| 上传失败 | 检查文件大小、类型、权限 |
| 文件夹删除慢 | 文件夹中文件较多时需要时间 |

更多问题请查看 **[快速开始指南](QUICKSTART.md)**

---

## 📁 项目结构

```
one-agent-4j-storage/
├── src/main/
│   ├── java/                        # Java源码
│   │   └── com/all/in/one/agent/storage/
│   │       ├── controller/          # API控制器
│   │       ├── service/             # 业务逻辑
│   │       └── util/                # 工具类
│   └── resources/
│       ├── static/                  # 前端资源
│       │   ├── index.html           # 主页面
│       │   └── app.js               # Vue应用
│       └── application-storage.yml  # 配置文件
├── FEATURES.md                      # 功能说明
├── QUICKSTART.md                    # 快速开始
├── README.md                        # 项目说明
└── pom.xml                          # Maven配置
```

---

## 📝 版本历史

### v2.0.0 (2024-11-24) - 全新改版

#### 🎉 重大更新
- 全新的Vue 3 + Element Plus界面
- 蓝白配色，简洁美观
- 完整的文件管理功能

#### ✨ 新增功能
- 分页加载（默认100条/页）
- 拖拽上传
- 批量操作
- 文件搜索
- Bucket切换
- 面包屑导航

#### 🔧 技术优化
- 简化后端API
- 移除数据库依赖
- 纯YAML配置
- CDN加载前端资源

---

## 🤝 贡献

欢迎提交Issue和Pull Request！

### 后续计划

- [ ] 文件预览（图片、PDF、文本）
- [ ] 临时分享链接
- [ ] 文件重命名
- [ ] 文件移动/复制
- [ ] 网格视图
- [ ] 暗色主题

---

## 📄 文档

- **[功能说明文档](FEATURES.md)** - 详细的功能介绍和API文档
- **[快速开始指南](QUICKSTART.md)** - 5分钟上手教程
- **[S3兼容性文档](S3-COMPATIBLE.md)** - 各种S3服务的配置示例
- **[配置指南](CONFIG-GUIDE.md)** - 配置详解

---

## 📞 技术支持

遇到问题？请提供：
1. 错误描述
2. 控制台日志
3. 浏览器控制台错误（F12）
4. 操作步骤

---

**享受使用S3文件管理系统！** 🎉
