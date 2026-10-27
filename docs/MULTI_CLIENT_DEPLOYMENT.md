# CEAMS 多客户端部署指南

## 📋 概述

本指南帮助你在多台Windows电脑上部署CEAMS JavaFX桌面应用,并安全地连接到中央MySQL数据库。

### 系统架构

```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│   Windows PC 1  │     │   Windows PC 2  │     │   Windows PC 3  │
│  JavaFX Client │     │  JavaFX Client  │     │  JavaFX Client  │
└────────┬────────┘     └────────┬────────┘     └────────┬────────┘
         │                       │                       │
         └───────────────────────┼───────────────────────┘
                                 │
                    ┌────────────▼────────────┐
                    │      MySQL 数据库        │
                    │    (中央数据服务器)       │
                    └─────────────────────────┘
```

## 🚀 部署步骤

### 第一阶段: 服务器准备(管理员执行一次)

#### 1.1 配置MySQL远程访问

编辑MySQL配置文件 `my.ini`:

```ini
[mysqld]
# 绑定到内网IP(不要使用0.0.0.0)
bind-address = 192.168.1.100

# 启用SSL(推荐用于生产环境)
require_secure_transport = ON

# 最大连接数
max_connections = 100
```

#### 1.2 创建数据库用户

以管理员身份登录MySQL,执行 `sql\create_client_users.sql`:

```bash
mysql -u root -p < sql\create_client_users.sql
```

#### 1.3 配置防火墙

在MySQL服务器电脑上,运行PowerShell(管理员):

```powershell
# 允许MySQL端口入站
New-NetFirewallRule -DisplayName "CEAMS MySQL" `
  -Direction Inbound -Protocol TCP `
  -LocalPort 3306 -Action Allow

# 允许应用端口(如果需要)
New-NetFirewallRule -DisplayName "CEAMS App" `
  -Direction Inbound -Protocol TCP `
  -LocalPort 9095 -Action Allow
```

### 第二阶段: 打包应用(管理员执行一次)

#### 2.1 构建客户端包

```bash
cd e:\sxassist
build-client.bat
```

打包完成后,在 `dist-client` 目录会包含:
- `ceams-1.0-SNAPSHOT.jar` - 应用主程序
- `run.bat` - 启动脚本
- `config\client-config.properties.example` - 配置模板

#### 2.2 分发到客户端电脑

将 `dist-client` 目录复制到每台客户端电脑,建议路径:
```
C:\Program Files\CEAMS\
```

### 第三阶段: 客户端配置

#### 3.1 创建配置文件

在客户端电脑上,创建配置文件:

**路径**: `C:\Program Files\CEAMS\config\client-config.properties`

内容示例:

```properties
# 数据库连接
DB_HOST=192.168.1.100
DB_PORT=3306
DB_NAME=ceams
DB_USERNAME=ceams_client_write
DB_PASSWORD=你的密码

# 应用配置
LOG_LEVEL=INFO
RECEIPT_PATH=./rephoto

# 安全配置
DB_USE_SSL=false
```

#### 3.2 配置环境变量(可选)

创建批处理文件 `start-ceams.bat`:

```batch
@echo off
set DB_HOST=192.168.1.100
set DB_PORT=3306
set DB_NAME=ceams
set DB_USERNAME=ceams_client_write
set DB_PASSWORD=你的密码
"C:\Program Files\CEAMS\ceams-1.0-SNAPSHOT.jar"
```

### 第四阶段: 安装运行时(每台客户端电脑)

#### 4.1 安装Java 17

1. 下载 JDK 17: https://adoptium.net/
2. 安装到默认路径
3. 配置环境变量:

```batch
setx JAVA_HOME "C:\Program Files\Eclipse Adoptium\jdk-17.0.5.8-hotspot"
setx PATH "%PATH%;%JAVA_HOME%\bin"
```

#### 4.2 验证Java安装

打开命令提示符,运行:

```bash
java -version
javac -version
```

## 🔒 数据安全措施

### 1. 用户权限控制

- **普通用户**: 只读账户 (`ceams_client_read`)
- **录入人员**: 读写账户 (`ceams_client_write`)
- **管理员**: 完整权限账户 (`ceams_admin`)

### 2. 网络隔离

**推荐方案**: 使用VPN

参见 `VPN_PROJECT_DEPLOYMENT.md` 部署VPN服务。

**备选方案**: 配置路由器/防火墙

- 只允许特定IP段访问MySQL服务器
- 启用MAC地址过滤
- 关闭不必要的端口

### 3. SSL/TLS加密

如果需要高安全性,配置MySQL SSL连接:

1. 生成SSL证书
2. 在MySQL服务器启用SSL
3. 客户端配置 `DB_USE_SSL=true`

### 4. 定期安全检查

- [ ] 每季度更换数据库密码
- [ ] 每月检查访问日志
- [ ] 每周备份数据库
- [ ] 监控异常登录尝试

## 🛠️ 故障排除

### 问题1: 连接被拒绝

**症状**: 
```
Connection refused: connect
```

**解决方案**:
1. 检查MySQL服务器是否运行:
   ```bash
   netstat -an | findstr 3306
   ```

2. 检查防火墙是否允许:
   ```powershell
   Get-NetFirewallRule -DisplayName "CEAMS MySQL"
   ```

3. 验证IP地址是否正确

### 问题2: 认证失败

**症状**:
```
Access denied for user 'ceams_client'@'客户端IP'
```

**解决方案**:
1. 确认用户名密码正确
2. 检查用户权限:
   ```sql
   SHOW GRANTS FOR 'ceams_client'@'%';
   ```
3. 确认客户端IP在允许范围内

### 问题3: 应用启动缓慢

**解决方案**:
1. 增加Java内存:
   ```batch
   java -Xms512m -Xmx1024m -jar ceams-1.0-SNAPSHOT.jar
   ```

2. 检查网络延迟:
   ```bash
   ping 192.168.1.100
   ```

## 📝 配置清单

部署前,准备以下信息:

- [ ] MySQL服务器IP地址: `_________`
- [ ] MySQL端口: `_________` (默认3306)
- [ ] 数据库名称: `_________` (默认ceams)
- [ ] 客户端用户名: `_________`
- [ ] 客户端密码: `_________`
- [ ] Java安装路径: `_________`

## 📞 技术支持

如果遇到问题,请收集以下信息:

1. 错误日志(位于 `logs\ceams.log`)
2. 网络连接测试结果
3. Java版本信息
4. 配置文件内容

---

**版本**: 1.0  
**更新日期**: 2026-05-17  
**适用范围**: CEAMS 1.0+
