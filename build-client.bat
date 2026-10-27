@echo off
REM 客户端打包脚本（Windows版本）
REM 用于构建独立的JavaFX客户端JAR包

echo ==========================================
echo 开始打包盛韵建筑账目管理系统 - 远程客户端
echo ==========================================

REM 清理旧的构建
echo 清理旧的构建...
call mvn clean

REM 编译客户端代码（跳过测试）
echo 编译客户端代码...
call mvn compile -DskipTests

REM 打包客户端JAR（不包含Spring Boot插件）
echo 打包客户端JAR...
call mvn package -DskipTests -Dmaven.test.skip=true

echo ==========================================
echo 打包完成！
echo ==========================================
echo 客户端JAR文件位置: target\
echo.
echo 运行客户端：
echo   java -jar target\sxassist-client-1.0-SNAPSHOT.jar
echo.
echo 配置文件位置：
echo   src\main\resources\client-config.properties
echo.
echo ==========================================
pause
