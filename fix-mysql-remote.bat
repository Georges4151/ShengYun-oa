@echo off
title MySQL Remote Connection Fix - Auto Detect

echo ============================================
echo    MySQL Remote Connection Fix
echo ============================================
echo.

echo Step 1: Finding MySQL service name...
for /f "tokens=2" %%i in ('sc query state^= all ^| findstr /i "mysql"') do (
    set MYSQL_SERVICE=%%i
)

if "%MYSQL_SERVICE%"=="" (
    echo [WARNING] Cannot find MySQL service
    echo.
    echo Please manually:
    echo 1. Open Services (services.msc)
    echo 2. Find MySQL service
    echo 3. Right-click - Properties
    echo 4. Note the Service name
    echo.
    echo Common names:
    echo   - MySQL
    echo   - MySQL80
    echo   - MySQL57
    echo   - MySQL8.0
    echo.
) else (
    echo [OK] Found MySQL service: %MYSQL_SERVICE%
)

echo.
echo Step 2: Testing MySQL connection...
mysql -u root -p123456 -e "SELECT 1" >nul 2>&1

if %errorlevel% neq 0 (
    echo [ERROR] Cannot connect to MySQL!
    echo.
    echo If password is different, edit this script
    echo and change "123456" to your actual password.
    pause
    exit /b 1
)

echo [OK] MySQL is accessible
echo.

echo Step 3: Configuring MySQL for remote access...
mysql -u root -p123456 -e "CREATE USER IF NOT EXISTS 'root'@'%' IDENTIFIED BY '123456';"
mysql -u root -p123456 -e "GRANT ALL PRIVILEGES ON *.* TO 'root'@'%' WITH GRANT OPTION;"
mysql -u root -p123456 -e "FLUSH PRIVILEGES;"
echo [OK] User configured
echo.

echo Step 4: Restarting MySQL service...
echo Please run this command to restart MySQL:
echo.
echo   net stop MySQL80
echo   net start MySQL80
echo.
echo (Replace MySQL80 with your actual service name)
echo.

echo Step 5: Rebuild Docker...
docker-compose down
docker-compose up -d --build
echo.
echo [OK] Complete!
echo.
echo Try accessing: http://localhost:9095
echo.
pause
