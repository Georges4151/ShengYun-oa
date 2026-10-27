# 建筑企业账目管理系统 - VPN安全部署指南

## 一、系统架构

```
Internet
    │
    ├─────────────────────────────────────────┐
    │  防火墙策略                               │
    │  ├─ 开放: UDP 1194 (VPN)               │
    │  ├─ 开放: TCP 9095 (应用) 仅VPN内网     │
    │  └─ 阻断: 其他所有端口公网访问            │
    └─────────────────────────────────────────┘
    │
    │ VPN隧道 (UDP 1194)
    │
┌───┴───────────────────────────────────────────┐
│  VPN服务器 (您当前的电脑)                      │
│  ├─ OpenVPN 服务                             │
│  ├─ VPN网络: 10.8.0.0/24                   │
│  ├─ 应用网络: 192.168.1.0/24               │
│  └─ 应用端口: TCP 9095                      │
└───┬───────────────────────────────────────────┘
    │
    │ VPN客户端连接
    │
    ├── 客户端1 (Windows/Android)
    │   └─ VPN IP: 10.8.0.2
    │
    ├── 客户端2 (Windows/Android)
    │   └─ VPN IP: 10.8.0.3
    │
    └── 客户端3 (Windows/Android)
        └─ VPN IP: 10.8.0.4
```

---

## 二、部署流程概览

```
第一阶段：证书生成（当前步骤）
├─ 1.1 生成CA证书
├─ 1.2 生成服务器证书
├─ 1.3 生成客户端证书
└─ 1.4 配置VPN服务器

第二阶段：应用部署
├─ 2.1 配置应用服务
├─ 2.2 启动应用服务
└─ 2.3 验证服务运行

第三阶段：客户端配置
├─ 3.1 配置用户电脑
├─ 3.2 分发客户端配置
└─ 3.3 测试连接

第四阶段：验证测试
├─ 4.1 VPN连接测试
├─ 4.2 应用访问测试
└─ 4.3 功能验证
```

---

## 三、详细部署步骤

### 第一阶段：证书生成

#### 步骤1：修复证书生成问题

根据之前的错误，EasyRSA需要使用特殊方式调用。请按以下步骤手动操作：

**方法A：使用EasyRSA Start.bat（推荐）**

```
1. 打开文件夹：C:\VPN-Config\EasyRSA-3.2.6
2. 双击运行 "EasyRSA Start.bat"
3. 会打开一个命令行窗口
4. 在打开的窗口中依次输入以下命令：
```

```bash
# 初始化PKI
./easyrsa init-pki

# 生成CA证书（会提示输入信息，全部直接回车）
./easyrsa build-ca

# 生成服务器证书
./easyrsa build-server-full server nopass

# 生成Diffie-Hellman参数（等待1-2分钟）
./easyrsa gen-dh

# 生成TLS密钥
openssl rand -hex 32 > ta.key
```

**方法B：手动在PowerShell中运行**

```
1. 以管理员身份打开PowerShell
2. 依次运行以下命令：
```

```powershell
# 进入目录
cd C:\VPN-Config\EasyRSA-3.2.6

# 设置PATH
$env:PATH = "$PWD\bin;$env:PATH"

# 初始化
.\bin\sh.exe easyrsa init-pki

# 生成CA
.\bin\sh.exe easyrsa build-ca
# 提示时输入：VPN-CA 作为Common Name

# 生成服务器证书
.\bin\sh.exe easyrsa build-server-full server nopass

# 生成DH
.\bin\sh.exe easyrsa gen-dh

# 生成TLS密钥
.\bin\openssl.exe rand -hex 32 -out ta.key
```

#### 步骤2：复制证书到配置目录

```powershell
# 创建服务器配置目录
mkdir C:\VPN-Config\server\keys

# 复制服务器证书
copy pki\ca.crt C:\VPN-Config\server\keys\
copy pki\issued\server.crt C:\VPN-Config\server\keys\
copy pki\private\server.key C:\VPN-Config\server\keys\
copy pki\dh.pem C:\VPN-Config\server\keys\
copy ta.key C:\VPN-Config\server\keys\
```

#### 步骤3：创建用户证书

为每个用户生成独立证书：

```powershell
# 为用户1生成证书
.\bin\sh.exe easyrsa build-client-full client1 nopass

# 为用户2生成证书（重复即可）
.\bin\sh.exe easyrsa build-client-full client2 nopass
```

---

### 第二阶段：VPN服务器配置

#### 步骤4：创建服务器配置文件

在 `C:\VPN-Config\server\` 创建 `server.ovpn` 文件：

```ini
; 服务器基本配置
port 1194
proto udp
dev tun
dev-node "OpenVPN"

; 证书和密钥
ca keys\ca.crt
cert keys\server.crt
key keys\server.key
dh keys\dh.pem
tls-auth keys\ta.key 0
key-direction 0

; 服务器网络配置
server 10.8.0.0 255.255.255.0

; 客户端IP持久化
ifconfig-pool-persist ipp.txt

; 内网路由配置（根据您的实际内网网段修改）
route 192.168.1.0 255.255.255.0
push "route 192.168.1.0 255.255.255.0"

; DNS配置
push "register-dns"
push "block-outside-dns"

; 加密配置
cipher AES-256-GCM
auth SHA256

; 保持连接
keepalive 10 60
ping-timer-rem
persist-key
persist-tun

; 日志
status openvpn-status.log
log-append openvpn.log
verb 3
explicit-exit-notify 1
```

#### 步骤5：复制配置到OpenVPN

```powershell
# 复制服务器配置到OpenVPN
copy C:\VPN-Config\server\server.ovpn "C:\Program Files\OpenVPN\config\"
copy C:\VPN-Config\server\keys\* "C:\Program Files\OpenVPN\config\keys\"
```

#### 步骤6：配置防火墙

```powershell
# 以管理员身份运行PowerShell

# 允许OpenVPN端口
New-NetFirewallRule -DisplayName "OpenVPN Server" -Direction Inbound -Action Allow -Protocol UDP -LocalPort 1194 -Profile Any

# 允许VPN客户端访问应用端口
New-NetFirewallRule -DisplayName "VPN to App" -Direction Inbound -Action Allow -Protocol TCP -LocalPort 9095 -RemoteAddress 10.8.0.0/24 -Profile Any
```

#### 步骤7：启动VPN服务

```powershell
# 启动OpenVPN服务
Start-Service OpenVPNService

# 设置自动启动
Set-Service -Name OpenVPNService -StartupType Automatic

# 检查服务状态
Get-Service OpenVPNService
```

---

### 第三阶段：应用服务部署

#### 步骤8：检查应用配置

确认应用配置允许VPN网络访问：

```properties
# application.properties

# 服务器绑定地址（0.0.0.0表示允许所有网络访问）
server.address=0.0.0.0
server.port=9095

# 跨域配置（允许VPN网络）
cors.allowed-origins=http://10.8.0.0/24,http://192.168.1.0/24

# 数据库配置
spring.datasource.url=jdbc:mysql://localhost:3306/ceams
```

#### 步骤9：启动应用服务

```batch
@echo off
cd /d "E:\sxassist"

echo 启动建筑企业账目管理系统...
echo 监听地址: 0.0.0.0
echo 服务端口: 9095
echo.

java -jar target\ceams-1.0-SNAPSHOT.jar ^
    --server.address=0.0.0.0 ^
    --server.port=9095

pause
```

---

### 第四阶段：客户端配置

#### 步骤10：为用户创建配置文件

为每个用户创建配置文件：

在 `C:\VPN-Config\clients\client1\` 创建 `client1.ovpn`：

```ini
client
dev tun
proto udp
remote 您的服务器公网IP 1194
resolv-retry infinite
nobind
persist-key
persist-tun

ca ca.crt
cert client1.crt
key client1.key
tls-auth ta.key 1
key-direction 1

cipher AES-256-GCM
auth SHA256
remote-cert-tls server
verb 3
```

复制用户证书：

```powershell
mkdir C:\VPN-Config\clients\client1
mkdir C:\VPN-Config\clients\client1\keys

copy pki\ca.crt C:\VPN-Config\clients\client1\keys\
copy pki\issued\client1.crt C:\VPN-Config\clients\client1\keys\
copy pki\private\client1.key C:\VPN-Config\clients\client1\keys\
copy ta.key C:\VPN-Config\clients\client1\keys\
copy client1.ovpn C:\VPN-Config\clients\client1\keys\
```

---

## 四、用户使用指南

### Windows用户

```
1. 安装OpenVPN客户端
   - 下载：https://openvpn.net/community-downloads/
   - 安装：一路下一步完成

2. 导入配置文件
   - 打开：C:\Program Files\OpenVPN\config\
   - 创建文件夹：user1
   - 复制配置文件到此处

3. 连接VPN
   - 右键点击系统托盘的🔑图标
   - 选择"连接"
   - 等待连接成功（图标变绿色）

4. 访问应用
   - 打开浏览器
   - 访问：http://192.168.1.100:9095
   - 或访问：http://10.8.0.1:9095
```

### Android用户

```
1. 安装OpenVPN Connect
   - Google Play搜索"OpenVPN Connect"
   - 安装并打开

2. 导入配置文件
   - 点击右上角"+"
   - 选择"Import Profile from File"
   - 选择.ovpn文件

3. 连接VPN
   - 点击连接按钮
   - 等待连接成功

4. 访问应用
   - 打开浏览器
   - 访问：http://192.168.1.100:9095
```

---

## 五、测试验证

### VPN连接测试

```powershell
# 1. 检查VPN服务状态
Get-Service OpenVPNService

# 2. 检查端口监听
netstat -an | findstr "1194"

# 3. 查看连接日志
Get-Content "C:\Program Files\OpenVPN\log\openvpn-status.log"
```

### 应用访问测试

```
1. 连接VPN后，打开浏览器
2. 访问：http://192.168.1.100:9095
3. 应该看到应用登录页面
4. 使用管理员账号登录
5. 测试各项功能是否正常
```

---

## 六、故障排除

### 问题1：VPN连接失败

```
检查项：
1. 确认服务器公网IP正确
2. 确认UDP 1194端口已开放
3. 确认防火墙允许OpenVPN
4. 检查客户端配置中的IP是否正确
```

### 问题2：连接成功但无法访问应用

```
检查项：
1. 确认应用服务正在运行
2. 确认应用监听0.0.0.0而非127.0.0.1
3. 确认防火墙允许VPN网络访问9095端口
4. 检查服务器路由配置
```

### 问题3：证书错误

```
检查项：
1. 确认CA证书在服务器和客户端一致
2. 确认证书未过期
3. 确认私钥与证书匹配
4. 尝试重新生成证书
```

---

## 七、安全建议

### 7.1 防火墙配置

```powershell
# 仅允许VPN网络访问应用
Remove-NetFirewallRule -DisplayName "Allow All to 9095"  # 删除旧的全局规则
New-NetFirewallRule -DisplayName "VPN to App" -Direction Inbound -Action Allow -Protocol TCP -LocalPort 9095 -RemoteAddress 10.8.0.0/24 -Profile Any
```

### 7.2 证书管理

```
建议：
1. 每个用户使用独立证书
2. 定期更换证书（建议每6个月）
3. 离职员工及时吊销证书
4. 私钥文件妥善保管，不要通过网络传输
```

### 7.3 备份策略

```
备份内容：
1. 所有证书文件
2. 服务器配置文件
3. 用户配置文件
4. 应用数据

备份频率：
- 每日增量备份
- 每周全量备份
- 重要更新后立即备份
```

---

## 八、快速参考

### 常用命令

```powershell
# 启动VPN服务
Start-Service OpenVPNService

# 停止VPN服务
Stop-Service OpenVPNService

# 查看连接状态
Get-Content "C:\Program Files\OpenVPN\log\openvpn-status.log"

# 重启VPN服务
Restart-Service OpenVPNService
```

### 文件位置

```
服务器配置：
- 配置文件：C:\Program Files\OpenVPN\config\
- 证书文件：C:\Program Files\OpenVPN\config\keys\
- 日志文件：C:\Program Files\OpenVPN\log\

应用服务：
- 应用JAR：E:\sxassist\target\ceams-1.0-SNAPSHOT.jar
- 配置文件：E:\sxassist\src\main\resources\application.properties

备份文件：
- VPN配置：C:\VPN-Config\
- 证书备份：C:\VPN-Certs\
```

---

## 九、技术支持

如遇问题，请提供：
1. 错误信息截图
2. 相关日志文件
3. 操作步骤描述

---

**文档版本**: 1.0
**最后更新**: 2026-05-17
**适用版本**: OpenVPN 2.6.x + CEAMS 1.0
