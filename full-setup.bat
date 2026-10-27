@echo off
title Complete MySQL + Docker Setup

echo ============================================
echo    Complete Setup Script
echo ============================================
echo.

echo Step 1: Configuring MySQL for remote access...
mysql -u root -p123456 -e "CREATE USER IF NOT EXISTS 'root'@'%' IDENTIFIED BY '123456';"
mysql -u root -p123456 -e "GRANT ALL PRIVILEGES ON *.* TO 'root'@'%' WITH GRANT OPTION;"
mysql -u root -p123456 -e "FLUSH PRIVILEGES;"
echo [OK] MySQL configured
echo.

echo Step 2: Finding MySQL config file...
set MYSQL_CNF=
if exist "C:\ProgramData\MySQL\MySQL Server 8.0\my.ini" set MYSQL_CNF=C:\ProgramData\MySQL\MySQL Server 8.0\my.ini

if "%MYSQL_CNF%"=="" (
    echo [WARNING] Cannot find config file
) else (
    echo Found: %MYSQL_CNF%
    findstr /C:"bind-address" "%MYSQL_CNF%" >nul 2>&1
    if %errorlevel% neq 0 (
        echo Adding bind-address=0.0.0.0...
        echo. >> "%MYSQL_CNF%"
        echo [mysqld] >> "%MYSQL_CNF%"
        echo bind-address=0.0.0.0 >> "%MYSQL_CNF%"
        echo [OK] Config updated
    ) else (
        echo [OK] bind-address already set
    )
)
echo.

echo Step 3: Restarting MySQL service...
net stop MySQL80
timeout /t 3 /nobreak >nul
net start MySQL80
echo [OK] MySQL restarted
echo.

echo Step 4: Stopping Docker...
docker-compose down
echo.

echo Step 5: Starting Docker (with build)...
docker-compose up -d --build
echo.

echo Step 6: Checking status...
docker-compose ps
echo.

echo ============================================
echo Setup complete!
echo ============================================
echo.
echo Access: http://localhost:9095
echo.
echo Default login:
echo   Username: admin
echo   Password: admin123
echo.
echo Check logs: docker-compose logs app --tail=30
echo.
pause
