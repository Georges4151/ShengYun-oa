# OpenVPN 服务器完整部署指南

## 一、部署架构概览

```
Internet
    │
    │
┌───┴─────────────────────────────────────────┐
│  防火墙 (仅开放UDP 1194端口)                 │
│  └── 将外部用户请求转发到VPN服务器           │
└────────────────┬────────────────────────────┘
                 │
                 │ VPN隧道 (UDP 1194)
                 │
┌────────────────┴────────────────────────────┐
│  OpenVPN 服务器                             │
│  ├── VPN网络: 10.8.0.0/24                 │
│  ├── 监听端口: UDP 1194                   │
│  └── 自动路由到内网: 192.168.1.0/24        │
└────────────────┬────────────────────────────┘
                 │
                 │ 内部路由
                 │
┌────────────────┴────────────────────────────┐
│  应用服务器 (192.168.1.100)                │
│  ├── Spring Boot: 9095                    │
│  ├── MySQL: 3306                          │
│  └── Redis: 6379                          │
└─────────────────────────────────────────────┘
```

---

## 二、服务器准备工作

### 2.1 系统要求

```
硬件要求（最低配置）：
  • CPU: 1核心
  • 内存: 1GB
  • 硬盘: 20GB
  • 网络: 固定公网IP

软件要求：
  • 操作系统: Windows Server 2012+
  • 或者: Windows 10/11 专业版

网络要求：
  • 固定公网IP地址（必需）
  • 防火墙开放UDP 1194端口
  • 建议：配置端口映射
```

### 2.2 查看服务器网络信息

```powershell
# 查看IP地址
ipconfig /all

# 查看公网IP（浏览器访问）
# https://www.ipaddress.com
# 或
# https://www.baidu.com/s?wd=ip

# 记录以下信息：
#   - 公网IP地址: _______________
#   - 内网IP地址: _______________
```

---

## 三、OpenVPN 服务器配置

### 3.1 创建配置目录结构

```powershell
# 1. 创建配置目录
mkdir C:\VPN-Config
mkdir C:\VPN-Config\server
mkdir C:\VPN-Config\clients
mkdir C:\VPN-Config\keys

# 2. 查看OpenVPN安装目录
cd "C:\Program Files\OpenVPN"
dir
```

### 3.2 生成证书（使用EasyRSA）

#### 第一步：初始化PKI

```powershell
# 1. 以管理员身份打开PowerShell
# 右键开始 → Windows PowerShell (管理员)

# 2. 进入EasyRSA目录
cd "C:\Program Files\OpenVPN\easy-rsa"

# 3. 复制配置文件
copy vars.bat.example vars.bat
copy openssl-1.0.0.cnf openssl.cnf

# 4. 编辑vars.bat配置证书信息
notepad vars.bat
```

#### 第二步：修改vars.bat

```bat
@echo off
set HOME=%ProgramFiles%\OpenVPN\easy-rsa
set KEY_CONFIG=openssl.cnf
set KEY_DIR=keys
set KEY_SIZE=2048
set KEY_COUNTRY=CN
set KEY_PROVINCE=Beijing
set KEY_CITY=Beijing
set KEY_ORG=CEAMS-VPN
set KEY_EMAIL=admin@company.com
set KEY_CN=VPN-CA
set KEY_OU=VPN-Server
set PKCS11_MODULE_PATH=dummy
set PKCS11_PIN= dummy
set KEY_NAME="EasyRSA"
set CA_EXPIRE=3650
set KEY_EXPIRE=3650
set KEY_ALTNAMES="server"
```

#### 第三步：生成CA证书

```powershell
# 在PowerShell中执行
.\vars.bat
.\clean-all.bat

# 生成CA证书（一路回车）
.\build-ca.bat

# 提示时：
#   Common Name: VPN-CA
#   其他直接回车
```

#### 第四步：生成服务器证书

```powershell
# 生成服务器证书（一路回车，签名时输入y）
.\build-key-server.bat server

# 提示时：
#   Common Name: server
#   Sign the certificate? [y/n]: y
#   1 out of 1 certificate requests certified, commit? [y/n]: y
```

#### 第五步：生成Diffie-Hellman参数

```powershell
# 生成DH密钥（等待2-5分钟）
.\build-dh.bat
```

#### 第六步：生成TLS密钥

```powershell
# 生成ta.key（防DDoS攻击）
"C:\Program Files\OpenVPN\bin\openvpn.exe" --genkey --secret keys\ta.key
```

### 3.3 创建服务器配置文件

```powershell
# 创建服务器配置
notepad C:\VPN-Config\server\server.ovpn
```

**server.ovpn 内容：**

```ini
; 服务器基本配置
port 1194
proto udp
dev tun
dev-node "OpenVPN"

; 证书和密钥配置
ca keys\ca.crt
cert keys\server.crt
key keys\server.key
dh keys\dh.pem
tls-auth keys\ta.key 0
key-direction 0

; 服务器网络配置
server 10.8.0.0 255.255.255.0

; 客户端访问内网配置
ifconfig-pool-persist ipp.txt
route 192.168.1.0 255.255.255.0
push "route 192.168.1.0 255.255.255.0"

; DNS配置
push "register-dns"
push "block-outside-dns"

; 安全加密配置
cipher AES-256-GCM
auth SHA256

; 保持连接
keepalive 10 60
ping-timer-rem
persist-key
persist-tun

; 日志配置
status openvpn-status.log
log-append openvpn.log
verb 3
explicit-exit-notify 1
```

### 3.4 复制证书文件到服务器目录

```powershell
# 复制证书文件
copy keys\ca.crt C:\VPN-Config\server\keys\
copy keys\server.crt C:\VPN-Config\server\keys\
copy keys\server.key C:\VPN-Config\server\keys\
copy keys\dh.pem C:\VPN-Config\server\keys\
copy keys\ta.key C:\VPN-Config\server\keys\
```

---

## 四、启动VPN服务

### 4.1 创建虚拟网卡

```powershell
# 1. 打开OpenVPN GUI（已安装会自动创建）

# 2. 或者手动创建tap设备
# 控制面板 → 网络和共享中心 → 更改适配器设置
# 如果看到"TAP-Windows Adapter V9"，说明已安装
```

### 4.2 启动服务

```powershell
# 方法1：通过服务管理器
# 1. 按 Win + R，输入 services.msc
# 2. 找到 "OpenVPN Service"
# 3. 右键 → 启动
# 4. 右键 → 属性 → 启动类型：自动

# 方法2：通过命令行
net start OpenVPNService

# 方法3：通过OpenVPN GUI
# 右键点击系统托盘图标
# 选择 "Connect"
```

### 4.3 验证服务运行

```powershell
# 检查服务状态
Get-Service OpenVPNService

# 检查端口监听
netstat -an | findstr "1194"

# 查看日志
Get-Content "C:\Program Files\OpenVPN\log\openvpn.log" -Tail 20
```

---

## 五、创建客户端配置

### 5.1 为每个用户生成证书

```powershell
# 进入证书目录
cd "C:\Program Files\OpenVPN\easy-rsa"

# 重新加载vars
.\vars.bat

# 为用户1生成证书
.\build-key.bat client1
# Common Name: client1
# 其他一路回车
# 签名时输入 y

# 为用户2生成证书（重复步骤）
.\build-key.bat client2
```

### 5.2 创建客户端配置模板

```powershell
# 为每个用户创建配置文件
notepad C:\VPN-Config\clients\client1.ovpn
```

**client1.ovpn 内容（**根据实际服务器IP修改**）：**

```ini
; 客户端基本配置
client
dev tun
proto udp
remote 你的服务器公网IP 1194
resolv-retry infinite
nobind
persist-key
persist-tun

; 证书和密钥配置
ca keys\ca.crt
cert keys\client1.crt
key keys\client1.key
tls-auth keys\ta.key 1
key-direction 1

; 安全加密配置
cipher AES-256-GCM
auth SHA256

; 服务器验证
remote-cert-tls server

; 日志
verb 3
```

### 5.3 打包用户配置包

```powershell
# 为每个用户创建配置包
mkdir C:\VPN-Config\clients\client1

# 复制配置文件
copy C:\VPN-Config\clients\client1.ovpn C:\VPN-Config\clients\client1\

# 复制证书文件
copy keys\ca.crt C:\VPN-Config\clients\client1\
copy keys\client1.crt C:\VPN-Config\clients\client1\
copy keys\client1.key C:\VPN-Config\clients\client1\
copy keys\ta.key C:\VPN-Config\clients\client1\

# 创建keys子目录并移动证书
mkdir C:\VPN-Config\clients\client1\keys
move C:\VPN-Config\clients\client1\*.crt C:\VPN-Config\clients\client1\keys\
move C:\VPN-Config\clients\client1\*.key C:\VPN-Config\clients\client1\keys\
move C:\VPN-Config\clients\client1\*.ovpn C:\VPN-Config\clients\client1\keys\

# 修改配置文件中的路径
notepad C:\VPN-Config\clients\client1\keys\client1.ovpn
```

**修改后的client1.ovpn：**

```ini
; 证书和密钥配置（相对路径）
ca ca.crt
cert client1.crt
key client1.key
tls-auth ta.key 1
key-direction 1
```

```powershell
# 打包为ZIP文件（可选）
Compress-Archive -Path C:\VPN-Config\clients\client1 -DestinationPath C:\VPN-Config\client1.zip
```

---

## 六、配置防火墙

### 6.1 Windows防火墙配置

```powershell
# 允许OpenVPN端口（UDP 1194）
New-NetFirewallRule -DisplayName "OpenVPN Server UDP 1194" `
    -Direction Inbound `
    -Action Allow `
    -Protocol UDP `
    -LocalPort 1194 `
    -Profile Any

# 允许VPN内网访问应用端口
New-NetFirewallRule -DisplayName "VPN to Application" `
    -Direction Inbound `
    -Action Allow `
    -Protocol TCP `
    -LocalPort 9095 `
    -RemoteAddress 10.8.0.0/24 `
    -Profile Any

# 允许VPN内网访问MySQL
New-NetFirewallRule -DisplayName "VPN to MySQL" `
    -Direction Inbound `
    -Action Allow `
    -Protocol TCP `
    -LocalPort 3306 `
    -RemoteAddress 10.8.0.0/24 `
    -Profile Any
```

### 6.2 路由器端口转发（如需要）

```
如果服务器在内网，需要配置路由器端口转发：

路由器配置：
  • 外部端口: UDP 1194
  • 内部IP: 服务器内网IP (192.168.1.100)
  • 内部端口: UDP 1194
  • 协议: UDP

建议：联系网络管理员配置
```

---

## 七、配置应用服务器

### 7.1 修改应用配置

```properties
# application.properties

# 服务器绑定地址（仅监听VPN网络）
server.address=0.0.0.0
server.port=9095

# 允许来自VPN网络的访问
cors.allowed-origins=http://10.8.0.0/24

# 数据库配置
spring.datasource.url=jdbc:mysql://localhost:3306/ceams
```

### 7.2 配置启动脚本

```batch
@echo off
REM start.bat - 允许所有网络访问

echo 启动建筑企业账目管理系统...
echo 监听地址: 0.0.0.0
echo 服务端口: 9095

java -jar target\ceams-1.0-SNAPSHOT.jar ^
    --server.address=0.0.0.0 ^
    --server.port=9095

pause
```

---

## 八、客户端分发和使用

### 8.1 分发文件清单

```
每个用户需要接收以下文件：

client1 配置包/
├── client1.ovpn      # 主配置文件
├── ca.crt           # CA证书
├── client1.crt      # 客户端证书
├── client1.key      # 客户端私钥
└── ta.key          # TLS密钥

⚠️ 重要提醒：
   • 通过安全渠道分发（加密U盘/加密压缩包）
   • 包含私钥的文件要特别保密
   • 建议分发后立即修改密码
```

### 8.2 Windows客户端安装步骤

```
用户接收文件后：

1. 安装OpenVPN
   • 下载安装包
   • 一路下一步完成安装

2. 复制配置文件
   • 创建文件夹：C:\Users\你的用户名\OpenVPN\config\
   • 将收到的5个文件放入此文件夹

3. 连接VPN
   • 打开OpenVPN GUI
   • 右键点击系统托盘图标
   • 选择"连接"
   • 等待连接成功（图标变绿色）

4. 访问应用系统
   • 打开浏览器
   • 访问 http://192.168.1.100:9095
```

### 8.3 Android客户端安装步骤

```
Android用户操作：

1. 安装OpenVPN Connect
   • Google Play搜索 "OpenVPN Connect"
   • 或应用宝搜索下载

2. 导入配置文件
   • 打开OpenVPN Connect
   • 点击右上角 "+"
   • 选择 "Import Profile from File"
   • 选择.ovpn文件

3. 连接VPN
   • 点击连接按钮
   • 输入用户名密码（如果配置了）
   • 等待连接成功

4. 访问应用系统
   • 打开浏览器
   • 访问 http://192.168.1.100:9095
```

---

## 九、测试和验证

### 9.1 服务器端测试

```powershell
# 1. 检查OpenVPN服务状态
Get-Service OpenVPNService

# 2. 检查端口监听
netstat -an | findstr "1194"
# 应该看到：UDP 0.0.0.0:1194

# 3. 查看连接日志
Get-Content "C:\Program Files\OpenVPN\log\openvpn-status.log"
```

### 9.2 客户端测试

```
客户端测试步骤：

1. 确认VPN连接成功
   • 图标为绿色
   • 显示"连接成功"

2. 测试网络连通性
   • 打开命令提示符
   • ping 192.168.1.100
   • 如果有响应 = 网络正常

3. 测试应用访问
   • 打开浏览器
   • 访问 http://192.168.1.100:9095
   • 应该看到应用登录页面
```

### 9.3 日志查看

```powershell
# OpenVPN连接日志
C:\Program Files\OpenVPN\log\openvpn.log

# OpenVPN状态日志
C:\Program Files\OpenVPN\log\openvpn-status.log

# 应用日志（如果使用Tomcat）
C:\tomcat\logs\catalina.out
```

---

## 十、常见问题解决

### 10.1 服务无法启动

```
可能原因：
  ❌ 配置文件语法错误
  ❌ 证书文件缺失
  ❌ 端口被占用

解决方法：
  1. 检查配置文件
     notepad C:\VPN-Config\server\server.ovpn
  
  2. 检查证书文件
     dir C:\VPN-Config\server\keys\
  
  3. 检查端口占用
     netstat -an | findstr "1194"
```

### 10.2 客户端连接失败

```
可能原因：
  ❌ 服务器IP错误
  ❌ 防火墙阻止
  ❌ 证书配置错误

解决方法：
  1. 确认服务器公网IP正确
     修改client.ovpn中的remote地址

  2. 检查服务器防火墙
     确认UDP 1194端口已开放

  3. 检查客户端证书
     确认ca.crt、client.crt、client.key、ta.key都存在
```

### 10.3 连接成功但无法访问内网

```
可能原因：
  ❌ 路由配置错误
  ❌ 服务器未启用IP转发

解决方法：
  1. 检查服务器路由配置
     route print
  
  2. 启用IP转发（Windows）
     # 以管理员运行PowerShell
     Set-ItemProperty -Path "HKLM:\SYSTEM\CurrentControlSet\Services\Tcpip\Parameters" -Name "IPEnableRouter" -Value 1 -Type DWord
  
  3. 重启服务器
```

---

## 十一、安全建议

### 11.1 服务器安全

```
✅ 建议的安全措施：

1. 使用强密码保护私钥
   • 为证书设置密码
   • 分发时使用加密压缩包

2. 限制用户数量
   • 根据实际需求分配账户
   • 定期清理不需要的账户

3. 定期更新证书
   • 建议每6-12个月更换
   • 及时吊销离职员工证书

4. 监控连接日志
   • 定期检查连接记录
   • 关注异常登录
```

### 11.2 证书吊销

```powershell
# 吊销用户证书（如果需要）
cd "C:\Program Files\OpenVPN\easy-rsa"
.\vars.bat
.\revoke-full.bat client1

# 将吊销列表复制到配置目录
copy keys\crl.pem C:\VPN-Config\server\keys\

# 在server.ovpn中添加：
crl-verify keys\crl.pem
```

### 11.3 定期维护

```
维护计划：

每月：
  • 检查连接日志
  • 检查证书有效期
  • 清理不需要的连接

每季度：
  • 安全审计
  • 更新OpenVPN版本
  • 备份证书和配置

每年：
  • 更换所有证书
  • 评估安全策略
  • 更新用户权限
```

---

## 十二、快速参考

### 12.1 关键命令

```powershell
# 启动VPN服务
net start OpenVPNService

# 停止VPN服务
net stop OpenVPNService

# 检查服务状态
Get-Service OpenVPNService

# 查看连接日志
Get-Content "C:\Program Files\OpenVPN\log\openvpn.log" -Tail 50

# 查看当前连接
Get-Content "C:\Program Files\OpenVPN\log\openvpn-status.log"
```

### 12.2 关键文件位置

```
服务器文件：
  • 配置文件：C:\VPN-Config\server\
  • 证书文件：C:\VPN-Config\server\keys\
  • 日志文件：C:\Program Files\OpenVPN\log\

客户端文件：
  • 客户端配置：C:\VPN-Config\clients\client1\
  • 用户配置文件：C:\Users\用户名\OpenVPN\config\

证书文件：
  • CA证书：ca.crt
  • 服务器证书：server.crt
  • 服务器私钥：server.key
  • 客户端证书：client1.crt
  • 客户端私钥：client1.key
  • TLS密钥：ta.key
  • DH参数：dh.pem
```

---

## 十三、技术支持

```
遇到问题时的排查步骤：

1. 查看日志
   C:\Program Files\OpenVPN\log\openvpn.log

2. 检查服务状态
   Get-Service OpenVPNService

3. 检查端口监听
   netstat -an | findstr "1194"

4. 检查防火墙规则
   Get-NetFirewallRule | Where-Object {$_.DisplayName -like "*OpenVPN*"}

5. 查看连接状态
   Get-Content "C:\Program Files\OpenVPN\log\openvpn-status.log"
```

---

**文档版本**: 1.0  
**最后更新**: 2026-05-12  
**适用版本**: OpenVPN 2.6.x  
**目标用户**: VPN服务器管理员
