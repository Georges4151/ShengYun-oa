# 企业内网VPN访问方案

## 一、方案概述

### 1.1 设计目标

实现建筑企业账目管理系统的**完全内网访问**架构：
- 所有用户必须通过VPN连接才能访问系统
- 禁止公网直接访问应用服务器
- 确保数据传输安全和企业数据保护

### 1.2 访问架构

```
Internet
    │
    │
┌───┴──────────────────────────────────────┐
│           企业防火墙                      │
│         (仅开放VPN端口)                   │
│          UDP 1194 (OpenVPN)             │
└────────┬───────────────────────────────┘
         │
         │ VPN隧道建立
         │
┌────────┴──────────────────────────────────────┐
│              OpenVPN 服务器                    │
│           (Windows Server)                     │
│                                               │
│   ┌─────────────────────────────────────────┐ │
│   │         VPN 虚拟网络                     │ │
│   │                                         │ │
│   │  VPN客户端分配IP: 10.8.0.x              │ │
│   │  内网服务器IP: 192.168.1.100            │ │
│   │                                         │ │
│   └───────────────┬─────────────────────────┘ │
│                   │                           │
│                   │ VPN路由转发                │
│                   │                           │
└───────────────────┼───────────────────────────┘
                    │
                    ▼
┌──────────────────────────────────────────────┐
│           内网应用服务器                       │
│  ┌────────────┐  ┌────────────┐  ┌─────────┐ │
│  │ Spring Boot │  │  MySQL    │  │  Redis  │ │
│  │   :9095    │  │   :3306   │  │  :6379  │ │
│  └────────────┘  └────────────┘  └─────────┘ │
│                                               │
│  IP: 192.168.1.100                          │
└──────────────────────────────────────────────┘
```

### 1.3 网络规划

#### IP地址分配

| 设备/网络 | IP地址 | 说明 |
|-----------|--------|------|
| VPN服务器 | 192.168.1.100 | 应用服务器（兼VPN） |
| VPN虚拟网络 | 10.8.0.0/24 | OpenVPN分配的虚拟IP |
| VPN客户端池 | 10.8.0.2 - 10.8.0.254 | 客户端连接后分配 |
| 企业内网 | 192.168.1.0/24 | 现有内网网段 |
| 数据库服务器 | 192.168.1.101 | MySQL（可选独立） |

---

## 二、服务器配置 (Windows Server)

### 2.1 OpenVPN服务器安装

#### 步骤1：下载OpenVPN

```
下载地址：https://openvpn.net/community-downloads/
选择：OpenVPN 2.6.x (Windows Installer)
```

#### 步骤2：安装OpenVPN

```powershell
# 使用管理员权限运行安装程序
# 安装选项：
# ✓ OpenVPN Service (作为系统服务运行)
# ✓ OpenVPN GUI (图形界面)
# ✓ OpenSSL Utilities
# ✓ OpenVPN RSA Module
```

#### 步骤3：配置服务器

```powershell
# 导航到OpenVPN配置目录
cd "C:\Program Files\OpenVPN\config"

# 创建服务器配置文件
# server.ovpn
```

#### 步骤4：服务器配置内容

```ini
# server.ovpn

# 基础配置
port 1194
proto udp
dev tun
dev-node "OpenVPN"

# SSL/TLS证书配置
ca ca.crt
cert server.crt
key server.key
dh dh.pem

# VPN网络配置
server 10.8.0.0 255.255.255.0

# 客户端访问内网
ifconfig-pool-persist ipp.txt
route 192.168.1.0 255.255.255.0

# 推送路由到客户端（客户端自动配置路由表）
push "route 192.168.1.0 255.255.255.0"

# DNS配置
push "register-dns"
push "block-outside-dns"

# 安全配置
cipher AES-256-GCM
auth SHA256
tls-auth ta.key 0
key-direction 0

# 性能优化
keepalive 10 60
ping-timer-rem
persist-key
persist-tun

# 日志配置
status openvpn-status.log
log-append openvpn.log
verb 3
```

#### 步骤5：生成证书

```powershell
# 以管理员身份运行 "OpenVPN EasyRSA Shell"

# 1. 初始化PKI
init-pki

# 2. 创建CA证书
build-ca
# 输入信息：
#   Country Name: CN
#   State: Beijing
#   Locality: Beijing
#   Organization: YourCompany
#   Common Name: VPN-CA
#   Email: admin@company.com

# 3. 生成服务器证书
build-key-server server
# 输入信息：
#   Common Name: server
#   签名请求时选 Yes

# 4. 生成Diffie-Hellman参数
build-dh

# 5. 生成TLS密钥
openvpn --genkey --secret ta.key
```

#### 步骤6：配置防火墙

```powershell
# Windows防火墙规则

# 允许OpenVPN端口
New-NetFirewallRule -DisplayName "OpenVPN Server" `
    -Direction Inbound `
    -Action Allow `
    -Protocol UDP `
    -LocalPort 1194 `
    -Profile Any

# 允许VPN流量转发
New-NetFirewallRule -DisplayName "VPN Internal Traffic" `
    -Direction Inbound `
    -Action Allow `
    -Protocol Any `
    -RemoteAddress 10.8.0.0/24 `
    -LocalAddress 192.168.1.0/24

# 启用IP转发
Set-ItemProperty -Path "HKLM:\SYSTEM\CurrentControlSet\Services\Tcpip\Parameters" `
    -Name "IPEnableRouter" `
    -Value 1 `
    -Type DWord
```

### 2.2 服务器安全配置

#### 禁用公网访问

```powershell
# 1. 配置Windows防火墙 - 仅允许VPN

# 移除或禁用公网HTTP/HTTPS规则（如果有）
Remove-NetFirewallRule -DisplayName "CEAMS Public HTTP" -ErrorAction SilentlyContinue
Remove-NetFirewallRule -DisplayName "CEAMS Public HTTPS" -ErrorAction SilentlyContinue

# 2. 仅允许VPN客户端访问9095端口
New-NetFirewallRule -DisplayName "CEAMS VPN Only" `
    -Direction Inbound `
    -Action Allow `
    -Protocol TCP `
    -LocalPort 9095 `
    -RemoteAddress 10.8.0.0/24 `
    -Profile Any

# 3. 阻止公网访问9095端口
New-NetFirewallRule -DisplayName "Block CEAMS Public" `
    -Direction Inbound `
    -Action Block `
    -Protocol TCP `
    -LocalPort 9095 `
    -RemoteAddress Any `
    -Profile Any

# 4. 阻止公网访问MySQL
New-NetFirewallRule -DisplayName "Block MySQL Public" `
    -Direction Inbound `
    -Action Block `
    -Protocol TCP `
    -LocalPort 3306 `
    -RemoteAddress Any `
    -Profile Any
```

#### IP安全策略

```powershell
# 仅允许VPN网段访问数据库
New-NetFirewallRule -DisplayName "MySQL VPN Only" `
    -Direction Inbound `
    -Action Allow `
    -Protocol TCP `
    -LocalPort 3306 `
    -RemoteAddress 10.8.0.0/24

# 阻止外部MySQL访问
New-NetFirewallRule -DisplayName "MySQL Block External" `
    -Direction Inbound `
    -Action Block `
    -Protocol TCP `
    -LocalPort 3306
```

---

## 三、客户端配置

### 3.1 Windows客户端配置

#### 下载安装OpenVPN客户端

```
下载地址：https://openvpn.net/community-downloads/
选择：OpenVPN 2.6.x (Windows Installer)
```

#### 客户端配置文件

```ini
# client.ovpn

client
dev tun
proto udp
remote YOUR_SERVER_IP 1194
resolv-retry infinite
nobind
persist-key
persist-tun

# 证书配置
ca ca.crt
cert client.crt
key client.key

# TLS配置
tls-auth ta.key 1
cipher AES-256-GCM
auth SHA256

# 服务器验证
remote-cert-tls server

verb 3
```

#### 获取客户端证书

```powershell
# 从服务器复制以下文件到客户端
# 位置: C:\Program Files\OpenVPN\config\

1. ca.crt           # CA证书（所有客户端相同）
2. client1.crt      # 客户端证书（每个用户不同）
3. client1.key      # 客户端密钥（每个用户不同）
4. ta.key          # TLS密钥（所有客户端相同）
5. client.ovpn     # 客户端配置文件
```

#### 创建客户端证书

```powershell
# 在OpenVPN服务器上为每个用户创建证书

# 在EasyRSA Shell中执行：
build-key client1  # 用户1
build-key client2  # 用户2
# ...

# 为每个用户创建独立配置文件夹：
mkdir C:\Users\vpn\client1
mkdir C:\Users\vpn\client2
```

### 3.2 Android客户端配置

#### 安装OpenVPN Connect

```
应用商店：Google Play / 应用宝
应用名称：OpenVPN Connect
```

#### 导入配置文件

```xml
<!-- android_vpn_config.ovpn -->

client
dev tun
proto udp
remote YOUR_SERVER_IP 1194
resolv-retry infinite
nobind
persist-key
persist-tun

ca ca.crt
cert android_client.crt
key android_client.key

tls-auth ta.key 1
cipher AES-256-GCM
auth SHA256
remote-cert-tls server

# Android特定配置
socket-flags TCP_NODELAY
```

#### Android连接步骤

```
1. 打开 OpenVPN Connect 应用
2. 点击 "Import" 导入配置文件
3. 选择 "Import Profile from File"
4. 选择下载的 .ovpn 文件
5. 点击 "Connect"
6. 输入用户名/密码（如果配置了）
7. 等待连接成功
```

### 3.3 配置文件分发

#### 安全分发方案

```markdown
# 推荐：使用安全的文件分发方式

## 方式1：IT部门手动分发

1. IT管理员生成用户证书
2. 打包配置文件（使用ZIP加密）
3. 通过安全渠道分发给用户（USB/加密邮件）
4. 提供安装指南

## 方式2：内部下载网站（推荐）

1. 搭建内部HTTPS网站
2. 用户使用公司邮箱登录
3. 下载个人专属配置文件
4. 下载后24小时自动删除

## 方式3：二维码分发

1. 生成配置文件二维码
2. 用户扫码导入
3. 适用于移动设备
```

---

## 四、应用服务器配置

### 4.1 修改应用配置

```properties
# application.properties

# 服务器绑定地址 - 仅监听内网
server.address=192.168.1.100
server.port=9095

# 允许的VPN客户端IP范围
cors.allowed-origins=http://10.8.0.0/24,http://192.168.1.0/24

# Redis配置
spring.redis.host=192.168.1.100
spring.redis.port=6379
```

### 4.2 配置启动脚本

```batch
@echo off
REM start.bat - 仅监听内网地址

set SERVER_IP=192.168.1.100
set SERVER_PORT=9095

echo 启动建筑企业账目管理系统...
echo 仅允许VPN内网访问
echo 服务器IP: %SERVER_IP%
echo 服务端口: %SERVER_PORT%

java -jar target\ceams-1.0-SNAPSHOT.jar ^
    --server.address=%SERVER_IP% ^
    --server.port=%SERVER_PORT%
```

### 4.3 服务注册

```powershell
# 创建Windows服务自动启动

sc create CEAMS binPath= "C:\path\to\java.exe -jar C:\path\to\ceams.jar" ^
    start= auto displayname= "CEAMS Application Server"

# 配置服务恢复选项
sc failure CEAMS reset= 86400 actions= restart/60000/restart/120000/""

# 启动服务
net start CEAMS
```

---

## 五、安全策略

### 5.1 访问控制矩阵

```
┌─────────────────────────────────────────────────────────────┐
│                    访问控制策略                              │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  公网 (Internet)                                            │
│     │                                                      │
│     │ ❌ 完全禁止                                          │
│     ▼                                                      │
│  ┌──────────────────────────────────────────────┐          │
│  │  防火墙: 阻止所有入站流量                    │          │
│  │  • 9095端口: 仅允许 10.8.0.0/24            │          │
│  │  • 3306端口: 仅允许 10.8.0.0/24            │          │
│  │  • 6379端口: 仅允许 10.8.0.0/24            │          │
│  └──────────────────────────────────────────────┘          │
│                         │                                   │
│                         │ 允许VPN                          │
│                         ▼                                   │
│  ┌──────────────────────────────────────────────┐          │
│  │  OpenVPN 服务器                             │          │
│  │  • UDP 1194: 开放                          │          │
│  │  • 证书+密码双重认证                        │          │
│  │  • 日志记录所有连接                         │          │
│  └──────────────────────────────────────────────┘          │
│                         │                                   │
│                         │ VPN隧道                          │
│                         ▼                                   │
│  ┌──────────────────────────────────────────────┐          │
│  │  VPN客户端 (10.8.0.x)                      │          │
│  │  • 认证后分配虚拟IP                         │          │
│  │  • 自动配置路由表                           │          │
│  └──────────────────────────────────────────────┘          │
│                         │                                   │
│                         │ 访问                              │
│                         ▼                                   │
│  ┌──────────────────────────────────────────────┐          │
│  │  应用服务器 (192.168.1.100)                 │          │
│  │  • 仅监听内网IP                             │          │
│  │  • 9095端口服务                            │          │
│  └──────────────────────────────────────────────┘          │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 5.2 用户认证配置

#### OpenVPN + 密码认证

```ini
# server.ovpn 添加
plugin "C:\Program Files\OpenVPN\plugin\openvpn-plugin-auth-pam.dll" "login"

# client.ovpn 添加
auth-user-pass
```

#### 创建VPN用户

```powershell
# Windows本地用户
net user vpnuser1 P@ssw0rd! /add /expires:never
net user vpnuser2 P@ssw0rd! /add /expires:never

# 将用户添加到VPN用户组
net localgroup "VPN Users" vpnuser1 /add
net localgroup "VPN Users" vpnuser2 /add
```

### 5.3 日志和监控

#### VPN连接日志

```powershell
# 配置OpenVPN日志
# server.ovpn
log-append "C:\Program Files\OpenVPN\log\openvpn.log"
status "C:\Program Files\OpenVPN\log\openvpn-status.log" 10

# 定期检查连接状态
Get-Content "C:\Program Files\OpenVPN\log\openvpn-status.log" -Tail 20
```

#### 审计日志

```markdown
# 记录内容：
1. 连接时间
2. 断开时间
3. 用户名
4. 分配IP
5. 连接时长
6. 数据传输量
```

---

## 六、部署步骤

### 6.1 服务器部署流程

```
阶段1: 服务器准备 (预计2小时)
  ├── 安装Windows Server 2019
  ├── 配置静态IP (192.168.1.100)
  ├── 安装OpenVPN服务器
  └── 配置Windows防火墙

阶段2: 证书配置 (预计1小时)
  ├── 生成CA证书
  ├── 生成服务器证书
  ├── 生成Diffie-Hellman参数
  ├── 生成TLS密钥
  └── 配置服务器使用证书

阶段3: 安全配置 (预计1小时)
  ├── 配置VPN认证方式
  ├── 创建VPN用户账户
  ├── 配置防火墙规则
  └── 禁用公网访问

阶段4: 应用部署 (预计1小时)
  ├── 部署Java应用
  ├── 配置仅监听内网
  ├── 配置数据库
  └── 测试访问
```

### 6.2 客户端部署流程

```
阶段1: 客户端软件安装 (预计30分钟/用户)
  ├── 下载OpenVPN客户端
  ├── 安装软件
  └── 重启计算机

阶段2: 配置导入 (预计10分钟/用户)
  ├── 导入配置文件
  ├── 导入证书
  └── 配置连接参数

阶段3: 测试验证 (预计10分钟/用户)
  ├── 连接VPN
  ├── 访问应用系统
  └── 测试各项功能
```

---

## 七、用户使用指南

### 7.1 Windows客户端使用

```
1. 连接到VPN
   • 打开OpenVPN GUI
   • 右键点击系统托盘图标
   • 选择 "Connect"
   • 输入用户名密码
   • 等待连接成功

2. 访问应用系统
   • 确保VPN已连接
   • 打开浏览器
   • 访问 http://192.168.1.100:9095
   • 或访问 http://内网域名:9095

3. 断开VPN
   • 完成工作后
   • 右键点击系统托盘图标
   • 选择 "Disconnect"
```

### 7.2 Android客户端使用

```
1. 连接到VPN
   • 打开OpenVPN Connect
   • 点击连接按钮
   • 输入用户名密码
   • 等待连接成功（显示绿色）

2. 访问应用系统
   • 确保VPN已连接
   • 打开浏览器或应用
   • 访问 http://192.168.1.100:9095

3. 断开VPN
   • 在OpenVPN Connect中
   • 点击断开按钮
```

### 7.3 常见问题

```
Q1: VPN连接成功但无法访问系统？
A1: 
   • 检查VPN状态是否显示已连接
   • 尝试ping 192.168.1.100
   • 检查防火墙是否阻止
   • 联系IT部门确认权限

Q2: 连接时提示证书错误？
A2:
   • 确认使用的是最新配置文件
   • 检查系统时间是否正确
   • 重新下载配置文件

Q3: 无法连接VPN服务器？
A3:
   • 检查网络是否正常
   • 确认VPN服务器地址正确
   • 检查防火墙设置
   • 联系IT部门确认账户状态
```

---

## 八、维护管理

### 8.1 用户管理

```powershell
# 添加新用户
net user vpnuser3 P@ssw0rd! /add
net localgroup "VPN Users" vpnuser3 /add

# 禁用用户
net user vpnuser1 /active:no

# 删除用户
net user vpnuser1 /delete

# 查看所有VPN用户
net localgroup "VPN Users"
```

### 8.2 证书管理

```powershell
# 吊销用户证书
# 在EasyRSA Shell中执行：
revoke-full client3

# 复制吊销列表到配置目录
copy C:\EasyRSA\pki\crl.pem C:\"Program Files"\OpenVPN\config\

# 更新服务器配置 server.ovpn 添加：
crl-verify crl.pem
```

### 8.3 日志审计

```powershell
# 查看VPN连接日志
Get-Content "C:\Program Files\OpenVPN\log\openvpn.log" -Tail 100

# 查看连接状态
Get-Content "C:\Program Files\OpenVPN\log\openvpn-status.log"

# 查看Windows事件日志
Get-EventLog -LogName Security -Newest 50 | Where-Object {$_.Message -like "*VPN*"}
```

### 8.4 备份配置

```powershell
# 备份VPN配置
$BackupDir = "C:\VPNBackup"
$Date = Get-Date -Format "yyyyMMdd_HHmmss"

# 备份证书
Copy-Item "C:\Program Files\OpenVPN\config\*.crt" "$BackupDir\certs\"
Copy-Item "C:\Program Files\OpenVPN\config\*.key" "$BackupDir\keys\"
Copy-Item "C:\Program Files\OpenVPN\config\*.ovpn" "$BackupDir\configs\"

# 压缩备份
Compress-Archive -Path "$BackupDir\*" -DestinationPath "C:\VPNBackup_$Date.zip"
```

---

## 九、性能优化

### 9.1 服务器优化

```ini
# server.ovpn 性能配置

# 启用压缩
comp-lzo adaptive

# 优化缓冲区
txqueuelen 1000

# 多核支持
multi-instance

# 连接优化
keepalive 10 60
ping-timer-rem
persist-key
persist-tun
```

### 9.2 带宽建议

```
小型团队 (5-20人):
  • VPN带宽: 10Mbps
  • 单用户平均: 500Kbps
  • 适合: 文档、报表、小文件

中型团队 (20-50人):
  • VPN带宽: 50Mbps
  • 单用户平均: 1Mbps
  • 适合: 图片、中等文件、在线操作

大型团队 (50+人):
  • VPN带宽: 100Mbps+
  • 单用户平均: 2Mbps
  • 建议: 考虑多服务器负载均衡
```

---

## 十、故障排除

### 10.1 常见错误

```
错误1: TLS handshake failed
原因: 证书时间不同步或损坏
解决: 
  • 检查服务器和客户端时间
  • 重新生成证书
  • 确认ta.key文件一致

错误2: Connection refused
原因: 服务器未运行或端口被阻止
解决:
  • 检查OpenVPN服务状态
  • 检查防火墙规则
  • 确认服务器IP正确

错误3: AUTH: Received AUTH_FAILED
原因: 用户名密码错误或账户被禁用
解决:
  • 确认用户名密码正确
  • 检查用户账户状态
  • 确认用户组权限
```

### 10.2 诊断命令

```powershell
# 检查OpenVPN服务状态
Get-Service -Name "OpenVPNService"

# 检查端口监听
netstat -an | findstr "1194"

# 测试网络连通性
Test-NetConnection -ComputerName 192.168.1.100 -Port 9095

# 查看VPN状态
Get-Content "C:\Program Files\OpenVPN\log\openvpn-status.log"
```

---

## 十一、成本估算

### 11.1 软件成本

```
OpenVPN社区版: 免费
Windows Server: 约2000-5000元（一次性）
域名（如需要）: 约50-100元/年
SSL证书: 免费（Let's Encrypt）
```

### 11.2 硬件成本

```
小型部署 (5-20用户):
  • 服务器: 5000-10000元
  • 网络设备: 1000-3000元
  • 合计: 6000-13000元

中型部署 (20-50用户):
  • 服务器: 10000-20000元
  • 网络设备: 3000-5000元
  • 合计: 13000-25000元
```

### 11.3 运维成本

```
带宽费用: 根据实际使用量
维护人员: 兼职或外包
技术支持: 可选年度服务合同

建议:
  • 小型企业: 兼职IT + 年度检查
  • 中型企业: 专职IT + 定期培训
```

---

## 十二、后续扩展

### 12.1 未来升级选项

```
短期 (1年内):
  • 添加双因素认证 (Google Authenticator)
  • 启用连接日志审计
  • 添加带宽监控

中期 (1-2年):
  • 部署VPN冗余服务器
  • 实施负载均衡
  • 添加入侵检测系统

长期 (2+年):
  • 迁移到云VPN服务
  • 部署SD-WAN
  • 实施零信任网络架构
```

### 12.2 合规建议

```
数据安全:
  • 定期安全审计
  • 加密存储敏感数据
  • 实施数据备份策略

访问控制:
  • 最小权限原则
  • 定期审查用户权限
  • 离职账户及时禁用

合规性:
  • 遵守相关法律法规
  • 记录审计日志
  • 定期安全评估
```

---

## 十三、总结

### 13.1 方案优势

✅ **安全**: 所有访问必须通过VPN，无法从公网直接访问  
✅ **可控**: 集中管理用户访问权限，可追溯审计  
✅ **可靠**: 使用成熟的OpenVPN技术，稳定可靠  
✅ **灵活**: 支持Windows、Mac、Linux、Android、iOS  
✅ **经济**: 使用开源软件，降低成本  

### 13.2 实施建议

1. **测试环境先行**: 先在测试环境验证所有配置
2. **分批部署**: 先部署IT部门，再推广到其他用户
3. **充分培训**: 确保所有用户了解VPN使用方法
4. **文档齐全**: 准备用户手册和故障排除指南
5. **技术支持**: 建立技术支持渠道，及时响应问题

---

**文档版本**: 1.0  
**最后更新**: 2026-05-12  
**适用系统版本**: CEAMS 1.0+  
**安全等级**: 企业级
