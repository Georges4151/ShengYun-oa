USE ceams;

-- 查看 stamp_positions 列当前的数据类型
SELECT 
    COLUMN_NAME, 
    COLUMN_TYPE, 
    DATA_TYPE, 
    CHARACTER_MAXIMUM_LENGTH
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = 'ceams'
AND TABLE_NAME = 'stamp_approvals'
AND COLUMN_NAME = 'stamp_positions';
