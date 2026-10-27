@echo off
title OpenVPN Deployment
echo ================================================
echo    OpenVPN Deployment
echo ================================================
echo.

:: Check admin rights
echo Checking admin rights...
net session >nul 2>&1
if %errorlevel% neq 0 (
    echo.
    echo ERROR: Please run as Administrator!
    echo Right-click and select "Run as Administrator"
    echo.
    pause
    exit /b 1
)

echo OK: Admin rights confirmed
echo.

:: Set paths
set EASYRSA_PATH=C:\VPN-Config\EasyRSA-3.2.6
set VPN_CONFIG_PATH=C:\VPN-Config
set OPENVPN_PATH=C:\Program Files\OpenVPN

echo ================================================
echo Step 1: Copy server certificates
echo ================================================
echo.

if not exist "%VPN_CONFIG_PATH%\server\keys" (
    mkdir "%VPN_CONFIG_PATH%\server\keys"
)
copy "%EASYRSA_PATH%\pki\ca.crt" "%VPN_CONFIG_PATH%\server\keys\"
copy "%EASYRSA_PATH%\pki\issued\server.crt" "%VPN_CONFIG_PATH%\server\keys\"
copy "%EASYRSA_PATH%\pki\private\server.key" "%VPN_CONFIG_PATH%\server\keys\"
copy "%EASYRSA_PATH%\pki\dh.pem" "%VPN_CONFIG_PATH%\server\keys\"
copy "%EASYRSA_PATH%\ta.key" "%VPN_CONFIG_PATH%\server\keys\"
echo OK: Server certificates copied
echo.

echo ================================================
echo Step 2: Copy client certificates
echo ================================================
echo.

if not exist "%VPN_CONFIG_PATH%\clients\client1\keys" (
    mkdir "%VPN_CONFIG_PATH%\clients\client1\keys"
)
copy "%EASYRSA_PATH%\pki\ca.crt" "%VPN_CONFIG_PATH%\clients\client1\keys\"
copy "%EASYRSA_PATH%\pki\issued\client1.crt" "%VPN_CONFIG_PATH%\clients\client1\keys\"
copy "%EASYRSA_PATH%\pki\private\client1.key" "%VPN_CONFIG_PATH%\clients\client1\keys\"
copy "%EASYRSA_PATH%\ta.key" "%VPN_CONFIG_PATH%\clients\client1\keys\"
echo OK: Client certificates copied
echo.

echo ================================================
echo Step 3: Copy to OpenVPN folder
echo ================================================
echo.

if not exist "%OPENVPN_PATH%\config" (
    mkdir "%OPENVPN_PATH%\config"
)
xcopy /E /I /Y "%VPN_CONFIG_PATH%\server" "%OPENVPN_PATH%\config"
echo OK: Config copied to OpenVPN
echo.

echo ================================================
echo Step 4: Configure Firewall
echo ================================================
echo.

echo Adding firewall rules...
netsh advfirewall firewall add rule name="OpenVPN Server" dir=in action=allow protocol=UDP localport=1194
netsh advfirewall firewall add rule name="VPN to App" dir=in action=allow protocol=TCP localport=9095 remoteip=10.8.0.0/24
echo OK: Firewall configured
echo.

echo ================================================
echo Step 5: Start OpenVPN Service
echo ================================================
echo.

sc query OpenVPNService >nul
if %errorlevel% equ 0 (
    echo Service found
    net start OpenVPNService
    sc config OpenVPNService start= auto
    echo OK: Service started and set to auto
) else (
    echo WARNING: OpenVPNService not found, please install OpenVPN first!
)

echo.
echo ================================================
echo Deployment Complete!
echo ================================================
echo.
echo IMPORTANT:
echo 1. Edit client.ovpn and replace with your real server public IP!
echo 2. Client configs: %VPN_CONFIG_PATH%\clients\client1\keys\
echo 3. Server config: %OPENVPN_PATH%\config\
echo.

pause
