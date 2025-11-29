# 构建和推送到阿里云 - 手动操作步骤

## 前提条件

确保已安装：
- Java 17+
- Maven 3.6+
- Docker

## 步骤 1：使用 Maven 构建 JAR 文件

在项目根目录执行：

```bash
mvn clean package -DskipTests
```

**等待构建完成**，你会看到：
```
[INFO] BUILD SUCCESS
```

构建成功后会生成文件：`target/one-agent-4j-storage-0.0.1-SNAPSHOT.jar`

---

## 步骤 2：构建 Docker 镜像

```bash
docker build -t registry.cn-hangzhou.aliyuncs.com/jqiang-test/s3-file-nexus:latest .
```

**等待构建完成**，你会看到：
```
Successfully built xxx
Successfully tagged registry.cn-hangzhou.aliyuncs.com/jqiang-test/s3-file-nexus:latest
```

---

## 步骤 3：登录阿里云容器镜像服务

```bash
docker login --username=wangjq.email@qq.com registry.cn-hangzhou.aliyuncs.com
```

输入密码（密码获取地址：https://cr.console.aliyun.com/）

登录成功会显示：
```
Login Succeeded
```

---

## 步骤 4：推送镜像到阿里云

```bash
docker push registry.cn-hangzhou.aliyuncs.com/jqiang-test/s3-file-nexus:latest
```

**等待推送完成**，你会看到上传进度条，最后显示镜像的 digest。

---

## 步骤 5：验证推送成功

```bash
docker pull registry.cn-hangzhou.aliyuncs.com/jqiang-test/s3-file-nexus:latest
```

能成功拉取即表示推送成功。

---

## 如果要打多个版本标签

```bash
# 打标签（例如 1.0.0 版本）
docker tag registry.cn-hangzhou.aliyuncs.com/jqiang-test/s3-file-nexus:latest \
           registry.cn-hangzhou.aliyuncs.com/jqiang-test/s3-file-nexus:1.0.0

# 推送指定版本
docker push registry.cn-hangzhou.aliyuncs.com/jqiang-test/s3-file-nexus:1.0.0
```

---

## 常见问题

### Maven 构建失败
- 检查 Java 版本：`java -version`（需要 17+）
- 检查 Maven 版本：`mvn -version`（需要 3.6+）

### Docker 构建失败提示找不到 JAR
- 确保步骤 1 已成功完成
- 检查 JAR 是否存在：`ls -lh target/*.jar`

### 推送失败
- 确保已登录：重新执行步骤 3
- 检查镜像名称是否正确：`docker images | grep s3-file-nexus`

---

## 完整命令汇总（依次执行）

```bash
# 1. 构建 JAR
mvn clean package -DskipTests

# 2. 构建镜像
docker build -t registry.cn-hangzhou.aliyuncs.com/jqiang-test/s3-file-nexus:latest .

# 3. 登录阿里云
docker login --username=wangjq.email@qq.com registry.cn-hangzhou.aliyuncs.com

# 4. 推送镜像
docker push registry.cn-hangzhou.aliyuncs.com/jqiang-test/s3-file-nexus:latest
```
