-- 劳动合同台账：补充合同原件附件（通用文件服务返回的存储名 + 原始文件名）
ALTER TABLE hr_contract
    ADD COLUMN file_path VARCHAR(255) NULL DEFAULT NULL COMMENT '合同原件存储名（文件服务 name）' AFTER file_id,
    ADD COLUMN file_name VARCHAR(255) NULL DEFAULT NULL COMMENT '合同原件原始文件名' AFTER file_path;
