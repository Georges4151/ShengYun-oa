-- ============================================
-- CEAMS 多客户端数据库安全配置
-- 创建专用数据库用户,限制权限
-- ============================================

USE ceams;

-- ============================================
-- 1. 创建客户端专用用户(只读权限)
-- ============================================
-- 注意: 请将 'client_password' 替换为强密码

CREATE USER IF NOT EXISTS 'ceams_client_read'@'%'
  IDENTIFIED BY 'client_read_password_strong';

GRANT SELECT ON ceams.* TO 'ceams_client_read'@'%';

-- ============================================
-- 2. 创建客户端专用用户(读写权限)
-- ============================================
-- 注意: 请将 'client_password' 替换为强密码

CREATE USER IF NOT EXISTS 'ceams_client_write'@'%'
  IDENTIFIED BY 'client_write_password_strong';

GRANT SELECT, INSERT, UPDATE, DELETE ON ceams.* TO 'ceams_client_write'@'%';

-- ============================================
-- 3. 创建管理员用户(完整权限)
-- ============================================
-- 仅限管理员电脑使用

CREATE USER IF NOT EXISTS 'ceams_admin'@'%'
  IDENTIFIED BY 'admin_password_strong';

GRANT ALL PRIVILEGES ON ceams.* TO 'ceams_admin'@'%';
GRANT GRANT OPTION ON ceams.* TO 'ceams_admin'@'%';

-- ============================================
-- 4. 刷新权限
-- ============================================
FLUSH PRIVILEGES;

-- ============================================
-- 5. 查看当前用户权限
-- ============================================
SHOW GRANTS FOR 'ceams_client_read'@'%';
SHOW GRANTS FOR 'ceams_client_write'@'%';
SHOW GRANTS FOR 'ceams_admin'@'%';

-- ============================================
-- 安全建议:
-- ============================================
-- 1. 定期更换密码(每3个月一次)
-- 2. 限制客户端IP范围(使用防火墙)
-- 3. 启用MySQL SSL/TLS加密
-- 4. 启用审计日志
-- 5. 定期备份数据库
