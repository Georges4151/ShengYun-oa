# syntax=docker/dockerfile:1
# ================= CEAMS 生产镜像（多阶段构建） =================
# 构建产物：ceams-2.0.0-SNAPSHOT.jar (Spring Boot fat jar, JDK 17)
# 运行时端口：8080；健康检查：/actuator/health
# 数据库迁移由 Flyway 在启动时自动执行，镜像内不 COPY 任何 db/schema.sql

# ===== 阶段一：构建 =====
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build

# 复制 pom 与源码并打包（跳过测试）；依赖通过 BuildKit 缓存挂载 /root/.m2 复用
COPY pom.xml ./
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn -B clean package -DskipTests -Dmaven.wagon.http.retryHandler.count=6 -Dmaven.wagon.httpconnectionManager.ttlSeconds=120

# ===== 阶段二：运行时 =====
FROM eclipse-temurin:17-jre AS runtime
WORKDIR /app

# 复制 fat jar 并重命名为 app.jar
COPY --from=build /build/target/ceams-2.0.0-SNAPSHOT.jar app.jar

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0"

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD wget -qO- http://localhost:8080/actuator/health >/dev/null 2>&1 || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]