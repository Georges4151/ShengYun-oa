@echo off
title CEAMS - Database Setup with Password

echo ============================================
echo    CEAMS - Database Setup
echo ============================================
echo.
echo Please enter MySQL root password (leave empty if no password):
set /p MYSQL_PWD="Password: "

if "%MYSQL_PWD%"=="" (
    echo Connecting without password...
    set MYSQL_CMD=mysql -u root
) else (
    echo Connecting with password...
    set MYSQL_CMD=mysql -u root -p%MYSQL_PWD%
)

echo.
echo Step 1: Testing connection...
%MYSQL_CMD% -e "SELECT 1" >nul 2>&1

if %errorlevel% neq 0 (
    echo.
    echo [ERROR] Cannot connect to MySQL!
    echo.
    echo Possible reasons:
    echo 1. MySQL is not running
    echo 2. Wrong password
    echo 3. User does not have permission
    echo.
    pause
    exit /b 1
)

echo [OK] MySQL connection successful!
echo.

echo Step 2: Creating database and tables...
%MYSQL_CMD% < "E:\sxassist\src\main\resources\db\init.sql"

if %errorlevel% equ 0 (
    echo [OK] Database created successfully!
) else (
    echo [ERROR] Failed to create database
    pause
    exit /b 1
)

echo.
echo Step 3: Updating application configuration...
if "%MYSQL_PWD%"=="" (
    echo No password - skipping config update
) else (
    echo Updating database password in config...
    powershell -Command "(Get-Content 'E:\sxassist\src\main\resources\application-docker.properties') -replace 'spring.datasource.password=', 'spring.datasource.password=%MYSQL_PWD%' | Set-Content 'E:\sxassist\src\main\resources\application-docker.properties'"
    powershell -Command "(Get-Content 'E:\sxassist\docker-compose.yml') -replace 'DB_PASSWORD:', 'DB_PASSWORD: %MYSQL_PWD%' | Set-Content 'E:\sxassist\docker-compose.yml'"
    echo [OK] Configuration updated!
)

echo.
echo ============================================
echo Database setup complete!
echo ============================================
echo.
echo Default accounts:
echo   Username: admin
echo   Password: admin123
echo.
echo   Username: manager
echo   Password: manager123
echo.
echo Next step: Run start-docker-en.bat
echo.
pause
