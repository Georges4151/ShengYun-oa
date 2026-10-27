-- 鍒涘缓閫氱煡琛?CREATE TABLE IF NOT EXISTS notification (
    id INT PRIMARY KEY AUTO_INCREMENT COMMENT '閫氱煡ID',
    title VARCHAR(200) NOT NULL COMMENT '閫氱煡鏍囬',
    content_type VARCHAR(20) NOT NULL COMMENT '鍐呭绫诲瀷锛歍EXT/RICH_TEXT/LINK',
    content TEXT NOT NULL COMMENT '閫氱煡鍐呭',
    link_url VARCHAR(500) COMMENT '鐩稿叧閾炬帴鍦板潃',
    priority VARCHAR(10) NOT NULL DEFAULT 'MEDIUM' COMMENT '浼樺厛绾э細HIGH/MEDIUM/LOW',
    notification_type VARCHAR(50) NOT NULL COMMENT '閫氱煡绫诲瀷锛歋YSTEM/BUSINESS/MARKETING',
    sender_id INT COMMENT '鍙戦€佽€匢D',
    sender_name VARCHAR(100) COMMENT '鍙戦€佽€呭鍚?,
    valid_start_time DATETIME COMMENT '鏈夋晥鏈熷紑濮嬫椂闂?,
    valid_end_time DATETIME COMMENT '鏈夋晥鏈熺粨鏉熸椂闂?,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT' COMMENT '鐘舵€侊細DRAFT/PUBLISHED/ARCHIVED',
    publish_time DATETIME COMMENT '鍙戝竷鏃堕棿',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '鍒涘缓鏃堕棿',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '鏇存柊鏃堕棿',
    INDEX idx_priority (priority),
    INDEX idx_type (notification_type),
    INDEX idx_status (status),
    INDEX idx_publish_time (publish_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='閫氱煡琛?;

-- 鍒涘缓鐢ㄦ埛閫氱煡鍏宠仈琛?CREATE TABLE IF NOT EXISTS user_notification (
    id INT PRIMARY KEY AUTO_INCREMENT COMMENT '涓婚敭ID',
    notification_id INT NOT NULL COMMENT '閫氱煡ID',
    user_id INT NOT NULL COMMENT '鐢ㄦ埛ID',
    is_read TINYINT(1) DEFAULT 0 COMMENT '鏄惁宸茶锛?-鏈/1-宸茶',
    read_time DATETIME COMMENT '闃呰鏃堕棿',
    is_deleted TINYINT(1) DEFAULT 0 COMMENT '鏄惁鍒犻櫎锛?-鏈垹闄?1-宸插垹闄?,
    channel VARCHAR(20) COMMENT '鎺ㄩ€佹笭閬擄細SYSTEM/EMAIL/SMS',
    push_status VARCHAR(20) DEFAULT 'PENDING' COMMENT '鎺ㄩ€佺姸鎬侊細PENDING/SENT/FAILED',
    push_time DATETIME COMMENT '鎺ㄩ€佹椂闂?,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '鍒涘缓鏃堕棿',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '鏇存柊鏃堕棿',
    UNIQUE KEY uk_user_notification (user_id, notification_id),
    INDEX idx_user_read (user_id, is_read),
    INDEX idx_notification (notification_id),
    INDEX idx_push_status (push_status),
    FOREIGN KEY (notification_id) REFERENCES notification(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='鐢ㄦ埛閫氱煡鍏宠仈琛?;

-- 鍒涘缓鐢ㄦ埛閫氱煡鍋忓ソ琛?CREATE TABLE IF NOT EXISTS user_notification_preference (
    id INT PRIMARY KEY AUTO_INCREMENT COMMENT '涓婚敭ID',
    user_id INT NOT NULL COMMENT '鐢ㄦ埛ID',
    notification_type VARCHAR(50) NOT NULL COMMENT '閫氱煡绫诲瀷锛歋YSTEM/BUSINESS/MARKETING',
    receive_system TINYINT(1) DEFAULT 1 COMMENT '鏄惁鎺ユ敹绯荤粺鍐呴€氱煡',
    receive_email TINYINT(1) DEFAULT 0 COMMENT '鏄惁鎺ユ敹閭欢閫氱煡',
    receive_sms TINYINT(1) DEFAULT 0 COMMENT '鏄惁鎺ユ敹鐭俊閫氱煡',
    quiet_mode TINYINT(1) DEFAULT 0 COMMENT '鏄惁鍏嶆墦鎵版ā寮?,
    quiet_start_time TIME COMMENT '鍏嶆墦鎵板紑濮嬫椂闂?,
    quiet_end_time TIME COMMENT '鍏嶆墦鎵扮粨鏉熸椂闂?,
    priority_threshold VARCHAR(10) DEFAULT 'LOW' COMMENT '浼樺厛绾ч槇鍊硷細HIGH/MEDIUM/LOW',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '鍒涘缓鏃堕棿',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '鏇存柊鏃堕棿',
    UNIQUE KEY uk_user_type (user_id, notification_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='鐢ㄦ埛閫氱煡鍋忓ソ琛?;

-- 涓虹幇鏈夌敤鎴峰垱寤洪粯璁ら€氱煡鍋忓ソ璁剧疆
INSERT INTO user_notification_preference (user_id, notification_type, receive_system, receive_email, receive_sms, quiet_mode, priority_threshold)
SELECT id, 'SYSTEM', 1, 0, 0, 0, 'LOW' FROM user
WHERE NOT EXISTS (
    SELECT 1 FROM user_notification_preference 
    WHERE user_notification_preference.user_id = user.id 
    AND user_notification_preference.notification_type = 'SYSTEM'
);

INSERT INTO user_notification_preference (user_id, notification_type, receive_system, receive_email, receive_sms, quiet_mode, priority_threshold)
SELECT id, 'BUSINESS', 1, 1, 0, 0, 'MEDIUM' FROM user
WHERE NOT EXISTS (
    SELECT 1 FROM user_notification_preference 
    WHERE user_notification_preference.user_id = user.id 
    AND user_notification_preference.notification_type = 'BUSINESS'
);

INSERT INTO user_notification_preference (user_id, notification_type, receive_system, receive_email, receive_sms, quiet_mode, priority_threshold)
SELECT id, 'MARKETING', 1, 0, 0, 0, 'HIGH' FROM user
WHERE NOT EXISTS (
    SELECT 1 FROM user_notification_preference 
    WHERE user_notification_preference.user_id = user.id 
    AND user_notification_preference.notification_type = 'MARKETING'
);
