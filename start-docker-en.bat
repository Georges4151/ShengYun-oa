@echo off
title CEAMS - Docker Deployment

echo.
echo ============================================
echo    CEAMS - Docker Deployment
echo ============================================
echo.

echo Checking Docker installation...
docker --version >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] Docker is not installed or not running!
    echo Please install Docker Desktop first
    echo Download: https://www.docker.com/get-started
    echo.
    pause
    exit /b 1
)

echo [OK] Docker is installed
echo.

docker-compose --version >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] Docker Compose is not installed!
    pause
    exit /b 1
)

echo [OK] Docker Compose is installed
echo.

echo ============================================
echo Starting services...
echo ============================================
echo.

docker-compose up -d --build

echo.
echo ============================================
echo Deployment complete!
echo ============================================
echo.
echo Service Status:
docker-compose ps
echo.
echo Access your application at: http://localhost:9095
echo.
echo Default Login:
echo   Username: admin
echo   Password: admin123
echo.
echo View logs: docker-compose logs -f
echo Stop services: docker-compose down
echo.
pause
