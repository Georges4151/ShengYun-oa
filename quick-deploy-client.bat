@echo off
REM ============================================
REM CEAMS 快速部署脚本 - 客户端
REM 一键打包并创建部署包
REM ============================================

setlocal

REM 设置编码
chcp 65001 > nul 2>&1

echo ============================================
echo CEAMS 快速部署工具
echo ============================================
echo.

REM 检查管理员权限
net session >nul 2>&1
if %ERRORLEVEL% neq 0 (
    echo [提示] 建议以管理员身份运行此脚本
    echo.
)

REM 显示当前配置
echo 当前配置:
echo - 项目目录: %cd%
echo - Maven: 
where mvn >nul 2>&1
if %ERRORLEVEL% equ 0 (
    echo   [OK] 已安装
) else (
    echo   [错误] 未找到Maven
)
echo - Java:
where java >nul 2>&1
if %ERRORLEVEL% equ 0 (
    java -version 2>&1 | findstr "version"
    echo   [OK] 已安装
) else (
    echo   [错误] 未找到Java
)
echo.

REM 询问是否继续
set /p confirm="是否开始打包? (Y/N): "
if /i not "%confirm%"=="Y" (
    echo 已取消
    pause
    exit /b
)

echo.
echo [1/4] 清理旧文件...
if exist "dist-client" rd /s /q "dist-client"
mkdir dist-client

echo [2/4] 构建应用...
call mvn clean package -DskipTests

if %ERRORLEVEL% neq 0 (
    echo.
    echo [错误] 构建失败!
    echo 请检查Maven配置和依赖
    pause
    exit /b 1
)

echo.
echo [3/4] 复制应用文件...
copy "target\ceams-1.0-SNAPSHOT.jar" "dist-client\" /Y
copy "run.bat" "dist-client\" /Y

echo.
echo [4/4] 创建配置模板...
mkdir "dist-client\config"

(
    echo # CEAMS 客户端配置
    echo # 请根据实际情况修改以下配置
    echo.
    echo # 数据库连接(必填)
    echo DB_HOST=192.168.1.100
    echo DB_PORT=3306
    echo DB_NAME=ceams
    echo DB_USERNAME=ceams_client_write
    echo DB_PASSWORD=请修改密码
    echo.
    echo # 应用配置(可选)
    echo LOG_LEVEL=INFO
    echo RECEIPT_PATH=./rephoto
    echo.
    echo # 安全配置(可选)
    echo DB_USE_SSL=false
) > "dist-client\config\client-config.properties.example"

echo.
echo ============================================
echo 打包完成!
echo ============================================
echo.
echo 打包文件位置: %cd%\dist-client
echo.
echo 下一步:
echo 1. 复制 dist-client 目录到目标电脑
echo 2. 在目标电脑上运行 dist-client\run.bat
echo.
echo 详细文档: docs\MULTI_CLIENT_DEPLOYMENT.md
echo.

pause
