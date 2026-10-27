-- ============================================
-- CEAMS 防火墙和网络安全配置
-- ============================================

-- ============================================
-- Windows 防火墙规则 (以管理员身份运行PowerShell)
-- ============================================

# 允许MySQL端口入站规则
New-NetFirewallRule -DisplayName "CEAMS MySQL" `
  -Direction Inbound `
  -Protocol TCP `
  -LocalPort 3306 `
  -Action Allow `
  -Profile Any `
  -Enabled True `
  -Description "允许CEAMS应用访问MySQL数据库"

# 允许Redis端口入站规则(如果使用)
New-NetFirewallRule -DisplayName "CEAMS Redis" `
  -Direction Inbound `
  -Protocol TCP `
  -LocalPort 6379 `
  -Action Allow `
  -Profile Any `
  -Enabled True `
  -Description "允许CEAMS应用访问Redis缓存"

# 允许应用端口入站规则(如果需要API访问)
New-NetFirewallRule -DisplayName "CEAMS API" `
  -Direction Inbound `
  -Protocol TCP `
  -LocalPort 9095 `
  -Action Allow `
  -Profile Any `
  -Enabled True `
  -Description "允许CEAMS API访问"

# ============================================
-- MySQL 服务器网络安全配置 (my.ini)
-- ============================================
-- 在MySQL服务器上修改 my.ini 或 my.cnf

-- 绑定地址(只允许内网IP访问)
-- bind-address = 192.168.1.100

-- 启用SSL加密
-- require_secure_transport = ON
-- ssl-ca = /path/to/ca.pem
-- ssl-cert = /path/to/server-cert.pem
-- ssl-key = /path/to/server-key.pem

-- 最大连接数
-- max_connections = 100

-- 连接超时
-- wait_timeout = 28800
-- interactive_timeout = 28800

-- ============================================
-- IP白名单配置
-- ============================================
-- 在MySQL服务器创建用户时限制IP

-- 只允许特定IP段访问(示例:192.168.1.0/24)
CREATE USER 'ceams_client'@'192.168.1.%' IDENTIFIED BY 'strong_password';

-- 只允许单个IP访问
CREATE USER 'ceams_admin'@'192.168.1.101' IDENTIFIED BY 'strong_password';

-- ============================================
-- 建议的安全措施
-- ============================================
-- 1. 使用VPN创建安全通道
-- 2. 配置路由器/防火墙限制IP段
-- 3. 启用MySQL审计插件
-- 4. 定期检查日志,监控异常访问
-- 5. 禁用root远程登录
