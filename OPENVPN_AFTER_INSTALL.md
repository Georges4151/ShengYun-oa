# OpenVPN 安装后配置指南

## 一、确认安装成功

### 1.1 检查安装

安装完成后，按以下步骤确认：

```
验证方法：

1. 检查系统托盘
   └─ 右下角应该出现 🔑 图标
   └─ 如果是灰色，说明未连接

2. 检查开始菜单
   └─ 开始 → 所有应用 → OpenVPN
   └─ 应该看到 "OpenVPN-GUI" 和 "OpenVPN EasyRSA"

3. 检查服务状态
   └─ 按 Win + R，输入 "services.msc"
   └─ 查找 "OpenVPNService"
   └─ 状态应为"正在运行"
```

### 1.2 目录结构

```
安装完成后，检查以下目录：

C:\Program Files\OpenVPN\
├── bin\          # 可执行文件
├── config\       # 配置文件（重要！）
├── log\          # 日志文件
├── easy-rsa\    # 证书生成工具
└── sample-config\ # 示例配置文件
```

---

## 二、下一步选择

根据您的需求，选择对应的配置方式：

### 情况A：您要搭建VPN服务器

```
✅ 如果您要搭建VPN服务器：
   → 需要在公司服务器上配置OpenVPN
   → 需要生成证书
   → 需要配置客户端

继续阅读：第三节（服务器配置）
```

### 情况B：您是VPN客户端用户

```
✅ 如果您要连接现有的VPN服务器：
   → 您需要从VPN管理员获取配置文件
   → 将配置文件放到指定目录
   → 导入配置并连接

继续阅读：第四节（客户端配置）
```

---

## 三、服务器配置（如果您要搭建服务器）

### 3.1 打开配置目录

```
操作步骤：

1. 打开文件资源管理器
2. 导航到：C:\Program Files\OpenVPN\config

⚠️ 注意：
   • 这个目录用于存放服务器配置文件
   • 客户端配置也放在这里
```

### 3.2 启动EasyRSA生成证书

```
证书生成步骤：

1. 以管理员身份打开命令提示符
   └─ 右键点击"命令提示符"
   └─ 选择"以管理员身份运行"

2. 进入EasyRSA目录
   cd "C:\Program Files\OpenVPN\easy-rsa"

3. 初始化PKI
   init-config

4. 编辑 vars.bat 文件（配置证书信息）
   notepad vars.bat
   
   修改以下内容（根据实际情况修改）：
   set KEY_COUNTRY=CN
   set KEY_PROVINCE=Beijing
   set KEY_CITY=Beijing
   set KEY_ORG=YourCompany
   set KEY_EMAIL=admin@company.com
   set KEY_CN=VPN-CA
   set KEY_NAME=VPN-Server

5. 保存并关闭

6. 执行证书生成（按顺序执行）
   vars
   clean-all
   build-ca
   # 提示时一路回车，使用默认值
```

### 3.3 生成服务器证书

```
生成服务器证书：

1. 在命令提示符中执行：
   build-key-server server
   
   # 提示"Common Name"时输入：server
   # 提示"Sign the certificate?"时输入：y
   # 提示"1 out of 1 certificate requests certified, commit?"时输入：y

2. 生成Diffie-Hellman参数（需要等待几分钟）
   build-dh

3. 生成TLS密钥
   openvpn --genkey --secret ta.key
```

### 3.4 生成客户端证书

```
为每个用户生成客户端证书：

# 为用户1生成证书
build-key user1
# Common Name: user1
# 其他一路回车
# 最后 y 确认

# 为用户2生成证书
build-key user2
# Common Name: user2

# 重复以上步骤为每个用户生成证书
```

### 3.5 创建服务器配置文件

```
创建服务器配置：

1. 在 C:\Program Files\OpenVPN\config\ 目录
2. 新建文件：server.ovpn
3. 粘贴以下内容：

port 1194
proto udp
dev tun
dev-node "OpenVPN"

# 证书配置
ca keys\ca.crt
cert keys\server.crt
key keys\server.key
dh keys\dh.pem

# 服务器网络配置
server 10.8.0.0 255.255.255.0

# 客户端访问内网
ifconfig-pool-persist ipp.txt
route 192.168.1.0 255.255.255.0
push "route 192.168.1.0 255.255.255.0"

# DNS配置
push "register-dns"
push "block-outside-dns"

# 安全配置
cipher AES-256-GCM
auth SHA256
tls-auth keys\ta.key 0
key-direction 0

# 性能
keepalive 10 60
ping-timer-rem
persist-key
persist-tun

# 日志
status openvpn-status.log
log-append openvpn.log
verb 3
```

### 3.6 创建客户端配置文件

```
创建客户端配置模板（为每个客户端生成）：

1. 在 C:\Program Files\OpenVPN\config\ 目录
2. 新建文件夹：keys
3. 复制以下文件到keys文件夹：
   - ca.crt
   - ta.key
   - user1.crt
   - user1.key

4. 新建文件：client1.ovpn
5. 粘贴以下内容（根据实际情况修改）：

client
dev tun
proto udp
remote 你的服务器IP 1194
resolv-retry infinite
nobind
persist-key
persist-tun

# 证书配置
ca keys\ca.crt
cert keys\user1.crt
key keys\user1.key
tls-auth keys\ta.key 1
key-direction 1

# 安全配置
cipher AES-256-GCM
auth SHA256
remote-cert-tls server

verb 3
```

### 3.7 启动VPN服务

```
启动服务器：

1. 打开"服务"应用
   └─ 按 Win + R
   └─ 输入 services.msc

2. 找到 "OpenVPN Service"
3. 右键点击 → 启动
4. 设置为自动启动
   └─ 右键 → 属性
   └─ 启动类型：自动

5. 检查是否启动成功
   └─ 查看日志：C:\Program Files\OpenVPN\log\openvpn.log
   └─ 查找 "Initialization Sequence Completed"
```

---

## 四、客户端配置（如果您要连接VPN）

### 4.1 从管理员获取配置文件

```
您需要从VPN管理员获取以下文件：

必需文件：
  □ client.ovpn    # 客户端配置文件
  □ ca.crt         # CA证书
  □ client.crt     # 客户端证书
  □ client.key     # 客户端私钥
  □ ta.key         # TLS密钥

⚠️ 重要：
   • 每个用户的证书文件是独立的
   • 不要使用他人的证书
   • 私钥文件要妥善保管
```

### 4.2 导入配置文件

```
Windows客户端配置步骤：

方法一：复制文件

1. 打开：C:\Program Files\OpenVPN\config
2. 创建文件夹存放您的配置文件，例如：user1
3. 将所有文件复制到此文件夹：
   C:\Program Files\OpenVPN\config\user1\
   ├── client.ovpn
   ├── ca.crt
   ├── client.crt
   ├── client.key
   └── ta.key

方法二：直接复制到config目录

1. 将所有文件直接复制到：
   C:\Program Files\OpenVPN\config\
```

### 4.3 连接VPN

```
连接步骤：

1. 找到系统托盘的OpenVPN图标 🔑
2. 右键点击图标
3. 选择"连接"或点击配置文件名
4. 如果需要输入用户名密码，输入管理员提供的凭据
5. 等待连接成功（图标变为绿色）
```

### 4.4 验证连接

```
确认连接成功：

1. 检查系统托盘图标
   └─ 绿色图标 = 已连接 ✅
   └─ 红色图标 = 未连接 ❌

2. 查看连接日志
   └─ 右键点击图标
   └─ 选择"查看日志"
   └─ 查找 "Initialization Sequence Completed"

3. 测试网络连通性
   └─ 打开命令提示符
   └─ ping 192.168.1.100
   └─ 如果有响应 = 连接成功 ✅
```

---

## 五、配置应用访问

### 5.1 确认VPN连接后的IP地址

```
连接VPN后，您会获得一个VPN虚拟IP：

VPN网络：10.8.0.0/24

您的VPN IP示例：10.8.0.2
VPN服务器IP：10.8.0.1
内网应用服务器：192.168.1.100
```

### 5.2 访问应用系统

```
连接VPN后，访问应用系统：

JavaFX桌面客户端：
1. 确认VPN已连接
2. 打开应用程序
3. 服务器地址配置为：192.168.1.100:9095

Web浏览器访问：
1. 确认VPN已连接
2. 打开浏览器
3. 访问：http://192.168.1.100:9095

Android应用：
1. 确认VPN已连接（Android也需要安装OpenVPN）
2. 打开应用程序
3. 服务器地址配置为：192.168.1.100:9095
```

### 5.3 Android配置（移动端）

```
Android手机连接VPN：

1. 安装OpenVPN Connect
   └─ Google Play搜索 "OpenVPN Connect"
   └─ 或从应用宝搜索下载

2. 导入配置文件
   └─ 打开OpenVPN Connect
   └─ 点击右上角 "+"
   └─ 选择 "Import Profile from File"
   └─ 选择您的 .ovpn 文件

3. 连接VPN
   └─ 点击连接按钮
   └─ 输入用户名密码（如需要）
   └─ 等待连接成功

4. 访问应用
   └─ 打开浏览器或应用
   └─ 访问 http://192.168.1.100:9095
```

---

## 六、常见问题解决

### 6.1 连接失败：无法连接到服务器

```
可能原因：
  ❌ 服务器IP错误
  ❌ 服务器端口未开放
  ❌ 防火墙阻止

解决方法：
  1. 确认服务器IP正确
  2. 确认服务器防火墙开放1194端口(UDP)
  3. 联系VPN管理员确认服务器状态
```

### 6.2 连接失败：证书错误

```
可能原因：
  ❌ 证书过期
  ❌ 证书文件损坏
  ❌ 配置文件路径错误

解决方法：
  1. 从管理员重新获取正确的证书
  2. 检查配置文件中的证书路径
  3. 确认证书文件完整
```

### 6.3 连接成功但无法访问内网

```
可能原因：
  ❌ 路由配置错误
  ❌ 服务器未配置内网路由

解决方法：
  1. 确认VPN服务器已正确配置route
  2. 检查客户端是否收到正确的路由推送
  3. 联系VPN管理员检查服务器配置
```

### 6.4 查看详细日志

```
查看连接日志：

1. 右键点击系统托盘OpenVPN图标
2. 选择"查看日志"
3. 日志文件位置：C:\Program Files\OpenVPN\log\

常见日志关键词：
  - "Initialization Sequence Completed" = 连接成功
  - "AUTH: Received control message" = 认证中
  - "connection refused" = 连接被拒绝
  - "TLS handshake failed" = TLS握手失败
```

---

## 七、安全建议

### 7.1 配置文件保护

```
⚠️ 重要安全提醒：

1. 妥善保管私钥
   ❌ 不要通过邮件发送
   ❌ 不要上传到网盘
   ❌ 不要放在共享文件夹
   
   ✅ 使用加密U盘传递
   ✅ 使用加密的即时通讯工具

2. 每个用户独立证书
   ✅ 避免共享证书
   ✅ 方便追踪和吊销

3. 定期更换证书
   ✅ 建议每3-6个月更换
```

### 7.2 使用建议

```
安全使用建议：

1. 定期更新OpenVPN客户端
   └─ 保持最新版本

2. 启用密码认证（如果服务器支持）
   └─ 增加安全性

3. 多人使用时
   ⚠️ 每人使用独立账户
   ⚠️ 避免共享账户

4. 断开连接
   └─ 使用完毕后及时断开VPN
```

---

## 八、下一步操作清单

```
完成OpenVPN安装后，按照以下清单操作：

□ 确认OpenVPN安装成功
□ 决定是搭建服务器还是连接客户端

如果是搭建服务器：
□ 生成CA证书
□ 生成服务器证书
□ 创建服务器配置文件
□ 启动VPN服务
□ 生成客户端证书
□ 分发客户端配置

如果是连接客户端：
□ 获取VPN配置文件
□ 导入配置文件
□ 连接VPN
□ 验证连接成功
□ 访问应用系统
□ 测试各项功能
```

---

## 九、获取帮助

```
如果遇到问题：

1. 查看日志
   └─ C:\Program Files\OpenVPN\log\

2. 常见问题参考
   └─ https://openvpn.net/community-resources/

3. 联系VPN管理员
   └─ 提供错误截图
   └─ 提供日志文件
```

---

**文档版本**: 1.0  
**最后更新**: 2026-05-12  
**适用版本**: OpenVPN 2.6.x / 3.x
