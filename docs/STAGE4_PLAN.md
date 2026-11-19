# CEAMS 重构 阶段 4 实施计划（notification / report / Electron / 小程序 / 敏感字段 / CI 验收）

> 生成时间：2026-10-29 ｜ 唯一事实来源：`docs/ARCHITECTURE.md` ｜ 阶段 3 已提交 dev（`ccc7262`）

## 0. 事实核对（规划前必须知晓的现状）

规划前阅读了架构文档、V1 建库脚本、pom、前端 apps/packages 结构、后端 bank 域模板、openapi 契约后，确认以下与任务假设一致/不一致的事实：

1. **notification 三表已在 V1 建好**。[V1__schema.sql](file:///e:/sxassist/src/main/resources/db/migration/V1__schema.sql)（L878-949）已含 `biz_notification`、`biz_user_notification`、`biz_user_notification_preference`，字段与 `ARCHITECTURE.md §2` 描述一致，且**均已有索引/唯一键**。因此 N1 的 Flyway 工作**不是重建表**，而是「V1 既有 DDL 与实体字段一致性核对 + 按需 V9 增量补齐」。**禁止修改 V1**。

2. **report/Dashboard 不单独成域**：架构明确其为「跨域只读应用层」，需以 `com.ceams.module.report` 包实现（只读聚合，多表统计写 MyBatis XML，不建表、不写事务）。后端当前**不存在**任何 report 相关代码。

3. **Electron 壳与小程序骨架已就绪，但仅骨架**：
   - [apps/desktop/electron/main.ts](file:///e:/sxassist/apps/desktop/electron/main.ts) 已配置 `contextIsolation/sandbox/nodeIntegration:false`、CSP、`app:get-version` IPC；[preload.ts](file:///e:/sxassist/apps/desktop/electron/preload.ts) 已用 `contextBridge` 暴露最小白名单 `ceamsDesktop`。**缺**：复用 Web 渲染层打包、USB 电子盖章（node-usb/drivelist + crypto + pdf-lib）、refresh 放系统钥匙串（keytar）、USB 印章读盘 IPC。
   - [apps/miniprogram](file:///e:/sxassist/apps/miniprogram/src) 已有 uni-app 骨架、`platform/request.ts`+`platform/http.ts` 适配层、`pages/index` 登录占位。**缺**：登录绑定、项目/进度款只读、报销待办审批+票据拍照、只读账目、通知列表；`baseUrl` 与 `getSignSecret` 仍是占位。

4. **CI 静态检查现状**：[pom.xml](file:///e:/sxassist/pom.xml) 已含 ArchUnit（1.3.0）+ PDFBox + Bouncy Castle + MyBatis-Plus + MapStruct。**缺** Checkstyle、Spotless、PMD、JaCoCo 插件与配置；覆盖率门槛未设。

5. **敏感字段现状（明文 + 工具重复）**：
   - 需加密字段（V1 已标「需加密」注释）：`phone`（sys_user L26 / contact L219 / subcontractor L404）、`account_number`（biz_payment_account L597 / biz_general_ledger L681）、税号（contact 域，字段名待核对）。
   - 现有 3 份重复 `SensitiveMaskUtil`（[iam](file:///e:/sxassist/src/main/java/com/ceams/module/iam/util/SensitiveMaskUtil.java) / [masterdata](file:///e:/sxassist/src/main/java/com/ceams/module/masterdata/util/SensitiveMaskUtil.java) / [project](file:///e:/sxassist/src/main/java/com/ceams/module/project/util/SensitiveMaskUtil.java)），N5 应收敛为共享能力。
   - HMAC 签名密钥当前为「配置注入 + 短期下发」最小实现（阶段 3 遗留），完整「AppSecret 绑定设备 + 密钥轮换」在 N5。

6. **前端通知/Dashboard 未接入**：[routes.ts](file:///e:/sxassist/apps/web/src/router/routes.ts) 中 `notification` 仍指向 `ModulePlaceholder`，无 Dashboard 路由；`packages/types` 与 `packages/api-client` 无 `notification.ts` / `report.ts`。

7. **既有模式基准**（沿用阶段 3 实际写法）：Converter 用 `@Mapper(componentModel="spring")` + 手写 default 方法；每域独立 `PageUtil(MAX_SIZE=100)`；DTO `@Valid`；写操作 `@Transactional(rollbackFor=Exception.class)`；高危接口 `@HmacSigned`；前端类型手写 `@ceams/types` + `@ceams/api-client` 域工厂，签名接口 `client.post(path, body, {signed:true})`。

---

## 1. 阶段 4 目标与范围界定

### 1.1 做什么

- **N1 notification 域**：三实体全量 CRUD + 通知发布（PUBLISHED 快照到用户通知）+ 分渠道推送（SYSTEM/EMAIL/SMS，本阶段 SYSTEM 真实、EMAIL/SMS 走适配器占位）+ 已读 + 免打扰/偏好。
- **N2 report/Dashboard**：跨域只读应用层，财务报表 / 项目成本报表 / 材料报表，多表统计写 MyBatis XML，配 Web Dashboard 页面。
- **N3 Electron 桌面端**：复用 Web 渲染层 + USB 电子盖章（node-usb/drivelist + crypto + pdf-lib 本地叠加）+ refresh 放系统钥匙串 + IPC 白名单收紧。
- **N4 微信小程序**：登录绑定、项目/进度款只读、报销待办审批+票据拍照、只读账目、通知列表；`wx.*` 收进平台适配层；PDF 走服务端生成 + `downloadFile` + `openDocument`。
- **N5 敏感字段加密与脱敏**：税号/银行账号/手机号库内加密、接口脱敏、日志脱敏、备份加密、导出走签名 URL 留痕、AppSecret 绑定设备。
- **N6 三端联调 + CI 补齐 + 阶段 4 验收**：Checkstyle/Spotless/PMD/JaCoCo + 覆盖率门槛 + 全量验收。

### 1.2 不做什么（明确排除）

- 不新增 Android / APK（已退场）。
- 不迁移旧数据、不动 V1/V2/V4~V8 已执行迁移；新增从 V9 起。
- EMAIL/SMS 真实外部通道不接（仅适配器接口 + SYSTEM 站内信真实落库）。
- 不引入新的持久化框架（CRUD 统一 MyBatis-Plus）。

---

## 2. 依赖与构建顺序

```text
N1 notification ──► N2 report ──► N3 desktop ──► N4 miniprogram ──► N5 sensitive ──► N6 CI/验收
                        ▲               ▲                ▲                  ▲
                        └─ 依赖已就绪域（expense/payment/bank/project/masterdata）
```

- **N1** 依赖：`iam`（user_id 引用、SecurityUtils）。
- **N2** 依赖：`expense`/`payment`/`bank`/`project`/`masterdata`（均已就绪）；按任务序排 N1 之后。
- **N3** 依赖：`apps/web` 渲染层（已就绪）+ `seal`（服务端 PDF 盖章已就绪）；USB 本地叠加需 `payment/expense` 等业务域（已就绪）。
- **N4** 依赖：`notification`（N1）+ `project`/`expense`/`payment`（已就绪）。
- **N5** 跨切：触及 payment/bank/masterdata/iam/project 既有实体，故排 N3/N4 之后、N6 之前集中处理。
- **N6** 依赖：全部。

---

## 3. 关键设计决策（与规范对齐）

1. **通知发布语义**：`Notification.status = PUBLISHED` 时，按 `UserNotificationPreference` 过滤目标用户（免打扰/优先级阈值），为每个接收人生成 `UserNotification`（channel=SYSTEM 优先落库，EMAIL/SMS 依偏好生成 `PENDING` 推送记录）；`REVOKED` 不回补已读状态，仅停止新增分发。
2. **免打扰判定**：`quiet_mode=1` 时，若当前时间落在 `quiet_start_time~quiet_end_time`，仅当通知 `priority` 高于 `priority_threshold` 才即时推送，否则降级为「免打扰期间暂存」（本阶段以 `UserNotification` 落库、推送记录标 `PENDING`）。
3. **report 只读**：不建实体/表；`ReportMapper.xml` 直接 `SELECT` 聚合，返回 `Map`/专用 VO；Controller 只读，**不开启事务**。
4. **Electron 渲染层复用**：桌面端 `dev` 走 `http://localhost:5173`，`build` 用 `apps/web/dist`（`loadFile`）；不复制 Web 业务代码，仅新增 `electron/`（主进程 + preload）与 USB 印章 IPC。
5. **USB 电子盖章**：`node-usb`/`drivelist` 读盘 + `crypto` 做密钥/校验 + `pdf-lib` 本地叠加（渲染进程不接触 Node，经 preload 白名单 IPC 调主进程执行）；服务端 PDF 盖章（阶段 3 已就绪）作为无 USB 时的兜底路径。
6. **refresh 钥匙串**：桌面端用 `keytar`（系统钥匙串）存 refresh token，替代 Web 的 HttpOnly Cookie。
7. **小程序 PDF**：服务端生成盖章/报表 PDF → `wx.downloadFile` → `wx.openDocument`；`wx.request/uploadFile/downloadFile` 全部封装进 `platform/`。
8. **敏感字段加密**：库内加密统一走 MyBatis-Plus `TypeHandler`（AES-GCM，密钥来自环境变量 `CEAMS_SENSITIVE_KEY`，不落库）；VO 出参统一脱敏走共享 `SensitiveMaskUtil`（收敛到 `com.ceams.common`）；日志脱敏走 `logback` 转换规则；导出走签名 URL + 审计留痕。
9. **AppSecret 设备绑定**：在 N5 将阶段 3「配置注入 + 短期下发」升级为「AppSecret 绑定设备 + 密钥轮换」（`iam` 域 `UserDevice` 扩展），客户端（Electron 钥匙串 / 小程序本地存储）持设备绑定 secret，服务端校验设备指纹。

---

## 4. 任务拆分

> 编号：`N-*` notification、`R-*` report、`D-*` desktop、`MP-*` miniprogram、`S-*` sensitive、`C-*` CI。所有后端任务遵守：MyBatis-Plus Lambda Wrapper、`ApiResponse`/`PageResult`、`@Valid` DTO、`@Transactional(rollbackFor=Exception.class)`、MetaObjectHandler 审计、Converter（沿用 project/bank 写法）、ID/金额/时间契约、高危接口 `@HmacSigned`。

### 4.1 N1 notification 域

- **N-0 冻结契约**：`docs/openapi/notification.openapi.yaml`（三实体 + 枚举 NotificationStatus/NotificationPriority/NotificationType/PushChannel/PushStatus + 发布/已读/免打扰动作）。
- **N-1 Notification 实体 + 枚举 + Mapper**：对齐 `biz_notification`；枚举 `NotificationStatus(DRAFT/PUBLISHED/REVOKED)`、`NotificationPriority(LOW/NORMAL/HIGH/URGENT)`、`NotificationType`（代码值）、`ContentType`。
- **N-2 UserNotification 实体 + Mapper**：对齐 `biz_user_notification`；`ReadStatus`、`PushChannel(SYSTEM/EMAIL/SMS)`、`PushStatus(PENDING/SENT/FAILED)`。
- **N-3 UserNotificationPreference 实体 + Mapper**：对齐 `biz_user_notification_preference`；唯一键 `(user_id, notification_type)`。
- **N-4 DTO/VO/Converter**：三实体 `@Valid` DTO；VO ID/时间序列化符合契约；`NotificationVO` 冗余 `publishTime`。
- **N-5 NotificationService（CRUD + 发布）**：分页过滤；create 默认 DRAFT；`publish` 触发分发（快照到 UserNotification）；`revoke` 停止新增分发；仅 DRAFT 可编辑。
- **N-6 分渠道推送 + 免打扰**：`NotificationPushService`（适配器接口 `EmailSender/SmsSender` + 站内信真实实现）；发布时按 `UserNotificationPreference` 过滤 + 免打扰判定 + 优先级阈值。
- **N-7 UserNotificationService（已读/批量已读/未读数）**：`read`/`readAll`/`unreadCount`；`GET /notifications/mine` 分页。
- **N-8 UserNotificationPreferenceService（偏好 CRUD + upsert）**：`(user_id, notification_type)` upsert。
- **N-9 Flyway 增量 V9**：核对 V1 三表，仅补齐缺失索引/列（若无缺失则 V9 空迁移占位，保证版本连续）。
- **N-10 前端类型 + api-client**：`packages/types/src/notification.ts` + `packages/api-client/src/domains/notification.ts`；`read` 系列标 `signed:false`（非资金/印章高危，普通鉴权即可）。

### 4.2 N2 report/Dashboard

- **R-1 冻结契约**：`docs/openapi/report.openapi.yaml`（财务/项目成本/材料三类报表 + Dashboard 概览，只读 GET）。
- **R-2 Report 应用层骨架**：`com.ceams.module.report.controller/dto/vo/service/serviceImpl/mapper`（**无 entity**）；`ReportService` 接口。
- **R-3 财务报表**：`ReportMapper.xml` 多表统计（收支/付款/银行流水/总账聚合，按月份/项目/账户维度）；`FinancialReportVO`。
- **R-4 项目成本报表**：`ProjectCostReportMapper.xml`（项目 + 预算 + 变更 + 进度款 + 成本分摊聚合）；`ProjectCostReportVO`。
- **R-5 材料报表**：`MaterialReportMapper.xml`（材料库 + 领用/租赁聚合）；`MaterialReportVO`。
- **R-6 Dashboard 概览接口**：`GET /api/v1/reports/dashboard` 返回待办数/未读通知数/近期收支/项目进度等聚合。
- **R-7 前端类型 + api-client + Dashboard 页面**：`packages/types/src/report.ts` + `packages/api-client/src/domains/report.ts` + `apps/web/src/views/report/ReportView.vue`（含三个报表 Tab + Dashboard 概览卡片），路由接入 `routes.ts`。

### 4.3 N3 Electron 桌面端

- **D-1 主进程渲染层接线**：`main.ts` 复用 `apps/web/dist`（loadFile）+ 生产 CSP 收紧；接入 `apps/web` 构建产物路径。
- **D-2 refresh 系统钥匙串**：主进程引入 `keytar`，IPC 白名单 `auth:get-refresh` / `auth:set-refresh` / `auth:clear-refresh`；preload 暴露 `getRefreshToken/setRefreshToken/clearRefreshToken`。
- **D-3 USB 电子盖章**：`electron/usb-seal.ts`（`drivelist` 枚举 + `node-usb` 读盘 + `crypto` 校验）；`pdf-lib` 本地叠加盖章（主进程，经 IPC）；preload 白名单 `seal:list-drives` / `seal:read-seal` / `seal:stamp-local`。
- **D-4 IPC 白名单收紧 + 类型**：`global.d.ts` 补充 `ceamsDesktop` 全量类型；主进程 IPC handler 与 preload 通道一一对应，拒绝未注册通道。
- **D-5 构建/类型校验**：`tsconfig` 通过；`pnpm --filter @ceams/desktop typecheck` 零错误；`electron .` 可启动（验证壳）。

### 4.4 N4 微信小程序（uni-app）

- **MP-1 平台适配层补全**：`platform/request.ts` 完善 `uploadFile/downloadFile` 封装；`platform/http.ts` 接线真实 `baseUrl` 与 `getSignSecret`（绑定设备 secret）。
- **MP-2 登录绑定**：`pages/login` 登录（手机号）+ 账号绑定；token/refresh 本地存储（短有效期）。
- **MP-3 项目/进度款只读**：`pages/project` 列表 + 详情 + 进度款只读查询（复用 `@ceams/api-client`）。
- **MP-4 报销待办审批 + 票据拍照**：`pages/expense` 待办列表 + 审批动作 + 票据拍照上传（`wx.chooseImage` + `uploadFile`）。
- **MP-5 只读账目**：`pages/payment` 收支/付款账户只读查询。
- **MP-6 通知列表**：`pages/notification` 通知列表 + 已读（依赖 N1）。
- **MP-7 PDF 预览**：服务端生成 PDF → `downloadFile` → `openDocument`（盖章/报表）。
- **MP-8 构建/类型校验**：`pnpm --filter @ceams/miniprogram typecheck` + `uni build -p mp-weixin` 零错误零 `any`。

### 4.5 N5 敏感字段加密与脱敏

- **S-1 字段清单核对**：核对 `phone`/`account_number`/税号/其他敏感字段精确位置与类型（sys_user/contact/subcontractor/payment_account/general_ledger）。
- **S-2 统一加密器**：`com.ceams.common.crypto.SensitiveEncryptor`（AES-GCM，密钥 `CEAMS_SENSITIVE_KEY` 环境变量注入）+ MyBatis-Plus `SensitiveTypeHandler`（实体敏感字段标注）。
- **S-3 脱敏收敛**：删除 3 份重复 `SensitiveMaskUtil`，收敛到 `com.ceams.common.mask.SensitiveMaskUtil`（手机号/账号/税号脱敏规则），VO 出参统一走 Jackson 脱敏注解或 converter 手写。
- **S-4 日志脱敏**：`logback` 转换规则或 AOP 脱敏，确保 `phone/account_number/tax_no` 不落日志明文。
- **S-5 备份加密 + 导出签名 URL**：备份加密配置；导出接口改签名 URL（短期有效）+ 审计留痕（复用 operation_log）。
- **S-6 AppSecret 设备绑定**：`iam` 域 `UserDevice` 扩展 + AppSecret 绑定/轮换接口；`SignSecretResolver` 改按设备解析；前端三端接线设备绑定 secret。

### 4.6 N6 CI 补齐 + 验收

- **C-1 Checkstyle**：pom 引入 + `checkstyle.xml`（Google 风格裁剪）+ 全量通过。
- **C-2 Spotless**：pom 引入 + `spotless` 格式化 + `check` 通过。
- **C-3 PMD**：pom 引入 + 规则集 + 全量通过（无阻断级告警）。
- **C-4 JaCoCo**：pom 引入 + 覆盖率门槛（行/分支基线，如 line ≥ 60% 关键域）。
- **C-5 三端联调**：Web/Desktop/MiniProgram 统一 HTTPS 网关联调（签名、鉴权、脱敏、分页契约核对）。
- **C-6 阶段 4 全量验收**：`mvn clean verify`（含新增测试）+ ArchUnit + Checkstyle/Spotless/PMD/JaCoCo + `pnpm typecheck/build` + Flyway 全量迁移（V9+）+ 应用启动。

---

## 5. 里程碑

| # | 里程碑 | 验收判据 | 依赖 |
|---|---|---|---|
| M1 | notification 域完成 | 三实体 CRUD + 发布分发 + 已读 + 免打扰；契约冻结；Flyway V9 | N-0~N-10 |
| M2 | report/Dashboard 完成 | 三类报表 + Dashboard 只读聚合；MyBatis XML 多表统计；Web Dashboard 页面 | R-1~R-7 |
| M3 | Electron 桌面端完成 | 复用 Web 渲染层 + USB 盖章 + refresh 钥匙串 + IPC 白名单 | D-1~D-5 |
| M4 | 微信小程序完成 | 登录/项目只读/报销审批+拍照/只读账目/通知/PDF；构建零错误 | MP-1~MP-8 |
| M5 | 敏感字段加密与脱敏 | 库内加密 + 脱敏收敛 + 日志脱敏 + 备份/导出签名 + AppSecret 设备绑定 | S-1~S-6 |
| M6 | CI 补齐 + 阶段 4 验收 | 静态检查全通过 + 覆盖率达标 + 三端联调 + 全量验收 | C-1~C-6 |

---

## 6. 验收标准

### 6.1 各里程碑功能验收点

- **notification**：发布 → 按偏好过滤 → 生成 `UserNotification`；已读/批量已读/未读数；免打扰 + 优先级阈值生效；`REVOKED` 停止新增分发；`Notification` 仅 DRAFT 可编辑。
- **report**：三类报表 + Dashboard 只读聚合返回正确；多表统计走 XML；无 entity/无写事务。
- **desktop**：壳启动复用 Web 渲染层；IPC 白名单最小暴露；refresh 落系统钥匙串；USB 盖章经 preload 白名单 IPC 本地叠加成功。
- **miniprogram**：登录绑定、项目/进度款只读、报销审批+票据拍照、只读账目、通知列表、PDF 预览；`wx.*` 仅在 `platform/`；构建零 `any`。
- **sensitive**：库内 `phone/account_number/税号` 密文；VO 出参脱敏；日志无明文敏感字段；导出签名 URL + 留痕；AppSecret 绑定设备。

### 6.2 整体阶段 4 验收点

1. 序列化契约：`ApiResponse`/`PageResult`、金额→字符串、ID→字符串、时间 ISO8601、空列表 `[]`。
2. 规范一致性：单表 CRUD 用 Lambda Wrapper；多表统计写 XML；写操作 `@Transactional`；DTO `@Valid`；Entity 不出 Controller。
3. 契约与前端类型一致：`notification`/`report` 契约冻结 → `@ceams/types` → `@ceams/api-client` → 页面，无 `any`。
4. Flyway：不改 V1/V2/V4~V8，新增 V9+；全量 migrate 通过。
5. 构建与架构约束：`mvn clean verify` 通过；ArchUnit 通过；Checkstyle/Spotless/PMD/JaCoCo 通过；`pnpm typecheck` + `pnpm build` + `uni build -p mp-weixin` 通过。
6. 安全：敏感字段库内密文、出参脱敏、日志脱敏、导出签名 URL 留痕、AppSecret 绑定设备、三端统一 HTTPS。

---

## 7. 风险点与应对

| 风险 | 应对 |
|---|---|
| notification 三表已在 V1 vs「V9 起建表」假设 | 只做「核对 + 增量补齐」，禁止动 V1；无缺失则 V9 空迁移占位 |
| EMAIL/SMS 真实通道未接 | 本阶段仅适配器接口 + SYSTEM 真实落库，EMAIL/SMS 生成 PENDING 记录，明确边界 |
| Electron 复用 Web 构建产物路径 | D-1 明确 dev/prod 双路径；prod 用 `apps/web/dist`，typecheck 校验 |
| node-usb 原生依赖编译/平台差异 | D-3 先 PoC；若 node-usb 编译困难，退化为 `drivelist` 读盘 + 服务端盖章兜底 |
| 敏感字段加密对既有查询影响 | 加密字段禁止直接 `=`/`like` 查询，按需加「密文索引」或调整查询维度；N5 需做既有功能回归 |
| 3 份 SensitiveMaskUtil 收敛引发改动面 | 统一共享后逐域回归，`mvn clean verify` 兜底 |
| AppSecret 设备绑定改造波及三端签名 | 保持 `getSignSecret` 抽象不变，仅替换 Resolver 实现；前端签名头生成逻辑复用 |
| CI 静态检查首次接入告警量大 | 采用「先跑通 + 基线阈值 + 逐步收紧」，PMD/Checkstyle 非阻断级先告警不失败 |
| 阶段跨度大、交叉改动多 | 每里程碑独立提交 dev；N5 集中回归；M6 全量验收兜底 |

---

## 8. 与后续的交接边界

阶段 4 为重构最后一阶段，无后续阶段交接。剩余可选项（非本阶段范围）：
- EMAIL/SMS 真实通道接入（需供应商凭据）。
- 真实对象存储 / CDN 与签名 URL 网关接入（当前签名 URL 为应用层实现）。
- 生产级监控告警、限流、防爬策略落地（架构 §9 已列，属部署运维）。
