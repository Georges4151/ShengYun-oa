@echo off
chcp 65001 > nul
echo 初始化MySQL数据库...

:: 设置工作目录为脚本所在目录
cd /d "%~dp0"

:: 创建数据目录和日志目录
if not exist ./mysql/data mkdir ./mysql/data
if not exist ./logs mkdir ./logs

:: 初始化MySQL
echo 初始化MySQL数据库...
./mysql/bin/mysqld --initialize-insecure --user=root --basedir=./mysql --datadir=./mysql/data > ./logs/mysql_init.log 2>&1
if %errorlevel% neq 0 (
    echo 错误：MySQL初始化失败，请查看 ./logs/mysql_init.log
    pause
    exit /b 1
)

:: 启动MySQL服务
echo 启动MySQL服务...
start /B "MySQL" ./mysql/bin/mysqld --defaults-file=./mysql/my.ini --console > ./logs/mysql.log 2>&1

:: 等待MySQL服务启动
echo 等待MySQL服务启动...
timeout /t 10 /nobreak > nul

:: 创建数据库和用户
echo 创建数据库和用户...
./mysql/bin/mysql -u root -e "CREATE DATABASE IF NOT EXISTS ceams CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;" > ./logs/mysql_setup.log 2>&1
./mysql/bin/mysql -u root -e "CREATE USER IF NOT EXISTS 'ceams'@'localhost' IDENTIFIED BY 'ceams123';" >> ./logs/mysql_setup.log 2>&1
./mysql/bin/mysql -u root -e "GRANT ALL PRIVILEGES ON ceams.* TO 'ceams'@'localhost';" >> ./logs/mysql_setup.log 2>&1
./mysql/bin/mysql -u root -e "FLUSH PRIVILEGES;" >> ./logs/mysql_setup.log 2>&1

:: 尝试添加字段（忽略错误，因为表可能还不存在）
echo 配置数据库表结构...
./mysql/bin/mysql -u ceams -pceams123 ceams -e "ALTER TABLE subcontractor_contracts ADD COLUMN IF NOT EXISTS paid_amount DECIMAL(18,2) DEFAULT 0.00 COMMENT '已支付金额';" >> ./logs/mysql_setup.log 2>&1

echo MySQL初始化完成！
pause