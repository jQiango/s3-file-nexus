# S3 File Nexus - 使用说明

## 快速开始

### 前提条件

- 已安装 Docker 和 Docker Compose

### 启动应用

1. 下载 `docker-compose.yml` 文件

2. 在文件所在目录执行：
   ```bash
   docker-compose up -d
   ```

3. 访问应用：**http://localhost:8081**

4. 在首页配置你的 S3 存储信息：
   - S3 端点地址
   - Access Key
   - Secret Key
   - 区域
   - 存储桶名称

### 停止应用

```bash
docker-compose down
```

### 查看日志

```bash
docker-compose logs -f
```

### 重启应用

```bash
docker-compose restart
```

## 支持的存储服务

- AWS S3
- MinIO
- 阿里云 OSS
- 腾讯云 COS
- 华为云 OBS
- 其他 S3 兼容存储

## 端口说明

- **8081** - Web 应用访问端口

## 常见问题

### 无法拉取镜像

如果提示无法拉取镜像，请配置 Docker 镜像加速器或联系管理员获取镜像访问权限。

### 修改端口

编辑 `docker-compose.yml`，修改端口映射：
```yaml
ports:
  - "你的端口:8081"
```

然后重新启动：
```bash
docker-compose down
docker-compose up -d
```

## 技术支持

如有问题，请访问项目仓库提交 Issue。
