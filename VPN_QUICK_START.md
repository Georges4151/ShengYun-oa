# VPN项目部署 - 快速开始指南

## 一句话总结
```
使用VPN让您的建筑企业账目管理系统只能通过安全隧道访问，
任何人必须先连接VPN才能使用系统。
```

---

## 🚀 快速部署（5步完成）

### 步骤1：生成证书（最关键）

**打开EasyRSA：**
```
1. 打开 C:\VPN-Config\EasyRSA-3.2.6
2. 双击 "EasyRSA Start.bat"
3. 在打开的窗口中输入：
```

```bash
./easyrsa init-pki
./easyrsa build-ca
./easyrsa build-server-full server nopass
./easyrsa gen-dh
openssl rand -hex 32 > ta.key
```

### 步骤2：复制证书

```powershell
mkdir C:\VPN-Config\server\keys
copy pki\ca.crt C:\VPN-Config\server\keys\
copy pki\issued\server.crt C:\VPN-Config\server\keys\
copy pki\private\server.key C:\VPN-Config\server\keys\
copy pki\dh.pem C:\VPN-Config\server\keys\
copy ta.key C:\VPN-Config\server\keys\
```

### 步骤3：创建服务器配置

创建 `C:\VPN-Config\server\server.ovpn`：

```ini
port 1194
proto udp
dev tun
server 10.8.0.0 255.255.255.0
ca keys\ca.crt
cert keys\server.crt
key keys\server.key
dh keys\dh.pem
tls-auth keys\ta.key 0
cipher AES-256-GCM
auth SHA256
```

### 步骤4：配置防火墙

```powershell
# 管理员PowerShell
New-NetFirewallRule -DisplayName "OpenVPN" -Direction Inbound -Action Allow -Protocol UDP -LocalPort 1194
New-NetFirewallRule -DisplayName "VPN-App" -Direction Inbound -Action Allow -Protocol TCP -LocalPort 9095 -RemoteAddress 10.8.0.0/24
```

### 步骤5：启动服务

```powershell
# 复制配置到OpenVPN
copy C:\VPN-Config\server\* "C:\Program Files\OpenVPN\config\" -Recurse

# 启动VPN
Start-Service OpenVPNService

# 启动应用
cd E:\sxassist
java -jar target\ceams-1.0-SNAPSHOT.jar --server.address=0.0.0.0
```

---

## 📱 用户使用流程

```
用户操作（每次使用）：

1. 连接VPN
   └─ 打开OpenVPN → 连接

2. 访问应用
   └─ 浏览器访问：http://192.168.1.100:9095

3. 完成工作

4. 断开VPN
   └─ 右键 → 断开
```

---

## ⚠️ 重要提醒

```
1. 应用必须监听 0.0.0.0
   ❌ 不能是 127.0.0.1

2. 防火墙必须允许VPN网络
   ✅ 10.8.0.0/24 可以访问 9095
   ❌ 公网不能直接访问 9095

3. 用户必须先连VPN
   ✅ 连接VPN后才能访问应用
   ❌ 不连VPN无法访问
```

---

## 🔍 快速检查清单

```
□ OpenVPN服务正在运行
□ 防火墙已配置
□ 应用监听0.0.0.0:9095
□ 用户有VPN配置文件
□ VPN连接正常
□ 应用访问正常
```

---

## 📞 常见问题

| 问题 | 解决方法 |
|------|----------|
| 无法连接VPN | 检查防火墙是否开放UDP 1194 |
| 连接成功但无法访问应用 | 检查应用是否监听0.0.0.0 |
| 证书错误 | 重新生成证书 |
| 服务启动失败 | 检查端口是否被占用 |

---

**详细文档**: [VPN_PROJECT_DEPLOYMENT.md](file:///e:/sxassist/VPN_PROJECT_DEPLOYMENT.md)
