-- 修改users表的role字段长度以支持新的角色常量
-- PROJECT_MANAGER 需要15个字符，所以设置为VARCHAR(20)

USE ceams;

-- 修改role字段长度
ALTER TABLE users MODIFY COLUMN role VARCHAR(20) NOT NULL DEFAULT 'EMPLOYEE' COMMENT '用户角色：EMPLOYEE-普通员工, FINANCE-财务, PROJECT_MANAGER-项目经理, MANAGER-经理, ADMIN-管理员';

-- 更新现有数据，将旧的角色名转换为新角色名
UPDATE users SET role = 'EMPLOYEE' WHERE role = 'staff';
UPDATE users SET role = 'FINANCE' WHERE role = 'finance';
UPDATE users SET role = 'MANAGER' WHERE role = 'manager';
UPDATE users SET role = 'ADMIN' WHERE role = 'admin';

-- 查看修改结果
SELECT user_id, username, name, role FROM users;
