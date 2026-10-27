@echo off
chcp 65001 > nul
setlocal enabledelayedexpansion

echo ============================================
echo  建筑企业账目管理系统 - 服务启动
echo ============================================
echo  时间: %date% %time%
echo.

:: 检查Java
java -version >nul 2>&1
if %errorLevel% neq 0 (
    echo [错误] 未检测到Java环境，请先安装JDK 17
    echo 下载地址: https://adoptium.net/
    pause
    exit /b 1
)

:: 检查构建
if not exist "target\ceams-1.0-SNAPSHOT.jar" (
    echo [警告] 未找到编译好的JAR文件
    echo 正在尝试构建项目...
    echo.
    call mvn clean package -DskipTests
    if !errorLevel! neq 0 (
        echo [错误] 项目构建失败
        pause
        exit /b 1
    )
)

:: 创建目录
mkdir logs 2>nul
mkdir backup 2>nul
mkdir rephoto 2>nul
mkdir contracts 2>nul
mkdir stamped_contracts 2>nul
mkdir tessdata 2>nul

echo [信息] 目录创建完成
echo.

:: 检查数据库连接
echo [信息] 检查数据库连接...
mysql -u root -e "SELECT 1" >nul 2>&1
if %errorLevel% neq 0 (
    echo [警告] 无法连接到MySQL数据库
    echo 请确保：
    echo   1. MySQL服务已启动
    echo   2. 数据库配置正确
    echo   3. 用户权限已配置
    echo.
)

:: 检查Redis
redis-cli ping >nul 2>&1
if %errorLevel% neq 0 (
    echo [警告] 无法连接到Redis
    echo 请确保Redis服务已启动
    echo.
)

:: 设置默认配置
set DB_HOST=localhost
set DB_PORT=3306
set DB_NAME=ceams
set DB_USERNAME=root
set DB_PASSWORD=
set REDIS_HOST=localhost
set REDIS_PORT=6379
set SERVER_PORT=9095
set JWT_SECRET=Ceams2024EnterpriseDeploymentSecretKeyAtLeast64CharactersLong
set LOG_LEVEL=INFO

:: 从环境变量读取配置（如果已设置）
if not "%DB_HOST%"=="" set DB_HOST=%DB_HOST%
if not "%DB_PORT%"=="" set DB_PORT=%DB_PORT%
if not "%DB_NAME%"=="" set DB_NAME=%DB_NAME%
if not "%DB_USERNAME%"=="" set DB_USERNAME=%DB_USERNAME%
if not "%DB_PASSWORD%"=="" set DB_PASSWORD=%DB_PASSWORD%
if not "%REDIS_HOST%"=="" set REDIS_HOST=%REDIS_HOST%
if not "%REDIS_PORT%"=="" set REDIS_PORT=%REDIS_PORT%
if not "%SERVER_PORT%"=="" set SERVER_PORT=%SERVER_PORT%
if not "%JWT_SECRET%"=="" set JWT_SECRET=%JWT_SECRET%

echo ============================================
echo  启动参数：
echo    数据库: %DB_HOST%:%DB_PORT%/%DB_NAME%
echo    Redis: %REDIS_HOST%:%REDIS_PORT%
echo    服务端口: %SERVER_PORT%
echo ============================================
echo.

:: 停止已有进程
if exist "ceams.pid" (
    set /p OLD_PID=<ceams.pid
    taskkill /PID !OLD_PID! /F >nul 2>&1
    del ceams.pid
)

:: 启动服务
echo [信息] 正在启动服务...
echo.

java -Xms512m -Xmx2048m ^
     -Dfile.encoding=UTF-8 ^
     -Dspring.profiles.active=production ^
     -Dserver.address=0.0.0.0 ^
     -Dserver.port=%SERVER_PORT% ^
     -Djdbc.url=jdbc:mysql://%DB_HOST%:%DB_PORT%/%DB_NAME%?useSSL=false^&serverTimezone=Asia/Shanghai ^
     -Djdbc.username=%DB_USERNAME% ^
     -Djdbc.password=%DB_PASSWORD% ^
     -Dredis.host=%REDIS_HOST% ^
     -Dredis.port=%REDIS_PORT% ^
     -Djwt.secret=%JWT_SECRET% ^
     -Dlogging.level.root=%LOG_LEVEL% ^
     -jar target/ceams-1.0-SNAPSHOT.jar ^
     >> logs/ceams-stdout.log 2>&1 &

echo %! > ceams.pid

echo [成功] 服务已在后台启动
echo [信息] PID: %!
echo [信息] 日志文件: logs\ceams-stdout.log
echo.
echo 服务启动需要10-30秒，请耐心等待...
echo.
echo 按任意键打开管理界面...
pause >nul

:: 打开浏览器
start http://localhost:%SERVER_PORT%

echo.
echo ============================================
echo  管理地址: http://localhost:%SERVER_PORT%
echo  停止服务: stop.bat
echo ============================================
