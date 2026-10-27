# ============================================
# Docker 部署指南
# ============================================

## 前置条件

1. Docker 和 Docker Compose 已安装
2. 至少 2GB 内存和 10GB 磁盘空间
3. 端口 9095, 3306, 6379 未被占用

---

## 快速开始

### 一键启动所有服务

```bash
# 方式1：使用脚本（Windows）
.\start-docker.bat

# 方式2：手动执行命令
docker-compose up -d
```

---

## 详细步骤

### 步骤1：构建并启动

```bash
# 构建并启动所有服务
docker-compose up -d --build

# 查看服务状态
docker-compose ps
```

### 步骤2：查看日志

```bash
# 查看所有服务日志
docker-compose logs -f

# 只查看应用服务日志
docker-compose logs -f app
```

### 步骤3：访问应用

在浏览器中打开：
```
http://localhost:9095
```

### 步骤4：停止服务

```bash
# 停止但保留数据
docker-compose down

# 停止并删除数据（慎用！）
docker-compose down -v
```

---

## 默认登录信息

| 角色 | 用户名 | 密码 |
|------|--------|------|
| 管理员 | admin | admin123 |
| 项目经理 | manager | manager123 |

---

## 服务说明

### 包含的服务

| 服务 | 容器名 | 端口 | 说明 |
|------|--------|------|------|
| 应用服务 | ceams-app | 9095 | Spring Boot 应用 |
| MySQL 数据库 | ceams-mysql | 3306 | 数据库服务 |
| Redis 缓存 | ceams-redis | 6379 | 缓存服务 |

### 数据持久化

所有数据都已配置持久化卷：
- MySQL 数据：ceams-mysql-data
- Redis 数据：ceams-redis-data
- 应用日志：ceams-logs
- 备份：ceams-backup
- 票据：ceams-receipts

---

## 常用命令

### 服务管理
```bash
# 启动服务
docker-compose up -d

# 停止服务
docker-compose stop

# 重启服务
docker-compose restart

# 查看服务状态
docker-compose ps
```

### 日志查看
```bash
# 查看实时日志
docker-compose logs -f

# 查看指定服务日志
docker-compose logs -f app
```

### 数据库管理
```bash
# 进入 MySQL 容器
docker-compose exec mysql mysql -uceams -pceams123456 ceams

# 备份数据库
docker-compose exec mysql mysqldump -uroot -proot123456 ceams > backup.sql

# 恢复数据库
docker-compose exec -T mysql mysql -uroot -proot123456 ceams < backup.sql
```

---

## 故障排查

### 查看服务状态
```bash
docker-compose ps
```

### 检查应用健康状态
```bash
curl http://localhost:9095/actuator/health
```

### 清理镜像（可选）
```bash
# 清理未使用的镜像
docker image prune -a
```

---

## 安全建议

1. **修改默认密码**
   - 在 docker-compose.yml 中修改数据库密码
   - 首次登录后修改管理员密码

2. **数据备份**
   - 定期备份数据库
   - 备份持久化卷
