# S3 File Nexus v1.0.0 - Docker使用说明

## 🚀 快速开始

### 拉取镜像

```bash
docker pull yourusername/s3-file-nexus:1.0.0
```

### 运行容器

```bash
docker run -d \
  -p 8081:8081 \
  --name s3-file-nexus \
  yourusername/s3-file-nexus:1.0.0
```

### 访问应用

打开浏览器访问：**http://localhost:8081/index.html**

---

## 🔧 高级配置

### 使用环境变量

```bash
docker run -d \
  -p 8081:8081 \
  --name s3-file-nexus \
  -e SPRING_PROFILES_ACTIVE=storage \
  -e SERVER_PORT=8081 \
  yourusername/s3-file-nexus:1.0.0
```

### 挂载数据卷

```bash
docker run -d \
  -p 8081:8081 \
  --name s3-file-nexus \
  -v $(pwd)/data:/app/data \
  -v $(pwd)/logs:/app/logs \
  -v $(pwd)/cache:/app/cache \
  yourusername/s3-file-nexus:1.0.0
```

### 连接外部MinIO

```bash
docker run -d \
  -p 8081:8081 \
  --name s3-file-nexus \
  -e STORAGE_BACKENDS_MINIO_ENDPOINT=http://your-minio:9000 \
  -e STORAGE_BACKENDS_MINIO_ACCESS_KEY_ID=your_key \
  -e STORAGE_BACKENDS_MINIO_ACCESS_KEY_SECRET=your_secret \
  yourusername/s3-file-nexus:1.0.0
```

---

## 🗄️ 配合MySQL使用

### 1. 启动MySQL容器

```bash
docker run -d \
  --name mysql \
  -e MYSQL_ROOT_PASSWORD=s3nexus123 \
  -e MYSQL_DATABASE=s3_nexus \
  -p 3306:3306 \
  mysql:8.0
```

### 2. 启动S3 File Nexus

```bash
docker run -d \
  -p 8081:8081 \
  --name s3-file-nexus \
  --link mysql:mysql \
  -e SPRING_DATASOURCE_URL=jdbc:mysql://mysql:3306/s3_nexus \
  -e SPRING_DATASOURCE_USERNAME=root \
  -e SPRING_DATASOURCE_PASSWORD=s3nexus123 \
  yourusername/s3-file-nexus:1.0.0
```

---

## 🐳 常用命令

### 查看日志

```bash
docker logs -f s3-file-nexus
```

### 停止容器

```bash
docker stop s3-file-nexus
```

### 启动容器

```bash
docker start s3-file-nexus
```

### 重启容器

```bash
docker restart s3-file-nexus
```

### 删除容器

```bash
docker rm -f s3-file-nexus
```

### 进入容器

```bash
docker exec -it s3-file-nexus sh
```

---

## 🔍 健康检查

```bash
# 检查容器状态
docker ps | grep s3-file-nexus

# 检查健康状态
docker inspect --format='{{.State.Health.Status}}' s3-file-nexus

# 测试API
curl http://localhost:8081/actuator/health
```

---

## 📊 环境变量列表

| 变量名 | 说明 | 默认值 |
|--------|------|--------|
| SPRING_PROFILES_ACTIVE | 配置文件 | storage |
| SERVER_PORT | 服务端口 | 8081 |
| SPRING_DATASOURCE_URL | 数据库URL | - |
| SPRING_DATASOURCE_USERNAME | 数据库用户名 | root |
| SPRING_DATASOURCE_PASSWORD | 数据库密码 | - |
| STORAGE_BACKENDS_MINIO_ENDPOINT | MinIO地址 | http://minio:9000 |
| STORAGE_BACKENDS_MINIO_ACCESS_KEY_ID | MinIO Key | minioadmin |
| STORAGE_BACKENDS_MINIO_ACCESS_KEY_SECRET | MinIO Secret | minioadmin |
| JAVA_OPTS | JVM参数 | -Xms512m -Xmx1024m |

---

## 💡 最佳实践

### 生产环境建议

```bash
docker run -d \
  -p 8081:8081 \
  --name s3-file-nexus \
  --restart unless-stopped \
  -v /data/s3-nexus/data:/app/data \
  -v /data/s3-nexus/logs:/app/logs \
  -v /data/s3-nexus/cache:/app/cache \
  -e JAVA_OPTS="-Xms1g -Xmx2g" \
  yourusername/s3-file-nexus:1.0.0
```

---

## 📚 更多信息

- GitHub: https://github.com/yourusername/s3-file-nexus
- Docker Hub: https://hub.docker.com/r/yourusername/s3-file-nexus
- 问题反馈: https://github.com/yourusername/s3-file-nexus/issues

---

🔥 **Like a Phoenix, Rising to Excellence** 🔥
