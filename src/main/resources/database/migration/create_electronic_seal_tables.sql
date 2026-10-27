-- 创建电子章表
CREATE TABLE IF NOT EXISTS electronic_seals (
    id INT PRIMARY KEY AUTO_INCREMENT,
    seal_name VARCHAR(100) NOT NULL,
    seal_type VARCHAR(50) NOT NULL,
    description TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'inactive',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 创建电子章申请表
CREATE TABLE IF NOT EXISTS electronic_seal_applications (
    id INT PRIMARY KEY AUTO_INCREMENT,
    seal_id INT NOT NULL,
    seal_name VARCHAR(100) NOT NULL,
    applicant_id INT NOT NULL,
    applicant_name VARCHAR(100) NOT NULL,
    purpose TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'pending',
    approver_id INT,
    approver_name VARCHAR(100),
    approval_comment TEXT,
    apply_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    approval_time DATETIME,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 创建电子章使用记录表
CREATE TABLE IF NOT EXISTS electronic_seal_usages (
    id INT PRIMARY KEY AUTO_INCREMENT,
    seal_id INT NOT NULL,
    seal_name VARCHAR(100) NOT NULL,
    user_id INT NOT NULL,
    user_name VARCHAR(100) NOT NULL,
    purpose TEXT NOT NULL,
    document_type VARCHAR(100),
    document_path VARCHAR(500),
    usage_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ip_address VARCHAR(50),
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 创建电子章盖章表
CREATE TABLE IF NOT EXISTS electronic_seal_stamps (
    id INT PRIMARY KEY AUTO_INCREMENT,
    seal_id INT NOT NULL,
    seal_name VARCHAR(100) NOT NULL,
    project_id INT NOT NULL,
    project_name VARCHAR(255) NOT NULL,
    contract_name VARCHAR(255) NOT NULL,
    contract_path VARCHAR(500) NOT NULL,
    stamp_position VARCHAR(50) NOT NULL,
    stamp_image TEXT,
    user_id INT,
    user_name VARCHAR(100),
    stamp_time DATETIME NOT NULL,
    ip_address VARCHAR(50),
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
