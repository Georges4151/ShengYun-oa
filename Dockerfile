# 多阶段构建：Maven 编译 -> JRE 运行
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn -B -q clean package -DskipTests && cp target/*-SNAPSHOT.jar /app/app.jar

FROM eclipse-temurin:17-jre
WORKDIR /app
ENV TZ=Asia/Shanghai
ENV SERVER_PORT=9095
COPY --from=build /app/app.jar ./app.jar
EXPOSE 9095
ENTRYPOINT ["java","-jar","app.jar"]
