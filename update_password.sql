-- 更新admin用户密码为admin123
-- BCrypt hash for 'admin123': $2a$10$N9qo8uLOickgx2ZMRZoMyeIjZRGdjGj/n3.rsQ5pPjZ5yVlWK5WAe
USE ceams;
UPDATE users SET password_hash = '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZRGdjGj/n3.rsQ5pPjZ5yVlWK5WAe' WHERE username='admin';
SELECT username, password_hash FROM users WHERE username='admin';
