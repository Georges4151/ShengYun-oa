-- 为电子章表添加U盘存储相关字段
ALTER TABLE electronic_seals ADD COLUMN storage_type VARCHAR(20) DEFAULT 'local' COMMENT '存储类型: local-本地, usb-U盘';
ALTER TABLE electronic_seals ADD COLUMN seal_file_id VARCHAR(50) COMMENT '电子章文件ID(加密文件名)';
ALTER TABLE electronic_seals ADD COLUMN usb_drive_id VARCHAR(100) COMMENT 'U盘唯一标识';
ALTER TABLE electronic_seals ADD COLUMN password_protected TINYINT(1) DEFAULT 0 COMMENT '是否密码保护';
ALTER TABLE electronic_seals ADD COLUMN last_used_time DATETIME COMMENT '最后使用时间';
ALTER TABLE electronic_seals ADD COLUMN usage_count INT DEFAULT 0 COMMENT '使用次数';

-- 更新现有数据
UPDATE electronic_seals SET storage_type = 'local', password_protected = 0 WHERE storage_type IS NULL;
