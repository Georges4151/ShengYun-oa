# CEAMS 应用启动说明

## 重要说明

**CEAMS 是一个 JavaFX 桌面应用，不是 Web 应用！**

- ✅ Spring Boot 后端 (提供 REST API)
- ✅ JavaFX 桌面客户端 (提供用户界面)

## 启动方式

### 方式1：直接运行（推荐）

```bash
# 在 Windows 上直接运行
start-local.bat
```

或者手动运行：

```bash
cd E:\sxassist
java -jar target\ceams-1.0-SNAPSHOT.jar
```

### 方式2：Docker 仅运行后端 API

如果只需要 REST API（不需要图形界面）：

```bash
docker-compose up -d
```

然后访问 API：
- API 基础地址: http://localhost:9095
- API 文档: http://localhost:9095/swagger-ui.html

## 为什么不需要 Docker？

1. **JavaFX 需要图形界面** - Docker 容器没有图形界面
2. **本地 MySQL** - 您已经有 MySQL 安装
3. **简单直接** - 直接运行更简单

## 系统要求

- Java 17 或更高版本
- MySQL 8.0 或更高版本
- Windows 10/11

## 默认账户

- 用户名: admin
- 密码: admin123
