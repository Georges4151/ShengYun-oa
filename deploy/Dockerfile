# ================= CEAMS 生产镜像（多阶段构建） =================
# 构建产物：ceams-2.0.0-SNAPSHOT.jar (Spring Boot fat jar, JDK 17)
# 运行时端口：8080；健康检查：/actuator/health
# 数据库迁移由 Flyway 在启动时自动执行，镜像内不 COPY 任何 db/schema.sql

# ===== 阶段一：构建 =====
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build

# 先复制 pom.xml 并预下载依赖，利用 Docker 层缓存（pom.xml 不变则不重复下载）
COPY pom.xml ./
RUN mvn -B dependency:go-offline

# 复制源码并打包（跳过测试）
COPY src ./src
RUN mvn -B clean package -DskipTests

# ===== 阶段二：运行时 =====
FROM eclipse-temurin:17-jre AS runtime
WORKDIR /app

# wget 用于容器健康检查
RUN apt-get update && apt-get install -y --no-install-recommends wget \
    && rm -rf /var/lib/apt/lists/*

# 复制 fat jar 并重命名为 app.jar
COPY --from=build /build/target/ceams-2.0.0-SNAPSHOT.jar app.jar

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0"

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD wget -qO- http://localhost:8080/actuator/health >/dev/null 2>&1 || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]