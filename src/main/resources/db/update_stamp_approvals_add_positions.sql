USE ceams;

ALTER TABLE stamp_approvals 
ADD COLUMN original_contract_path VARCHAR(500) COMMENT '原始合同文件路径' AFTER contract_path,
ADD COLUMN stamp_positions TEXT COMMENT '印章位置信息JSON' AFTER original_contract_path;

SELECT 'stamp_approvals表更新成功' AS result;
