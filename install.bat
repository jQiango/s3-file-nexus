@echo off
chcp 65001 >nul
title S3 File Nexus - 一键安装程序

echo ==========================================
echo   S3 File Nexus v1.0.0 Phoenix
echo   Like a Phoenix, Rising to Excellence
echo ==========================================
echo.

:: 检查Java
echo [1/5] 检查Java环境...
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo ❌ 未检测到Java环境！
    echo.
    echo 请先安装JDK 17或更高版本
    echo 下载地址: https://adoptium.net/
    echo.
    pause
    exit /b 1
)
echo ✅ Java环境检查通过

:: 检查端口
echo.
echo [2/5] 检查端口占用...
netstat -ano | findstr :8081 >nul 2>&1
if %errorlevel% equ 0 (
    echo ⚠️  警告: 端口8081已被占用！
    echo 请关闭占用该端口的程序，或修改配置文件中的端口号
    pause
)

:: 创建目录
echo.
echo [3/5] 创建数据目录...
if not exist "data" mkdir data
if not exist "logs" mkdir logs
if not exist "cache" mkdir cache
echo ✅ 目录创建完成

:: 复制配置文件
echo.
echo [4/5] 配置应用...
if not exist "application.yml" (
    echo 创建默认配置文件...
    (
        echo server:
        echo   port: 8081
        echo.
        echo spring:
        echo   profiles:
        echo     active: storage
    ) > application.yml
)
echo ✅ 配置完成

:: 启动应用
echo.
echo [5/5] 启动应用...
echo.
echo ==========================================
echo   启动中，请稍候...
echo ==========================================
echo.

start "S3 File Nexus" java -jar s3-file-nexus-1.0.0.jar

timeout /t 5 /nobreak >nul

echo.
echo ✅ 启动成功！
echo.
echo ==========================================
echo   访问地址:
echo   http://localhost:8081/index.html
echo ==========================================
echo.
echo 按任意键打开浏览器...
pause >nul

start http://localhost:8081/index.html
