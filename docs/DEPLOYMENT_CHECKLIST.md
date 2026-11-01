# CEAMS 上线部署清单

> 生成时间：2026-11-01 ｜ 适用版本：`ceams 2.0.0-SNAPSHOT`（分支 `dev`）
> 依据：`docs/ARCHITECTURE.md`、`src/main/resources/application.yml`、`docs/STAGE4_PLAN.md`
> 说明：本文档只覆盖「代码完成 → 生产可用」之间的缺口，不重复架构设计内容。

---

## 0. 现状盘点

### 已具备

| 项 | 现状 |
|---|---|
| 后端可执行包 | `target/ceams-2.0.0-SNAPSHOT.jar`（Spring Boot fat jar，端口 8080） |
| JDK 目标版本 | `pom.xml` 中 `java.version = 17`（生产用 JDK 17 LTS 即可，无需 19） |
| 数据库迁移 | Flyway 自动执行 `V1`、`V2`、`V4`~`V10`（`baseline-on-migrate=false`、`validate-on-migrate=true`） |
| 配置外置化 | 全部关键配置已支持环境变量覆盖（见 §3） |
| 健康检查 | `/actuator/health`（已实测 `{"status":"UP"}`） |
| Web 前端 | `apps/web` Vite 构建，`VITE_API_BASE_URL` 可注入 |
| 小程序 | `apps/miniprogram` uni-app，产物 `dist/build/mp-weixin` |
| Electron | `apps/desktop` 可 `pnpm build && electron .` 启动 |
| CI 静态检查 | Checkstyle / Spotless / PMD / JaCoCo（非阻断）+ ArchUnit（阻断） |

### 上线前必须补齐（阻断项）

| # | 缺口 | 影响 | 优先级 |
|---|---|---|---|
| **A1** | **后端无 `/api/v1/files/upload` 端点** — 小程序票据拍照调用该接口，但后端仅有银行流水 CSV 导入的 `MultipartFile` | 小程序「报销票据拍照上传」必定 404，功能不可用 | **P0** |
| **A2** | **Electron 无 `electron-builder` 配置** — `apps/desktop/package.json` 只有 `start`，无打包脚本 | 无法产出 Windows 安装包，桌面端无法交付 | **P0** |
| **A3** | **根目录 `Dockerfile` / `docker-compose.yml` 是旧系统遗留** — 指向 `ceams-1.0-SNAPSHOT.jar`、端口 `9095`、`db/schema.sql` | 直接使用会构建出错误镜像，必须新写 | **P0** |
| **A4** | 备份加密仅有配置占位（`application.yml` 第 76~78 行注释） | 数据库备份为明文，不满足安全基线 | **P1** |
| **A5** | 生产环境密钥全部使用仓库内 dev 默认值 | 敏感字段加密、JWT 可被伪造 | **P1** |
| **A6** | 上传文件存储位置未定义（无对象存储、无本地目录配置） | A1 补齐时需同时确定落盘位置与访问方式 | **P1** |

> 另外，根目录存在大量旧系统文件（`androidclient/`、`deploy/`、`*_VPN_*.md`、`build-client.bat`、`installer.iss` 等），**与 2.0 架构无关，不可复用**。

---

## 1. 基础设施要求

| 组件 | 规格建议 | 说明 |
|---|---|---|
| 应用服务器 | Linux x86_64，JDK 17，2C4G 起 | 单体应用，单实例即可；多实例需注意上传文件共享 |
| MySQL | 8.0，独立实例 | 库名 `ceams`，字符集 `utf8mb4`，时区 `Asia/Shanghai` |
| Redis | 6/7，独立实例 | 用途：HMAC nonce 防重放（**不可用本地内存替代**）、缓存 |
| Nginx | 反向代理 + TLS 终止 | 对外只暴露 443 |
| 域名 | 已备案（中国大陆）HTTPS 域名 | `api.xxx`（后端）、`app.xxx`（Web 静态） |
| 对象存储（可选） | 阿里云 OSS / MinIO | 若多实例部署，上传文件不能落本地盘 |

### 端口规划

| 端口 | 用途 | 对外 |
|---|---|---|
| 8080 | 后端 HTTP | 否（仅 Nginx 内网访问） |
| 3306 | MySQL | 否 |
| 6379 | Redis | 否 |
| 443/80 | Nginx | 是 |

---

## 2. 部署架构

```text
浏览器 ─┐
Electron ─┼──► Nginx(443)
小程序 ──┘      ├── /api/**       → 反代 → Spring Boot 8080
                ├── /actuator/**  → 反代 → Spring Boot 8080
                └── /*            → Web 静态资源（apps/web/dist）

Spring Boot ──► MySQL 8（Flyway 迁移）
     └────────► Redis（nonce / 缓存）
```

三端共用同一套 `/api/v1` 契约，**小程序必须走 HTTPS**（微信强制）。

---

## 3. 生产环境变量清单（必须逐项确认）

> 来源：`src/main/resources/application.yml`。**所有 `${VAR:默认值}` 在生产必须显式设置**，默认值仅用于开发。

### 3.1 数据库 / Redis

| 变量 | 必填 | 示例 | 备注 |
|---|---|---|---|
| `MYSQL_HOST` | 是 | `10.0.0.5` | |
| `MYSQL_PORT` | 是 | `3306` | |
| `MYSQL_DATABASE` | 是 | `ceams` | |
| `MYSQL_USER` | 是 | `ceams` | 建议专用账号，非 root |
| `MYSQL_PASSWORD` | 是 | 强随机 | |
| `REDIS_HOST` | 是 | `10.0.0.6` | |
| `REDIS_PORT` | 是 | `6379` | |
| `REDIS_PASSWORD` | 是 | 强随机 | 生产必须设密码 |

### 3.2 安全密钥（**必改，禁止沿用仓库默认值**）

| 变量 | 要求 | 生成方式 |
|---|---|---|
| `JWT_SECRET` | ≥ 32 字节随机串 | `openssl rand -base64 48` |
| `CEAMS_SENSITIVE_KEY` | **Base64 编码的 32 字节**（AES-256-GCM） | `openssl rand -base64 32` |
| `EXPORT_SECRET` | 随机串 ≥ 32 字节 | `openssl rand -base64 48` |
| `SIGN_WINDOW_MILLIS` | 默认 `300000`（±5 分钟） | 通常不改 |
| `EXPORT_URL_TTL_MILLIS` | 默认 `300000`（5 分钟） | 按业务调整 |

> ⚠️ `CEAMS_SENSITIVE_KEY` 一旦上线**不可随意更换**：更换后库内已加密的税号 / 银行账号 / 手机号将无法解密。必须纳入密钥托管与备份流程。

### 3.3 前端

| 应用 | 变量 | 示例 |
|---|---|---|
| Web | `VITE_API_BASE_URL` | `https://api.xxx` |
| Web | `VITE_ROUTER_MODE` | `history`（静态托管需配 Nginx fallback） |
| 小程序 | `VITE_API_BASE_URL` | `https://api.xxx`（**必须 HTTPS 且已加入微信白名单**） |

---

## 4. 后端部署步骤

### 4.1 需要新写的 Dockerfile（A3）

根目录旧 `Dockerfile` 不可用。新文件要点：

```dockerfile
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B dependency:go-offline
COPY src ./src
RUN mvn -B clean package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/ceams-2.0.0-SNAPSHOT.jar ./app.jar
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD wget -qO- http://localhost:8080/actuator/health || exit 1
ENTRYPOINT ["java","-XX:MaxRAMPercentage=75","-jar","app.jar"]
```

> 不要 `COPY db/schema.sql`——建库完全由 Flyway 管理。

### 4.2 启动

```bash
export MYSQL_HOST=... MYSQL_PORT=3306 MYSQL_DATABASE=ceams MYSQL_USER=ceams MYSQL_PASSWORD=...
export REDIS_HOST=... REDIS_PORT=6379 REDIS_PASSWORD=...
export JWT_SECRET=... CEAMS_SENSITIVE_KEY=... EXPORT_SECRET=...
java -jar app.jar
```

### 4.3 首次启动校验

1. 日志出现 `Successfully applied N migrations`（N 应为 V1/V2/V4~V10 条数）
2. `curl -s http://127.0.0.1:8080/actuator/health` → `{"status":"UP"}`
3. 数据库中 `flyway_schema_history` 全部 `success = 1`
4. 表数量：`sys_*` 9 张 + `biz_*` 30 张

### 4.4 数据库准备

- 建库：`CREATE DATABASE ceams DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_general_ci;`
- **不要手工执行任何 SQL**，迁移交由 Flyway
- 若目标库已有历史数据，`baseline-on-migrate=false` 会导致迁移中断 → 必须确认是空库
- 迁移前必须全量备份

---

## 5. Web 部署

```bash
pnpm install
pnpm --filter @ceams/web build        # 产物 apps/web/dist
```

Nginx：

```nginx
server {
    listen 443 ssl;
    server_name app.xxx;

    location / {
        root /var/www/ceams-web;
        try_files $uri $uri/ /index.html;   # history 路由必需
    }

    location /api/ {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto https;
    }

    client_max_body_size 20m;   # 与 multipart.max-file-size 一致
}
```

---

## 6. Electron 交付（A2）

当前 `apps/desktop` 只有 `start`，需新增打包能力：

1. 引入 `electron-builder`（devDependency）
2. 新增配置：
   - `directories.output` → `release/`
   - `files` → `dist-electron/**`、`node_modules/**`、内置 Web 产物
   - `win.target` → `nsis`
   - `nsis.oneClick: false`、`allowToChangeInstallationDirectory: true`
3. 打包前必须先构建 Web（`pnpm --filter @ceams/web build:electron`，`base: './'`，产物进 `dist-electron`）
4. Windows 代码签名证书（可选但强烈建议；否则 SmartScreen 拦截）
5. 发布物：安装包 + 版本号 + 更新通道（如需自动更新，另接 `electron-updater`）

验证要点：安装后首次启动能加载页面、`contextIsolation` 生效、USB 印章设备枚举正常。

---

## 7. 微信小程序发布

### 7.1 构建

```bash
pnpm --filter @ceams/miniprogram build:mp-weixin   # 产物 dist/build/mp-weixin
```

> 生产构建用 `build:mp-weixin`（production 模式）；`build:mp-weixin:dev` 仅供本地联调（指向 localhost）。

### 7.2 微信公众平台配置（**缺一不可**）

| 配置项 | 值 |
|---|---|
| AppID | `wx3ba625ae02189da2`（已写入 `manifest.json`） |
| request 合法域名 | `https://api.xxx` |
| uploadFile 合法域名 | `https://api.xxx` |
| downloadFile 合法域名 | `https://api.xxx`（PDF 预览 `downloadFile` + `openDocument`） |
| 服务器域名端口 | 必须 443，证书有效 |

### 7.3 上传与审核

1. 开发者工具「上传」→ 填版本号与备注
2. 微信后台「版本管理」→ 设为体验版 → 内部测试
3. 提交审核（需类目资质、隐私协议）
4. 审核通过后发布

### 7.4 小程序必须先在开发者工具里完成人工 UI 验收

（登录 → 项目列表 → 项目详情/进度款 → 报销待办 → 票据拍照 → 通知 → 账目），此项尚未完成。

---

## 8. 安全基线落地

| 项 | 动作 |
|---|---|
| 密钥 | §3.2 三项全部替换为生产值，纳入密钥管理与轮换计划 |
| 数据库账号 | 专用账号，仅授予 `ceams` 库必要权限 |
| 网络 | MySQL / Redis / 8080 不对外暴露，仅安全组内网互通 |
| TLS | 443 强制 HTTPS，HTTP 301 跳转；证书自动续期 |
| 备份加密（A4） | `mysqldump \| gpg --encrypt` 或托管数据库自动加密备份；备份文件存异地 |
| 日志脱敏 | 已内置 `MaskingMessageConverter`，确认生产 logback 使用该 converter |
| 导出留痕 | 已实现签名 URL（`EXPORT_SECRET` + TTL），确认 `url-ttl-millis` 符合安全要求 |
| 设备绑定 | 高危接口走 `X-Device-Id` + AppSecret，确认设备表 `app_secret` 已加密（`SensitiveTypeHandler`） |
| 账号 | 上线前修改 `admin/admin123` 初始密码；关闭 Swagger UI 对外访问 |

---

## 9. 上线检查清单（逐项打勾）

### 上线前

- [ ] A1：补齐 `/api/v1/files/upload`（含鉴权、大小限制 20MB、类型白名单、落盘路径）并在小程序实机验证拍照上传
- [ ] A2：Electron 打包出 NSIS 安装包，安装后可启动并加载页面
- [ ] A3：新 Dockerfile 构建成功，镜像启动后 health = UP
- [ ] A4：备份加密脚本就绪并演练一次恢复
- [ ] A5：三项密钥已替换，且 `CEAMS_SENSITIVE_KEY` 已备份到密钥库
- [ ] MySQL 空库已创建，迁移前全量备份策略就绪
- [ ] Nginx 反向代理 + TLS 生效，`/api/v1/auth/login` 走 HTTPS 可通
- [ ] Web 构建产物部署，history 路由刷新不 404
- [ ] 小程序合法域名三项已在微信后台配置
- [ ] 小程序开发者工具 UI 全流程通过
- [ ] 初始管理员密码已修改
- [ ] Swagger UI / `/v3/api-docs` 未对外暴露
- [ ] 旧系统（Android / VPN / JavaFX 桌面）下线通知已发出

### 上线后

- [ ] `/actuator/health` 持续 UP
- [ ] 日志无敏感字段泄漏（抽查手机号/银行账号/税号）
- [ ] Flyway 历史全部 success
- [ ] 三端登录 + 各域核心流程抽测
- [ ] 备份任务首次执行成功且备份文件可解密

---

## 10. 回滚方案

| 场景 | 回滚动作 |
|---|---|
| 应用异常 | 回退到上一版本 jar / 镜像，保留原环境变量 |
| 数据库迁移失败 | Flyway 不支持自动回滚 → **必须**在迁移前做全量备份；回滚 = 恢复备份后回退旧版本 |
| 前端异常 | Nginx 切回上一版 `dist` 目录 |
| 小程序异常 | 微信后台回退到上一线上版本 |

> 原则：数据库迁移只前进不后退，回滚依赖备份恢复，因此**迁移前备份是唯一安全网**。

---

## 11. 旧系统退场

| 资产 | 处理 |
|---|---|
| `androidclient/` | 归档后从主仓移除（架构已明确不再产出 Android） |
| VPN 相关（`*.ovpn`、`deploy-vpn-*.bat`、`*_VPN_*.md`） | 服务端关闭，文档归档 |
| 旧 `Dockerfile` / `docker-compose.yml` / `DEPLOYMENT*.md` | 归档，由本文档与新部署文件替代 |
| 旧 JavaFX 桌面（`vue/`、`app/` 等遗留目录） | 归档 |

> 建议在独立分支做一次清理提交，先归档再删除，避免误删仍在使用的脚本。
