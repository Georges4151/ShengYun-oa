-- 创建电子章表
CREATE TABLE IF NOT EXISTS electronic_seals (
  id int NOT NULL AUTO_INCREMENT,
  seal_name varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  seal_type varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  description text COLLATE utf8mb4_unicode_ci,
  status varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'inactive',
  create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 创建电子章申请表
CREATE TABLE IF NOT EXISTS electronic_seal_applications (
  id int NOT NULL AUTO_INCREMENT,
  seal_id int NOT NULL,
  seal_name varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  applicant_id int NOT NULL,
  applicant_name varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  purpose text COLLATE utf8mb4_unicode_ci NOT NULL,
  status varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'pending',
  approver_id int DEFAULT NULL,
  approver_name varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  approval_comment text COLLATE utf8mb4_unicode_ci,
  apply_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  approval_time datetime DEFAULT NULL,
  create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY seal_id (seal_id),
  KEY applicant_id (applicant_id),
  KEY status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 创建电子章使用记录表
CREATE TABLE IF NOT EXISTS electronic_seal_usages (
  id int NOT NULL AUTO_INCREMENT,
  seal_id int NOT NULL,
  seal_name varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  user_id int NOT NULL,
  user_name varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  purpose text COLLATE utf8mb4_unicode_ci NOT NULL,
  document_type varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  document_path varchar(500) COLLATE utf8mb4_unicode_ci NOT NULL,
  usage_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  ip_address varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY seal_id (seal_id),
  KEY user_id (user_id),
  KEY usage_time (usage_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;