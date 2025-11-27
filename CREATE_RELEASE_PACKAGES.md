# 创建发布包指南

## 📦 发布包类型

为了满足不同用户需求，我们提供以下几种发布包：

### 1. 基础JAR包
- **文件名**: `s3-file-nexus-1.0.0.jar`
- **适合**: 有Java环境的开发者
- **大小**: ~50MB

### 2. Windows完整包
- **文件名**: `s3-file-nexus-1.0.0-windows.zip`
- **包含**: JAR + 启动脚本 + 配置示例
- **适合**: Windows用户
- **大小**: ~50MB

### 3. Docker镜像
- **仓库**: `yourusername/s3-file-nexus:1.0.0`
- **适合**: 有Docker环境的用户
- **大小**: ~200MB

### 4. Docker Compose包
- **文件名**: `s3-file-nexus-1.0.0-compose.zip`
- **包含**: docker-compose.yml + 配置文件
- **适合**: 想要完整环境的用户
- **大小**: ~10MB

---

## 🔨 创建步骤

### 准备工作

```bash
# 创建发布目录
mkdir releases
cd releases
```

---

### 包1: 基础JAR包

```bash
# 构建JAR
cd ..
mvn clean package -DskipTests

# 复制到发布目录
copy target\one-agent-4j-storage-0.0.1-SNAPSHOT.jar releases\s3-file-nexus-1.0.0.jar
```

**创建README**:

```markdown
# S3 File Nexus v1.0.0

## 快速启动

java -jar s3-file-nexus-1.0.0.jar --spring.profiles.active=storage

## 访问

http://localhost:8081/index.html

详细文档: https://github.com/yourusername/s3-file-nexus
```

---

### 包2: Windows完整包

```bash
# 创建Windows包目录
mkdir s3-file-nexus-1.0.0-windows
cd s3-file-nexus-1.0.0-windows

# 复制文件
copy ..\s3-file-nexus-1.0.0.jar .
copy ..\..\install.bat .
copy ..\..\start-demo.bat .
copy ..\..\README.md README.txt
copy ..\..\src\main\resources\init\storage.sql .
```

**创建快速开始文件** (`快速开始.txt`):

```
==========================================
  S3 File Nexus v1.0.0 - 快速开始
==========================================

1. 确保已安装 JDK 17+

2. 双击运行 install.bat

3. 等待启动完成

4. 浏览器访问: http://localhost:8081

5. 首次使用需要配置MinIO:
   - 下载MinIO: https://min.io/download
   - 或使用Docker: docker run -p 9000:9000 minio/minio server /data

详细文档: https://github.com/yourusername/s3-file-nexus

==========================================
  遇到问题？
  https://github.com/yourusername/s3-file-nexus/issues
==========================================
```

**打包**:

```bash
cd ..
powershell Compress-Archive -Path s3-file-nexus-1.0.0-windows -DestinationPath s3-file-nexus-1.0.0-windows.zip
```

---

### 包3: Docker镜像

```bash
# 构建镜像
docker build -t s3-file-nexus:1.0.0 .
docker tag s3-file-nexus:1.0.0 s3-file-nexus:latest

# 推送到Docker Hub
docker login
docker tag s3-file-nexus:1.0.0 yourusername/s3-file-nexus:1.0.0
docker tag s3-file-nexus:1.0.0 yourusername/s3-file-nexus:latest
docker push yourusername/s3-file-nexus:1.0.0
docker push yourusername/s3-file-nexus:latest

# 保存为tar文件（可选）
docker save s3-file-nexus:1.0.0 | gzip > s3-file-nexus-1.0.0-docker.tar.gz
```

---

### 包4: Docker Compose完整包

```bash
# 创建Compose包目录
mkdir s3-file-nexus-1.0.0-compose
cd s3-file-nexus-1.0.0-compose

# 复制文件
copy ..\..\docker-compose.yml .
copy ..\..\Dockerfile .
copy ..\..\src\main\resources\init\storage.sql .
copy ..\..\DOCKER_DEPLOY.md README.md
```

**创建启动脚本** (`start.bat`):

```batch
@echo off
echo Starting S3 File Nexus with Docker Compose...
docker-compose up -d
echo.
echo Services starting...
timeout /t 30 /nobreak
echo.
echo Open http://localhost:8081
echo MinIO Console: http://localhost:9001
pause
```

**创建 `.env.example`**:

```env
# Database
DB_PASSWORD=s3nexus123

# MinIO
MINIO_USER=minioadmin
MINIO_PASSWORD=minioadmin

# Application
APP_PORT=8081
MINIO_API_PORT=9000
MINIO_CONSOLE_PORT=9001
```

**打包**:

```bash
cd ..
powershell Compress-Archive -Path s3-file-nexus-1.0.0-compose -DestinationPath s3-file-nexus-1.0.0-compose.zip
```

---

## 📋 发布清单

创建完所有包后，你应该有以下文件：

```
releases/
├── s3-file-nexus-1.0.0.jar                    # 基础JAR包
├── s3-file-nexus-1.0.0-windows.zip            # Windows完整包
├── s3-file-nexus-1.0.0-docker.tar.gz          # Docker镜像文件
└── s3-file-nexus-1.0.0-compose.zip            # Docker Compose包
```

---

## 🚀 上传到GitHub Release

```bash
# 使用GitHub CLI
gh release create v1.0.0 \
  --title "v1.0.0 Phoenix - The Rise of Modern S3 Management" \
  --notes-file RELEASE_NOTES.md \
  s3-file-nexus-1.0.0.jar \
  s3-file-nexus-1.0.0-windows.zip \
  s3-file-nexus-1.0.0-docker.tar.gz \
  s3-file-nexus-1.0.0-compose.zip \
  storage.sql
```

---

## 📊 包大小估算

| 包类型 | 预估大小 | 说明 |
|--------|----------|------|
| JAR包 | ~50MB | Spring Boot应用 |
| Windows包 | ~50MB | JAR + 脚本 |
| Docker镜像(tar) | ~200MB | 完整Linux镜像 |
| Compose包 | ~10MB | 配置文件 |
| 总计 | ~310MB | 所有包 |

---

## 🎯 发布顺序建议

1. **构建JAR** → 最基础的包
2. **创建Windows包** → Windows用户友好
3. **构建Docker镜像** → 推送到Docker Hub
4. **创建Compose包** → 最完整的体验
5. **测试所有包** → 确保可用
6. **上传到GitHub** → 正式发布

---

## ✅ 测试检查清单

### JAR包测试
- [ ] 能够正常启动
- [ ] 访问8081端口成功
- [ ] 页面正常显示
- [ ] 基本功能可用

### Windows包测试
- [ ] install.bat正常运行
- [ ] 自动打开浏览器
- [ ] 页面可访问
- [ ] 脚本无错误

### Docker镜像测试
- [ ] 镜像构建成功
- [ ] 容器启动正常
- [ ] 健康检查通过
- [ ] 应用可访问

### Compose包测试
- [ ] 所有服务启动
- [ ] 服务间通信正常
- [ ] MinIO可访问
- [ ] MySQL正常工作
- [ ] 应用完整功能可用

---

## 📝 Release说明模板

每个包都应该在Release说明中明确说明：

```markdown
## 📦 下载

根据你的需求选择合适的包：

### 🟢 推荐：Docker Compose (开箱即用)
- **s3-file-nexus-1.0.0-compose.zip** (10MB)
- 包含完整环境（应用+MinIO+MySQL）
- 一条命令启动：`docker-compose up -d`
- 最佳体验，零配置

### 🐳 Docker镜像
- **Docker Hub**: `docker pull yourusername/s3-file-nexus:1.0.0`
- **离线包**: s3-file-nexus-1.0.0-docker.tar.gz (200MB)

### 💻 Windows完整包
- **s3-file-nexus-1.0.0-windows.zip** (50MB)
- 双击安装，自动启动
- 适合Windows用户

### ☕ Java JAR包
- **s3-file-nexus-1.0.0.jar** (50MB)
- 需要JDK 17+
- 适合开发者

### 🗄️ 数据库脚本
- **storage.sql** - MySQL初始化脚本
```

---

<div align="center">

## 🎉 发布包准备完成！

按照这个指南创建所有包，让用户可以选择最适合他们的方式！

</div>
