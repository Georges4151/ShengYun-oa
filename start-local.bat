@echo off
title CEAMS - Local Startup

echo ============================================
echo    CEAMS - Local Startup
echo ============================================
echo.

echo This script will start the application directly
echo on your Windows machine (not in Docker).
echo.

echo Step 1: Checking Java...
java -version >nul 2>&1

if %errorlevel% neq 0 (
    echo [ERROR] Java is not installed or not in PATH!
    echo Please install Java 17 or higher.
    pause
    exit /b 1
)

echo [OK] Java is available
echo.

echo Step 2: Checking JAR file...
if not exist "target\ceams-1.0-SNAPSHOT.jar" (
    echo [WARNING] JAR file not found!
    echo Building project...
    call mvn clean package -DskipTests
)

echo [OK] JAR file ready
echo.

echo Step 3: Starting application...
echo.
echo ============================================
echo Starting CEAMS...
echo ============================================
echo.

java -jar target\ceams-1.0-SNAPSHOT.jar

pause
