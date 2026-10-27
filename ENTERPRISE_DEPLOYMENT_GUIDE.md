# 建筑企业账目管理系统 - 企业级部署方案

## 📋 系统架构概览

```
┌─────────────────────────────────────────────────────────┐
│                    客户端层                              │
├─────────────────────────────────────────────────────────┤
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐     │
│  │ JavaFX桌面   │  │ Android     │  │ Web浏览器   │     │
│  │ 客户端       │  │ 移动端      │  │ (可选)      │     │
│  └──────┬──────┘  └──────┬──────┘  └──────┬──────┘     │
│         │                │                │            │
└─────────┼────────────────┼────────────────┼────────────┘
          │                │                │
          └────────────────┼────────────────┘
                           │ HTTPS/REST API (9095)
                           ▼
┌─────────────────────────────────────────────────────────┐
│                    服务层                                │
├─────────────────────────────────────────────────────────┤
│  ┌─────────────────────────────────────────────────┐   │
│  │           Spring Boot 后端服务                   │   │
│  │  - 认证授权 (JWT)                                │   │
│  │  - 业务逻辑层                                    │   │
│  │  - 数据访问层 (MyBatis)                          │   │
│  └─────────────────────────────────────────────────┘   │
│         │                           │                  │
│  ┌──────┴──────┐           ┌────────┴────────┐         │
│  │ MySQL 8.0   │           │ Redis 6.0+      │         │
│  │ (主数据库)   │           │ (缓存/会话)     │         │
│  └─────────────┘           └─────────────────┘         │
└─────────────────────────────────────────────────────────┘
```

## 🎯 部署模式选择

### 方案A：本地局域网部署（推荐小型企业）

**适用场景：**
- 团队成员在同一办公室
- 5-50人规模
- 数据安全性要求高
- 网络带宽充足

**架构：**
```
办公室网络
  ├── 应用服务器 (Windows Server/Linux)
  │     ├── Spring Boot后端 (9095)
  │     ├── MySQL 8.0
  │     └── Redis
  ├── JavaFX桌面客户端 (局域网访问)
  └── Android移动端 (通过VPN访问)
```

### 方案B：云服务器部署（推荐中大型企业）

**适用场景：**
- 分布式团队
- 50人以上规模
- 需要移动办公
- 希望降低IT维护成本

**架构：**
```
云服务器 (阿里云/腾讯云/华为云)
  ├── 应用服务器 (2核4G起步)
  ├── MySQL云数据库
  ├── Redis云缓存
  └── 公网IP + 域名
        │
        ├── JavaFX桌面客户端 (VPN访问)
        └── Android/iOS移动端 (公网访问)
```

---

## 📦 第一步：服务器环境准备

### 1.1 服务器配置要求

#### 最低配置（小型企业）
```yaml
CPU: 2核心
内存: 4GB
硬盘: 100GB SSD
操作系统: Windows Server 2019 / Ubuntu 22.04 LTS
网络: 100Mbps
```

#### 推荐配置（中型企业）
```yaml
CPU: 4核心
内存: 8GB
硬盘: 256GB SSD
操作系统: Windows Server 2019 / Ubuntu 22.04 LTS
网络: 100Mbps
```

#### 生产环境配置（大型企业）
```yaml
CPU: 8核心
内存: 16GB
硬盘: 512GB SSD
操作系统: Windows Server 2019 / Ubuntu 22.04 LTS / CentOS 8
网络: 100Mbps+
数据库: 独立云数据库服务器
缓存: 独立Redis服务器
```

### 1.2 安装必需软件

#### Windows Server 环境

```powershell
# 1. 安装 Chocolatey (包管理器)
Set-ExecutionPolicy BiexecutionPolicy -Scope CurrentUser
irm https://chocolatey.org/install.ps1 | iex

# 2. 安装 Java 17
choco install openjdk17 -y

# 3. 安装 MySQL 8.0
choco install mysql -y

# 4. 安装 Redis
choco install redis-64 -y

# 5. 安装 Maven
choco install maven -y
```

#### Linux (Ubuntu 22.04) 环境

```bash
# 1. 更新系统
sudo apt update && sudo apt upgrade -y

# 2. 安装 Java 17
sudo apt install openjdk-17-jdk -y

# 3. 安装 MySQL 8.0
sudo apt install mysql-server -y
sudo mysql_secure_installation

# 4. 安装 Redis
sudo apt install redis-server -y

# 5. 安装 Maven
sudo apt install maven -y

# 6. 安装 Nginx (反向代理)
sudo apt install nginx -y
```

---

## 🗄️ 第二步：数据库配置

### 2.1 创建数据库和用户

```sql
-- 登录MySQL
mysql -u root -p

-- 创建数据库
CREATE DATABASE ceams CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 创建专用用户 (生产环境务必使用强密码)
CREATE USER 'ceams_user'@'localhost' IDENTIFIED BY 'YourStrongPassword123!';
CREATE USER 'ceams_user'@'%' IDENTIFIED BY 'YourStrongPassword123!';

-- 授予权限
GRANT ALL PRIVILEGES ON ceams.* TO 'ceams_user'@'localhost';
GRANT ALL PRIVILEGES ON ceams.* TO 'ceams_user'@'%';
FLUSH PRIVILEGES;

-- 验证
SHOW GRANTS FOR 'ceams_user'@'localhost';
```

### 2.2 初始化数据库表

```bash
# 进入项目目录
cd /path/to/sxassist/src/main/resources/db

# 执行数据库迁移脚本
mysql -u ceams_user -p ceams < migration/V1.0.10__Create_Notification_Tables.sql
mysql -u ceams_user -p ceams < migration/V1.0.11__Add_Project_Manager_Field.sql
mysql -u ceams_user -p ceams < migration/V1.0.12__Add_Receipt_Unique_Constraints.sql
mysql -u ceams_user -p ceams < migration/V1.0.13__Add_Receipt_Number_Field.sql
```

### 2.3 数据库性能优化 (MySQL 8.0)

```ini
# /etc/mysql/mysql.conf.d/mysqld.cnf

[mysqld]
# 基础配置
max_connections = 200
character-set-server = utf8mb4
collation-server = utf8mb4_unicode_ci

# 缓存配置
innodb_buffer_pool_size = 2G  # 设置为服务器内存的50-70%
innodb_log_file_size = 512M
innodb_flush_log_at_trx_commit = 2

# 查询优化
slow_query_log = 1
slow_query_log_file = /var/log/mysql/slow.log
long_query_time = 2

# 连接优化
wait_timeout = 600
interactive_timeout = 600
```

---

## 🚀 第三步：后端服务部署

### 3.1 环境变量配置

#### Windows 环境变量配置

```powershell
# 创建部署目录
mkdir C:\ceams\app
mkdir C:\ceams\logs
mkdir C:\ceams\backup
mkdir C:\ceams\rephoto
mkdir C:\ceams\contracts
mkdir C:\ceams\stamped_contracts
mkdir C:\ceams\tessdata

# 设置环境变量 (系统级别)
# 打开: 系统属性 -> 高级 -> 环境变量 -> 系统变量

# 配置清单：
DB_HOST=localhost
DB_PORT=3306
DB_NAME=ceams
DB_USERNAME=ceams_user
DB_PASSWORD=YourStrongPassword123!

REDIS_HOST=localhost
REDIS_PORT=6379

SERVER_PORT=9095

JWT_SECRET=生成一个64位随机密钥
JWT_EXPIRATION=86400000

# 邮件配置
MAIL_HOST=smtp.exmail.qq.com
MAIL_PORT=587
MAIL_USERNAME=your_email@company.com
MAIL_PASSWORD=your_email_password

# 日志级别
LOG_LEVEL=INFO
APP_LOG_LEVEL=INFO
```

#### Linux 环境变量配置

```bash
# 创建环境变量文件
sudo nano /etc/ceams/env

# 文件内容：
DB_HOST=localhost
DB_PORT=3306
DB_NAME=ceams
DB_USERNAME=ceams_user
DB_PASSWORD=YourStrongPassword123!
REDIS_HOST=localhost
REDIS_PORT=6379
SERVER_PORT=9095
JWT_SECRET=生成一个64位随机密钥
JWT_EXPIRATION=86400000
MAIL_HOST=smtp.exmail.qq.com
MAIL_PORT=587
MAIL_USERNAME=your_email@company.com
MAIL_PASSWORD=your_email_password

# 设置权限
sudo chmod 600 /etc/ceams/env
```

### 3.2 构建应用

```bash
# Windows PowerShell
cd e:\sxassist

# 清理并构建
mvn clean package -DskipTests

# Linux/macOS
cd /path/to/sxassist
mvn clean package -DskipTests
```

### 3.3 配置应用启动脚本

#### Windows 启动脚本 (start.bat)

```batch
@echo off
setlocal

REM 设置工作目录
cd /d "%~dp0"

REM 加载环境变量
set DB_HOST=localhost
set DB_PORT=3306
set DB_NAME=ceams
set DB_USERNAME=ceams_user
set DB_PASSWORD=YourStrongPassword123!
set REDIS_HOST=localhost
set REDIS_PORT=6379
set SERVER_PORT=9095
set JWT_SECRET=your-64-char-random-secret-key-here-must-be-at-least-64-chars
set JWT_EXPIRATION=86400000
set LOG_LEVEL=INFO
set APP_LOG_LEVEL=INFO

REM 创建日志目录
if not exist "logs" mkdir logs
if not exist "backup" mkdir backup
if not exist "rephoto" mkdir rephoto
if not exist "contracts" mkdir contracts
if not exist "stamped_contracts" mkdir stamped_contracts

echo ============================================
echo  建筑企业账目管理系统 - 后端服务启动中
echo ============================================
echo  时间: %date% %time%
echo  PID文件: ceams.pid
echo ============================================

REM 启动应用
java -Xms512m -Xmx2048m ^
     -Dfile.encoding=UTF-8 ^
     -Dspring.profiles.active=production ^
     -jar target/ceams-1.0-SNAPSHOT.jar ^
     --server.address=0.0.0.0 ^
     >> logs/ceams-stdout.log 2>&1 &

echo %! > ceams.pid

echo 服务已在后台启动
echo 日志文件: logs/ceams-stdout.log
echo 按任意键退出...
pause
```

#### Linux 服务脚本 (ceams.service)

```ini
[Unit]
Description=CEAMS - Construction Enterprise Accounting Management System
After=network.target mysql.service redis.service
Wants=mysql.service redis.service

[Service]
Type=simple
User=ceams
Group=ceams
WorkingDirectory=/opt/ceams
Environment="DB_HOST=localhost"
Environment="DB_PORT=3306"
Environment="DB_NAME=ceams"
Environment="DB_USERNAME=ceams_user"
Environment="DB_PASSWORD=YourStrongPassword123!"
Environment="REDIS_HOST=localhost"
Environment="REDIS_PORT=6379"
Environment="SERVER_PORT=9095"
Environment="JWT_SECRET=your-64-char-random-secret-key-here-must-be-at-least-64-chars"
ExecStart=/usr/bin/java -Xms512m -Xmx2048m -jar /opt/ceams/ceams-1.0-SNAPSHOT.jar
Restart=always
RestartSec=10
StandardOutput=append:/var/log/ceams/stdout.log
StandardError=append:/var/log/ceams/stderr.log

[Install]
WantedBy=multi-user.target
```

**安装服务：**
```bash
# 复制服务文件
sudo cp ceams.service /etc/systemd/system/

# 重载 systemd
sudo systemctl daemon-reload

# 启用服务 (开机自启)
sudo systemctl enable ceams

# 启动服务
sudo systemctl start ceams

# 查看状态
sudo systemctl status ceams

# 查看日志
sudo journalctl -u ceams -f
```

### 3.4 Nginx 反向代理配置 (可选)

```nginx
# /etc/nginx/sites-available/ceams

server {
    listen 80;
    server_name ceams.yourcompany.com;

    # 强制跳转HTTPS
    return 301 https://$server_name$request_uri;
}

server {
    listen 443 ssl http2;
    server_name ceams.yourcompany.com;

    # SSL证书配置
    ssl_certificate /etc/ssl/certs/ceams.crt;
    ssl_certificate_key /etc/ssl/private/ceams.key;
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers HIGH:!aNULL:!MD5;

    # 日志
    access_log /var/log/nginx/ceams_access.log;
    error_log /var/log/nginx/ceams_error.log;

    # 上传文件大小限制
    client_max_body_size 100M;

    location / {
        proxy_pass http://127.0.0.1:9095;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;

        # WebSocket支持 (如果需要)
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";

        # 超时配置
        proxy_connect_timeout 60s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
    }

    # 静态文件 (如果需要)
    location /uploads {
        alias /opt/ceams/uploads;
        expires 30d;
        add_header Cache-Control "public, immutable";
    }
}
```

---

## 💻 第四步：JavaFX 桌面客户端部署

### 4.1 打包为可执行文件

#### 使用 Launch4j 打包 (Windows)

```xml
<!-- launch4j.xml -->
<?xml version="1.0" encoding="UTF-8"?>
<launch4j configDtd="http://launch4j.sourceforge.net/xml/launch4j_1_0.dtd">
    <launch4j configDtd="http://launch4j.sourceforge.net/xml/launch4j_1_0.dtd">
        <dontWrapJar>true</dontWrapJar>
        <headerType>gui</headerType>
        <jar>ceams-1.0-SNAPSHOT.jar</jar>
        <outfile>盛韵建筑.exe</outfile>
        <jre>
            <path>.</path>
            <minVersion>17.0.0</minVersion>
            <maxVersion>17.0.99</maxVersion>
        </jre>
        <sysProps>
            <prop name="file.encoding">UTF-8</prop>
        </sysProps>
    </launch4j>
</launch4j>
```

### 4.2 客户端配置

#### config.properties
```properties
# 服务器地址配置
server.host=${SERVER_IP}
server.port=9095

# 连接超时
connection.timeout=30000
connection.read.timeout=60000

# 认证配置
auth.token.refresh.interval=3600000

# 本地缓存
cache.enabled=true
cache.size=100MB
```

### 4.3 部署分发包结构

```
CEAMS-Client-Setup/
├── 盛韵建筑.exe          # 主程序
├── JRE/                  # 运行环境
│   └── jre-17/
├── config/
│   └── config.properties # 配置文件 (可由IT统一管理)
├── resources/
│   └── assets/           # 资源文件
├── README.txt            # 使用说明
└── SETUP.bat             # 初始化脚本
```

---

## 📱 第五步：Android 移动端部署

### 5.1 修改服务器地址

#### 方式一：直接修改代码

```java
// androidclient/app/src/main/java/com/ceams/client/RetrofitClient.java
public class RetrofitClient {
    // 企业部署时修改此地址
    private static final String BASE_URL = "http://YOUR_SERVER_IP:9095";
    // 或使用域名
    // private static final String BASE_URL = "https://ceams.yourcompany.com";
}
```

#### 方式二：配置文件 (推荐)

```java
// 使用 BuildConfig
private static final String BASE_URL = BuildConfig.SERVER_URL;
```

### 5.2 网络安全配置

#### Android 9+ 需要配置明文流量

```xml
<!-- androidclient/app/src/main/res/xml/network_security_config.xml -->
<?xml version="1.0" encoding="utf-8"?>
<network-security-config>
    <!-- 测试环境 -->
    <domain-config cleartextTrafficPermitted="true">
        <domain includeSubdomains="true">10.0.2.2</domain>
        <domain includeSubdomains="true">localhost</domain>
    </domain-config>

    <!-- 企业内网 -->
    <domain-config cleartextTrafficPermitted="true">
        <domain includeSubdomains="true">192.168.0.0</domain>
        <domain includeSubdomains="true">192.168.1.0</domain>
    </domain-config>

    <!-- 生产环境使用HTTPS -->
    <base-config cleartextTrafficPermitted="false">
        <trust-anchors>
            <certificates src="system"/>
        </trust-anchors>
    </base-config>
</network-security-config>
```

### 5.3 构建发布版本

```bash
cd androidclient

# 清理构建
./gradlew clean

# 构建Release APK
./gradlew assembleRelease

# 签名配置 (需要在 local.properties 或 gradle.properties 中配置)
# keystore.properties
keyAlias=ceams
keyPassword=your_key_password
storeFile=/path/to/ceams.jks
storePassword=your_store_password
```

### 5.4 Android 应用分发

#### 企业内部分发方案

```markdown
1. **内部应用商店**
   - 使用蒲公英/Fir.im等内测平台
   - 搭建私有FIR或史莱姆分发平台
   - 配置下载链接和更新检测

2. **MDM移动设备管理**
   - 企业微信/钉钉集成
   - Intunes / AirWatch
   - 统一推送和管控

3. **手动安装**
   - 生成APK/ABB包
   - 官网下载页面
   - IT部门统一安装
```

---

## 🔐 第六步：安全加固

### 6.1 防火墙配置

#### Windows 防火墙

```powershell
# 开放必要端口
New-NetFirewallRule -DisplayName "CEAMS Backend" -Direction Inbound `
    -Action Allow -Protocol TCP -LocalPort 9095

# 仅允许内网访问 (推荐)
New-NetFirewallRule -DisplayName "CEAMS Backend (Internal)" -Direction Inbound `
    -Action Allow -Protocol TCP -LocalPort 9095 `
    -RemoteAddress 192.168.0.0/16
```

#### Linux 防火墙 (UFW)

```bash
# 开放必要端口
sudo ufw allow 9095/tcp
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp

# 仅允许内网访问 (推荐)
sudo ufw allow from 192.168.0.0/16 to any port 9095

# 启用防火墙
sudo ufw enable
sudo ufw status
```

### 6.2 数据库安全

```sql
-- 强制密码复杂度
SET GLOBAL validate_password.policy=MEDIUM;
SET GLOBAL validate_password.length=12;

-- 限制用户访问源
REVOKE ALL PRIVILEGES ON ceams.* FROM 'ceams_user'@'%';
GRANT ALL PRIVILEGES ON ceams.* TO 'ceams_user'@'localhost';
GRANT ALL PRIVILEGES ON ceams.* TO 'ceams_user'@'192.168.%.%';

-- 启用审计日志
SET GLOBAL general_log = 'ON';
SET GLOBAL general_log_file = '/var/log/mysql/audit.log';
```

### 6.3 HTTPS 证书配置

#### 使用 Let's Encrypt (免费)

```bash
# 安装 Certbot
sudo apt install certbot python3-certbot-nginx

# 获取证书
sudo certbot --nginx -d ceams.yourcompany.com

# 自动续期
sudo certbot renew --dry-run
```

### 6.4 定期安全更新

```bash
# Linux 自动安全更新
sudo apt install unattended-upgrades
sudo dpkg-reconfigure unattended-upgrades
```

---

## 💾 第七步：备份策略

### 7.1 自动备份脚本

#### 数据库备份 (MySQL)

```bash
#!/bin/bash
# backup.sh

# 配置
BACKUP_DIR="/opt/ceams/backup"
DB_NAME="ceams"
DB_USER="ceams_user"
DB_PASS="YourStrongPassword123!"
DATE=$(date +%Y%m%d_%H%M%S)
RETENTION_DAYS=30

# 创建备份目录
mkdir -p $BACKUP_DIR

# 备份数据库
mysqldump -u$DB_USER -p$DB_PASS $DB_NAME | gzip > $BACKUP_DIR/ceams_$DATE.sql.gz

# 备份上传文件
tar -czf $BACKUP_DIR/uploads_$DATE.tar.gz /opt/ceams/rephoto /opt/ceams/contracts

# 删除旧备份
find $BACKUP_DIR -name "*.sql.gz" -mtime +$RETENTION_DAYS -delete
find $BACKUP_DIR -name "*.tar.gz" -mtime +$RETENTION_DAYS -delete

# 同步到远程服务器 (可选)
# rsync -avz $BACKUP_DIR/ backup@remote-server:/backup/ceams/

echo "备份完成: $DATE"
```

#### Windows 备份脚本

```batch
@echo off
setlocal

set BACKUP_DIR=C:\ceams\backup
set DATE=%date:~0,4%%date:~5,2%%date:~8,2%_%time:~0,2%%time:~3,2%%time:~6,2%
set DATE=%DATE: =0%

mkdir %BACKUP_DIR% 2>nul

mysqldump -u ceams_user -pYourPassword ceams | gzip > %BACKUP_DIR%\ceams_%DATE%.sql.gz

echo Backup completed: %DATE%
```

### 7.2 定时任务配置

#### Linux Cron

```bash
# 编辑 crontab
crontab -e

# 每天凌晨2点执行备份
0 2 * * * /opt/ceams/scripts/backup.sh >> /var/log/ceams_backup.log 2>&1

# 每周日凌晨3点执行完整备份
0 3 * * 0 /opt/ceams/scripts/full_backup.sh >> /var/log/ceams_full_backup.log 2>&1
```

#### Windows 任务计划程序

```powershell
# 创建备份任务
schtasks /create /tn "CEAMS Backup" /tr "C:\ceams\backup.bat" /sc daily /st 02:00
```

---

## 📊 第八步：监控和维护

### 8.1 应用健康检查

```bash
# 检查服务状态
curl http://localhost:9095/actuator/health

# 检查数据库连接
mysql -u ceams_user -p -e "SELECT 1"

# 检查Redis
redis-cli ping
```

### 8.2 日志管理

```bash
# Linux 日志轮转
# /etc/logrotate.d/ceams
/opt/ceams/logs/*.log {
    daily
    rotate 30
    compress
    delaycompress
    notifempty
    create 0644 ceams ceams
    postrotate
        systemctl reload ceams > /dev/null 2>&1 || true
    endscript
}
```

### 8.3 性能监控

```yaml
# Spring Boot Actuator 配置
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: always
  metrics:
    export:
      prometheus:
        enabled: true
```

---

## ✅ 部署检查清单

### 部署前检查
- [ ] 服务器硬件满足最低要求
- [ ] 操作系统已更新
- [ ] Java 17 已安装
- [ ] MySQL 8.0 已安装并配置
- [ ] Redis 已安装并配置
- [ ] Maven 已安装

### 数据库配置
- [ ] 数据库已创建
- [ ] 用户权限已配置
- [ ] 数据库迁移脚本已执行
- [ ] 性能优化参数已设置
- [ ] 备份策略已配置

### 后端服务
- [ ] 应用已构建
- [ ] 环境变量已配置
- [ ] 服务启动脚本已创建
- [ ] 服务已注册为系统服务
- [ ] 防火墙端口已开放
- [ ] Nginx反向代理已配置 (可选)
- [ ] HTTPS证书已配置 (可选)

### 客户端部署
- [ ] JavaFX桌面客户端已打包
- [ ] 配置文件已准备
- [ ] 分发包已创建
- [ ] Android APK已构建
- [ ] 服务器地址已更新

### 安全配置
- [ ] 强密码已设置
- [ ] 防火墙已配置
- [ ] 数据库审计已启用
- [ ] HTTPS已启用 (生产环境)
- [ ] 备份已测试

### 监控和运维
- [ ] 日志配置已完成
- [ ] 监控告警已设置
- [ ] 备份脚本已测试
- [ ] 恢复流程已文档化
- [ ] 运维手册已编写

---

## 🆘 故障排除

### 服务启动失败

```bash
# 1. 检查Java版本
java -version

# 2. 检查端口占用
netstat -tulpn | grep 9095
# Linux: lsof -i:9095

# 3. 查看日志
tail -f logs/ceams.log

# 4. 检查数据库连接
mysql -u ceams_user -p -e "SHOW DATABASES;"

# 5. 检查Redis
redis-cli ping
```

### 性能问题

```sql
-- 查看慢查询
SHOW VARIABLES LIKE 'slow_query_log%';
SELECT * FROM mysql.slow_log;

-- 查看连接数
SHOW STATUS LIKE 'Threads_connected';
SHOW VARIABLES LIKE 'max_connections';

-- 查看缓存命中率
SHOW STATUS LIKE 'Innodb_buffer_pool%';
```

### 网络问题

```bash
# 测试连通性
ping server_ip
telnet server_ip 9095

# 检查防火墙状态
# Windows
netsh advfirewall status
# Linux
sudo ufw status
sudo iptables -L
```

---

## 📞 技术支持

如遇部署问题，请提供以下信息：
1. 操作系统和版本
2. 错误日志完整内容
3. 服务状态检查结果
4. 网络配置信息

---

**文档版本**: 1.0
**最后更新**: 2026-05-12
**适用系统版本**: CEAMS 1.0+
