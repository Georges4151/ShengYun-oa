# CEAMS 重构架构与契约（v1，冻结版）

> 本文档是全面重写的唯一事实来源，覆盖领域划分、数据模型、技术规范、OpenAPI 契约、三端页面地图与构建顺序。所有后续任务拆分与编码以此为准；契约变更须单独提 PR 并做兼容性评审。

## 1. 目标与总体架构

建筑企业账目管理系统（CEAMS）全面重写为「Spring Boot 模块化单体 + Vue3/TS 三端」：

```text
Spring Boot 3.x (JDK17) 模块化单体 + MySQL8 + Redis + Flyway + OpenAPI
                    │ /api/v1（唯一契约）
        ┌───────────┼────────────┐
   apps/web     apps/desktop   apps/miniprogram
  Vue3+Vite     Electron壳      uni-app 微信小程序
        └── packages/api-client / types / ui-web / utils ──┘
```

- 桌面端不再使用 JavaFX，改为 Electron 复用 Web 渲染层。
- 移动端为微信小程序（轻量移动办公），**不产出 Android**。
- 三端统一走公网 HTTPS，**取消 VPN**。
- 新建空库，Flyway 管理，仅初始化管理员/角色/印章/系统配置，**不做旧数据迁移**。

## 2. 限界上下文（8 域，按 `com.ceams.module.{domain}` 分包）

| 域 | 聚合/实体 | 核心能力 |
|---|---|---|
| iam | User、Role、Permission、UserDevice、RefreshToken、OperationLog | 登录、JWT 双轮换、RBAC、设备绑定、会话吊销、审计留痕 |
| masterdata | Category、Contact、Material、Equipment、EquipmentLease | 分类树、往来单位、材料库、设备台账与租赁 |
| project | Project、Budget、ChangeRequest、ProgressPayment、Subcontractor、SubcontractorContract、SettlementProof | 项目台账、预算/变更、进度款、分包合同、结算凭证 |
| expense | Receipt、ApprovalRecord、ReceiptVerification | 票据上报、三级审批、财务核验、付款关联 |
| payment | Account、PaymentAccount、PaymentTransaction、CostAllocation | 收支流水、付款账户、付款交易、成本分摊 |
| bank | BankStatement、GeneralLedger、ReconciliationResult | 流水导入、总账、自动匹配、差异处理 |
| seal | ElectronicSeal、ElectronicSealApplication、StampApproval、ElectronicSealStamp、ElectronicSealUsage | 印章管理、用印审批、PDF 盖章、使用审计 |
| notification | Notification、UserNotification、UserNotificationPreference | 通知发布、分渠道推送、已读、免打扰 |

> 报表/Dashboard 不单独成域，作为跨域只读应用层（report 模块）。

## 3. 数据模型要点

- 统一 `BaseEntity`：雪花 ID（DB bigint / JSON 字符串）、创建与更新时间、创建与更新人、逻辑删除、乐观锁版本号。
- 金额一律 `decimal(18,2)` + `BigDecimal`，JSON 序列化为字符串；时间 `LocalDateTime` + ISO8601。
- 枚举用 `varchar` + 代码值（不落中文）；敏感字段（税号、银行账号、手机号）库内加密。
- 物理外键仅保留核心 1:1，其余用逻辑引用 + 索引。
- 表名前缀：sys_（iam）/ biz_（业务）/ 其余按域。

## 4. 后端技术规范（一致性强制）

- 持久层统一 `mybatis-plus-spring-boot3-starter`：单表 CRUD 用 `BaseMapper`/`IService`，条件用 Lambda Wrapper（禁止字符串列名），仅多表/报表/复杂统计写 XML。
- 分页用 `Page`/`IPage`，默认每页 20、上限 100；注册分页插件。
- `MetaObjectHandler` 自动填充审计字段；写操作统一 `@Transactional(rollbackFor=Exception.class)`，读操作不开事务。
- Entity 不出 Controller，用 MapStruct 转换；DTO/VO 分目录；Jakarta Validation 校验 DTO，业务校验在 Service。
- 统一异常：`BusinessException` + 全局处理器；响应 `ApiResponse<T>`。
- 缓存：Spring Cache + Redis，key `ceams:{module}:{id}`；日志仅 SLF4J。
- 用 MyBatis-Plus Generator 生成骨架再改造，避免各模块写法漂移。
- CI 强制：ArchUnit（Controller 不得依赖 Mapper、Entity 不得出现于 Controller）、Checkstyle/Spotless/PMD/JaCoCo。

## 5. 前端技术规范

- Web 与 Electron 统一 Vue3 `<script setup lang="ts">` + Composition API + Pinia；类型由 OpenAPI 生成，禁止 `any`。
- 业务代码只能经生成的 api-client 或统一请求封装访问接口，组件内禁止直接 axios/fetch/`wx.request`。
- Electron 开启 contextIsolation/sandbox/CSP，渲染进程不碰 Node，IPC 白名单。
- 小程序把 `wx.*` 收进平台适配层：request/uploadFile/downloadFile；PDF 预览走服务端生成 + `wx.downloadFile` + `wx.openDocument`。
- 跨端格式统一：ID 字符串、金额字符串、时间 ISO8601、分页字段 `page/size` 与 `records/total/current/size`、空列表返回 `[]`。

## 6. OpenAPI v1 契约

- 路径 `/api/v1/{group}/...`，group = 域（auth/masterdata/project/expense/payment/bank/seal/notification）。
- 统一响应 `{ code, message, data, traceId, timestamp }`；分页 `PageResult{ records, total, page, size }`。
- 错误码 RFC7807 + 业务码（AUTH_/VALID_/BIZ_/SIGN_ 前缀）；HTTP 语义严格。
- 鉴权：`Authorization: Bearer <access>`（15min JWT）+ refresh（7d 轮换、吊销校验）+ RBAC + 设备绑定。
- 高风险接口（seal/stamp、expense/approve、payment/execute）加 `X-Sign: HMAC-SHA256(timestamp+nonce+bodyHash)` + `X-Timestamp`/`X-Nonce`，服务端 ±5min 窗口 + Redis nonce 防重放。

## 7. 三端页面地图

| 模块 | Web 全量 | 桌面(Electron 复用) | 小程序(uni-app) |
|---|---|---|---|
| 登录/账号 | 登录、角色、设备管理 | 同左 | 登录、账号绑定 |
| 主数据 | 全 CRUD | 同左 | 无 |
| 项目合同 | 全功能 | 同左 | 项目/进度款只读查询 |
| 报销 | 上报、三级审批、核验 | 同左 | 待办审批、票据拍照上传 |
| 资金 | 收支、付款、成本分摊 | 同左 | 只读账目查询 |
| 对账 | 流水导入、对账、差异 | 同左 | 无 |
| 印章 | 印章/申请/审批 | 同左 + USB 电子盖章 | 无 |
| 通知 | 发布/推送管理 | 同左 | 通知列表 |

桌面端独有：USB 电子印章（node-usb/drivelist + crypto + pdf-lib 叠加）。

## 8. 构建顺序与阶段拆分

依赖序：`iam → masterdata → project → expense → payment → bank/seal → notification → report`（seal 依赖 project，payment 依赖 expense）。

- 阶段 2：后端骨架、BaseEntity/响应/异常/安全过滤器、Flyway 全量建库、api-client+types 共享包、实现 iam/masterdata/project。
- 阶段 3：expense（含签名防重放）、payment、bank、seal（PDF 盖章）、apps/web 全量页面。
- 阶段 4：notification、report/Dashboard、Electron 桌面与 USB 盖章、小程序、三端联调、敏感字段加密与脱敏、验收。

## 9. 安全基线

- 三端统一公网 HTTPS，Nginx/WAF 终止 TLS、限流、防爬、审计；MySQL/Redis/文件存储不暴露公网。
- 敏感字段库内加密、接口脱敏、日志脱敏、备份加密；下载/导出走签名 URL 并留痕。
- 客户端不存 AppSecret；Web refresh 放在 HttpOnly Cookie，Electron 放系统钥匙串，小程序短有效期本地存储。

## 10. 已退场/归档

- Android 客户端整体退场；后端删除 MobileApiController + 2 个 Mobile DTO。
- VPN/OpenVPN 相关文件（*.ovpn、VPN_*.md、OPENVPN_*.md、deploy-vpn*/deploy-certs）归档退场。
- 遗留嵌套仓库已归档为 `src/.git.archived`；旧基线 tag `legacy-1.0`。