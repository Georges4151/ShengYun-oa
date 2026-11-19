# CEAMS 建筑企业账目管理系统（V2）

面向建筑企业的多端一体化业务管理系统，覆盖项目、材料、分包、账目、报销、银行流水、印章、文档、人事行政、流程审批与经营报表等场景。

后端为 Spring Boot 单体服务，前端为 pnpm monorepo，同一套接口契约同时驱动 Web、桌面端（Electron）与微信小程序。

> 仓库旧版 README 描述的是已下线的 JavaFX 1.0 桌面版，1.0 已弃用并归档在 `master` 基线提交中；当前 `dev` 分支为 V2 全量实现。

---

## 一、系统架构

```
┌──────────────────────┬──────────────────────┬──────────────────────┐
│  Web (Vue 3 + Vite)  │ Desktop (Electron)   │ 小程序 (uni-app)      │
└──────────┬───────────┴──────────┬───────────┴──────────┬───────────┘
           │                      │                      │
           └────────── 统一 API 客户端 @ceams/api-client ──┘
                                  │ HTTPS / JWT / HMAC
                     ┌────────────▼────────────┐
                     │  Spring Boot 3 后端      │
                     │  REST /api/v1            │
                     └───┬────────┬────────┬───┘
                         │        │        │
                    ┌────▼───┐ ┌──▼───┐ ┌─▼──────────┐
                    │ MySQL 8│ │Redis │ │OnlyOffice   │
                    └────────┘ └──────┘ │Ollama(AI)   │
                                         └─────────────┘
```

## 二、技术栈

| 层次 | 技术 |
|------|------|
| 后端 | Java 17、Spring Boot 3、Spring Security、MyBatis-Plus、Flyway、Redis |
| 数据库 | MySQL 8（Flyway 增量迁移，当前至 V47） |
| 前端 | Vue 3、TypeScript、Vite、Element Plus、Pinia、Vue Router |
| 桌面端 | Electron + electron-builder（NSIS 安装包） |
| 小程序 | uni-app（Vue 3）编译微信小程序 |
| 文档服务 | OnlyOffice Document Server（在线编辑） |
| AI | Ollama（文本 qwen2.5:7b / 视觉 qwen2.5vl:7b），默认关闭 |
| 工程化 | pnpm workspace、ESLint、vue-tsc、Docker Compose |

## 三、目录结构

```
.
├── src/                      # 后端 Spring Boot 源码
│   ├── main/java/com/ceams/
│   │   ├── common/           # 统一响应、异常、安全、审计
│   │   └── module/           # iam / project / masterdata / expense / payment /
│   │                         # bank / seal / doc / bpm / hr / admin / report / ai
│   ├── main/resources/
│   │   ├── db/migration/     # Flyway 迁移脚本 V1～V47
│   │   └── application.yml   # 统一配置（环境变量注入）
│   └── test/java/            # 单元测试
├── apps/
│   ├── web/                  # Web 端（Vue 3 + Element Plus）
│   ├── desktop/              # 桌面端（Electron 主进程与打包配置）
│   └── miniprogram/          # 微信小程序（uni-app）
├── packages/
│   ├── api-client/           # 三端共用 HTTP 客户端与域接口
│   ├── types/                # 共享 TS 类型与错误码
│   ├── ui-web/               # Web 通用组件
│   └── utils/                # 加密、签名、格式化等工具
├── deploy/                   # 生产部署（Dockerfile + compose，端口 8080）
├── docs/                     # 设计文档、OpenAPI 契约、启动指南
├── scripts/                  # 备份脚本、种子数据
└── docker-compose.yml        # 本地开发编排（端口 9095，复用 ceams-mysql/redis）
```

## 四、功能模块

- **基础平台**：JWT 双令牌（访问令牌 + 一次性刷新令牌）、RBAC 权限、设备绑定与 HMAC 签名、操作审计。
- **项目与主数据**：项目台账、进度款、变更申请、分包商与分包合同、结算凭证、材料、设备租赁、供应商与分类。
- **资金与账目**：收支账目、报销（含票据上传与 AI 识别）、银行流水导入与对账匹配、应收应付。
- **印章管理**：印章台账 → 用印申请 → 审批 → PDF 盖章 → 使用审计闭环，高危接口强制签名校验。
- **文档中心**：目录权限、版本管理、OnlyOffice 在线编辑、正文抽取与分块索引、知识库 RAG 检索。
- **流程审批**：BPM 模板与版本、节点流转、待办/已办、审批留痕。
- **人事行政**：员工档案、考勤、请假、绩效、合同台账、工资条、报修、车辆、资产、访客。
- **报表与集成**：经营指标、Excel 导出（签名临时下载链接）、通知中心、企业微信推送、AI 日报与只读查询工具。

## 五、快速开始

详细步骤见 [docs/STARTUP_GUIDE.md](docs/STARTUP_GUIDE.md)，核心命令如下。

### 1. 后端（Docker，推荐）

```powershell
docker network create ceams-net
docker network connect ceams-net ceams-mysql
docker network connect ceams-net ceams-redis
docker compose up -d --build
curl http://localhost:9095/actuator/health   # {"status":"UP"}
```

### 2. Web 端

```powershell
pnpm install
pnpm dev                                     # http://localhost:5173
```

### 3. 小程序

```powershell
pnpm --filter @ceams/miniprogram dev:mp-weixin
# 微信开发者工具打开 apps/miniprogram/dist/dev/mp-weixin，勾选「不校验合法域名」
```

### 4. 桌面端

```powershell
pnpm --filter @ceams/desktop start           # 开发调试
pnpm --filter @ceams/desktop dist            # 打包 NSIS 安装包
```

### 端口约定（本地开发）

| 服务 | 端口 |
|------|------|
| 后端 API | 9095 |
| Web 前端 | 5173 |
| MySQL | 13306（宿主）→ 3306（容器） |
| Redis | 6379 |
| OnlyOffice | 8081 |
| Ollama | 11434（仅回环） |

默认账号：`admin / admin123`；新建用户未指定密码时默认 `123456`。

## 六、生产部署

生产环境使用 [deploy/docker-compose.yml](deploy/docker-compose.yml)（端口 8080），敏感配置通过 `.env` 注入（`MYSQL_ROOT_PASSWORD`、`REDIS_PASSWORD`、`JWT_SECRET`、`CEAMS_SENSITIVE_KEY`、`EXPORT_SECRET`），详见 [deploy/README.md](deploy/README.md)。

> 根目录 compose 仅用于本地开发，切勿用于生产；两者不可混用。

## 七、数据库与配置

- 数据库结构由 Flyway 管理，启动时自动执行 `src/main/resources/db/migration` 下的增量脚本，**禁止修改已执行的历史脚本**，变更请新增 `V{n}__xxx.sql`。
- 所有敏感配置（JWT 密钥、AES 密钥、导出签名密钥）必须通过环境变量注入，仓库内仅有开发占位值。
- AI 能力由 `AI_ENABLED` 控制，默认关闭；启用前需拉取 Ollama 模型。

## 八、分支与协作

| 分支 | 用途 |
|------|------|
| `master` | 稳定分支，仅接受来自 `dev` 的 PR 合并 |
| `dev` | 日常集成分支，功能开发与联调提交至此 |

提交信息格式：`[类型] 模块/范围: 简短描述`，类型取 `feat / fix / docs / style / refactor / test / chore`。

常规流程：在 `dev` 开发 → 推送 `origin dev` → 创建 `dev → master` 的 PR → 评审通过后合并。

## 九、测试与校验

```powershell
pnpm -r --if-present typecheck     # 全端 TS 类型检查
pnpm lint                          # ESLint
mvn test                           # 后端单元测试
```

## 十、安全约定

- 文件上传强制扩展名白名单、大小限制与路径穿越防护。
- 盖章、用印等高危接口强制 HMAC 签名（`X-Sign` / `X-Timestamp` / `X-Nonce`，±5 分钟 + nonce 防重放）。
- 导出与盖章文件下载使用限时签名 URL，默认 5 分钟有效。
- 敏感字段（金额等）数据库加密存储，报表统计必须在 Java 层解密后汇总。
- 知识库检索先做权限过滤再送入模型上下文，AI 回答必须附带来源引用。
