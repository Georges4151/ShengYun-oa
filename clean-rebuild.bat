@echo off
title Clean Docker Build

echo ============================================
echo    Clean Docker Rebuild
echo ============================================
echo.

echo Step 1: Stopping Docker...
docker-compose down
echo.

echo Step 2: Removing Docker image...
docker rmi sxassist-app:latest -f
echo.

echo Step 3: Clearing Docker cache...
docker builder prune -f
echo.

echo Step 4: Rebuilding from scratch...
docker-compose build --no-cache
echo.

echo Step 5: Starting services...
docker-compose up -d
echo.

echo Step 6: Waiting for startup...
timeout /t 10 /nobreak >nul
echo.

echo Step 7: Checking status...
docker-compose ps
echo.

echo Step 8: Showing recent logs...
docker-compose logs app --tail=20
echo.

echo ============================================
echo Complete!
echo ============================================
echo.
echo Try: http://localhost:9095
echo.
pause
