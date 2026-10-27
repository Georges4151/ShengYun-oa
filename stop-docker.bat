@echo off
title Stop Docker

echo Stopping Docker containers...
docker-compose down

echo.
echo Docker containers stopped.
echo.
echo To start the application locally, run:
echo   start-local.bat
echo.
pause
