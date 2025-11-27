@echo off
chcp 65001 >nul
echo ==========================================
echo   创建 Docker Compose 发布包
echo ==========================================
echo.

:: 创建临时目录
set PACKAGE_DIR=s3-file-nexus-1.0.0-compose
if exist %PACKAGE_DIR% (
    echo 删除旧的包目录...
    rmdir /s /q %PACKAGE_DIR%
)

echo 创建包目录...
mkdir %PACKAGE_DIR%

:: 复制必需文件
echo 复制文件...
copy docker-compose.yml %PACKAGE_DIR%\
copy Dockerfile %PACKAGE_DIR%\
copy .dockerignore %PACKAGE_DIR%\
copy DOCKER_DEPLOY.md %PACKAGE_DIR%\README.md

:: 复制SQL脚本
mkdir %PACKAGE_DIR%\init
copy src\main\resources\init\storage.sql %PACKAGE_DIR%\init\

:: 创建环境变量示例
echo 创建环境变量示例...
(
echo # 数据库配置
echo MYSQL_ROOT_PASSWORD=s3nexus123
echo MYSQL_DATABASE=s3_nexus
echo.
echo # MinIO配置
echo MINIO_ROOT_USER=minioadmin
echo MINIO_ROOT_PASSWORD=minioadmin
echo.
echo # 应用端口
echo APP_PORT=8081
echo MINIO_API_PORT=9000
echo MINIO_CONSOLE_PORT=9001
) > %PACKAGE_DIR%\.env.example

:: 创建快速启动脚本
echo 创建启动脚本...
(
echo @echo off
echo chcp 65001 ^>nul
echo echo ==========================================
echo echo   启动 S3 File Nexus
echo echo ==========================================
echo echo.
echo echo [1/3] 检查Docker环境...
echo docker --version ^>nul 2^>^&1
echo if %%errorlevel%% neq 0 ^(
echo     echo ❌ Docker未安装！请先安装Docker Desktop
echo     pause
echo     exit /b 1
echo ^)
echo docker-compose --version ^>nul 2^>^&1
echo if %%errorlevel%% neq 0 ^(
echo     echo ❌ Docker Compose未安装！
echo     pause
echo     exit /b 1
echo ^)
echo echo ✅ Docker环境检查通过
echo echo.
echo echo [2/3] 启动服务...
echo docker-compose up -d
echo echo.
echo echo [3/3] 等待服务启动...
echo timeout /t 30 /nobreak ^>nul
echo echo.
echo echo ==========================================
echo echo   启动完成！
echo echo ==========================================
echo echo.
echo echo 访问地址：
echo echo   - S3 File Nexus: http://localhost:8081
echo echo   - MinIO Console: http://localhost:9001
echo echo.
echo echo MinIO 默认凭证:
echo echo   用户名: minioadmin
echo echo   密码: minioadmin
echo echo.
echo echo 按任意键打开浏览器...
echo pause ^>nul
echo start http://localhost:8081
) > %PACKAGE_DIR%\start.bat

:: 创建停止脚本
(
echo @echo off
echo echo 停止 S3 File Nexus...
echo docker-compose down
echo echo 已停止所有服务
echo pause
) > %PACKAGE_DIR%\stop.bat

:: 创建README
echo 创建说明文件...
(
echo ==========================================
echo   S3 File Nexus v1.0.0 Docker Compose
echo ==========================================
echo.
echo 快速开始:
echo.
echo 1. 确保已安装 Docker 和 Docker Compose
echo 2. 双击运行 start.bat
echo 3. 等待服务启动完成
echo 4. 访问 http://localhost:8081
echo.
echo 包含服务:
echo   - S3 File Nexus ^(主应用^)
echo   - MySQL 8.0 ^(数据库^)
echo   - MinIO ^(对象存储^)
echo.
echo 详细文档: README.md
echo.
echo ==========================================
) > %PACKAGE_DIR%\快速开始.txt

:: 打包
echo.
echo 打包中...
powershell Compress-Archive -Path %PACKAGE_DIR% -DestinationPath %PACKAGE_DIR%.zip -Force

echo.
echo ==========================================
echo   ✅ 打包完成！
echo ==========================================
echo.
echo 输出文件: %PACKAGE_DIR%.zip
echo.
pause
