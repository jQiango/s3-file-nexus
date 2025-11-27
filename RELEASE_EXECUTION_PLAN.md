# S3 File Nexus v1.0.0 - 发布执行方案

## 🎯 目标

发布三种版本：
1. ✅ 单JAR包
2. ✅ Docker镜像
3. ✅ Docker Compose完整包

---

## 📋 执行清单

### 阶段1: 本地构建 (30分钟)

#### ☑️ 步骤1: 构建所有本地包 (10分钟)

```bash
# 执行主构建脚本
build-release.bat
```

**预期输出**:
```
releases/
├── s3-file-nexus-1.0.0.jar           ✅ JAR包
├── storage.sql                        ✅ 数据库脚本
├── s3-file-nexus-1.0.0-compose.zip   ✅ Compose包
├── README-JAR.md                      ✅ JAR使用说明
└── README-DOCKER.md                   ✅ Docker使用说明
```

**检查点**:
- [ ] JAR包大小约50MB
- [ ] Compose包约10MB
- [ ] 所有README文件存在

---

#### ☑️ 步骤2: 测试JAR包 (5分钟)

```bash
# 测试JAR能否启动
cd releases
java -jar s3-file-nexus-1.0.0.jar --spring.profiles.active=storage

# 在浏览器测试
# http://localhost:8081/index.html

# 停止: Ctrl+C
cd ..
```

**检查点**:
- [ ] 应用成功启动
- [ ] 端口8081可访问
- [ ] 页面正常显示

---

#### ☑️ 步骤3: 构建Docker镜像 (10分钟)

```bash
# 构建镜像
docker build -t s3-file-nexus:1.0.0 -t s3-file-nexus:latest .

# 查看镜像
docker images | findstr s3-file-nexus

# 测试运行
docker run -d -p 8082:8081 --name test-nexus s3-file-nexus:1.0.0

# 等待启动
timeout /t 20 /nobreak

# 测试访问
# http://localhost:8082/index.html

# 清理测试
docker stop test-nexus
docker rm test-nexus
```

**检查点**:
- [ ] 镜像构建成功
- [ ] 镜像大小约200MB
- [ ] 容器启动正常
- [ ] 健康检查通过

---

#### ☑️ 步骤4: 测试Docker Compose (5分钟)

```bash
# 启动完整环境
docker-compose up -d

# 等待服务启动
timeout /t 30 /nobreak

# 查看服务状态
docker-compose ps

# 查看日志
docker-compose logs

# 测试访问
# S3 Nexus: http://localhost:8081
# MinIO: http://localhost:9001

# 清理
docker-compose down
```

**检查点**:
- [ ] 所有服务状态为healthy
- [ ] S3 Nexus可访问
- [ ] MinIO可访问
- [ ] 服务间通信正常

---

### 阶段2: 发布到远程 (20分钟)

#### ☑️ 步骤5: 推送到GitHub (5分钟)

```bash
# 确保代码已提交
git status

# 如有未提交的，先提交
git add .
git commit -m "build: 准备v1.0.0发布包"

# 推送代码
git push origin master

# 创建并推送标签
git tag -a v1.0.0 -m "Release v1.0.0 Phoenix"
git push origin v1.0.0
```

**检查点**:
- [ ] 代码已推送
- [ ] 标签v1.0.0已创建
- [ ] GitHub上可见标签

---

#### ☑️ 步骤6: 推送Docker镜像 (10分钟)

```bash
# 登录Docker Hub (替换yourusername)
docker login

# 标记镜像
docker tag s3-file-nexus:1.0.0 yourusername/s3-file-nexus:1.0.0
docker tag s3-file-nexus:1.0.0 yourusername/s3-file-nexus:latest

# 推送镜像
docker push yourusername/s3-file-nexus:1.0.0
docker push yourusername/s3-file-nexus:latest
```

**检查点**:
- [ ] 镜像推送成功
- [ ] Docker Hub上可见镜像
- [ ] latest标签已更新

---

#### ☑️ 步骤7: 创建GitHub Release (5分钟)

**方式A: 使用GitHub CLI (推荐)**

```bash
# 安装gh cli: https://cli.github.com/

# 创建Release
gh release create v1.0.0 \
  --title "v1.0.0 \"Phoenix\" - The Rise of Modern S3 Management" \
  --notes-file RELEASE_NOTES.md \
  releases/s3-file-nexus-1.0.0.jar \
  releases/storage.sql \
  releases/s3-file-nexus-1.0.0-compose.zip \
  releases/README-JAR.md \
  releases/README-DOCKER.md
```

**方式B: 使用Web界面**

1. 访问: https://github.com/yourusername/s3-file-nexus/releases/new
2. 选择标签: v1.0.0
3. Release标题: `v1.0.0 "Phoenix" - The Rise of Modern S3 Management`
4. 描述: 粘贴 RELEASE_NOTES.md 的内容
5. 上传文件:
   - s3-file-nexus-1.0.0.jar
   - storage.sql
   - s3-file-nexus-1.0.0-compose.zip
   - README-JAR.md
   - README-DOCKER.md
6. 勾选 "Set as the latest release"
7. 点击 "Publish release"

**检查点**:
- [ ] Release已创建
- [ ] 所有文件已上传
- [ ] 可以下载所有文件
- [ ] README显示正确

---

### 阶段3: 配置和验证 (10分钟)

#### ☑️ 步骤8: 配置仓库 (5分钟)

1. **About设置**:
   - 描述: `🚀 Enterprise-grade S3 object storage management system`
   - Website: (可选)
   - Topics: `s3, object-storage, file-manager, spring-boot, vue3, docker, minio`

2. **README徽章**:
   在README.md顶部已经有了，确认链接正确

3. **文档链接**:
   - README.md 链接正确
   - QUICKSTART.md 存在
   - DOCKER_DEPLOY.md 存在

**检查点**:
- [ ] About已设置
- [ ] Topics已添加
- [ ] 徽章显示正常
- [ ] 文档链接有效

---

#### ☑️ 步骤9: 验证发布 (5分钟)

**验证清单**:

1. **JAR包验证**:
   ```bash
   # 下载JAR包
   wget https://github.com/yourusername/s3-file-nexus/releases/download/v1.0.0/s3-file-nexus-1.0.0.jar

   # 测试运行
   java -jar s3-file-nexus-1.0.0.jar
   ```
   - [ ] 可以下载
   - [ ] 可以运行
   - [ ] 功能正常

2. **Docker镜像验证**:
   ```bash
   # 拉取镜像
   docker pull yourusername/s3-file-nexus:1.0.0

   # 运行测试
   docker run -d -p 8081:8081 yourusername/s3-file-nexus:1.0.0
   ```
   - [ ] 可以拉取
   - [ ] 可以运行
   - [ ] 功能正常

3. **Compose包验证**:
   ```bash
   # 下载Compose包
   wget https://github.com/yourusername/s3-file-nexus/releases/download/v1.0.0/s3-file-nexus-1.0.0-compose.zip

   # 解压并运行
   unzip s3-file-nexus-1.0.0-compose.zip
   cd s3-file-nexus-1.0.0-compose
   docker-compose up -d
   ```
   - [ ] 可以下载
   - [ ] 可以启动
   - [ ] 所有服务正常

---

## 📊 时间估算

| 阶段 | 任务 | 预估时间 |
|------|------|----------|
| 阶段1 | 本地构建和测试 | 30分钟 |
| 阶段2 | 远程发布 | 20分钟 |
| 阶段3 | 配置和验证 | 10分钟 |
| **总计** | | **60分钟** |

---

## 🎯 成功标准

### 发布完成后应该有:

1. ✅ **GitHub Release v1.0.0**:
   - JAR包可下载
   - SQL脚本可下载
   - Compose包可下载
   - README文件齐全

2. ✅ **Docker Hub**:
   - `yourusername/s3-file-nexus:1.0.0` 可用
   - `yourusername/s3-file-nexus:latest` 已更新

3. ✅ **文档完整**:
   - README.md 更新
   - 快速开始指南
   - Docker使用说明
   - 发布说明

4. ✅ **功能验证**:
   - JAR包可运行
   - Docker镜像可用
   - Compose完整可用

---

## 🐛 常见问题

### Q: 构建失败怎么办？
```bash
# 清理后重试
mvn clean
build-release.bat
```

### Q: Docker镜像推送失败？
```bash
# 检查登录
docker login

# 检查镜像标签
docker images | findstr s3-file-nexus
```

### Q: GitHub Release上传失败？
- 检查文件大小限制（单文件<2GB）
- 检查网络连接
- 使用gh cli重试

---

## 📞 下一步

发布完成后:

1. ✅ 在社交媒体宣传
2. ✅ 提交到awesome lists
3. ✅ 撰写技术博客
4. ✅ 制作演示视频
5. ✅ 收集用户反馈

---

<div align="center">

## 🎉 准备好了吗？

**执行 build-release.bat 开始构建！**

🔥 **Like a Phoenix, Rising to Excellence** 🔥

</div>
