# CEAMS 客户端故障排除指南

## 快速诊断

运行以下命令快速诊断问题:

```batch
@echo off
echo ============================================
echo CEAMS 客户端诊断工具
echo ============================================
echo.

echo [1] 检查Java环境
java -version
echo.

echo [2] 检查网络连接
echo 请输入数据库服务器IP:
set /p DB_IP=
ping %DB_IP%
echo.

echo [3] 检查端口连通性
telnet %DB_IP% 3306
echo.

echo [4] 检查配置文件
if exist "config\client-config.properties" (
    echo 配置文件存在
    type "config\client-config.properties"
) else (
    echo 配置文件不存在!
)
echo.

echo [5] 检查日志
if exist "logs\ceams.log" (
    echo 最近10行日志:
    powershell Get-Content logs\ceams.log -Tail 10
) else (
    echo 日志文件不存在
)
echo.

pause
```

## 常见问题及解决方案

### ❌ 问题1: "Java not found" 或 "javaw不是内部或外部命令"

**原因**: Java未安装或环境变量未配置

**解决方案**:

1. 下载并安装 JDK 17: https://adoptium.net/

2. 配置环境变量:
```batch
setx JAVA_HOME "C:\Program Files\Eclipse Adoptium\jdk-17.0.5.8-hotspot"
setx PATH "%PATH%;%JAVA_HOME%\bin"
```

3. 重启命令提示符

4. 验证安装:
```batch
java -version
```

### ❌ 问题2: "Connection refused" 或 "无法连接到数据库"

**原因**: 
- MySQL服务器未运行
- 防火墙阻止连接
- IP地址错误

**诊断步骤**:

1. 检查MySQL服务状态(在服务器上):
```bash
net start | findstr MySQL
```

2. 测试网络连接:
```batch
ping 192.168.1.100
telnet 192.168.1.100 3306
```

3. 检查防火墙(在服务器上,以管理员):
```powershell
Get-NetFirewallRule -DisplayName "*MySQL*"
```

**解决方案**:

- 确认MySQL服务器已启动
- 确认IP地址正确
- 开放防火墙端口:
```powershell
New-NetFirewallRule -DisplayName "CEAMS MySQL" `
  -Direction Inbound -Protocol TCP `
  -LocalPort 3306 -Action Allow
```

### ❌ 问题3: "Access denied for user"

**原因**: 
- 用户名或密码错误
- 用户权限不足
- IP地址不在允许范围内

**诊断步骤**:

1. 在MySQL服务器上检查用户:
```sql
SELECT user, host FROM mysql.user WHERE user LIKE 'ceams%';
```

2. 检查用户权限:
```sql
SHOW GRANTS FOR 'ceams_client_write'@'%';
```

**解决方案**:

1. 确认密码正确
2. 创建或更新用户:
```sql
CREATE USER 'ceams_client_write'@'%' IDENTIFIED BY '你的密码';
GRANT SELECT, INSERT, UPDATE, DELETE ON ceams.* TO 'ceams_client_write'@'%';
FLUSH PRIVILEGES;
```

3. 限制IP范围(更安全):
```sql
CREATE USER 'ceams_client_write'@'192.168.1.%' IDENTIFIED BY '你的密码';
GRANT SELECT, INSERT, UPDATE, DELETE ON ceams.* TO 'ceams_client_write'@'192.168.1.%';
```

### ❌ 问题4: 应用启动后界面空白或卡死

**原因**: 
- 内存不足
- 显卡驱动问题
- Java版本不兼容

**解决方案**:

1. 增加内存分配:
```batch
java -Xms512m -Xmx1024m -jar ceams-1.0-SNAPSHOT.jar
```

2. 更新显卡驱动

3. 使用软件渲染(备用):
```batch
java -Dprism.order=sw -jar ceams-1.0-SNAPSHOT.jar
```

### ❌ 问题5: 数据保存失败或丢失

**原因**: 
- 并发冲突
- 网络中断
- 数据库连接超时

**预防措施**:

1. 避免多人同时编辑同一数据
2. 使用稳定网络连接
3. 定期手动保存
4. 启用自动备份

**恢复数据**:

```sql
-- 查看最近删除的数据
SELECT * FROM receipt WHERE delete_time > DATE_SUB(NOW(), INTERVAL 7 DAY);
```

### ❌ 问题6: 登录失败,提示"用户名或密码错误"

**原因**:
- 输入错误
- 账户被禁用
- 密码已过期

**解决方案**:

1. 确认使用正确的用户名密码
2. 联系管理员检查账户状态
3. 重置密码(需要管理员权限):
```sql
UPDATE user SET password = '新密码的BCrypt哈希' WHERE username = 'xxx';
```

### ❌ 问题7: 应用启动报错 "No suitable driver"

**原因**: MySQL驱动未找到

**解决方案**:

1. 检查jar文件是否完整
2. 重新打包:
```batch
build-client.bat
```

3. 检查依赖是否包含MySQL驱动

### ❌ 问题8: 网络正常但应用经常断开连接

**原因**:
- MySQL连接超时设置过短
- 网络不稳定
- 服务器负载过高

**解决方案**:

1. 修改MySQL超时设置(服务器端):
```sql
SET GLOBAL wait_timeout = 28800;
SET GLOBAL interactive_timeout = 28800;
```

2. 修改应用配置:
```properties
spring.datasource.hikari.maximum-pool-size=5
spring.datasource.hikari.connection-timeout=30000
```

3. 检查网络设备

## 日志分析

### 查看错误日志

```batch
type logs\ceams.log | findstr /i "error exception failed"
```

### 查看数据库连接日志

```batch
type logs\ceams.log | findstr /i "datasource jdbc connection"
```

### 查看登录日志

```batch
type logs\ceams.log | findstr /i "login auth security"
```

## 获取帮助

如果以上方法都无法解决问题,请收集以下信息:

1. **错误信息截图**
2. **日志文件** (`logs\ceams.log`)
3. **配置文件** (`config\client-config.properties`)
4. **Java版本** (`java -version`)
5. **网络测试结果** (ping和telnet测试)
6. **操作系统版本** (`winver`)

联系技术支持时提供以上信息,可以加快问题解决速度。

---

**最后更新**: 2026-05-17  
**适用版本**: CEAMS 1.0+
