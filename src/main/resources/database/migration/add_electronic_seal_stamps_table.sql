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
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (seal_id) REFERENCES electronic_seals(id),
    FOREIGN KEY (project_id) REFERENCES projects(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
