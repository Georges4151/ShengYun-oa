# ============================================
# 数据库配置指南
# ============================================

## 前提条件
1. MySQL 5.7 或 8.0 已安装并运行
2. 有 MySQL 用户名和密码 (通常是 root 用户
3. 服务器可以连接到 MySQL

## 步骤一：数据库配置步骤

### 1. 使用命令行创建数据库
```bash
# 连接到 MySQL
mysql -u root -p
# 输入密码后，执行以下命令
```

### 2. 运行初始化脚本
```sql
# 在 MySQL 命令行中执行
source e:/sxassist/src/main/resources/db/schema.sql;
```

或者直接在命令行执行：
```bash
mysql -u root -p < e:/sxassist/src/main/resources/db/schema.sql
```

---

## 步骤二：修改应用配置

### 编辑 application.properties 文件

打开文件：`e:\sxassist\src\main\resources\application.properties

将以下内容更新为您的实际配置：

```properties
# 数据库配置
spring.datasource.url=jdbc:mysql://localhost:3306/ceams?useSSL=false&serverTimezone=Asia/Shanghai&characterEncoding=utf8
spring.datasource.username=root
spring.datasource.password=你的MySQL密码
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
```

## 步骤三：检查配置

1. `spring.datasource.username`: 您的MySQL用户名 (通常是 root)
2. `spring.datasource.password`: 您的MySQL密码
3. `spring.datasource.url`: 如果MySQL服务器地址 (本地是 localhost

## 步骤四：重启应用

修改配置后，重启应用：

```powershell
cd e:\sxassist
java -jar target\ceams-1.0-SNAPSHOT.jar
```

## 默认登录信息：
- 用户名：admin
- 密码：admin123
