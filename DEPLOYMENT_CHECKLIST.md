# 建筑企业账目管理系统 - 部署检查清单

## 📋 快速部署流程

### ☐ 阶段一：环境准备 (预计30分钟)

#### 1. 服务器环境
- [ ] **操作系统**: Windows Server 2019 / Ubuntu 22.04 LTS
- [ ] **硬件配置**: 2核CPU / 4GB内存 / 100GB硬盘
- [ ] **网络**: 固定IP地址

#### 2. 必需软件安装
- [ ] **Java 17**: `java -version` 验证
- [ ] **MySQL 8.0**: `mysql --version` 验证
- [ ] **Redis 6.0+**: `redis-cli ping` 验证
- [ ] **Maven 3.6+**: `mvn --version` 验证

#### 3. 目录创建
```bash
mkdir ceams\app
mkdir ceams\logs
mkdir ceams\backup
mkdir ceams\rephoto
mkdir ceams\contracts
mkdir ceams\stamped_contracts
mkdir ceams\tessdata
```

---

### ☐ 阶段二：数据库配置 (预计20分钟)

#### 1. MySQL配置
- [ ] **创建数据库**
  ```sql
  CREATE DATABASE ceams CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
  ```

- [ ] **创建用户**
  ```sql
  CREATE USER 'ceams_user'@'localhost' IDENTIFIED BY 'StrongPassword123!';
  GRANT ALL PRIVILEGES ON ceams.* TO 'ceams_user'@'localhost';
  FLUSH PRIVILEGES;
  ```

- [ ] **执行迁移脚本**
  ```bash
  mysql -u ceams_user -p ceams < src/main/resources/db/migration/V1.0.10__Create_Notification_Tables.sql
  mysql -u ceams_user -p ceams < src/main/resources/db/migration/V1.0.11__Add_Project_Manager_Field.sql
  mysql -u ceams_user -p ceams < src/main/resources/db/migration/V1.0.12__Add_Receipt_Unique_Constraints.sql
  mysql -u ceams_user -p ceams < src/main/resources/db/migration/V1.0.13__Add_Receipt_Number_Field.sql
  ```

- [ ] **验证数据库**
  ```sql
  SHOW DATABASES;
  USE ceams;
  SHOW TABLES;
  ```

#### 2. Redis配置
- [ ] **启动Redis服务**
  ```bash
  # Windows
  redis-server

  # Linux
  sudo systemctl start redis
  sudo systemctl enable redis
  ```

- [ ] **验证Redis连接**
  ```bash
  redis-cli ping
  # 应返回: PONG
  ```

---

### ☐ 阶段三：应用配置 (预计15分钟)

#### 1. 环境变量配置
- [ ] **数据库连接**
  ```properties
  DB_HOST=localhost
  DB_PORT=3306
  DB_NAME=ceams
  DB_USERNAME=ceams_user
  DB_PASSWORD=YourStrongPassword123!
  ```

- [ ] **Redis连接**
  ```properties
  REDIS_HOST=localhost
  REDIS_PORT=6379
  REDIS_PASSWORD=
  ```

- [ ] **JWT配置**
  ```properties
  JWT_SECRET=生成一个64位随机密钥
  JWT_EXPIRATION=86400000
  ```

- [ ] **邮件配置**
  ```properties
  MAIL_HOST=smtp.exmail.qq.com
  MAIL_PORT=587
  MAIL_USERNAME=your_email@company.com
  MAIL_PASSWORD=your_email_password
  ```

#### 2. 使用配置向导
- [ ] 运行 `deploy\setup-env.bat`
- [ ] 填写配置信息
- [ ] 验证配置文件生成

---

### ☐ 阶段四：构建和部署 (预计30分钟)

#### 1. 构建项目
- [ ] **清理构建**
  ```bash
  mvn clean
  ```

- [ ] **打包项目**
  ```bash
  mvn package -DskipTests
  ```

- [ ] **验证JAR文件**
  ```bash
  ls target/ceams-1.0-SNAPSHOT.jar
  ```

#### 2. 启动服务
- [ ] **运行启动脚本**
  ```bash
  deploy\start.bat
  ```

- [ ] **验证服务启动**
  ```bash
  curl http://localhost:9095/actuator/health
  ```

- [ ] **检查日志**
  ```bash
  tail -f logs/ceams-stdout.log
  ```

#### 3. 防火墙配置
- [ ] **Windows防火墙**
  ```powershell
  New-NetFirewallRule -DisplayName "CEAMS Backend" -Direction Inbound -Action Allow -Protocol TCP -LocalPort 9095
  ```

- [ ] **Linux防火墙**
  ```bash
  sudo ufw allow 9095/tcp
  ```

---

### ☐ 阶段五：客户端部署 (预计20分钟)

#### 1. JavaFX桌面客户端
- [ ] **打包客户端**
  ```bash
  # 使用Launch4j打包
  ```

- [ ] **配置服务器地址**
  - 编辑 `config.properties`
  - 设置正确的服务器IP和端口

- [ ] **分发部署**
  - 创建分发包
  - 复制到客户端电脑
  - 运行初始化脚本

#### 2. Android移动端
- [ ] **修改服务器地址**
  ```java
  // RetrofitClient.java
  private static final String BASE_URL = "http://YOUR_SERVER_IP:9095";
  ```

- [ ] **构建APK**
  ```bash
  cd androidclient
  ./gradlew assembleRelease
  ```

- [ ] **配置网络安全** (Android 9+)
  - 编辑 `network_security_config.xml`
  - 允许明文流量或配置HTTPS

- [ ] **签名APK**
  - 配置签名信息
  - 生成签名APK

---

### ☐ 阶段六：安全加固 (预计15分钟)

#### 1. 密码安全
- [ ] **数据库密码**: 强密码 (12位以上，包含大小写字母、数字、特殊字符)
- [ ] **JWT密钥**: 64位随机字符串
- [ ] **管理员密码**: 强密码，定期更换

#### 2. 访问控制
- [ ] **限制数据库访问源**
  ```sql
  REVOKE ALL ON ceams.* FROM 'ceams_user'@'%';
  GRANT SELECT, INSERT, UPDATE, DELETE ON ceams.* TO 'ceams_user'@'localhost';
  ```

- [ ] **配置防火墙规则**
  ```bash
  # 仅允许内网访问
  sudo ufw allow from 192.168.0.0/16 to any port 9095
  ```

#### 3. HTTPS配置 (生产环境)
- [ ] **申请SSL证书**
  ```bash
  # Let's Encrypt
  sudo certbot --nginx -d ceams.yourcompany.com
  ```

- [ ] **配置Nginx反向代理**
  - 编辑 `/etc/nginx/sites-available/ceams`
  - 启用HTTPS

---

### ☐ 阶段七：备份策略 (预计10分钟)

#### 1. 配置自动备份
- [ ] **创建备份脚本**
  ```bash
  #!/bin/bash
  mysqldump -u ceams_user -p ceams | gzip > backup/ceams_$(date +%Y%m%d).sql.gz
  ```

- [ ] **配置定时任务**
  ```bash
  crontab -e
  # 每天凌晨2点执行
  0 2 * * * /opt/ceams/backup.sh
  ```

#### 2. 验证备份
- [ ] **手动执行备份**
  ```bash
  ./backup.sh
  ls -lh backup/
  ```

- [ ] **测试恢复流程**
  ```bash
  mysql -u ceams_user -p ceams < backup/ceams_20240101.sql.gz
  ```

---

### ☐ 阶段八：监控和运维 (持续)

#### 1. 健康检查
- [ ] **API健康检查**
  ```bash
  curl http://localhost:9095/actuator/health
  ```

- [ ] **数据库连接**
  ```bash
  mysql -u ceams_user -p -e "SELECT 1"
  ```

- [ ] **Redis连接**
  ```bash
  redis-cli ping
  ```

#### 2. 日志管理
- [ ] **配置日志轮转**
  ```bash
  # /etc/logrotate.d/ceams
  /opt/ceams/logs/*.log {
      daily
      rotate 30
      compress
  }
  ```

- [ ] **日志位置**
  ```bash
  tail -f logs/ceams-stdout.log
  journalctl -u ceams -f
  ```

#### 3. 性能监控
- [ ] **监控指标**
  - CPU使用率 < 80%
  - 内存使用率 < 85%
  - 磁盘空间 > 20%
  - 数据库连接数 < max_connections的80%

---

## ✅ 部署验证清单

### 功能测试
- [ ] **登录功能**
  - [ ] 使用管理员账户登录
  - [ ] 验证JWT token生成
  - [ ] 测试会话过期

- [ ] **项目管理**
  - [ ] 创建新项目
  - [ ] 编辑项目信息
  - [ ] 删除项目

- [ ] **票据管理**
  - [ ] 上传票据图片
  - [ ] OCR识别
  - [ ] 票据审批流程

- [ ] **电子印章**
  - [ ] USB印章连接
  - [ ] PDF盖章功能
  - [ ] 批量盖章

- [ ] **报表功能**
  - [ ] 财务报表生成
  - [ ] Excel导出
  - [ ] PDF导出

### 移动端测试
- [ ] **Android客户端**
  - [ ] 登录功能
  - [ ] 待审批列表
  - [ ] 审批操作
  - [ ] 消息通知

---

## 📞 故障排除快速参考

### 服务启动失败
```bash
# 1. 检查端口占用
netstat -ano | findstr :9095

# 2. 检查Java版本
java -version

# 3. 查看错误日志
tail -100 logs/ceams-stdout.log

# 4. 检查数据库连接
mysql -u ceams_user -p -e "SHOW DATABASES;"
```

### 客户端无法连接
```bash
# 1. 检查服务状态
curl http://localhost:9095/actuator/health

# 2. 检查防火墙
netsh advfirewall status

# 3. 检查网络连通性
ping server_ip
telnet server_ip 9095
```

### 性能问题
```bash
# 1. 检查资源使用
top
htop

# 2. 检查数据库慢查询
SHOW VARIABLES LIKE 'slow_query_log%';
SELECT * FROM mysql.slow_log ORDER BY start_time DESC LIMIT 10;

# 3. 检查连接数
SHOW STATUS LIKE 'Threads_connected';
```

---

## 📞 技术支持联系

如遇问题，请提供：
1. 操作系统和版本
2. 错误日志完整内容
3. 服务状态检查结果
4. 网络配置信息

---

**检查清单版本**: 1.0
**最后更新**: 2026-05-12
**适用系统版本**: CEAMS 1.0+
