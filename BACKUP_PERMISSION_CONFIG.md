# 备份与恢复权限控制配置说明

## 一、权限控制概述

### 1.1 功能说明

为了保护企业核心数据安全，系统对**备份与恢复功能**实施了严格的权限控制：

- **访问控制**：只有特定角色的用户才能看到备份管理功能
- **操作控制**：只有授权用户才能执行备份和恢复操作
- **操作审计**：所有备份和恢复操作都会记录操作用户信息

### 1.2 权限策略

```
┌─────────────────────────────────────────────────────────┐
│                    角色权限表                            │
├─────────────────────────────────────────────────────────┤
│  角色              │  查看备份  │  创建备份  │  恢复备份  │
├───────────────────┼────────────┼────────────┼────────────┤
│  EMPLOYEE (员工)   │     ❌     │     ❌     │     ❌     │
│  FINANCE (财务)    │     ❌     │     ❌     │     ❌     │
│  PROJECT_MANAGER   │     ❌     │     ❌     │     ❌     │
│  MANAGER (经理)    │     ✅     │     ✅     │     ✅     │
│  ADMIN (管理员)    │     ✅     │     ✅     │     ✅     │
└───────────────────┴────────────┴────────────┴────────────┘
```

## 二、技术实现

### 2.1 后端控制器权限控制

**文件**: `src/main/java/com/ceams/controller/BackupController.java`

```java
@Controller
@RequestMapping("/backup")
@PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")  // 关键配置
public class BackupController {
    // 所有方法都受此权限控制
}
```

**说明**:
- 使用 Spring Security 的 `@PreAuthorize` 注解
- 限制所有 `/backup/**` 路径的访问
- 仅允许 `MANAGER` 和 `ADMIN` 角色访问

### 2.2 前端界面权限控制

#### 2.2.1 ModernMainController (现代风格界面)

**文件**: `src/main/java/com/ceams/ui/controller/ModernMainController.java`

```java
@FXML
private void createBackup() {
    // 权限检查：仅经理和管理员可执行备份
    if (!userSessionService.hasBackupPermission()) {
        Alert warningAlert = new Alert(Alert.AlertType.WARNING);
        warningAlert.setTitle("权限不足");
        warningAlert.setContentText("抱歉，您没有权限执行备份操作。\n\n备份和恢复功能仅对经理和管理员开放。\n\n如需此权限，请联系系统管理员。");
        warningAlert.showAndWait();
        return;
    }
    // 执行备份...
}

@FXML
private void restoreBackup() {
    // 权限检查：仅经理和管理员可执行恢复
    if (!userSessionService.hasBackupPermission()) {
        // 权限不足提示...
        return;
    }
    // 执行恢复...
}
```

#### 2.2.2 MainController (传统风格界面)

**文件**: `src/main/java/com/ceams/ui/controller/MainController.java`

权限检查逻辑与 ModernMainController 相同。

### 2.3 权限判断服务

**文件**: `src/main/java/com/ceams/service/UserSessionService.java`

```java
/**
 * 检查用户是否有备份管理权限
 * 仅 MANAGER 和 ADMIN 角色可以执行备份和恢复操作
 * @return true表示有权限
 */
public boolean hasBackupPermission() {
    if (currentUser == null) {
        return false;
    }
    String role = currentUser.getRole();
    if (role == null) {
        return false;
    }
    String roleLower = role.toLowerCase();
    return "manager".equals(roleLower) || "admin".equals(roleLower);
}
```

## 三、权限配置详解

### 3.1 系统内置角色

| 角色常量 | 角色名称 | 权限等级 | 备份权限 |
|---------|---------|---------|----------|
| `EMPLOYEE` | 员工 | 1 | ❌ 无 |
| `FINANCE` | 财务 | 2 | ❌ 无 |
| `PROJECT_MANAGER` | 项目经理 | 3 | ❌ 无 |
| `MANAGER` | 经理 | 4 | ✅ 有 |
| `ADMIN` | 管理员 | 5 | ✅ 有 |

### 3.2 角色常量定义

**文件**: `src/main/java/com/ceams/constant/RoleConstants.java`

```java
public class RoleConstants {
    public static final String EMPLOYEE = "EMPLOYEE";
    public static final String FINANCE = "FINANCE";
    public static final String PROJECT_MANAGER = "PROJECT_MANAGER";
    public static final String MANAGER = "MANAGER";
    public static final String ADMIN = "ADMIN";
}
```

### 3.3 权限继承关系

```
ADMIN (管理员)
  └── 拥有所有权限
      └── 备份和恢复 ✅

MANAGER (经理)
  └── 业务管理权限
      └── 备份和恢复 ✅

PROJECT_MANAGER (项目经理)
  └── 项目管理权限
      └── 备份和恢复 ❌

FINANCE (财务)
  └── 财务管理权限
      └── 备份和恢复 ❌

EMPLOYEE (员工)
  └── 基本访问权限
      └── 备份和恢复 ❌
```

## 四、用户操作流程

### 4.1 有权限用户操作流程

```
用户登录 (MANAGER/ADMIN)
    │
    ▼
进入系统主界面
    │
    ▼
点击「备份」或「恢复」按钮
    │
    ▼
权限检查通过
    │
    ▼
执行备份/恢复操作
    │
    ▼
系统记录操作日志 (包含用户ID)
    │
    ▼
显示操作结果
```

### 4.2 无权限用户操作流程

```
用户登录 (非MANAGER/ADMIN角色)
    │
    ▼
进入系统主界面
    │
    ▼
点击「备份」或「恢复」按钮
    │
    ▼
权限检查不通过
    │
    ▼
显示权限不足提示
    │
    ▼
返回主界面（操作被拒绝）
```

### 4.3 权限不足提示信息

当普通用户尝试访问备份功能时，会显示以下提示：

```
┌─────────────────────────────────────────┐
│ ⚠️ 权限不足                              │
├─────────────────────────────────────────┤
│                                         │
│  抱歉，您没有权限执行备份操作。          │
│                                         │
│  备份和恢复功能仅对经理和管理员开放。    │
│                                         │
│  如需此权限，请联系系统管理员。          │
│                                         │
│                        [确定]           │
└─────────────────────────────────────────┘
```

## 五、操作日志记录

### 5.1 日志记录内容

每次备份和恢复操作都会记录以下信息：

| 字段 | 说明 | 示例 |
|------|------|------|
| userId | 操作用户ID | 1 |
| backupFileName | 备份文件名 | backup_full_2026-05-12.sql.zip |
| success | 操作是否成功 | true |
| message | 操作消息 | 备份成功 |

### 5.2 日志表结构

```sql
CREATE TABLE operation_logs (
    id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT,
    operation_type VARCHAR(50),
    operation_details TEXT,
    success BOOLEAN,
    message TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### 5.3 日志查询示例

```sql
-- 查询所有备份操作
SELECT * FROM operation_logs 
WHERE operation_type = 'BACKUP' 
ORDER BY created_at DESC;

-- 查询某个用户的备份操作
SELECT * FROM operation_logs 
WHERE user_id = 1 
AND operation_type = 'BACKUP'
ORDER BY created_at DESC;

-- 查询备份失败记录
SELECT * FROM operation_logs 
WHERE operation_type = 'BACKUP' 
AND success = false
ORDER BY created_at DESC;
```

## 六、配置管理

### 6.1 启用/禁用自动备份

**配置文件**: `src/main/resources/application.properties`

```properties
# 是否启用自动备份 (true=启用, false=禁用)
backup.auto.enabled=true

# 自动备份间隔天数
backup.interval.days=7

# 备份保留天数
backup.keep.days=30
```

### 6.2 修改权限策略

如需修改权限策略，编辑以下文件：

#### 后端权限配置

**文件**: `src/main/java/com/ceams/controller/BackupController.java`

```java
// 修改允许访问的角色
@PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
// 例如：添加项目经理权限
// @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN', 'PROJECT_MANAGER')")
```

#### 前端权限配置

**文件**: `src/main/java/com/ceams/service/UserSessionService.java`

```java
public boolean hasBackupPermission() {
    String roleLower = role.toLowerCase();
    // 修改权限判断逻辑
    return "manager".equals(roleLower) || "admin".equals(roleLower);
    // 例如：添加项目经理权限
    // return "manager".equals(roleLower) || "admin".equals(roleLower) 
    //        || "project_manager".equals(roleLower);
}
```

## 七、安全建议

### 7.1 密码安全

1. **定期更换密码**：建议每3个月更换一次
2. **强密码策略**：密码至少12位，包含大小写字母、数字和特殊字符
3. **密码不共享**：每个用户使用独立账户

### 7.2 权限管理

1. **最小权限原则**：只授予用户完成工作所需的最小权限
2. **定期审查**：定期审查用户权限配置
3. **离职处理**：员工离职时及时禁用或删除账户

### 7.3 备份策略

1. **定期备份**：建议每天至少备份一次
2. **离线备份**：定期将备份文件复制到离线存储
3. **异地备份**：重要数据建议异地备份
4. **备份测试**：定期测试备份恢复功能

### 7.4 审计监控

1. **操作日志**：启用详细的操作日志记录
2. **异常告警**：设置备份失败的告警通知
3. **定期审计**：定期审查操作日志，发现异常及时处理

## 八、故障排除

### 8.1 问题：用户无法看到备份按钮

**可能原因**：
1. 用户角色不是 MANAGER 或 ADMIN
2. UI界面未正确加载

**解决方案**：
1. 检查用户角色配置
2. 联系系统管理员修改用户角色

### 8.2 问题：用户点击备份按钮后提示权限不足

**可能原因**：
1. 用户角色不是 MANAGER 或 ADMIN
2. Session未正确保存用户信息

**解决方案**：
1. 确认用户角色为 MANAGER 或 ADMIN
2. 重新登录系统
3. 检查数据库中用户角色配置

### 8.3 问题：操作日志显示 userId 为 null

**可能原因**：
1. 用户通过自动备份任务执行操作
2. Session信息未正确传递

**解决方案**：
1. 自动备份任务的 userId 为 null 是正常的
2. 手动执行时会记录正确的 userId

## 九、相关文档

- [用户权限管理指南](docs/PERMISSION_MANAGEMENT.md)
- [备份恢复操作手册](docs/BACKUP_RECOVERY_MANUAL.md)
- [操作日志配置说明](docs/OPERATION_LOG.md)

---

**文档版本**: 1.0
**最后更新**: 2026-05-12
**适用版本**: CEAMS 1.0+
**维护人员**: 系统管理员
