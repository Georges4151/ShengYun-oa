-- 电子章表
CREATE TABLE IF NOT EXISTS electronic_seals (
    id INT AUTO_INCREMENT PRIMARY KEY,
    seal_name VARCHAR(100) NOT NULL COMMENT '印章名称',
    seal_type VARCHAR(50) COMMENT '印章类型',
    description TEXT COMMENT '描述',
    status VARCHAR(20) DEFAULT 'inactive' COMMENT '状态：active-启用，inactive-未启用，expired-已过期',
    seal_image LONGTEXT COMMENT '印章图片（Base64编码）',
    storage_type VARCHAR(20) DEFAULT 'local' COMMENT '存储类型：local-本地存储，usb-U盘存储',
    seal_file_id VARCHAR(100) COMMENT '印章文件ID',
    usb_drive_id VARCHAR(100) COMMENT 'U盘ID',
    password_protected BOOLEAN DEFAULT FALSE COMMENT '是否密码保护',
    last_used_time DATETIME COMMENT '最后使用时间',
    usage_count INT DEFAULT 0 COMMENT '使用次数',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_seal_name (seal_name),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='电子章表';

-- 电子章申请表
CREATE TABLE IF NOT EXISTS electronic_seal_applications (
    id INT AUTO_INCREMENT PRIMARY KEY,
    seal_id INT COMMENT '印章ID',
    seal_name VARCHAR(100) COMMENT '印章名称',
    applicant_id INT COMMENT '申请人ID',
    applicant_name VARCHAR(100) COMMENT '申请人姓名',
    purpose TEXT COMMENT '申请用途',
    status VARCHAR(20) DEFAULT 'pending' COMMENT '状态：pending-待审批，approved-已批准，rejected-已拒绝',
    approver_id INT COMMENT '审批人ID',
    approver_name VARCHAR(100) COMMENT '审批人姓名',
    approval_comment TEXT COMMENT '审批意见',
    apply_time DATETIME COMMENT '申请时间',
    approval_time DATETIME COMMENT '审批时间',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_seal_id (seal_id),
    INDEX idx_applicant_id (applicant_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='电子章申请表';

-- 电子章使用记录表
CREATE TABLE IF NOT EXISTS electronic_seal_usages (
    id INT AUTO_INCREMENT PRIMARY KEY,
    seal_id INT COMMENT '印章ID',
    seal_name VARCHAR(100) COMMENT '印章名称',
    user_id INT COMMENT '使用人ID',
    user_name VARCHAR(100) COMMENT '使用人姓名',
    purpose TEXT COMMENT '使用用途',
    document_type VARCHAR(50) COMMENT '文档类型',
    document_path VARCHAR(500) COMMENT '文档路径',
    usage_time DATETIME COMMENT '使用时间',
    ip_address VARCHAR(50) COMMENT 'IP地址',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_seal_id (seal_id),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='电子章使用记录表';

-- 电子章盖章表
CREATE TABLE IF NOT EXISTS electronic_seal_stamps (
    id INT AUTO_INCREMENT PRIMARY KEY,
    seal_id INT COMMENT '印章ID',
    seal_name VARCHAR(100) COMMENT '印章名称',
    project_id INT COMMENT '项目ID',
    project_name VARCHAR(200) COMMENT '项目名称',
    contract_name VARCHAR(200) COMMENT '合同名称',
    contract_path VARCHAR(500) COMMENT '合同路径',
    stamp_position VARCHAR(500) COMMENT '盖章位置',
    stamp_image LONGTEXT COMMENT '印章图片',
    stamp_time DATETIME COMMENT '盖章时间',
    user_id INT COMMENT '操作人ID',
    user_name VARCHAR(100) COMMENT '操作人姓名',
    ip_address VARCHAR(50) COMMENT 'IP地址',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_seal_id (seal_id),
    INDEX idx_project_id (project_id),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='电子章盖章表';

-- 盖章审批表
CREATE TABLE IF NOT EXISTS stamp_approvals (
    id INT AUTO_INCREMENT PRIMARY KEY,
    stamp_id INT COMMENT '盖章记录ID',
    contract_name VARCHAR(200) COMMENT '合同名称',
    contract_path VARCHAR(500) COMMENT '盖章后合同路径',
    original_contract_path VARCHAR(500) COMMENT '原始合同路径',
    stamp_positions LONGTEXT COMMENT '盖章位置JSON',
    seal_id INT COMMENT '印章ID',
    seal_name VARCHAR(100) COMMENT '印章名称',
    seal_code VARCHAR(50) COMMENT '印章编码',
    seal_permission VARCHAR(50) COMMENT '印章权限',
    project_id INT COMMENT '项目ID',
    project_name VARCHAR(200) COMMENT '项目名称',
    applicant_id INT COMMENT '申请人ID',
    applicant_name VARCHAR(100) COMMENT '申请人姓名',
    approver_id INT COMMENT '审批人ID',
    approver_name VARCHAR(100) COMMENT '审批人姓名',
    status VARCHAR(20) DEFAULT 'pending' COMMENT '状态：pending-待审批，approved-已批准，rejected-已拒绝',
    reject_reason TEXT COMMENT '驳回原因',
    apply_time DATETIME COMMENT '申请时间',
    approval_time DATETIME COMMENT '审批时间',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_stamp_id (stamp_id),
    INDEX idx_seal_id (seal_id),
    INDEX idx_project_id (project_id),
    INDEX idx_applicant_id (applicant_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='盖章审批表';
