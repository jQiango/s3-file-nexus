# Docker 部署指南

## 🐳 快速开始

### 前置要求

- Docker 20.10+
- Docker Compose 2.0+

### 一键启动

```bash
# 克隆项目
git clone https://github.com/yourusername/s3-file-nexus.git
cd s3-file-nexus

# 启动所有服务
docker-compose up -d

# 查看日志
docker-compose logs -f
```

就这么简单！🎉

---

## 📋 启动后访问

启动完成后，访问以下地址：

| 服务 | 地址 | 说明 |
|------|------|------|
| **S3 File Nexus** | http://localhost:8081 | 主应用 |
| **MinIO Console** | http://localhost:9001 | 对象存储管理 |
| **MySQL** | localhost:3306 | 数据库 |

### 默认凭证

**MinIO**:
- 用户名: `minioadmin`
- 密码: `minioadmin`

**MySQL**:
- 用户名: `root`
- 密码: `s3nexus123`
- 数据库: `s3_nexus`

---

## 🎯 快速体验步骤

### 1. 启动服务
```bash
docker-compose up -d
```

等待所有服务启动（约30-60秒）。

### 2. 访问应用
打开浏览器：http://localhost:8081/index.html

### 3. 配置存储（首次使用）

应用已预配置MinIO：
- Endpoint: http://minio:9000
- Access Key: minioadmin
- Secret Key: minioadmin

### 4. 创建存储桶

访问MinIO Console：http://localhost:9001
1. 登录（minioadmin/minioadmin）
2. 点击 "Create Bucket"
3. 输入名称：`demo-bucket`
4. 点击 "Create"

### 5. 开始使用

回到S3 File Nexus，开始上传和管理文件！

---

## 🔧 常用命令

### 启动服务
```bash
docker-compose up -d
```

### 停止服务
```bash
docker-compose down
```

### 重启服务
```bash
docker-compose restart
```

### 查看日志
```bash
# 所有服务日志
docker-compose logs -f

# 特定服务日志
docker-compose logs -f s3-nexus
docker-compose logs -f minio
docker-compose logs -f mysql
```

### 查看状态
```bash
docker-compose ps
```

### 清理数据（危险！）
```bash
# 停止并删除所有容器、网络、卷
docker-compose down -v
```

---

## 🔍 健康检查

### 检查服务状态
```bash
docker-compose ps
```

应该看到所有服务状态为 `healthy` 或 `running`。

### 手动健康检查
```bash
# S3 File Nexus
curl http://localhost:8081/actuator/health

# MinIO
curl http://localhost:9000/minio/health/live

# MySQL
docker-compose exec mysql mysqladmin ping -h localhost -u root -ps3nexus123
```

---

## 🛠️ 高级配置

### 修改端口

编辑 `docker-compose.yml`：

```yaml
services:
  s3-nexus:
    ports:
      - "8082:8081"  # 修改为8082
```

### 修改数据库密码

编辑 `docker-compose.yml`：

```yaml
environment:
  - SPRING_DATASOURCE_PASSWORD=your_new_password
  - MYSQL_ROOT_PASSWORD=your_new_password
```

### 使用外部MinIO

如果你已有MinIO服务：

```yaml
environment:
  - STORAGE_BACKENDS_MINIO_ENDPOINT=http://your-minio:9000
  - STORAGE_BACKENDS_MINIO_ACCESS_KEY_ID=your_key
  - STORAGE_BACKENDS_MINIO_ACCESS_KEY_SECRET=your_secret
```

并注释掉MinIO服务：

```yaml
# minio:
#   ...
```

---

## 📊 监控和日志

### 实时日志监控
```bash
docker-compose logs -f --tail=100
```

### 查看资源使用
```bash
docker stats
```

### 导出日志
```bash
docker-compose logs > logs.txt
```

---

## 🔐 生产环境建议

### 1. 修改默认密码

```yaml
environment:
  # MySQL
  - MYSQL_ROOT_PASSWORD=${DB_PASSWORD}

  # MinIO
  - MINIO_ROOT_USER=${MINIO_USER}
  - MINIO_ROOT_PASSWORD=${MINIO_PASSWORD}
```

创建 `.env` 文件：
```env
DB_PASSWORD=your_secure_password
MINIO_USER=your_admin_user
MINIO_PASSWORD=your_secure_password
```

### 2. 使用卷挂载

```yaml
volumes:
  - ./data:/app/data
  - ./logs:/app/logs
  - ./mysql-data:/var/lib/mysql
  - ./minio-data:/data
```

### 3. 启用HTTPS

使用nginx反向代理：

```nginx
server {
    listen 443 ssl;
    server_name your-domain.com;

    ssl_certificate /path/to/cert.pem;
    ssl_certificate_key /path/to/key.pem;

    location / {
        proxy_pass http://localhost:8081;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

### 4. 限制资源

```yaml
services:
  s3-nexus:
    deploy:
      resources:
        limits:
          cpus: '2'
          memory: 2G
        reservations:
          cpus: '0.5'
          memory: 512M
```

---

## 🐛 故障排查

### 服务无法启动

1. **检查端口占用**:
```bash
netstat -ano | findstr :8081
netstat -ano | findstr :9000
netstat -ano | findstr :3306
```

2. **查看日志**:
```bash
docker-compose logs
```

3. **重新构建**:
```bash
docker-compose down
docker-compose build --no-cache
docker-compose up -d
```

### 无法连接MinIO

1. **检查MinIO健康状态**:
```bash
docker-compose exec minio curl http://localhost:9000/minio/health/live
```

2. **查看MinIO日志**:
```bash
docker-compose logs minio
```

3. **重启MinIO**:
```bash
docker-compose restart minio
```

### 数据库连接失败

1. **检查MySQL状态**:
```bash
docker-compose exec mysql mysqladmin ping -h localhost -u root -ps3nexus123
```

2. **查看MySQL日志**:
```bash
docker-compose logs mysql
```

3. **重新初始化数据库**:
```bash
docker-compose down -v
docker-compose up -d
```

---

## 📦 数据备份和恢复

### 备份数据

```bash
# 备份MySQL数据
docker-compose exec mysql mysqldump -u root -ps3nexus123 s3_nexus > backup.sql

# 备份MinIO数据
docker-compose exec minio mc mirror /data ./minio-backup
```

### 恢复数据

```bash
# 恢复MySQL数据
docker-compose exec -T mysql mysql -u root -ps3nexus123 s3_nexus < backup.sql

# 恢复MinIO数据
docker-compose exec minio mc mirror ./minio-backup /data
```

---

## 🔄 更新应用

### 更新到新版本

```bash
# 拉取最新代码
git pull

# 重新构建并启动
docker-compose down
docker-compose build --no-cache
docker-compose up -d
```

---

## 🎓 最佳实践

1. ✅ **定期备份数据**
2. ✅ **使用环境变量管理密码**
3. ✅ **监控日志和性能**
4. ✅ **定期更新镜像**
5. ✅ **限制资源使用**
6. ✅ **配置健康检查**
7. ✅ **使用HTTPS**
8. ✅ **设置防火墙规则**

---

## 📞 获取帮助

遇到问题？

- 📖 查看 [README.md](README.md)
- 🐛 提交 [Issue](https://github.com/yourusername/s3-file-nexus/issues)
- 💬 参与 [Discussions](https://github.com/yourusername/s3-file-nexus/discussions)

---

<div align="center">

## 🎉 享受使用 S3 File Nexus！

🔥 **Like a Phoenix, Rising to Excellence** 🔥

[Homepage](https://github.com/yourusername/s3-file-nexus) • [Documentation](README.md)

</div>
