# CEAMS 启动步骤说明

本文档对应当前仓库结构（Spring Boot 后端 + web / desktop / miniprogram 三端 monorepo），
本地开发推荐使用「后端 Docker 容器 + Web 前端 Vite 开发服务器」的组合方式。

---

## 一、环境准备（首次执行）

| 依赖 | 版本要求 | 说明 |
|------|----------|------|
| JDK | 17+ | 本地直接跑后端时需要；纯 Docker 方式可不装 |
| Node.js | ≥ 20.19.0 | 前端构建 |
| pnpm | ≥ 9.0.0 | `npm i -g pnpm` |
| Docker Desktop | 已安装并启动 | 运行 MySQL / Redis / 后端 / OnlyOffice / Ollama |
| 微信开发者工具 | 最新稳定版 | 仅小程序端需要 |

安装前端依赖（仓库根目录执行一次即可）：

```powershell
cd e:\sxassist
pnpm install
```

---

## 二、启动后端（Docker 方式，推荐）

根目录 [docker-compose.yml](../docker-compose.yml) 用于本地开发编排，包含
`app`（Spring Boot，宿主端口 **9095**）、`ollama`、`onlyoffice` 三个服务，
并复用已在运行的 `ceams-mysql`、`ceams-redis` 容器（同一外部网络 `ceams-net`）。

### 1. 首次执行的网络准备

```powershell
docker network create ceams-net
docker network connect ceams-net ceams-mysql
docker network connect ceams-net ceams-redis
```

> 只有第一步需要执行；网络已存在时会提示 already exists，可忽略。
> `ceams-mysql` / `ceams-redis` 若尚未创建，请先用 [deploy/docker-compose.yml](../deploy/docker-compose.yml) 启动。

### 2. 构建并启动

```powershell
cd e:\sxassist
docker compose up -d --build
```

首次构建会下载 Maven 依赖，较慢；后续修改代码后需重新执行 `--build` 使改动生效
（容器运行的是镜像，不是源码目录）。

### 3. 验证

```powershell
docker compose ps
curl http://localhost:9095/actuator/health   # 期望 {"status":"UP"}
```

### 4. 关闭

```powershell
docker compose down
```

> ⚠️ 生产部署请使用 [deploy/docker-compose.yml](../deploy/docker-compose.yml)（端口 8080、密钥由 `.env` 注入），
> 根目录这份仅供本地开发，两者不可混用。

---

## 三、启动 Web 前端

```powershell
cd e:\sxassist
pnpm dev            # 等价于 pnpm --filter @ceams/web dev
```

浏览器访问 http://localhost:5173

- 开发环境通过 Vite 代理把 `/api` 转发到后端：
  [.env.development.local](../apps/web/.env.development.local) 中 `VITE_PROXY_TARGET=http://localhost:9095`。
- 若后端改到其他端口，修改该文件中的 `VITE_PROXY_TARGET` 后重启 dev server。
- 前端构建产物：`pnpm build`（输出到 `apps/web/dist`）。

---

## 四、启动微信小程序

```powershell
cd e:\sxassist
pnpm --filter @ceams/miniprogram dev:mp-weixin
```

1. 用微信开发者工具打开目录 `apps/miniprogram/dist/dev/mp-weixin`。
2. 详情 → 本地设置 → 勾选「不校验合法域名、web-view（业务域名）、TLS 版本以及 HTTPS 证书」。
3. 接口地址由 [.env.development.local](../apps/miniprogram/.env.development.local) 控制，当前为 `http://localhost:9095`。
4. 真机调试需把地址改成电脑局域网 IP（手机与电脑同一网络）。
5. 若接口地址变更后仍请求旧域名，执行「清除全部缓存」再重新编译。

> 注意：`uni build -p mp-weixin` 只读取 `.env.production` 与 `.env`，
> 生产构建务必显式配置 `VITE_API_BASE_URL`，否则会回退到占位域名 `https://api.example.com`。

---

## 五、启动桌面端（Electron）

```powershell
cd e:\sxassist
pnpm --filter @ceams/desktop start   # 编译主进程并启动 electron（开发调试）
```

打包安装包：

```powershell
pnpm --filter @ceams/desktop dist    # 产物在 apps/desktop/release
```

---

## 六、服务端口一览

| 服务 | 端口 | 地址 |
|------|------|------|
| 后端 API | 9095 | http://localhost:9095 |
| 健康检查 | 9095 | http://localhost:9095/actuator/health |
| Web 前端 | 5173 | http://localhost:5173 |
| MySQL | 13306（宿主）→ 3306（容器） | jdbc:mysql://localhost:13306/ceams |
| Redis | 6379 | localhost:6379 |
| OnlyOffice | 8081 | http://localhost:8081 |
| Ollama | 11434（仅回环） | http://127.0.0.1:11434 |

---

## 七、默认账号

| 账号 | 密码 | 角色 |
|------|------|------|
| admin | admin123 | 系统管理员 |
| staff01 / hr01 / pm01 | admin123 | 普通业务角色 |
| zhangsan | 123456 | 普通用户 |

> 新建用户未指定密码时默认密码为 `123456`；密码不对时在 Web 端「用户管理」中重置。

---

## 八、常见问题

| 现象 | 原因与处理 |
|------|------------|
| 前端接口 404 / 连接失败 | 后端未启动或端口不一致，检查 `VITE_PROXY_TARGET` 与 `docker compose ps` |
| 后端启动即退出 | 查看 `docker compose logs -f app`，多为 MySQL/Redis 未就绪或不在 `ceams-net` 网络 |
| 修改后端代码不生效 | 容器运行旧镜像，执行 `docker compose up -d --build app` 重建 |
| OnlyOffice 打开文档报错 | JWT 密钥需与 `onlyoffice` 服务一致；清理缓存时需同步清理数据库 `onlyoffice.task_result` 残留记录 |
| AI 功能不可用 | `AI_ENABLED` 默认关闭；Ollama 首次需拉取模型：`docker exec ceams-ollama ollama pull qwen2.5:7b` |
| 小程序请求 `ERR_NAME_NOT_RESOLVED` | 构建未注入 `VITE_API_BASE_URL`，回退到了占位域名 `api.example.com` |
