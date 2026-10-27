# OpenVPN 下载与安装指南

## 一、下载方式

### 1.1 官方网站下载（推荐）

**下载地址**: https://openvpn.net/community-downloads/

```
OpenVPN 官方下载页面
支持平台：
  ✅ Windows
  ✅ macOS
  ✅ Linux
  ✅ Android
  ✅ iOS
```

**选择版本**:
- Windows: 下载 `OpenVPN-2.6.x-install.exe`
- macOS: 下载 `OpenVPN-2.6.x.pkg`
- Android: 在Google Play搜索 "OpenVPN Connect"

---

### 1.2 各平台下载链接

#### Windows 客户端下载

```
官方下载:
https://openvpn.net/downloads/openvpn-install-latest-stable.exe

镜像下载（如果官网慢）:
- TUNA镜像: https://mirrors.tuna.tsinghua.edu.cn/openvpn/
-USTC镜像: https://mirrors.ustc.edu.cn/openvpn/
```

#### Android 客户端下载

```
Google Play（推荐）:
https://play.google.com/store/apps/details?id=net.openvpn.openvpn

APK直接下载（国内）:
- 应用宝: 搜索 "OpenVPN Connect"
- 酷安: 搜索 "OpenVPN"
```

#### macOS 客户端下载

```
官方下载:
https://openvpn.net/downloads/openvpn-connect-v3-macos.dmg

Homebrew安装:
brew install openvpn
brew install --cask tunnelblick  # 图形界面
```

---

## 二、Windows安装步骤

### 2.1 下载安装包

1. 打开浏览器访问：https://openvpn.net/community-downloads/
2. 找到 "Windows Installers" 部分
3. 点击 `OpenVPN-2.6.x-install.exe` 下载

### 2.2 安装OpenVPN

```
安装步骤：

1. 双击下载的 .exe 文件
   └─ 如果弹出安全提示，点击"运行"

2. 选择安装语言
   └─ 选择 "English" 或 "简体中文"（如果有）

3. 点击 "Next"

4. 阅读许可协议
   └─ 选择 "I agree"

5. 选择安装组件（保持默认即可）
   └─ ☑ OpenVPN Service
   └─ ☑ OpenVPN GUI
   └─ ☑ OpenSSL Utilities
   └─ ☑ OpenVPN RSA Module
   └─ ☑ Tap-Windows Virtual Ethernet Adapter

6. 选择安装位置
   └─ 建议保持默认：C:\Program Files\OpenVPN

7. 点击 "Install" 开始安装

8. 如果弹出驱动程序安装提示
   └─ 选择 "安装"

9. 安装完成
   └─ 点击 "Finish"
```

### 2.3 验证安装

```
验证步骤：

1. 检查系统托盘
   └─ 右下角应该出现 OpenVPN 图标 🔑

2. 检查开始菜单
   └─ 应该出现 "OpenVPN" 程序组

3. 检查安装目录
   └─ C:\Program Files\OpenVPN\
   └─ 应该包含 bin、config 等文件夹
```

---

## 三、Android安装步骤

### 3.1 Google Play安装（推荐）

```
步骤：

1. 打开 Google Play
2. 搜索 "OpenVPN Connect"
3. 点击 "安装"
4. 安装完成后点击 "打开"
5. 授予必要权限
```

### 3.2 APK安装（国内用户）

```
步骤：

1. 下载APK文件
   └─ 应用宝、酷安等应用商店
   └─ 或从官网下载：https://openvpn.net/client-client-connect-v3-for-android/

2. 打开APK文件
   └─ 如果提示"禁止安装未知来源应用"
   └─ 前往 设置 > 安全 > 允许安装未知来源应用

3. 点击安装
4. 安装完成后打开应用
```

---

## 四、macOS安装步骤

### 4.1 方式一：官方安装包

```
步骤：

1. 下载 DMG 文件
   └─ https://openvpn.net/downloads/openvpn-connect-v3-macos.dmg

2. 双击 DMG 文件

3. 拖动 OpenVPN 到 Applications 文件夹

4. 打开应用程序
   └─ 首次运行会提示"来自开发者"确认
   └─ 点击"打开"

5. 安装完成
```

### 4.2 方式二：Homebrew（推荐）

```bash
# 终端安装命令

# 1. 安装 Homebrew（如果没有）
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"

# 2. 安装 OpenVPN 命令行工具
brew install openvpn

# 3. 安装 Tunnelblick（图形界面）
brew install --cask tunnelblick

# 4. 启动 Tunnelblick
open -a Tunnelblick
```

---

## 五、Linux安装步骤

### 5.1 Ubuntu/Debian

```bash
# 1. 更新软件源
sudo apt update

# 2. 安装 OpenVPN
sudo apt install openvpn

# 3. 安装网络管理器插件（可选）
sudo apt install network-manager-openvpn-gnome
```

### 5.2 CentOS/RHEL

```bash
# 1. 安装 EPEL 仓库
sudo yum install epel-release

# 2. 安装 OpenVPN
sudo yum install openvpn

# 3. 安装 Easy-RSA（证书工具）
sudo yum install easy-rsa
```

### 5.3 Arch Linux

```bash
# 安装 OpenVPN
sudo pacman -S openvpn

# 安装图形界面管理工具（可选）
sudo pacman -S networkmanager-openvpn
```

---

## 六、下载后配置

### 6.1 获取配置文件

安装完成后，您需要从VPN管理员处获取以下文件：

```
必需文件：
  1. client.ovpn      # 客户端配置文件
  2. ca.crt           # CA证书（所有用户相同）
  3. client.crt       # 客户端证书（每个用户不同）
  4. client.key       # 客户端私钥（每个用户不同）
  5. ta.key          # TLS密钥（所有用户相同）

配置文件存放位置：
  Windows: C:\Program Files\OpenVPN\config\
  macOS:   /Applications/Tunnelblick.app/Contents/Resources/
  Linux:   /etc/openvpn/
  Android: 直接导入 .ovpn 文件
```

### 6.2 导入配置文件

#### Windows

```
方法1：复制文件
1. 将所有配置文件复制到
   C:\Program Files\OpenVPN\config\

方法2：右键导入
1. 右键点击 client.ovpn 文件
2. 选择 "Start OpenVPN on this config file"
```

#### Android

```
导入步骤：
1. 打开 OpenVPN Connect
2. 点击右上角 "+"
3. 选择 "Import Profile from File"
4. 选择 client.ovpn 文件
5. 输入用户名和密码（如果配置了）
6. 点击 "Connect"
```

---

## 七、常见问题

### Q1: 下载速度很慢怎么办？

```
解决方案：

1. 使用镜像源
   └─ 清华镜像: https://mirrors.tuna.tsinghua.edu.cn/openvpn/
   └─ 中科大镜像: https://mirrors.ustc.edu.cn/openvpn/

2. 使用下载工具
   └─ IDM (Internet Download Manager)
   └─ 迅雷

3. 使用手机热点下载
   └─ 部分地区教育网访问国外慢
```

### Q2: 安装时提示驱动安装失败？

```
解决方案：

1. 以管理员身份运行安装程序
   └─ 右键点击安装文件
   └─ 选择"以管理员身份运行"

2. 关闭杀毒软件
   └─ 部分杀毒软件会阻止驱动安装

3. 安装Tap驱动单独包
   └─ 下载地址: https://build.openvpn.net/downloads/releases/
   └─ 选择 tap-windows-9.x.x.exe
```

### Q3: Android提示"连接被拒绝"？

```
检查项：

1. 确认OpenVPN服务器地址正确
   └─ 检查 client.ovpn 中的 remote 配置

2. 确认服务器端口开放
   └─ 常用端口: 1194 (UDP), 443 (TCP)

3. 检查防火墙
   └─ 手机设置中允许OpenVPN应用

4. 确认配置文件正确
   └─ 证书、密钥是否完整
```

### Q4: 如何确认安装成功？

```
Windows:
1. 打开"服务"应用
2. 查找 "OpenVPNService"
3. 状态应为"正在运行"

macOS:
1. 打开"系统偏好设置"
2. 查找"网络"
3. 应能看到 tap/tun 接口

Android:
1. 打开 OpenVPN Connect
2. 应能看到配置列表
3. 可以点击连接按钮
```

---

## 八、安全注意事项

### 8.1 配置文件安全

```
⚠️ 重要提醒：

1. 妥善保管私钥
   ❌ 不要通过邮件发送
   ❌ 不要上传到网盘
   ✅ 使用加密的U盘传递
   ✅ 使用加密的即时通讯工具

2. 定期更新证书
   ❌ 不要使用过期证书
   ✅ 设置证书过期提醒

3. 多设备管理
   ⚠️ 每个设备使用独立证书
   ⚠️ 设备丢失时及时吊销证书
```

### 8.2 使用建议

```
安全建议：

1. 连接时检查服务器证书
   └─ 确保证书指纹匹配

2. 避免在公共WiFi下使用
   └─ 即使有VPN也有风险

3. 定期更新客户端
   └─ 保持最新版本

4. 启用密码保护
   └─ OpenVPN支持用户密码认证
```

---

## 九、版本选择建议

### 9.1 推荐版本

| 操作系统 | 推荐版本 | 说明 |
|----------|----------|------|
| Windows 10/11 | 2.6.x | 最新稳定版 |
| macOS 10.15+ | 3.3.x | OpenVPN Connect |
| Android 10+ | 3.3.x | OpenVPN Connect |
| Linux (最新) | 2.5.x+ | 系统包管理器版本 |

### 9.2 版本说明

```
OpenVPN 版本说明：

2.4.x - 长期支持版
  └─ 兼容性好，稳定
  └─ 适合企业环境

2.5.x - 稳定版
  └─ 新特性，性能优化
  └─ 推荐新部署使用

2.6.x - 最新版
  └─ 最新功能
  └─ 可能存在小问题

3.x.x - OpenVPN Connect
  └─ 全新设计的客户端
  └─ 用户体验更好
  └─ 推荐桌面端使用
```

---

## 十、联系支持

### 10.1 官方资源

```
官方网站: https://openvpn.net/
文档中心: https://openvpn.net/community-resources/
社区论坛: https://forums.openvpn.net/
GitHub: https://github.com/OpenVPN/openvpn
```

### 10.2 获取帮助

```
如果遇到问题：

1. 查看官方文档
   └─ https://openvpn.net/community-resources/

2. 查看常见问题
   └─ https://openvpn.net/faq/

3. 社区提问
   └─ https://forums.openvpn.net/

4. 联系VPN管理员
   └─ 提供错误截图
   └─ 提供日志文件
```

---

**文档版本**: 1.0  
**最后更新**: 2026-05-12  
**适用版本**: OpenVPN 2.4+ / 3.x
