@echo off
chcp 65001 > nul
setlocal enabledelayedexpansion

echo ============================================
echo  建筑企业账目管理系统 - 数据库初始化向导
echo ============================================
echo.

:: 配置数据库连接
set /p DB_HOST="数据库地址 [localhost]: "
if "%DB_HOST%"=="" set DB_HOST=localhost

set /p DB_PORT="数据库端口 [3306]: "
if "%DB_PORT%"=="" set DB_PORT=3306

set /p DB_USER="数据库用户名 [root]: "
if "%DB_USER%"=="" set DB_USER=root

set /p DB_PASS="数据库密码 (直接回车跳过): "

set /p DB_NAME="数据库名称 [ceams]: "
if "%DB_NAME%"=="" set DB_NAME=ceams

echo.
echo ============================================
echo  配置信息：
echo    服务器: %DB_HOST%:%DB_PORT%
echo    数据库: %DB_NAME%
echo    用户: %DB_USER%
echo ============================================
echo.

:: 准备MySQL命令
set MYSQL_CMD=mysql -h %DB_HOST% -P %DB_PORT% -u %DB_USER%
if not "%DB_PASS%"=="" set MYSQL_CMD=%MYSQL_CMD% -p%DB_PASS%

:: 检查连接
echo [信息] 检查数据库连接...
%MYSQL_CMD% -e "SELECT 1" >nul 2>&1
if %errorLevel% neq 0 (
    echo [错误] 无法连接到数据库，请检查配置
    echo.
    echo 常见问题：
    echo   1. MySQL服务是否启动
    echo   2. 用户名和密码是否正确
    echo   3. 防火墙是否允许3306端口
    echo.
    pause
    exit /b 1
)
echo [成功] 数据库连接正常
echo.

:: 创建数据库
echo [信息] 创建数据库...
%MYSQL_CMD% -e "CREATE DATABASE IF NOT EXISTS %DB_NAME% CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
if %errorLevel% neq 0 (
    echo [错误] 创建数据库失败
    pause
    exit /b 1
)
echo [成功] 数据库已创建
echo.

:: 创建专用用户 (可选)
set /p CREATE_USER="是否创建专用用户？ (y/n) [n]: "
if /i "%CREATE_USER%"=="y" (
    set /p NEW_USER="请输入新用户名: "
    set /p NEW_PASS="请输入新密码: "

    echo.
    echo [信息] 创建专用用户...
    %MYSQL_CMD% -e "CREATE USER IF NOT EXISTS '%NEW_USER%'@'localhost' IDENTIFIED BY '%NEW_PASS%';"
    %MYSQL_CMD% -e "CREATE USER IF NOT EXISTS '%NEW_USER%'@'%' IDENTIFIED BY '%NEW_PASS%';"
    %MYSQL_CMD% -e "GRANT ALL PRIVILEGES ON %DB_NAME%.* TO '%NEW_USER%'@'localhost';"
    %MYSQL_CMD% -e "GRANT ALL PRIVILEGES ON %DB_NAME%.* TO '%NEW_USER%'@'%';"
    %MYSQL_CMD% -e "FLUSH PRIVILEGES;"
    echo [成功] 专用用户已创建
    echo.
)

:: 执行迁移脚本
echo ============================================
echo  数据库迁移脚本
echo ============================================
echo.

set MIGRATION_DIR=%~dp0..\src\main\resources\db\migration
set /p RUN_MIGRATION="是否执行数据库迁移脚本？ (y/n) [y]: "
if not "%RUN_MIGRATION%"=="n" (
    if exist "%MIGRATION_DIR%" (
        for %%f in ("%MIGRATION_DIR%\*.sql") do (
            echo [执行] %%~nxf
            %MYSQL_CMD% %DB_NAME% < "%%f"
            if !errorLevel! neq 0 (
                echo [警告] %%~nxf 执行失败，继续下一个...
            ) else (
                echo [成功] %%~nxf 执行完成
            )
            echo.
        )
    ) else (
        echo [信息] 未找到迁移脚本目录
    )
)

:: 创建初始化数据
echo ============================================
echo  创建初始化数据
echo ============================================
echo.

set /p CREATE_DATA="是否创建默认管理员账户？ (y/n) [y]: "
if not "%CREATE_DATA%"=="n" (
    set /p ADMIN_USER="管理员用户名 [admin]: "
    if "%ADMIN_USER%"=="" set ADMIN_USER=admin

    set /p ADMIN_PASS="管理员密码: "
    if "%ADMIN_PASS%"=="" (
        echo [警告] 密码不能为空，跳过创建
    ) else (
        echo [信息] 创建管理员账户...

        :: 检查表是否存在
        %MYSQL_CMD% %DB_NAME% -e "SHOW TABLES LIKE 'users';" >nul 2>&1
        if %errorLevel% equ 0 (
            :: 生成密码哈希 (简化版本，实际应使用BCrypt)
            %MYSQL_CMD% %DB_NAME% -e "INSERT INTO users (username, password, role, created_at)
                                     VALUES ('%ADMIN_USER%', '%ADMIN_PASS%', 'ADMIN', NOW())
                                     ON DUPLICATE KEY UPDATE password='%ADMIN_PASS%', updated_at=NOW();"
            echo [成功] 管理员账户已创建/更新
            echo   用户名: %ADMIN_USER%
            echo   密码: %ADMIN_PASS%
            echo.
            echo [警告] 请尽快修改密码为强密码！
        ) else (
            echo [信息] users表不存在，跳过创建用户
            echo   请先运行数据库迁移脚本
        )
    )
)

echo.
echo ============================================
echo  数据库初始化完成！
echo ============================================
echo.
echo  下一步：
echo   1. 运行 setup-env.bat 配置应用参数
echo   2. 运行 mvn clean package -DskipTests 构建项目
echo   3. 运行 start.bat 启动服务
echo.
pause
