@echo off
title Find MySQL Service

echo Finding MySQL services...

sc query state= all | findstr /i "mysql"

echo.
echo Common MySQL service names:
echo   MySQL
echo   MySQL80
echo   MySQL57
echo   MySQL8.0
echo   MySQL Server
echo.

echo Press any key to continue...
pause >nul
