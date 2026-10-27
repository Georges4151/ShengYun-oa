USE ceams;

CREATE TABLE IF NOT EXISTS stamp_approvals (
    id INT PRIMARY KEY AUTO_INCREMENT,
    stamp_id INT,
    contract_name VARCHAR(255),
    contract_path VARCHAR(500),
    original_contract_path VARCHAR(500),
    stamp_positions TEXT,
    seal_id INT,
    seal_name VARCHAR(100),
    seal_code VARCHAR(50),
    seal_permission TEXT,
    project_id INT,
    project_name VARCHAR(255),
    applicant_id INT,
    applicant_name VARCHAR(100),
    approver_id INT,
    approver_name VARCHAR(100),
    status VARCHAR(20) DEFAULT 'pending',
    reject_reason TEXT,
    apply_time DATETIME,
    approval_time DATETIME,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE stamp_approvals 
ADD COLUMN IF NOT EXISTS original_contract_path VARCHAR(500) COMMENT '原始合同文件路径' AFTER contract_path;

ALTER TABLE stamp_approvals 
ADD COLUMN IF NOT EXISTS stamp_positions TEXT COMMENT '印章位置信息JSON' AFTER original_contract_path;

ALTER TABLE stamp_approvals 
ADD COLUMN IF NOT EXISTS seal_id INT COMMENT '印章ID' AFTER stamp_positions;

ALTER TABLE stamp_approvals 
ADD COLUMN IF NOT EXISTS seal_name VARCHAR(100) COMMENT '印章名称' AFTER seal_id;

ALTER TABLE stamp_approvals 
ADD COLUMN IF NOT EXISTS seal_code VARCHAR(50) COMMENT '印章编码' AFTER seal_name;

ALTER TABLE stamp_approvals 
ADD COLUMN IF NOT EXISTS seal_permission TEXT COMMENT '印章权限说明' AFTER seal_code;

ALTER TABLE stamp_approvals 
ADD COLUMN IF NOT EXISTS approval_time DATETIME COMMENT '审批时间' AFTER reject_reason;

SELECT 'stamp_approvals表创建/更新成功' AS result;
