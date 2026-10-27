@echo off
title CEAMS - Database Setup

echo ============================================
echo    CEAMS - Database Setup
echo ============================================
echo.

echo Step 1: Checking MySQL...
mysql -u root -e "SELECT 1" >nul 2>&1

if %errorlevel% neq 0 (
    echo [ERROR] Cannot connect to MySQL!
    echo.
    echo Please make sure:
    echo 1. MySQL is installed and running
    echo 2. Root user has no password or set in config
    echo.
    pause
    exit /b 1
)

echo [OK] MySQL is accessible
echo.

echo Step 2: Creating database...
mysql -u root < "E:\sxassist\src\main\resources\db\init.sql"

if %errorlevel% equ 0 (
    echo [OK] Database created successfully!
) else (
    echo [ERROR] Failed to create database
    pause
    exit /b 1
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
echo Next step: Run start-local.bat
echo.
pause
