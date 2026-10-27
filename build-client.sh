#!/bin/bash

# 客户端打包脚本
# 用于构建独立的JavaFX客户端JAR包

echo "=========================================="
echo "开始打包盛韵建筑账目管理系统 - 远程客户端"
echo "=========================================="

# 清理旧的构建
echo "清理旧的构建..."
mvn clean

# 编译客户端代码（跳过测试）
echo "编译客户端代码..."
mvn compile -DskipTests

# 打包客户端JAR（不包含Spring Boot插件）
echo "打包客户端JAR..."
mvn package -DskipTests -Dmaven.test.skip=true

# 获取版本号
VERSION=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout)
JAR_NAME="sxassist-client-${VERSION}.jar"

echo "=========================================="
echo "打包完成！"
echo "=========================================="
echo "客户端JAR文件: target/${JAR_NAME}"
echo ""
echo "运行客户端："
echo "  java -jar target/${JAR_NAME}"
echo ""
echo "配置文件位置："
echo "  src/main/resources/client-config.properties"
echo ""
echo "=========================================="
