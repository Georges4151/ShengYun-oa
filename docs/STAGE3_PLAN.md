# CEAMS 重构 阶段 3 实施计划（expense / payment / bank / seal + Web 全量页面）

> 生成时间：2026-10-28 ｜ 唯一事实来源：`docs/ARCHITECTURE.md` ｜ 阶段 2 已提交 dev（`de15c48`）

## 0. 事实核对（规划前必须知晓的现状）

规划前阅读了架构文档、project 域后端模板、全局基础类、Flyway 脚本、OpenAPI 契约、前端 api-client/types/views 后，发现以下几处与任务假设不一致、直接影响任务拆分的事实：

1. **四域数据表已在 V1 建库脚本中全部存在**。[V1__schema.sql](file:///e:/sxassist/src/main/resources/db/migration/V1__schema.sql) 已包含 `biz_receipt`、`biz_approval_record`、`biz_receipt_verification`、`biz_account`、`biz_payment_account`、`biz_payment_transaction`、`biz_cost_allocation`、`biz_bank_statement`、`biz_general_ledger`、`biz_reconciliation_result`、`biz_electronic_seal`、`biz_electronic_seal_application`、`biz_stamp_approval`、`biz_electronic_seal_stamp`、`biz_electronic_seal_usage`，且**均已有索引**。因此阶段 3 的 Flyway 工作**不是重新建表**，而是「V1 既有 DDL 与实体字段一致性核对 + 增量脚本（V5+）补齐缺失列/索引」。**禁止修改 V1**。

2. **HMAC 签名骨架已存在但未启用**：
   - [HmacSignUtil.java](file:///e:/sxassist/src/main/java/com/ceams/common/sign/HmacSignUtil.java) 已实现 `sha256Hex/sign/withinWindow/constantTimeEquals`，但 `checkNonceOnce` 是占位实现（恒返回 true，未接 Redis）。
   - [ErrorCode.java](file:///e:/sxassist/src/main/java/com/ceams/common/exception/ErrorCode.java) 已含 `SIGN_TIMESTAMP_EXPIRED/SIGN_NONCE_REPLAY/SIGN_INVALID`。
   - 前端 [http-client.ts](file:///e:/sxassist/packages/api-client/src/http-client.ts) 已支持 `signed` 自动加 `X-Sign/X-Timestamp/X-Nonce`，[sign.ts](file:///e:/sxassist/packages/utils/src/sign.ts) 已实现签名计算；但 [http.ts](file:///e:/sxassist/apps/web/src/services/http.ts) 的 `getSignSecret` 尚未接线。
   - Redis 依赖 `spring-boot-starter-data-redis` 已存在。

3. **PDF 库尚未引入**：pom 仅有 Bouncy Castle（`bcprov/bcpkix`），**没有任何 PDF 生成/叠加库**，需新增依赖（见 §3.2）。

4. **CI 静态检查未落地**：pom 只有 `spring-boot-starter-test`，**没有 ArchUnit / Checkstyle / Spotless / PMD / JaCoCo**。阶段 3 验收「ArchUnit 通过」需在本阶段补齐 ArchUnit 依赖与规则类。

5. **前端类型非自动生成**：[packages/types](file:///e:/sxassist/packages/types/src) 是手写 TS 类型，无 OpenAPI→TS 代码生成脚本。阶段 3 以「冻结契约 = 唯一事实来源」，人工保持 `@ceams/types` 与 `@ceams/api-client` 与契约严格同步。

6. **既有模式基准**：project 域 [ChangeRequestConverter.java](file:///e:/sxassist/src/main/java/com/ceams/module/project/converter/ChangeRequestConverter.java) 用 `@Mapper(componentModel = "spring")` + **手写 default 方法**（非 MapStruct 注解自动映射）；[PageUtil.java](file:///e:/sxassist/src/main/java/com/ceams/module/project/util/PageUtil.java) 是每域独立实现。阶段 3 各域**沿用 project 域实际写法**保证一致，仅明确复用处抽共享能力。

---

## 1. 阶段 3 目标与范围界定

### 1.1 做什么

在阶段 2 已就绪的骨架（BaseEntity/ApiResponse/PageResult/异常/安全过滤器/审计填充/分页插件）与三域模板之上，完成四个限界上下文的后端全量实现 + Web 全量页面：

- **expense 域**：票据上报（Receipt）、三级审批（ApprovalRecord）、财务核验（ReceiptVerification）、付款关联；`expense/approve` 挂 HMAC 签名防重放。
- **payment 域**：收支流水（Account）、付款账户（PaymentAccount）、付款交易（PaymentTransaction）、成本分摊（CostAllocation）；`payment/execute` 挂 HMAC。
- **bank 域**：银行流水导入（BankStatement）、总账（GeneralLedger）、自动匹配与差异处理（ReconciliationResult）。
- **seal 域**：印章管理（ElectronicSeal）、用印申请（ElectronicSealApplication）、用印审批（StampApproval）、PDF 盖章（ElectronicSealStamp，服务端能力）、使用审计（ElectronicSealUsage）；`seal/stamp` 挂 HMAC。
- **apps/web 四模块页面**：报销、资金、对账、印章。
- **每个域冻结 OpenAPI 契约**到 `docs/openapi/{expense,payment,bank,seal}.openapi.yaml`。
- **共享能力**：统一 HMAC 过滤器/注解、PDF 盖章服务、银行流水通用导入器。

### 1.2 不做什么（明确排除）

- notification 域、report/Dashboard 模块（阶段 4）。
- Electron 桌面端、USB 电子盖章（node-usb + crypto + pdf-lib 叠加）——阶段 4；但 seal 域**服务端 PDF 盖章能力**属于本阶段。
- 微信小程序端（阶段 4）。
- 敏感字段库内加密、接口脱敏、日志脱敏——阶段 4。本阶段敏感字段（`account_number` 等）**明文落库与出参**，仅在 Entity/VO 注释与契约 description 标注「【需加密】（阶段4）」。
- 报表/统计类跨域 XML（bank 自动匹配若需多表统计可写 XML，但 Dashboard/Report 不在范围）。

---

## 2. 依赖与构建顺序

### 2.1 域间依赖

```text
expense ──► payment ──► bank / seal
   ▲            ▲           │
   └── project(已就绪)       └── seal 依赖 project(已就绪)
   └── masterdata(Category/Contact/Material，payment 引用)
```

- **expense** 依赖：`project`、`iam`、`payment`（`biz_receipt_verification.payment_account_id` 引用 `biz_payment_account`——反向依赖点）。
- **payment** 依赖：`expense`（`biz_account.receipt_id`）、`project`、`masterdata`。
- **bank** 依赖：`payment`（`account_id`、`transaction_id`）。
- **seal** 依赖：`project`、`iam`。

> 交叉点处理：`ReceiptVerification`（expense）引用 `PaymentAccount`（payment）。不建跨域物理外键；由 payment 暴露只读 `PaymentAccountQueryService`，expense 仅注入该接口，避免包循环依赖。

### 2.2 推荐构建顺序

1. **共享能力前置**（HMAC 过滤器/注解、nonce Redis、PDF 依赖引入、导入解析器骨架）。
2. **expense 域**（含契约冻结 + HMAC `expense/approve`）。
3. **payment 域**（含契约冻结 + HMAC `payment/execute`；`PaymentAccount` 最小读接口先于 expense 核验使用）。
4. **bank 域**（依赖 payment）。
5. **seal 域**（依赖 project；PDF 盖章能力 + HMAC `seal/stamp`）。
6. **Web 四模块页面**（各域后端冻结后可并行推进）。

### 2.3 各域内部实体实现先后

- **expense**：`Receipt` → `ApprovalRecord` → `ReceiptVerification`。
- **payment**：`PaymentAccount` → `Account` → `PaymentTransaction` → `CostAllocation`。
- **bank**：`BankStatement` → `GeneralLedger` → `ReconciliationResult`。
- **seal**：`ElectronicSeal` → `ElectronicSealApplication` → `StampApproval` → `ElectronicSealStamp` → `ElectronicSealUsage`。

---

## 3. 共享能力规划

### 3.1 HMAC 签名防重放（统一 Filter + 注解，三处复用）

落点均在 `com.ceams.common.sign` 包，不动已有 `HmacSignUtil` 纯函数：

| 组件 | 路径 | 职责 |
|---|---|---|
| `HmacSignUtil` | 已有 | 保留纯函数 |
| `@HmacSigned` 注解 | `com.ceams.common.sign.HmacSigned` | 标注 Controller 方法/类 |
| `HmacSignatureFilter` | `com.ceams.common.sign.HmacSignatureFilter` | `OncePerRequestFilter`，读取三头并常量时间比较 |
| `NonceStore` 接口 | `com.ceams.common.sign.NonceStore` | `tryAcquire(nonce, ttlMillis): boolean` |
| `RedisNonceStore` | `com.ceams.common.sign.RedisNonceStore` | `SET key NX EX`，key=`ceams:sign:nonce:{nonce}` |
| `SignSecretResolver` | `com.ceams.common.sign.SignSecretResolver` | 从 `ceams.sign.secret` 配置解析共享 secret |

校验流程（`SecurityConfig` 中 `addFilterAfter(jwt, UsernamePasswordAuthenticationFilter.class)`）：
1. 仅命中 `@HmacSigned` 标注 handler 时执行。
2. 校验 `X-Timestamp` ±5min 窗口。
3. `NonceStore.tryAcquire(nonce, 窗口)`，失败抛 `SIGN_NONCE_REPLAY`。
4. 重算 `HMAC-SHA256(timestamp+nonce+bodyHash)`，`constantTimeEquals` 比对，失败抛 `SIGN_INVALID`。
5. 失败经 `GlobalExceptionHandler` 转 `ApiResponse.error(SIGN_*, ...)`。

**bodyHash 关键点**：基于已序列化原始请求体字节计算，用 `ContentCachingRequestWrapper` 缓存；空 body 时 bodyHash 为空串（与前端 `sign.ts` 约定一致）。

**secret 下发**：本阶段最小实现——`ceams.sign.secret` 配置注入 + 前端 `GET /api/v1/auth/sign-secret`（短期 JWT 保护）下发；完整「AppSecret 绑定设备」留阶段 4。

**前端**：`http.ts` 的 `getSignSecret` 改为从后端短期密钥接口获取并缓存；api-client 高危方法传 `signed: true`。

### 3.2 PDF 盖章工具（服务端能力，本阶段）

- 依赖选型：默认 **Apache PDFBox 3.x**（Apache-2.0、JDK17 兼容）；备选 OpenPDF；iText 因 AGPL 谨慎。
- 落点：`com.ceams.common.pdf.PdfStampService` 接口 + `com.ceams.module.seal.service.impl.PdfBoxStampServiceImpl` 实现。
- 印章图像从 `biz_electronic_seal.seal_image/seal_file_id` 读取（`storage_type=FILE` 走本地文件/对象存储）；`USB_DRIVE/CLOUD` 本阶段不接。

### 3.3 通用流水导入器（bank 域）

- `com.ceams.module.bank.importer.StatementRow`、`StatementImportParser`（接口）、`CsvStatementImportParser`（CSV）、`StatementImporter`（校验 + 去重 + 批量落库）。（注：`import` 为 Java 保留关键字，故包名用 `importer` 而非 `import`。）
- 列映射配置化，支持表头别名；Excel（.xlsx）留扩展点，本阶段先 CSV。

### 3.4 契约与前端类型一致性

- 每个域先写冻结 OpenAPI，格式复刻 `project.openapi.yaml`。
- 前端 `@ceams/types` 域文件 + `@ceams/api-client` 域工厂与契约字段/枚举严格一一对应，禁止 `any`。
- 顺序：契约冻结 → 类型同步 → api-client → 页面。

---

## 4. 任务拆分

> 编号：`S-*` 共享能力、`E-*` expense、`P-*` payment、`B-*` bank、`L-*` seal、`W-*` Web。所有后端任务遵守：MyBatis-Plus Lambda Wrapper、`ApiResponse`、`@Valid` DTO、`@Transactional(rollbackFor=Exception.class)`、MetaObjectHandler 审计、Converter（沿用 project 写法）、ID/金额/时间契约。

### 4.1 共享能力任务

- **S-1 HMAC 注解与过滤器**：`@HmacSigned` + `HmacSignatureFilter` + `SignSecretResolver`，接入 `SecurityConfig`。完成标准：缺失/过期/重复 nonce/错误签名分别返回 `SIGN_*`，未标注接口不受影响。
- **S-2 nonce 防重放（Redis）**：`NonceStore` + `RedisNonceStore`，替换占位。完成标准：同一 nonce 第二次请求返回 `SIGN_NONCE_REPLAY`，TTL 自动过期。
- **S-3 PDF 依赖引入 + 盖章服务**：pom 引入 PDFBox，实现 `PdfStampService`。完成标准：单测覆盖默认居中/指定坐标/多页叠加，产出合法 PDF。
- **S-4 银行流水导入解析器**：`StatementRow` + `StatementImportParser` + `CsvStatementImportParser`。完成标准：标准 CSV 头解析通过，重复 `bank_reference` 识别。
- **S-5 前端签名密钥接线**：`http.ts` 的 `getSignSecret` 接线 + api-client 高危方法标 `signed: true`。完成标准：三域高危调用携带三头且服务端校验通过。

### 4.2 expense 域任务

- **E-0 冻结契约**：`docs/openapi/expense.openapi.yaml`（三实体 + 枚举 + 审批/核验动作，标注 HMAC）。
- **E-1 Receipt 实体 + 枚举 + Mapper**：对齐 `biz_receipt`；枚举 `ReceiptType/VerificationStatus/PaymentStatus/PaymentMethod/ApprovalLevel`。
- **E-2 ApprovalRecord 实体 + Mapper**：对齐 `biz_approval_record`。
- **E-3 ReceiptVerification 实体 + Mapper**：对齐 `biz_receipt_verification`。
- **E-4 Receipt DTO/VO/Converter**：`@Valid`；VO ID/金额序列化符合契约。
- **E-5 ReceiptService（CRUD + 上报）**：分页过滤、create 默认 PENDING/UNPAID/level0、update/delete 仅草稿/驳回态。
- **E-6 三级审批（HMAC）**：`POST /api/v1/expenses/{id}/approve|reject` 标 `@HmacSigned`；逐级推进、越级/重复审批拦 `BIZ_STATE_CONFLICT`。
- **E-7 财务核验 + 付款关联**：核验通过更新 Receipt；核验金额>票据金额拦；核验前必须已审批通过；经 `PaymentAccountQueryService` 校验账户存在。
- **E-8 Flyway 增量（V5+）**：核对 V1 既有 DDL，仅补齐缺失索引/列。
- **E-9 前端类型 + api-client**：`packages/types/src/expense.ts` + `packages/api-client/src/domains/expense.ts`，approve/reject 标 `signed`。

### 4.3 payment 域任务

- **P-0 PaymentAccount 只读查询服务（跨域前置）**：`exists(id)/getById(id)`，供 expense 注入。
- **P-1 冻结契约**：`docs/openapi/payment.openapi.yaml`（含 `/payment/execute` HMAC 标注）。
- **P-2 PaymentAccount CRUD**：余额增减仅由交易执行驱动，不允许外部直接改；`account_number` 标注「需加密（阶段4）」。
- **P-3 Account（收支流水）CRUD**：引用校验；状态机 DRAFT→CONFIRMED/CANCELLED。
- **P-4 PaymentTransaction CRUD**：关联账户 + receipt + project。
- **P-5 付款执行（HMAC）**：`POST /api/v1/payments/execute` 标 `@HmacSigned`；余额不足拦 `BIZ_UNPROCESSABLE`；余额/交易/票据回写同一事务；幂等（同一 receipt 重复执行拦）。
- **P-6 CostAllocation CRUD**：分摊金额合计 ≤ 记录金额；回写 `allocation_status`。
- **P-7 Flyway 增量（V6+）**。
- **P-8 前端类型 + api-client**：execute 标 `signed`。

### 4.4 bank 域任务

- **B-1 冻结契约**：`docs/openapi/bank.openapi.yaml`。
- **B-2 BankStatement CRUD + 导入接口**：`POST /api/v1/bank/statements/import`（multipart），复用 S-4；去重 `account_id+bank_reference`；返回成功/跳过/失败数。
- **B-3 GeneralLedger CRUD**：`account_number` 标注「需加密（阶段4）」。
- **B-4 自动匹配 + 对账结果**：金额 + 日期±N天 + 户名/流水号匹配，生成 `MATCHED/UNMATCHED/PARTIAL_MATCHED`；差异 `discrepancy_type`；可 `RESOLVED`。
- **B-5 Flyway 增量（V7+）**。
- **B-6 前端类型 + api-client**：导入走 `FormData`。

### 4.5 seal 域任务

- **L-1 冻结契约**：`docs/openapi/seal.openapi.yaml`（含 `/seal/stamp` HMAC 标注）。
- **L-2 ElectronicSeal CRUD**：`storage_type`（FILE/USB_DRIVE/CLOUD）枚举；禁用态不可申请/盖章。
- **L-3 ElectronicSealApplication CRUD**：状态机 PENDING→APPROVED/REJECTED/CANCELLED。
- **L-4 StampApproval CRUD**：审批通过/驳回（reject_reason）。
- **L-5 PDF 盖章（HMAC）**：`POST /api/v1/seals/stamp` 标 `@HmacSigned`；前置校验「已审批通过 + 印章 ACTIVE」；产出盖章 PDF + `stamp_image`；审计落库。
- **L-6 ElectronicSealStamp / ElectronicSealUsage**：盖章记录查询、使用审计分页、`usage_count` 自增。
- **L-7 Flyway 增量（V8+）**。
- **L-8 前端类型 + api-client**：stamp 标 `signed`；PDF 下载接口封好。

### 4.6 Web 页面任务（apps/web）

页面模式复刻 `ProjectView.vue` + `components/{Entity}List.vue`；枚举中文标签加进 `labels.ts`。

- **W-1 路由替换占位**：`routes.ts` 四模块从 `ModulePlaceholder` 指向真实视图。
- **W-2 报销模块页面**：`views/expense/ExpenseView.vue` + Receipt/Approval/Verification 列表。
- **W-3 资金模块页面**：`views/payment/PaymentView.vue` + Account/PaymentAccount/Transaction/CostAllocation 列表。
- **W-4 对账模块页面**：`views/bank/BankView.vue` + Statement（含导入）/Ledger/Reconciliation 列表。
- **W-5 印章模块页面**：`views/seal/SealView.vue` + Seal/Application/Approval/Stamp/Usage 列表。
- **W-6 标签与构建联调**：`labels.ts` 新增四域标签；`npm run build` 零错误零 `any`。

---

## 5. 里程碑

| # | 里程碑 | 验收判据 | 依赖 |
|---|---|---|---|
| M1 | 共享能力就绪 | HMAC + Redis nonce 生效；PDFBox 盖章单测通过；CSV 解析通过；前端密钥接线 | S-1~S-5 |
| M2 | expense 域后端 + 契约冻结 | 三实体 CRUD + 三级审批 + 核验通过；`expense/approve` HMAC 验证通过；Flyway V5 | E-0~E-8 |
| M3 | payment 域完成 | 四实体 CRUD + `payment/execute`（余额事务/幂等/HMAC）；Flyway V6 | P-0~P-7 |
| M4 | bank + seal 域完成 | 流水导入/匹配/差异；印章申请/审批/PDF 盖章（HMAC）/审计；Flyway V7/V8 | B-*、L-* |
| M5 | Web 四模块页面完成 | 四模块可交互；类型与 api-client 接入；web 构建零错误零 `any` | W-1~W-6 |
| M6 | 端到端联调 + 阶段 3 验收 | 三处 HMAC 防重放验证；序列化契约核对；ArchUnit + 前后端构建全通过 | 全部 |

---

## 6. 验收标准

### 6.1 各域功能验收点

- **expense**：票据上报（编号唯一、金额非负、默认 PENDING/UNPAID/level0）；三级审批逐级推进落 `ApprovalRecord`、越级/重复/非待审批态拦 `BIZ_STATE_CONFLICT`；核验（未审批通过不可核验、核验金额>票据金额拦、核验后 VERIFIED + verified_by/at）；`expense/approve` HMAC 防重放/过期/篡改。
- **payment**：账户 CRUD（余额不被外部篡改）；流水 CRUD（引用校验、状态机）；成本分摊（合计 ≤ 记录金额、回写 allocation_status）；`payment/execute`（余额不足拦、同事务回写、幂等、HMAC）。
- **bank**：CSV 导入去重 + 成功/跳过/失败统计；自动匹配生成 `MATCHED/UNMATCHED/PARTIAL_MATCHED`、差异 `discrepancy_type` 准确、可 `RESOLVED`。
- **seal**：印章 CRUD（禁用/归档不可申请盖章）；申请状态机；审批通过/驳回；`seal/stamp`（前置校验 + PDF 盖章合法 + Stamp/Usage 落库 + usage_count 自增 + HMAC）。

### 6.2 整体阶段 3 验收点

1. HMAC 三处高危接口统一经 `HmacSignatureFilter`（非重复实现）；时间窗/nonce/常量时间比较验证通过。
2. 序列化契约：`ApiResponse`/`PageResult`、金额→字符串、ID→字符串、时间 ISO8601、空列表 `[]`。
3. 规范一致性：单表 CRUD 用 Lambda Wrapper；多表匹配才写 XML；写操作 `@Transactional`；DTO `@Valid`；Entity 不出 Controller。
4. 四份 OpenAPI 契约与实现、前端类型一致；无 `any`。
5. Flyway：不改 V1/V2/V4，新增 V5+ 增量；全量 migrate 通过。
6. 构建与架构约束：`mvn clean verify` 通过；**ArchUnit** 通过（Controller 不依赖 Mapper、Entity 不出现于 Controller）；`npm run build` 通过。
7. 敏感字段边界：`account_number` 等本阶段明文，仅注释/契约标注「需加密（阶段4）」。

---

## 7. 风险点与应对

| 风险 | 应对 |
|---|---|
| V1 已建四域表 vs「V5 起建表」假设 | 明确只做「核对 + 增量补齐」，禁止动 V1 |
| PDF 盖章依赖选型 | 默认 PDFBox 3.x，S-3 先行 PoC 单测，失败切 OpenPDF |
| 流水导入格式兼容 | 首期 CSV + 表头别名 + 编码探测（UTF-8/GBK）；模板下载 + 预览校验；失败行单独回报 |
| HMAC 时钟偏移与 nonce 存储 | 时间窗配置化；前端取服务端时间校准；Redis `SET NX EX`，异常时快速失败不放行 |
| bodyHash 与请求体缓存 | `ContentCachingRequestWrapper` 缓存；空 body bodyHash 为空串 |
| 契约与前端类型漂移 | 契约冻结→类型→api-client→页面顺序交付；W 阶段强制 no `any` + build 校验 |
| expense↔payment 反向依赖 | payment 暴露只读 `PaymentAccountQueryService`（P-0 前置） |
| ArchUnit 未落地 | 本阶段补齐 ArchUnit 依赖与规则类；其余静态检查归阶段 4 |
| 敏感字段明文外泄 | 边界明确不加密；注释/契约标注；不新增日志打印敏感字段 |
| HMAC secret 下发 | 最小实现：配置注入 + 短期密钥接口；完整设备绑定留阶段 4 |

---

## 8. 与阶段 4 的交接边界

| 项 | 阶段 3 | 阶段 4 |
|---|---|---|
| notification 域、report/Dashboard | 不实现 | 全量实现 |
| Electron 桌面端 | 不实现 | 桌面壳 + IPC 白名单 |
| USB 电子盖章 | 不实现；仅服务端 PDF 盖章 | 桌面端 USB 读取 + 本地叠加 |
| 微信小程序端 | 不实现 | uni-app + 平台适配层 |
| 敏感字段加密/脱敏 | 不实现；仅注释/契约标注 | 统一加密器 + 脱敏 + 解密读取 |
| AppSecret 设备绑定 | 最小实现：配置注入 + 短期密钥 | 完整设备绑定 + 密钥轮换 |
| Checkstyle/Spotless/PMD/JaCoCo | 只补 ArchUnit | 补齐其余静态检查与覆盖率门槛 |
| Excel（.xlsx）流水导入 | 仅 CSV + 扩展点 | Apache POI 全格式 + 模板 |
| report 跨域只读模块 | 不实现 | 跨域只读聚合 |
