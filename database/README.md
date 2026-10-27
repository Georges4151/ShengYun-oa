# 盖章审批表创建操作指南

## 问题描述
项目启动后，打开"我的盖章审批"窗口时出现 FXML 加载错误，原因是数据库中缺少 `stamp_approvals` 表。

## 解决方案

### 方法一：使用批处理脚本（推荐）

1. **打开命令行窗口**
   - 按 `Win + R` 打开运行窗口
   - 输入 `cmd` 并按回车

2. **进入数据库脚本目录**
   ```bash
   cd E:\sxassist\database
   ```

3. **执行批处理脚本**
   ```bash
   execute_create_table.bat
   ```

4. **验证结果**
   - 看到 "✓ 表创建成功！" 提示表示操作成功

### 方法二：使用 MySQL 命令行

1. **打开 MySQL 命令行**
   ```bash
   mysql -u root -p
   ```
   输入密码：`123456`

2. **切换到 ceams 数据库**
   ```sql
   USE ceams;
   ```

3. **执行 SQL 脚本**
   ```sql
   source E:/sxassist/database/create_stamp_approvals_table.sql;
   ```

4. **验证表是否创建成功**
   ```sql
   SHOW TABLES LIKE 'stamp_approvals';
   DESC stamp_approvals;
   ```

### 方法三：使用 MySQL Workbench 或其他数据库管理工具

1. **打开 MySQL Workbench**
2. **连接到数据库**
   - Host: `localhost`
   - Port: `3306`
   - Username: `root`
   - Password: `123456`

3. **打开 SQL 脚本文件**
   - File → Open SQL Script
   - 选择 `E:\sxassist\database\create_stamp_approvals_table.sql`

4. **执行脚本**
   - 点击 ⚡（Execute）按钮

5. **验证结果**
   - 在左侧 Schema 面板中刷新 `ceams` 数据库
   - 查看 `Tables` 下是否有 `stamp_approvals` 表

### 方法四：使用 Navicat 或其他数据库管理工具

1. **打开 Navicat**
2. **连接到 MySQL 数据库**
3. **选择 `ceams` 数据库**
4. **右键 → 运行 SQL 文件**
5. **选择 `E:\sxassist\database\create_stamp_approvals_table.sql`**
6. **点击"开始"执行**
7. **刷新数据库查看结果**

## 表结构说明

创建的 `stamp_approvals` 表包含以下字段：

| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | INT | 主键ID，自增 |
| apply_user_id | INT | 申请人ID |
| apply_user_name | VARCHAR(100) | 申请人姓名 |
| apply_department | VARCHAR(200) | 申请部门 |
| stamp_type | VARCHAR(50) | 印章类型 |
| stamp_reason | VARCHAR(500) | 盖章原因 |
| apply_time | DATETIME | 申请时间 |
| status | VARCHAR(20) | 审批状态 |
| approver_id | INT | 审批人ID |
| approver_name | VARCHAR(100) | 审批人姓名 |
| approve_time | DATETIME | 审批时间 |
| approve_remark | TEXT | 审批备注/驳回原因 |
| create_time | DATETIME | 创建时间 |
| update_time | DATETIME | 更新时间 |
| is_deleted | TINYINT(1) | 删除标记 |

## 验证步骤

执行完 SQL 脚本后，请按以下步骤验证：

1. **检查表是否存在**
   ```sql
   USE ceams;
   SHOW TABLES LIKE 'stamp_approvals';
   ```

2. **查看表结构**
   ```sql
   DESC stamp_approvals;
   ```

3. **查看表信息**
   ```sql
   SELECT COUNT(*) FROM stamp_approvals;
   ```

## 重启项目

表创建成功后，请按以下步骤重启项目：

1. **停止当前运行的项目**（如果正在运行）
   - 按 `Ctrl + C` 停止

2. **重新编译项目**
   ```bash
   cd E:\sxassist
   mvn clean compile
   ```

3. **启动项目**
   ```bash
   mvn spring-boot:run
   ```

4. **测试功能**
   - 登录系统
   - 进入"电子章管理" → "我的盖章审批"
   - 确认窗口正常打开，无错误提示

## 常见问题

### Q1: 执行脚本时提示"Access denied for user"
**A:** 数据库密码不正确，请检查 `application.properties` 文件中的数据库密码配置。

### Q2: 执行脚本时提示"Unknown database 'ceams'"
**A:** 数据库不存在，需要先创建数据库：
```sql
CREATE DATABASE ceams DEFAULT CHARACTER SET utf8mb4;
```

### Q3: 表创建成功但仍然报错
**A:** 请检查：
1. 项目是否已重启
2. 是否清除了编译缓存（执行 `mvn clean`）
3. 数据库连接是否正常

### Q4: 如何确认数据库连接信息
**A:** 查看 `E:\sxassist\src\main\resources\application.properties` 文件：
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/ceams
spring.datasource.username=root
spring.datasource.password=123456
```

## 联系支持

如果以上步骤仍无法解决问题，请提供以下信息：
1. 错误截图
2. 数据库版本
3. 项目日志文件（`E:\sxassist\logs\ceams.log`）
