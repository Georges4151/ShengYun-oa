#!/usr/bin/env bash
#
# CEAMS 建筑企业账目管理系统 - MySQL 加密备份脚本
#
# 功能：
#   1. 使用 mysqldump 导出指定数据库（--single-transaction --quick，不锁表）
#   2. 通过 GPG 对称加密导出结果（AES256）
#   3. 按 KEEP_DAYS 自动清理过期备份，保留最近 N 天
#   4. 任一步骤失败则以非零状态退出
#
# 依赖命令：mysqldump、gpg、find
#
# 环境变量（均可通过 export 或调用前注入）：
#   DB_HOST              数据库地址        （默认 localhost）
#   DB_PORT              数据库端口        （默认 3306）
#   DB_NAME              数据库名          （默认 ceams）
#   DB_USER              数据库用户        （默认 root）
#   DB_PASSWORD          数据库密码        （必填）
#   ENCRYPT_PASSPHRASE   GPG 对称加密口令  （必填，请妥善保管）
#   BACKUP_DIR           备份输出目录      （默认 ./backups）
#   KEEP_DAYS            保留最近 N 天     （默认 7）
#
# 用法示例：
#   export DB_PASSWORD='your-db-password' \
#          ENCRYPT_PASSPHRASE='your-gpg-passphrase'
#   bash backup-mysql.sh
#
# 恢复示例（需 ENCRYPT_PASSPHRASE）：
#   gpg --batch --yes --passphrase "$ENCRYPT_PASSPHRASE" -d ceams-XXXX.sql.gpg | mysql -h "$DB_HOST" -u "$DB_USER" -p "$DB_NAME"

set -euo pipefail

# ===== 读取环境变量（带默认值） =====
DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-3306}"
DB_NAME="${DB_NAME:-ceams}"
DB_USER="${DB_USER:-root}"
DB_PASSWORD="${DB_PASSWORD:-}"
ENCRYPT_PASSPHRASE="${ENCRYPT_PASSPHRASE:-}"
BACKUP_DIR="${BACKUP_DIR:-./backups}"
KEEP_DAYS="${KEEP_DAYS:-7}"

# ===== 必要参数校验 =====
if [[ -z "${DB_PASSWORD}" ]]; then
    echo "[错误] 环境变量 DB_PASSWORD 未设置" >&2
    exit 1
fi
if [[ -z "${ENCRYPT_PASSPHRASE}" ]]; then
    echo "[错误] 环境变量 ENCRYPT_PASSPHRASE 未设置" >&2
    exit 1
fi

# ===== 依赖命令检查 =====
for cmd in mysqldump gpg find date; do
    if ! command -v "${cmd}" >/dev/null 2>&1; then
        echo "[错误] 未找到命令: ${cmd}" >&2
        exit 1
    fi
done

# ===== 创建备份目录 =====
mkdir -p "${BACKUP_DIR}"

TIMESTAMP="$(date +%F_%H%M%S)"
OUTPUT_FILE="${BACKUP_DIR}/ceams-${TIMESTAMP}.sql.gpg"
TMP_FILE="${OUTPUT_FILE}.tmp"

echo "[信息] 开始备份数据库 ${DB_NAME}@${DB_HOST}:${DB_PORT} -> ${OUTPUT_FILE}"

# 使用 MYSQL_PWD 避免在命令行暴露密码（mysqldump 从环境变量读取）
export MYSQL_PWD="${DB_PASSWORD}"

# ===== 备份并加密 =====
# 通过临时文件输出，失败时不残留半成品
if mysqldump \
      --single-transaction \
      --quick \
      --host="${DB_HOST}" \
      --port="${DB_PORT}" \
      --user="${DB_USER}" \
      "${DB_NAME}" \
    | gpg --batch --yes --passphrase "${ENCRYPT_PASSPHRASE}" \
      --symmetric --cipher-algo AES256 \
      --output "${TMP_FILE}"; then
    mv "${TMP_FILE}" "${OUTPUT_FILE}"
else
    rm -f "${TMP_FILE}"
    echo "[错误] 备份或加密失败" >&2
    exit 1
fi

# 清理敏感环境变量
unset MYSQL_PWD

echo "[成功] 备份完成: ${OUTPUT_FILE}"

# ===== 清理过期备份（保留最近 KEEP_DAYS 天） =====
find "${BACKUP_DIR}" -name '*.sql.gpg' -mtime +"${KEEP_DAYS}" -delete

echo "[信息] 清理完成（保留最近 ${KEEP_DAYS} 天）"