@echo off
chcp 65001 > nul
setlocal

echo ============================================
echo  建筑企业账目管理系统 - 环境配置向导
echo ============================================
echo.

:: 检查管理员权限
net session >nul 2>&1
if %errorLevel% neq 0 (
    echo [警告] 建议使用管理员权限运行以创建系统环境变量
    echo.
)

echo 请输入以下配置信息：
echo.

:: 数据库配置
set /p DB_HOST="数据库地址 [localhost]: "
if "%DB_HOST%"=="" set DB_HOST=localhost

set /p DB_PORT="数据库端口 [3306]: "
if "%DB_PORT%"=="" set DB_PORT=3306

set /p DB_NAME="数据库名称 [ceams]: "
if "%DB_NAME%"=="" set DB_NAME=ceams

set /p DB_USER="数据库用户名: "
if "%DB_USER%"=="" (
    echo [错误] 数据库用户名不能为空
    pause
    exit /b 1
)

set /p DB_PASS="数据库密码: "
if "%DB_PASS%"=="" (
    echo [错误] 数据库密码不能为空
    pause
    exit /b 1
)

echo.

:: Redis配置
set /p REDIS_HOST="Redis地址 [localhost]: "
if "%REDIS_HOST%"=="" set REDIS_HOST=localhost

set /p REDIS_PORT="Redis端口 [6379]: "
if "%REDIS_PORT%"=="" set REDIS_PORT=6379

set /p REDIS_PASS="Redis密码 (可选，直接回车跳过): "

echo.

:: 邮件配置
set /p MAIL_HOST="邮件服务器地址 [smtp.qq.com]: "
if "%MAIL_HOST%"=="" set MAIL_HOST=smtp.qq.com

set /p MAIL_PORT="邮件端口 [587]: "
if "%MAIL_PORT%"=="" set MAIL_PORT=587

set /p MAIL_USER="邮件用户名: "
set /p MAIL_PASS="邮件密码/授权码: "

echo.

:: JWT密钥
set /p JWT_SECRET="JWT密钥 (至少64位，留空自动生成): "

if "%JWT_SECRET%"=="" (
    :: 生成随机JWT密钥
    set CHARS=ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*
    set JWT_SECRET=
    for /L %%i in (1,1,64) do (
        set /a idx=!random! %% 70
        for %%j in (!idx!) do set JWT_SECRET=!JWT_SECRET!!CHARS:~%%j,1!
    )
    echo 已生成JWT密钥
)

echo.

:: 生成配置文件
echo ============================================
echo 正在生成配置文件...
echo ============================================

set CONFIG_FILE=%~dp0config\app-config.properties
mkdir "%~dp0config" 2>nul

(
echo # 建筑企业账目管理系统 - 应用配置
echo # 生成时间: %date% %time%
echo.
echo # 数据库配置
echo spring.datasource.url=jdbc:mysql://%DB_HOST%:%DB_PORT%/%DB_NAME%?useSSL=false^&serverTimezone=Asia/Shanghai^&characterEncoding=utf8
echo spring.datasource.username=%DB_USER%
echo spring.datasource.password=%DB_PASS%
echo spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
echo.
echo # MyBatis配置
echo mybatis.mapper-locations=classpath:mapper/*.xml
echo mybatis.type-aliases-package=com.ceams.model.entity
echo mybatis.configuration.map-underscore-to-camel-case=true
echo.
echo # 服务器配置
echo server.port=9095
echo.
echo # JWT配置
echo jwt.secret=%JWT_SECRET%
echo jwt.expiration=86400000
echo.
echo # Redis配置
echo spring.redis.host=%REDIS_HOST%
echo spring.redis.port=%REDIS_PORT%
) > "%CONFIG_FILE%"

if not "%REDIS_PASS%"=="" (
    echo spring.redis.password=%REDIS_PASS% >> "%CONFIG_FILE%"
)

if not "%MAIL_USER%"=="" (
    (
    echo.
    echo # 邮件配置
    echo spring.mail.host=%MAIL_HOST%
    echo spring.mail.port=%MAIL_PORT%
    echo spring.mail.username=%MAIL_USER%
    echo spring.mail.password=%MAIL_PASS%
    echo spring.mail.properties.mail.smtp.auth=true
    echo spring.mail.properties.mail.smtp.starttls.enable=true
    echo.
    ) >> "%CONFIG_FILE%"
)

echo.
echo ============================================
echo 配置文件已生成: %CONFIG_FILE%
echo ============================================
echo.
echo 接下来请：
echo 1. 将配置文件复制到 src/main/resources/application.properties
echo 2. 运行 mvn clean package -DskipTests 构建项目
echo 3. 运行 start.bat 启动服务
echo.
pause
