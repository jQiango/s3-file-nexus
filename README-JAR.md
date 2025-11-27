# S3 File Nexus v1.0.0 - JAR包使用说明

## 🚀 快速开始

### 环境要求
- JDK 17 或更高版本
- 一个S3兼容的对象存储（如MinIO）

### 启动应用

```bash
java -jar s3-file-nexus-1.0.0.jar --spring.profiles.active=storage
```

### 访问应用

打开浏览器访问：**http://localhost:8081/index.html**

---

## ⚙️ 配置MinIO（首次使用）

### 选项1: 使用Docker快速启动MinIO

```bash
docker run -d \
  -p 9000:9000 \
  -p 9001:9001 \
  --name minio \
  -e "MINIO_ROOT_USER=minioadmin" \
  -e "MINIO_ROOT_PASSWORD=minioadmin" \
  minio/minio server /data --console-address ":9001"
```

访问MinIO控制台：http://localhost:9001
- 用户名: minioadmin
- 密码: minioadmin

### 选项2: 下载MinIO

访问 https://min.io/download 下载适合你系统的版本。

---

## 🗄️ 配置数据库（可选）

如需持久化存储配置：

1. **创建数据库**:
```sql
CREATE DATABASE one_agent_4j DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

2. **导入表结构**:
```bash
mysql -u root -p one_agent_4j < storage.sql
```

3. **修改配置**:
创建 `application-storage.yml` 文件并配置数据库连接。

---

## 🔧 自定义配置

### 修改端口

```bash
java -jar s3-file-nexus-1.0.0.jar --server.port=8082
```

### 指定配置文件

```bash
java -jar s3-file-nexus-1.0.0.jar --spring.config.location=./application.yml
```

### 调整内存

```bash
java -Xms512m -Xmx2048m -jar s3-file-nexus-1.0.0.jar
```

---

## 📚 文档

- 完整文档: https://github.com/yourusername/s3-file-nexus
- 快速开始: https://github.com/yourusername/s3-file-nexus/blob/master/QUICKSTART.md
- 问题反馈: https://github.com/yourusername/s3-file-nexus/issues

---

## 💡 常见问题

**Q: 端口被占用怎么办？**
A: 使用 `--server.port=其他端口` 参数修改端口。

**Q: 如何停止应用？**
A: 按 `Ctrl+C` 停止应用。

**Q: 如何后台运行？**
A:
```bash
# Windows
start /b java -jar s3-file-nexus-1.0.0.jar > logs.txt 2>&1

# Linux
nohup java -jar s3-file-nexus-1.0.0.jar > logs.txt 2>&1 &
```

---

🔥 **Like a Phoenix, Rising to Excellence** 🔥
