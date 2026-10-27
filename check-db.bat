@echo off
title Check MySQL Database

echo ============================================
echo    MySQL Database Check
echo ============================================
echo.

echo Checking MySQL connection...
mysql -u root -e "SELECT VERSION();" >nul 2>&1

if %errorlevel% equ 0 (
    echo [OK] MySQL is running and accessible
    echo.
    echo Existing databases:
    mysql -u root -e "SHOW DATABASES;"
    echo.
    echo Creating CEAMS database...
    mysql -u root < E:\sxassist\src\main\resources\db\schema.sql
    echo [OK] Database created
) else (
    echo [ERROR] Cannot connect to MySQL
    echo Please check:
    echo 1. MySQL is running
    echo 2. Root password is correct
    echo.
    echo To set password, edit docker-compose.yml and change DB_PASSWORD
)

echo.
echo ============================================
echo Check complete!
echo ============================================
echo.
pause
