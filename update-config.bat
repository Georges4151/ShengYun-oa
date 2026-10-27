@echo off
title CEAMS - Update Database Password

echo ============================================
echo    CEAMS - Update Configuration
echo ============================================
echo.
echo MySQL password: 123456
echo.

echo Step 1: Updating application-docker.properties...
powershell -Command "(Get-Content 'E:\sxassist\src\main\resources\application-docker.properties') -replace 'spring.datasource.password=', 'spring.datasource.password=123456' | Set-Content 'E:\sxassist\src\main\resources\application-docker.properties'"

echo Step 2: Updating docker-compose.yml...
powershell -Command "(Get-Content 'E:\sxassist\docker-compose.yml') -replace 'DB_PASSWORD:', 'DB_PASSWORD: 123456' | Set-Content 'E:\sxassist\docker-compose.yml'"

echo [OK] Configuration updated!
echo.
echo ============================================
echo Setup complete!
echo ============================================
echo.
echo Next step: Run start-docker-en.bat
echo.
pause
