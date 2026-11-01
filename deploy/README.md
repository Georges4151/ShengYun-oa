# CEAMS 生产部署说明

本目录提供生产环境容器化部署所需的基础设施文件：

- `Dockerfile`：多阶段构建镜像（maven 构建 + JRE 运行时），产物 `ceams-2.0.0-SNAPSHOT.jar`，端口 `8080`，健康检查 `/actuator/health`。
- `docker-compose.yml`：编排 `mysql` / `redis` / `app` 三个服务。
- `../scripts/backup-mysql.sh`：MySQL 加密备份脚本（mysqldump + GPG 对称加密）。

> 注意：根目录的旧 `Dockerfile` / `docker-compose.yml` 是历史遗留（指向 1.0-SNAPSHOT、端口 9095），已废弃，请使用本目录下的新文件。

## 1. 快速启动

```bash
# 1) 准备环境变量（写入本目录 .env，勿提交 git）
cd deploy
cat > .env <<'EOF'
MYSQL_ROOT_PASSWORD=<数据库root密码>
REDIS_PASSWORD=<redis密码>

JWT_SECRET=<随机长字符串，建议 >= 64 字符>
CEAMS_SENSITIVE_KEY=<Base64 编码的 32 字节（见下方生成命令）>
EXPORT_SECRET=<随机长字符串>
EOF

# 2) 构建并启动（app 会等待 mysql/redis 健康后启动，Flyway 自动建库迁移）
docker compose up -d --build

# 3) 查看状态
docker compose ps
curl http://localhost:8080/actuator/health   # 期望 {"status":"UP"}
```

## 2. 环境变量清单

| 变量 | 服务 | 必填 | 说明 |
|------|------|------|------|
| `MYSQL_ROOT_PASSWORD` | mysql | 是 | MySQL root 密码 |
| `MYSQL_DATABASE` | mysql/app | 否 | 数据库名，默认 `ceams` |
| `MYSQL_HOST` | app | - | 由 compose 固定为 `mysql` |
| `MYSQL_PORT` | app | 否 | 默认 `3306` |
| `MYSQL_USER` | app | 否 | 默认 `root` |
| `MYSQL_PASSWORD` | app | - | 复用 `MYSQL_ROOT_PASSWORD` |
| `REDIS_PASSWORD` | redis/app | 是 | Redis 访问密码（`--requirepass`） |
| `REDIS_HOST` | app | - | 由 compose 固定为 `redis` |
| `REDIS_PORT` | app | 否 | 默认 `6379` |
| `JWT_SECRET` | app | 是 | JWT 签名密钥，建议 >= 64 字符 |
| `CEAMS_SENSITIVE_KEY` | app | 是 | 敏感字段 AES-256-GCM 密钥，Base64 编码的 32 字节 |
| `EXPORT_SECRET` | app | 是 | 导出/下载签名 URL 专用密钥 |

> 说明：`MYSQL_HOST` / `REDIS_HOST` 在 compose 内已固定为服务名，无需在 `.env` 覆盖。

## 3. 密钥生成命令

```bash
# CEAMS_SENSITIVE_KEY：Base64 编码的 32 字节随机密钥（去掉末尾换行）
openssl rand -base64 32 | tr -d '\n'

# JWT_SECRET：64+ 字符随机串（hex 输出天然无特殊字符）
openssl rand -hex 48

# EXPORT_SECRET：随机长字符串
openssl rand -base64 48 | tr -d '\n'

# Redis 密码（可选，随机）
openssl rand -hex 16
```

## 4. 备份

```bash
# 需在能访问 MySQL 且已安装 mysqldump/gpg 的机器上执行
export DB_HOST=127.0.0.1 DB_PORT=3306 DB_NAME=ceams DB_USER=root
export DB_PASSWORD='<数据库密码>' ENCRYPT_PASSPHRASE='<GPG口令>'
export BACKUP_DIR=/data/backups KEEP_DAYS=7
bash ../scripts/backup-mysql.sh
```

## 5. 备注

- `FILE_UPLOAD_DIR`（默认 `./data/files`）在当前 `src/main/resources/application.yml` 中未见显式声明，compose 未注入；如需持久化上传文件，可在 `app` 服务追加卷挂载。
- 数据均通过命名卷 `mysql-data` / `redis-data` 持久化，`docker compose down` 不会删除数据；如需彻底重置请使用 `down -v`。