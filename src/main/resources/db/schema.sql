-- ============================================
-- 建筑企业账目管理系统数据库初始化脚本
-- 版本: 1.0
-- 日期: 2026-05-17
-- ============================================

-- 创建数据库
CREATE DATABASE IF NOT EXISTS ceams DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE ceams;

-- ============================================
-- 1. 用户表 (user)
-- ============================================
CREATE TABLE IF NOT EXISTS `user` (
    `user_id` INT NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '用户ID',
    `username` VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    `password` VARCHAR(255) NOT NULL COMMENT '密码',
    `name` VARCHAR(100) NOT NULL COMMENT '姓名',
    `role` VARCHAR(50) NOT NULL COMMENT '角色: ADMIN, MANAGER, PROJECT_MANAGER, FINANCE, EMPLOYEE',
    `status` VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态: ACTIVE, INACTIVE',
    `email` VARCHAR(100) COMMENT '邮箱',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_username (`username`),
    INDEX idx_role (`role`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- ============================================
-- 2. 项目表 (project)
-- ============================================
CREATE TABLE IF NOT EXISTS `project` (
    `project_id` INT NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '项目ID',
    `project_name` VARCHAR(200) NOT NULL COMMENT '项目名称',
    `project_code` VARCHAR(50) COMMENT '项目编号',
    `location` VARCHAR(200) COMMENT '项目地点',
    `start_date` DATE COMMENT '开始日期',
    `end_date` DATE COMMENT '结束日期',
    `total_budget` DECIMAL(18,2) COMMENT '总预算',
    `manager_id` INT COMMENT '项目经理ID',
    `status` VARCHAR(50) DEFAULT 'PLANNING' COMMENT '状态: PLANNING, IN_PROGRESS, COMPLETED, CLOSED',
    `description` TEXT COMMENT '项目描述',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_project_code (`project_code`),
    INDEX idx_manager (`manager_id`),
    INDEX idx_status (`status`),
    FOREIGN KEY (`manager_id`) REFERENCES `user`(`user_id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='项目表';

-- ============================================
-- 3. 供应商表 (subcontractor)
-- ============================================
CREATE TABLE IF NOT EXISTS `subcontractor` (
    `subcontractor_id` INT NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '供应商ID',
    `company_name` VARCHAR(200) NOT NULL COMMENT '公司名称',
    `contact_name` VARCHAR(100) COMMENT '联系人姓名',
    `contact_phone` VARCHAR(20) COMMENT '联系电话',
    `contact_email` VARCHAR(100) COMMENT '联系邮箱',
    `address` VARCHAR(200) COMMENT '地址',
    `tax_number` VARCHAR(50) COMMENT '税号',
    `status` VARCHAR(20) DEFAULT 'ACTIVE' COMMENT '状态',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_company_name (`company_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='供应商表';

-- ============================================
-- 4. 材料表 (material)
-- ============================================
CREATE TABLE IF NOT EXISTS `material` (
    `material_id` INT NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '材料ID',
    `material_name` VARCHAR(200) NOT NULL COMMENT '材料名称',
    `material_code` VARCHAR(50) COMMENT '材料编号',
    `category` VARCHAR(100) COMMENT '分类',
    `unit` VARCHAR(20) COMMENT '单位',
    `price` DECIMAL(18,2) COMMENT '单价',
    `specification` VARCHAR(200) COMMENT '规格',
    `status` VARCHAR(20) DEFAULT 'ACTIVE' COMMENT '状态',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_material_code (`material_code`),
    INDEX idx_category (`category`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='材料表';

-- ============================================
-- 5. 账目表 (account)
-- ============================================
CREATE TABLE IF NOT EXISTS `account` (
    `account_id` INT NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '账目ID',
    `account_code` VARCHAR(50) COMMENT '账目编号',
    `account_name` VARCHAR(200) NOT NULL COMMENT '账目名称',
    `type` VARCHAR(50) COMMENT '类型: INCOME, EXPENSE',
    `category` VARCHAR(100) COMMENT '分类',
    `project_id` INT COMMENT '项目ID',
    `amount` DECIMAL(18,2) COMMENT '金额',
    `date` DATE COMMENT '日期',
    `description` TEXT COMMENT '描述',
    `status` VARCHAR(20) DEFAULT 'PENDING' COMMENT '状态: PENDING, APPROVED, REJECTED',
    `created_by` INT COMMENT '创建人ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_account_code (`account_code`),
    INDEX idx_project_id (`project_id`),
    INDEX idx_type (`type`),
    INDEX idx_status (`status`),
    FOREIGN KEY (`project_id`) REFERENCES `project`(`project_id`) ON DELETE SET NULL,
    FOREIGN KEY (`created_by`) REFERENCES `user`(`user_id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='账目表';

-- ============================================
-- 6. 票据表 (receipt)
-- ============================================
CREATE TABLE IF NOT EXISTS `receipt` (
    `receipt_id` INT NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '票据ID',
    `receipt_code` VARCHAR(50) COMMENT '票据编号',
    `type` VARCHAR(50) COMMENT '票据类型',
    `amount` DECIMAL(18,2) COMMENT '金额',
    `amount_in_words` VARCHAR(200) COMMENT '金额大写',
    `date` DATE COMMENT '日期',
    `description` TEXT COMMENT '描述',
    `project_id` INT COMMENT '项目ID',
    `supplier_id` INT COMMENT '供应商ID',
    `file_path` VARCHAR(500) COMMENT '文件路径',
    `file_original_name` VARCHAR(200) COMMENT '原始文件名',
    `status` VARCHAR(20) DEFAULT 'PENDING' COMMENT '状态: PENDING, VERIFIED, REJECTED',
    `created_by` INT COMMENT '创建人ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_receipt_code (`receipt_code`),
    INDEX idx_project_id (`project_id`),
    INDEX idx_status (`status`),
    FOREIGN KEY (`project_id`) REFERENCES `project`(`project_id`) ON DELETE SET NULL,
    FOREIGN KEY (`supplier_id`) REFERENCES `subcontractor`(`subcontractor_id`) ON DELETE SET NULL,
    FOREIGN KEY (`created_by`) REFERENCES `user`(`user_id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='票据表';

-- ============================================
-- 7. 审批记录表 (approval_record)
-- ============================================
CREATE TABLE IF NOT EXISTS `approval_record` (
    `record_id` INT NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '记录ID',
    `receipt_id` INT COMMENT '票据ID',
    `account_id` INT COMMENT '账目ID',
    `type` VARCHAR(50) COMMENT '审批类型',
    `step` INT COMMENT '审批步骤',
    `approver_id` INT COMMENT '审批人ID',
    `action` VARCHAR(50) COMMENT '动作: APPROVE, REJECT',
    `comment` TEXT COMMENT '意见',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_receipt_id (`receipt_id`),
    INDEX idx_account_id (`account_id`),
    INDEX idx_approver_id (`approver_id`),
    FOREIGN KEY (`receipt_id`) REFERENCES `receipt`(`receipt_id`) ON DELETE CASCADE,
    FOREIGN KEY (`account_id`) REFERENCES `account`(`account_id`) ON DELETE CASCADE,
    FOREIGN KEY (`approver_id`) REFERENCES `user`(`user_id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='审批记录表';

-- ============================================
-- 8. 电子印章表 (electronic_seal)
-- ============================================
CREATE TABLE IF NOT EXISTS `electronic_seal` (
    `seal_id` INT NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '印章ID',
    `seal_name` VARCHAR(200) NOT NULL COMMENT '印章名称',
    `seal_code` VARCHAR(50) COMMENT '印章编号',
    `type` VARCHAR(50) COMMENT '印章类型',
    `status` VARCHAR(20) DEFAULT 'ACTIVE' COMMENT '状态',
    `creator_id` INT COMMENT '创建人ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_seal_code (`seal_code`),
    INDEX idx_creator_id (`creator_id`),
    FOREIGN KEY (`creator_id`) REFERENCES `user`(`user_id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='电子印章表';

-- ============================================
-- 9. 操作日志表 (operation_log)
-- ============================================
CREATE TABLE IF NOT EXISTS `operation_log` (
    `log_id` INT NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '日志ID',
    `user_id` INT COMMENT '用户ID',
    `username` VARCHAR(50) COMMENT '用户名',
    `action` VARCHAR(200) COMMENT '操作',
    `module` VARCHAR(100) COMMENT '模块',
    `details` TEXT COMMENT '详情',
    `ip_address` VARCHAR(50) COMMENT 'IP地址',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_user_id (`user_id`),
    INDEX idx_create_time (`create_time`),
    FOREIGN KEY (`user_id`) REFERENCES `user`(`user_id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='操作日志表';

-- ============================================
-- 初始化默认数据
-- ============================================

-- 插入默认管理员用户 (密码: admin123)
INSERT INTO `user` (`username`, `password`, `name`, `role`, `email`, `status`) VALUES
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6zXyO', '系统管理员', 'ADMIN', 'admin@example.com', 'ACTIVE');

-- 插入默认经理用户 (密码: manager123)
INSERT INTO `user` (`username`, `password`, `name`, `role`, `email`, `status`) VALUES
('manager', '$2a$10$U0q8qV7y3fV2Jx9LkG6vSejX2dZ9h8Z7y3X2W1eQ1aW9j8K8L0aK', '项目经理', 'MANAGER', 'manager@example.com', 'ACTIVE');

-- ============================================
-- 初始化完成
-- ============================================
SELECT '数据库初始化完成！' AS message;
