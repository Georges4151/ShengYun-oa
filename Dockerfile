# 多阶段构建 Dockerfile
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# 复制 pom.xml 和源文件
COPY pom.xml .
COPY src ./src

# 构建项目（跳过测试以加快构建）
RUN mvn clean package -DskipTests

# 运行时阶段
FROM eclipse-temurin:17-jre
WORKDIR /app

# 从构建阶段复制 jar 文件
COPY --from=build /app/target/ceams-1.0-SNAPSHOT.jar ./app.jar

# 复制数据库初始化脚本
COPY src/main/resources/db/schema.sql /docker-entrypoint-initdb.d/

# 暴露端口
EXPOSE 9095

# 健康检查
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
    CMD curl -f http://localhost:9095/actuator/health || exit 1

# 启动命令
ENTRYPOINT ["java", "-jar", "app.jar"]
