CREATE TABLE IF NOT EXISTS notification (
    id INT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(200) NOT NULL,
    content_type VARCHAR(20) NOT NULL,
    content TEXT NOT NULL,
    link_url VARCHAR(500),
    priority VARCHAR(10) NOT NULL DEFAULT 'MEDIUM',
    notification_type VARCHAR(50) NOT NULL,
    sender_id INT,
    sender_name VARCHAR(100),
    valid_start_time DATETIME,
    valid_end_time DATETIME,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    publish_time DATETIME,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_priority (priority),
    INDEX idx_type (notification_type),
    INDEX idx_status (status),
    INDEX idx_publish_time (publish_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS user_notification (
    id INT PRIMARY KEY AUTO_INCREMENT,
    notification_id INT NOT NULL,
    user_id INT NOT NULL,
    is_read TINYINT(1) DEFAULT 0,
    read_time DATETIME,
    is_deleted TINYINT(1) DEFAULT 0,
    channel VARCHAR(20),
    push_status VARCHAR(20) DEFAULT 'PENDING',
    push_time DATETIME,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_notification (user_id, notification_id),
    INDEX idx_user_read (user_id, is_read),
    INDEX idx_notification (notification_id),
    INDEX idx_push_status (push_status),
    FOREIGN KEY (notification_id) REFERENCES notification(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS user_notification_preference (
    id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL,
    notification_type VARCHAR(50) NOT NULL,
    receive_system TINYINT(1) DEFAULT 1,
    receive_email TINYINT(1) DEFAULT 0,
    receive_sms TINYINT(1) DEFAULT 0,
    quiet_mode TINYINT(1) DEFAULT 0,
    quiet_start_time TIME,
    quiet_end_time TIME,
    priority_threshold VARCHAR(10) DEFAULT 'LOW',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_type (user_id, notification_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;