@echo off
chcp 65001 >nul
title S3 File Nexus - 构建发布包

echo ==========================================
echo   S3 File Nexus v1.0.0 发布构建
echo   Like a Phoenix, Rising to Excellence
echo ==========================================
echo.

:: 检查Maven
echo [检查] Maven环境...
mvn --version >nul 2>&1
if %errorlevel% neq 0 (
    echo ❌ Maven未安装！
    pause
    exit /b 1
)
echo ✅ Maven检查通过
echo.

:: 创建releases目录
echo [准备] 创建发布目录...
if not exist releases mkdir releases
if exist releases\*.* del /q releases\*.*
echo ✅ 发布目录已准备
echo.

:: ==========================================
:: 方案1: 构建JAR包
:: ==========================================
echo ==========================================
echo   [1/3] 构建 JAR 包
echo ==========================================
echo.

echo [构建] 编译项目...
call mvn clean package -DskipTests
if %errorlevel% neq 0 (
    echo ❌ 构建失败！
    pause
    exit /b 1
)
echo ✅ 编译完成
echo.

echo [打包] 准备JAR发布包...
copy target\one-agent-4j-storage-0.0.1-SNAPSHOT.jar releases\s3-file-nexus-1.0.0.jar
copy src\main\resources\init\storage.sql releases\
copy README-JAR.md releases\README-JAR.md
echo ✅ JAR包已准备: s3-file-nexus-1.0.0.jar
echo.

:: ==========================================
:: 方案2: Docker镜像信息
:: ==========================================
echo ==========================================
echo   [2/3] Docker 镜像信息
echo ==========================================
echo.
echo 📝 Docker镜像需要手动构建和推送:
echo.
echo   docker build -t s3-file-nexus:1.0.0 .
echo   docker tag s3-file-nexus:1.0.0 yourusername/s3-file-nexus:1.0.0
echo   docker push yourusername/s3-file-nexus:1.0.0
echo.
copy README-DOCKER.md releases\
echo ✅ Docker使用说明已准备
echo.

:: ==========================================
:: 方案3: Docker Compose包
:: ==========================================
echo ==========================================
echo   [3/3] 构建 Docker Compose 包
echo ==========================================
echo.

echo [打包] 创建Compose包...
call create-compose-package.bat
if exist s3-file-nexus-1.0.0-compose.zip (
    move s3-file-nexus-1.0.0-compose.zip releases\
    echo ✅ Compose包已准备: s3-file-nexus-1.0.0-compose.zip
) else (
    echo ⚠️  Compose包创建失败
)
echo.

:: ==========================================
:: 总结
:: ==========================================
echo ==========================================
echo   ✅ 构建完成！
echo ==========================================
echo.
echo 📦 发布文件列表:
echo.
dir /b releases
echo.
echo ==========================================
echo   下一步操作:
echo ==========================================
echo.
echo 1. ✅ JAR包已准备
echo    releases\s3-file-nexus-1.0.0.jar
echo    releases\storage.sql
echo.
echo 2. 📝 构建并推送Docker镜像:
echo    docker build -t yourusername/s3-file-nexus:1.0.0 .
echo    docker push yourusername/s3-file-nexus:1.0.0
echo.
echo 3. ✅ Docker Compose包已准备
echo    releases\s3-file-nexus-1.0.0-compose.zip
echo.
echo 4. 🚀 上传到GitHub Release:
echo    gh release create v1.0.0 ^
echo      --title "v1.0.0 Phoenix" ^
echo      --notes-file RELEASE_NOTES.md ^
echo      releases\s3-file-nexus-1.0.0.jar ^
echo      releases\storage.sql ^
echo      releases\s3-file-nexus-1.0.0-compose.zip
echo.
echo ==========================================
echo.
pause
