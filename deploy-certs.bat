@echo off
title OpenVPN 证书部署脚本
echo ================================================
echo    OpenVPN 证书部署
echo ================================================
echo.

:: 检查管理员权限
echo 检查管理员权限...
net session >nul 2>&1
if %errorlevel% neq 0 (
    echo.
    echo 错误: 请右键点击此文件，选择"以管理员身份运行"！
    echo.
    pause
    exit /b 1
)

echo OK: 管理员权限确认
echo.

:: 设置路径
set EASYRSA_PATH=C:\VPN-Config\EasyRSA-3.2.6
set VPN_CONFIG_PATH=C:\VPN-Config

echo 正在复制证书...
echo.

:: 复制服务器证书
echo 1/2 复制服务器证书...
if not exist "%VPN_CONFIG_PATH%\server\keys" (
    mkdir "%VPN_CONFIG_PATH%\server\keys"
)
copy "%EASYRSA_PATH%\pki\ca.crt" "%VPN_CONFIG_PATH%\server\keys\" >nul
copy "%EASYRSA_PATH%\pki\issued\server.crt" "%VPN_CONFIG_PATH%\server\keys\" >nul
copy "%EASYRSA_PATH%\pki\private\server.key" "%VPN_CONFIG_PATH%\server\keys\" >nul
copy "%EASYRSA_PATH%\pki\dh.pem" "%VPN_CONFIG_PATH%\server\keys\" >nul
copy "%EASYRSA_PATH%\ta.key" "%VPN_CONFIG_PATH%\server\keys\" >nul
echo OK: 服务器证书已复制

:: 复制用户证书
echo.
echo 2/2 复制客户端证书...
if not exist "%VPN_CONFIG_PATH%\clients\client1\keys" (
    mkdir "%VPN_CONFIG_PATH%\clients\client1\keys"
)
copy "%EASYRSA_PATH%\pki\ca.crt" "%VPN_CONFIG_PATH%\clients\client1\keys\" >nul
copy "%EASYRSA_PATH%\pki\issued\client1.crt" "%VPN_CONFIG_PATH%\clients\client1\keys\" >nul
copy "%EASYRSA_PATH%\pki\private\client1.key" "%VPN_CONFIG_PATH%\clients\client1\keys\" >nul
copy "%EASYRSA_PATH%\ta.key" "%VPN_CONFIG_PATH%\clients\client1\keys\" >nul
echo OK: 客户端证书已复制

echo.
echo ================================================
echo 证书复制完成！
echo ================================================
echo.
echo 服务器证书位置: %VPN_CONFIG_PATH%\server\keys\
echo 客户端证书位置: %VPN_CONFIG_PATH%\clients\client1\keys\
echo.
echo 下一步:
echo 1. 复制服务器配置到 OpenVPN 目录
echo 2. 配置防火墙
echo 3. 启动服务
echo.

pause
