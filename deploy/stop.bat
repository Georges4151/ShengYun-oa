@echo off
chcp 65001 > nul
setlocal

echo ============================================
echo  建筑企业账目管理系统 - 服务停止
echo ============================================
echo.

:: 读取PID文件
if exist "ceams.pid" (
    set /p PID=<ceams.pid
    echo [信息] 正在停止进程 PID: %PID%
    taskkill /PID %PID% /F >nul 2>&1
    del ceams.pid
    echo [成功] 进程已停止
) else (
    echo [警告] 未找到PID文件，尝试查找并停止进程...
    for /f "tokens=2" %%i in ('tasklist /fi "windowtitle eq *ceams*" /fo list ^| find "PID:"') do (
        set PID=%%i
        goto :stop_process
    )

    :: 查找Java进程
    wmic process where "commandline like '%%ceams-1.0-SNAPSHOT%%'" call terminate >nul 2>&1
    echo [成功] 已尝试停止所有相关Java进程
)

echo.
echo ============================================
echo  服务已停止
echo ============================================
pause
