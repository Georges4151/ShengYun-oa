# CEAMS AI 智能化升级方案（本地 Ollama）

> 版本：v1.4 ｜ 生成时间：2026-11-06 ｜ 更新：2026-11-07 ｜ 适用分支：`dev` ｜ 状态：M4 已实施（T13–T14 实测通过）

---

## 0. 结论摘要

- **定位**：AI 只做「理解、检索、生成建议」，**不做任何写操作**；审批、付款、盖章必须人工确认后由既有接口执行。
- **模型**：本机 Ollama + `qwen2.5:7b`（q4_K_M，Ollama 的 7B 即指令微调版），纯 CPU/8GB 显存均可运行。
- **架构**：新增 `ai` 模块，自写轻量 `OllamaClient`（Spring `RestClient` + SSE），**不引入 Spring AI**（其要求 Boot 3.4+，与当前 3.2.5 不兼容）。
- **关键约束**：不把用户问题拼成 SQL 交给模型执行，只允许调用**预注册只读工具**（内部走现有 Service + 权限校验）。
- **降级**：`ceams.ai.enabled=false` 或 Ollama 不可用时，AI 接口返回「未启用」，**主系统所有功能不受影响**。

---

## 1. 目标与边界

### 1.1 业务目标

| 目标 | 量化口径 |
|---|---|
| 减少查数成本 | 财务/项目经理用自然语言取数，无需翻列表与导出 Excel |
| 缩短审批时间 | 报销预审自动给出风险点，审批人只看结论 |
| 提升对账效率 | 银行流水与账目给出匹配候选与理由 |
| 沉淀经营摘要 | 自动生成日报/周报的文字结论 |

### 1.2 明确不做

- 不自动审批、不自动付款、不自动盖章、不自动修改任何业务数据
- 不让模型生成可执行 SQL / 脚本
- 不把完整银行卡号、身份证、密钥送入模型（只给掩码）
- 不承诺离线模型替代人工判断，输出一律标注「AI 建议，请核实」

---

## 2. 硬件与模型选型

### 2.1 本机运行条件

| 机器条件 | 可跑模型 | 实测预期 |
|---|---|---|
| 纯 CPU + 16GB 内存 | `qwen2.5:7b` q4_K_M | 首字 3~6s，生成约 4~6 tok/s |
| 8GB 显存（如 RTX 3060/4060） | 同上 | 首字 <1s，20~30 tok/s |
| 16GB 显存 | `qwen2.5:14b-instruct` | 效果更好，速度减半 |

### 2.2 模型清单

| 模型 | 用途 | 何时引入 |
|---|---|---|
| `qwen2.5:7b`（本机已安装 4.7GB） | 问答、摘要、审批意见草稿 | **M1 即引入** |
| `qwen2.5:14b-instruct` | 对账分析、风险归因 | M3（显存 ≥16GB 时） |
| `qwen2.5-vl:7b` | 票据/合同图像理解（替代 OCR 链路） | M4 单独评估 |
| `bge-m3` | 知识库检索向量化 | 仅在引入规章制度知识库时 |

### 2.3 Ollama 部署（Windows）

```powershell
# 安装（任选其一）
winget install Ollama.Ollama
# 或从 https://ollama.com/download 下载安装包

# 拉取模型
ollama pull qwen2.5:7b

# 常驻服务默认监听 127.0.0.1:11434
ollama serve

# 验证
curl http://localhost:11434/api/tags
```

若需内网其他机器访问，设置 `OLLAMA_HOST=0.0.0.0:11434` 并限制防火墙来源网段。

---

## 3. 总体架构

```
Web / Electron / 小程序
      │  POST /api/v1/ai/chat（SSE 流式）
      ▼
┌──────────────────────────────────────────────┐
│ ai 模块（新增）                                │
│  AiChatController                             │
│   ├─ AiChatService        会话编排、上下文裁剪  │
│   ├─ PromptBuilder        系统提示 + 权限上下文 │
│   ├─ ReadOnlyToolRegistry 只读工具白名单        │
│   │    └─ 调用既有 Service（带权限校验）        │
│   ├─ OllamaClient         /api/chat 流式转发   │
│   └─ AiAuditService       落库审计              │
└──────────────────────────────────────────────┘
      │ HTTP
      ▼
Ollama（本机 11434，可选依赖）
```

### 3.1 分层与现有架构一致性

- 新增模块目录：`src/main/java/com/ceams/module/ai/{controller,service,service/impl,client,dto,vo,tool,config}`
- 不修改 project/expense/payment/bank/seal/report 等业务模块的 Service 签名；工具层通过调用其 **只读查询方法** 取数
- 复用既有：`ApiResponse`、`BusinessException`、`SecurityUtils`、`@PreAuthorize`、Flyway、MyBatis-Plus

### 3.2 为什么自写 OllamaClient

| 方案 | 结论 |
|---|---|
| Spring AI 1.x | 要求 Boot 3.4+，与 3.2.5 不兼容，升级影响全项目 → **不采用** |
| OkHttp/WebFlux | 需引入新依赖 → 不必要 |
| **Spring `RestClient` + `text/event-stream`** | spring-web 6.1 自带，零新增依赖 → **采用** |

---

## 4. 数据模型（Flyway V12）

| 表 | 用途 | 关键字段 |
|---|---|---|
| `ai_conversation` | 会话 | `id, user_id, title, create_time, update_time, deleted` |
| `ai_message` | 单条消息 | `id, conversation_id, role(USER/ASSISTANT/TOOL), content, tool_calls(JSON), model, prompt_tokens, completion_tokens, latency_ms, create_time` |
| `ai_feedback` | 用户评价 | `id, message_id, user_id, score(1~5), remark, create_time` |

要求：
- 全部逻辑删除 + 审计字段沿用项目既有 `BaseEntity` 规范
- `tool_calls` 存 JSON 文本，记录工具名、参数、返回行数（不存全量业务数据）

---

## 5. 接口设计

| 方法 | 路径 | 说明 | 权限 |
|---|---|---|---|
| GET | `/api/v1/ai/status` | 返回 `{enabled, model, reachable, latencyMs}` | 登录用户 |
| POST | `/api/v1/ai/chat` | SSE 流式对话，事件：`meta` / `delta` / `tool` / `done` / `error` | 登录用户 |
| POST | `/api/v1/ai/chat/sync` | 非流式对话（小程序用），一次性返回 `answer` / `toolCalls` / `latencyMs` | 登录用户 |
| GET | `/api/v1/ai/conversations` | 我的会话列表（分页） | 本人 |
| GET | `/api/v1/ai/conversations/{id}/messages` | 会话消息 | 本人 |
| POST | `/api/v1/ai/feedback` | 提交评价 | 本人 |
| GET | `/api/v1/bank/statements/{id}/match-candidates` | 银行流水匹配候选 + 匹配分 + 理由（只读，不自动对账） | SYS_ADMIN/FINANCE |
| GET | `/api/v1/ai/contracts/{id}/risk-hints` | 合同条款要点与风险提示（只读，不写回合同） | 登录用户 |

请求体：

```json
{
  "conversationId": "string | null",
  "message": "本月报销一共多少钱？",
  "context": { "page": "expense-pending" }
}
```

SSE 事件示例：

```
event: meta
data: {"conversationId":"...","model":"qwen2.5:7b"}

event: delta
data: {"text":"本月报销合计 "}

event: tool
data: {"name":"queryExpenseSummary","args":{"period":"2026-11"},"rows":3}

event: done
data: {"messageId":"...","latencyMs":5230}
```

---

## 6. 只读工具清单（P0 首批）

| 工具名 | 能力 | 数据来源 | 权限前置 |
|---|---|---|---|
| `queryProjectSummary` | 按关键词/状态查项目：合同额、实际发生、状态 | `biz_project` | 全员 |
| `queryReceivable` | 项目进度款：已申请/已批准/已支付/逾期 | `biz_progress_payment` | 全员（本人可见范围） |
| `queryExpenseSummary` | 期间报销合计、状态分布、待办数 | `biz_receipt`、`biz_receipt_verification` | STAFF 仅本人，FINANCE/SYS_ADMIN 全部 |
| `queryCashflow` | 期间收入/支出合计与 Top 分类 | `biz_payment_transaction`、`biz_account` | FINANCE/SYS_ADMIN |
| `queryPendingApprovals` | 当前用户待审批清单与金额合计 | `biz_approval_record` | 按角色 |
| `queryBankUnmatched` | 期间未匹配银行流水数与金额 | `biz_bank_statement`、`biz_reconciliation_result` | FINANCE/SYS_ADMIN |
| `queryBudgetUsage` | 项目预算使用率与超支预警 | `biz_budget`、`biz_project` | 全员（本人可见范围） |
| `querySealUsage` | 期间用印次数与用印人分布 | `biz_electronic_seal_usage` | SYS_ADMIN/FINANCE |
| `queryBankMatchCandidates`（M4 扩展，共 9 个） | 单条银行流水的系统侧匹配候选：来源、金额差异、日期差、匹配分、理由 | `biz_bank_statement`、`biz_payment_transaction`、`biz_general_ledger` | SYS_ADMIN/FINANCE |

工具实现硬约束：

1. 每个工具一个类，实现 `ReadOnlyTool` 接口（`name` / `description` / `parameterSchema` / `execute`）
2. `execute` 内部必须复用现有 Service 查询方法，不新写 SQL
3. 返回结果统一限行（默认 ≤200 行），超限截断并提示
4. 工具注册表在启动时校验：禁止出现写操作、禁止反射调用任意 SQL

---

## 7. 分期计划与验收

### M1 基础设施（1 个迭代）

| 任务 | 交付 | 验收 |
|---|---|---|
| T1 | `OllamaClient`（`/api/tags` 探活、`/api/chat` 流式） | 单测 + 本地实测流式返回 |
| T2 | `ceams.ai.*` 配置 + 开关 | `enabled=false` 时接口返回未启用 |
| T3 | `GET /api/v1/ai/status` | Ollama 停止时 `reachable=false`，不抛 500 |
| T4 | Flyway V12 三张表 | 迁移成功，审计可落库 |

### M2 问答助手（2 个迭代）

| 任务 | 交付 | 验收 |
|---|---|---|
| T5 | 8 个只读工具 | 20 条真实问题抽样，数字与库内一致 |
| T6 | `/api/v1/ai/chat` SSE + 多轮上下文 | 首字 ≤8s（CPU），断线可重连 |
| T7 | Web 右侧「AI 助手」抽屉 | 不遮挡主流程，可最小化 |
| T8 | 小程序首页新增「智能助手」入口 | 与既有首页卡片风格一致 |

### M3 摘要与预审（2 个迭代）

| 任务 | 交付 | 验收 |
|---|---|---|
| T9 | 日报摘要生成（报表聚合 → 固定口径文字 + 可选 AI 润色 + 数字保全校验） | 数字与库内一致；润色新增数字即回退模板 |
| T10 | 每日 08:00 定时生成 + 手动「立即生成」+ 漏跑补录 | 定时/手动均可产出；历史补录不发通知 |
| T11 | 报销预审（金额异常 / 重复报销 / 要件缺失） | 只输出提示，不自动驳回；规则单测覆盖 |
| T12 | Web 通知页 + AI 抽屉日报入口 + 报销页预审面板 + 小程序通知展示 | typecheck/build 通过，失败降级不阻断主流程 |

本期落地：日报（周报暂不做）；推送渠道为站内通知 + AI 抽屉（不接外部 IM）。

### M4 对账与合同（已实施）

- 银行流水匹配候选 + 理由（只读，候选仅供人工复核，不自动确认对账）
- 合同条款要点与风险提示（只读，不写回合同）
- 合同文档解析（PDF/Word/扫描件）**本期不做**：`biz_subcontractor_contract` 无合同正文字段、无附件上传/列表接口、无文本抽取能力，待补齐合同附件能力后再评估；本期以结构化字段 + 确定性规则覆盖

---

## 8. 安全与合规

| 项 | 措施 |
|---|---|
| 数据不出内网 | Ollama 仅监听本机/内网，禁止公网暴露 |
| 越权 | 工具执行前用 `SecurityUtils.getCurrentUserId()` + 现有角色校验；STAFF 只能查本人数据 |
| 只读 | 工具层不注入 Mapper 写方法；独立只读数据库账号（可选强化） |
| 提示注入 | 用户输入、工具返回内容均作为「数据」置于独立消息段，禁止拼进系统指令 |
| 幻觉 | 涉及金额的回答必须来自工具结果；系统提示强制「无数据则回答无数据」 |
| 脱敏 | 银行卡号、手机号、身份证只输出掩码 |
| 留痕 | `ai_message` 记录工具调用与耗时，满足财务审计追溯 |

---

## 9. 运维与监控

```yaml
ceams:
  ai:
    enabled: ${AI_ENABLED:false}
    base-url: ${OLLAMA_BASE_URL:http://localhost:11434}
    model: ${OLLAMA_MODEL:qwen2.5:7b}
    timeout-seconds: ${AI_TIMEOUT_SECONDS:120}
    max-context-messages: 10
    max-tool-rows: 200
    stream: true
```

| 指标 | 采集方式 |
|---|---|
| 服务可达性 | `/api/v1/ai/status` 探活 + 定时任务写日志 |
| 响应耗时 | `ai_message.latency_ms` 统计 P95 |
| 失败率 | 调用异常计数（Ollama 超时/连接拒绝） |
| 显存/内存 | Ollama 日志 + `OLLAMA_MAX_LOADED_MODELS=1` |

降级策略：探活结果缓存 60s（成功与失败均缓存，避免前端轮询打爆 Ollama）；失败时 `/api/v1/ai/status` 仍返回 HTTP 200 且 `reachable=false`，前端入口提示「AI 服务暂不可用」，主流程不受影响。探活使用独立短超时（连接 2s / 读取 3s），不会拖慢状态接口。

---

## 10. 风险与回滚

| 风险 | 影响 | 应对 |
|---|---|---|
| 小模型幻觉编造数字 | 决策错误 | 金额只能来自工具；回答附数据来源；界面标注「请核实」 |
| CPU 推理过慢 | 体验差 | SSE 流式 + 加载态；必要时引导使用 14B/GPU 服务器 |
| 显存不足 OOM | 服务崩溃 | 限制同时加载 1 个模型；提供纯 CPU 模式 |
| 依赖本地模型不当 | 上线受阻 | 开关默认关闭，灰度开启 |
| 审计数据膨胀 | 存储增长 | 会话保留 180 天，定时清理任务 |

回滚：`AI_ENABLED=false` + 前端入口隐藏即可，无需回滚代码与数据。

---

## 11. 与现有系统的改动面

| 位置 | 改动 |
|---|---|
| `src/main/java/com/ceams/module/ai/**` | 新增模块（不触碰业务模块） |
| `src/main/resources/db/migration/V12__ai_tables.sql` | 新增三张表 |
| `src/main/resources/application.yml` | 新增 `ceams.ai.*` |
| `apps/web/src/views/**` | 新增 AI 助手抽屉 + 路由入口 |
| `apps/miniprogram/src/pages/**` | 首页新增智能助手入口卡片 |
| `security` | 仅新增路径放行规则（`/api/v1/ai/**` 需登录） |

---

## 12. 评审关注点（需你确认）

1. **硬件（已确认）**：本机为 RTX 4060 Laptop 8GB 显存，按 7B（`qwen2.5:7b`）运行；14B 留待后续换机或独立推理机。
2. **首批场景**：是否认可 P0 只做「只读问答」，P2 报销预审放到 M3。
3. **可见范围**：STAFF 角色是否只能问本人数据（当前方案按此设计）。
4. **审计保留期**：180 天是否满足财务要求。

---

## 13. 实施记录

### M1（2026-11-07，`dev`）

| 项 | 结果 |
|---|---|
| 配置开关 | `ceams.ai.enabled/base-url/model/timeout-seconds/max-context-messages/max-tool-rows/stream`，默认关闭、默认模型 `qwen2.5:7b` |
| OllamaClient | `/api/tags` 探活（连接 2s / 读取 3s）+ `/api/chat` NDJSON 流式，解析逻辑独立可单测 |
| 状态接口 | `GET /api/v1/ai/status` → `{enabled, model, reachable, latencyMs}`，未登录返回 401 |
| 数据表 | Flyway V12 建立 `ai_conversation` / `ai_message` / `ai_feedback`，实测库版本推进到 v12 |
| 审计落库 | `AiAuditService` 创建会话 + 追加消息（`@Transactional`），单测覆盖 |
| 单测 | 22 例全部通过（OllamaClient 11 / AiStatusServiceImpl 5 / AiAuditServiceImpl 6） |
| 实测 | Ollama 停止：HTTP 200 + `reachable=false`；Ollama 运行：HTTP 200 + `reachable=true`（43ms） |
| 格式 | Spotless 对 `module/ai` 零告警（剩余 4 个为 file 模块历史遗留） |

已修复的两个实施期缺陷：① `OllamaClient` 双构造器导致 Spring 无法注入（生产构造器补 `@Autowired`）；② 默认模型名与本机已装模型不一致（改为 `qwen2.5:7b`，避免探活恒为 false）。

下一步 M2：8 个只读工具 + `POST /api/v1/ai/chat`（SSE）+ Web 助手抽屉 + 小程序入口。

### M2（2026-11-07，`dev`）

| 任务 | 结果 |
|---|---|
| T5 工具层 | `module/ai/tool/` 落地 `ReadOnlyTool` / `ToolResult` / `ToolRegistry` / `ToolSecurity` / `ToolArgs` / `ToolPeriod` / `ToolSchemas` / `PageCollector`；8 个只读工具全部注册，启动时校验「非空、唯一、必须在白名单内」，越权工具直接启动失败 |
| T5 口径 | ai 模块内零 SQL、零 Mapper；只复用业务 Service 新增的只读方法（项目、进度款、预算、报销汇总与待办、银行未匹配、用印汇总），返回统一限行（每页 100、最多 2 页，超过 `maxToolRows` 截断） |
| T6 聊天链路 | `POST /api/v1/ai/chat`（`text/event-stream`）：`meta` / `delta` / `tool` / `done` / `error`，`data` 强制单行 JSON；最多 2 轮工具调用，超出后不再下发 `tools` 强制作答 |
| T6 会话 | `GET /api/v1/ai/conversations`、`GET /api/v1/ai/conversations/{id}/messages`、`POST /api/v1/ai/feedback`，均校验会话归属；落库失败不阻断 `done` |
| T6 异步 | `AiChatExecutorConfiguration`：`core=4/max=8/queue=32`，`DelegatingSecurityContextAsyncTaskExecutor` 透传 `SecurityContext`（ThreadLocal 不跨线程） |
| T6 时间语义 | 系统提示注入 `Asia/Shanghai` 当前日期与本月/上月/今年换算；`ToolPeriod.resolve()` 兜底：缺失→当前月、越界→当前月并标记 `periodFallback`、格式非法仍报参数错误 |
| T6 安全护栏 | 数据类问题（报销/项目/预算/流水/审批/用印/金额/期间等关键词或 `yyyy-MM`）首轮若未调用工具即输出数字，服务端直接替换为固定安全提示，杜绝幻觉金额外泄；首轮 delta 先缓冲、校验后再回放 |
| T7 Web | 右侧 420px「AI 助手」抽屉（`modal=false` + 穿透遮罩）、悬浮入口、停止生成、新建会话、`context.page` 透传、未启用/不可达禁用输入；`packages/types` 与 `packages/api-client` 新增 ai 域，`services/ai.ts` 用 `fetch` + `ReadableStream` 解析 SSE，401 复用单飞刷新后重试一次 |
| 实测 | 实测会话「本月报销一共多少钱？」→ `event:tool(queryExpenseSummary, period=2026-11)` → 多帧 `delta` → `done`（latency ≈ 8.3s）；「上月报销多少钱？」→ `period=2026-10`；「今年报销多少钱？」→ `2026-01-01~2026-11-07`；无工具问候走纯文本；会话/消息/评价接口与 `ai_conversation`/`ai_message`/`ai_feedback` 落库均验证通过 |
| T8 小程序 | 首页「常用功能」新增「智能助手」卡片，新增 `pages/ai/assistant` 只读问答页（服务探活、对话气泡、工具调用折叠、失败回填、新会话）；移动端改用 `POST /api/v1/ai/chat/sync` 一次性返回，`packages/types`/`packages/api-client` 同步新增 `AiChatSyncPayload`/`AiChatSyncResult`/`chatSync` |
| 已知限制 | ① qwen2.5:7b 对「那上月呢」这类省略式追问可能不调用工具，已由护栏兜底（不再输出未溯源金额，改为提示明确期间）；② 宿主库业务表为空，数字核对仅覆盖「无数据→0/空」场景，真实金额需在有数据环境复测 |

后续口径备注：Web 端 `lint` 存在既有约 3150 条样式类 warning（非本次引入），本次以 `typecheck` + `build` + `eslint --quiet` 为通过判据；Spotless 全量检查仍有 `module/file` 4 个历史遗留文件违规，`module/ai` 为零告警。

### M3（2026-11-07，`dev`）

| 任务 | 结果 |
|---|---|
| T9 摘要生成 | `V13__ai_daily_summary.sql` 建 `ai_daily_summary`（`summary_date + period_type` 唯一键、审计列/version）；`AiDailySummaryService` 聚合报销汇总、待审批、资金收支（财务明细按日过滤 ACCOUNT/PAYMENT_TRANSACTION）、未对账流水、用印次数，输出固定口径模板；可选 Ollama 润色仅做文字排版，双向数字保全校验（模板数字必须全在、润色不得新增数字），失败/超时/空结果回退 `TEMPLATE` |
| T9 口径 | 指标键固定 10 个；金额 ¥ + 千分位两位小数、净额带符号；分布按固定中文标签输出，无数据为「暂无」；统计日按 Asia/Shanghai 昨日 |
| T10 调度 | `AiSummarySchedulingConfiguration` 条件启用 `@EnableScheduling`；`AiSummaryScheduler` 每日 08:00（`ceams.ai.summary.cron`，zone=Asia/Shanghai）生成昨日并补录窗口内缺失；补录历史只落库不发通知，异常只记 warn |
| T10 接口 | `GET /api/v1/ai/summaries`、`GET /latest`、`GET /{date}`、`POST /generate`；统一 `hasAnyRole('SYS_ADMIN','FINANCE')`；无数据 404；同日重复生成覆盖更新且不重复通知 |
| 通知 | `NotificationService.createAndPublishToUsers` 定向发布（SYSTEM/SENT、幂等）；`UserService.listActiveUserIdsByRoleCodes` 解析 FINANCE/SYS_ADMIN 活跃用户；`UserNotificationVO` 增加 title/content/linkUrl/notificationType/priority/publishTime，批量回填无 N+1；深链 `ceams://ai/summary?date=...` |
| T11 预审 | `ExpensePreAuditService` 只读规则：金额超阈值（`ceams.ai.preaudit.amount-threshold`，默认 5000）、项目预算使用率（≥90% 中、≥100% 高）、同票号重复（高）、同金额同日重复（中）、缺发票附件（高）、核验驳回（高）/待核验（中）；`ReceiptService.findPotentialDuplicates` 领域只读查询（排除自身、限 5 条）；单规则异常只记 warn；只输出提示，绝不自动驳回 |
| T11 接口 | `GET /api/v1/ai/preaudit/receipts?limit=20`、`GET /api/v1/ai/preaudit/receipts/{id}`，`hasAnyRole('SYS_ADMIN','FINANCE')` |
| T12 Web | 通知占位页替换为 `NotificationView`（分页/未读/全部已读/正文折叠/日报深链）；AI 抽屉新增「日报摘要」视图（最新/按日期、刷新、FINANCE/SYS_ADMIN 可立即生成、404 空态）；报销页新增 AI 预审面板（风险标签、免责声明、失败降级不阻断列表） |
| T12 小程序 | 通知列表展示 title/content/publishTime，旧数据回退通知编号；未新增路由 |
| 单测 | 后端 130 例全绿（M3 新增 25 例：摘要 10 + 通知 3 + 预审 12）；Spotless `module/ai` 零违规（仅 file 模块 4 处历史遗留） |
| 前端验证 | types/api-client/web/miniprogram typecheck 通过；web build 通过；eslint --quiet 通过 |
| 实测 | Flyway 迁移至 v13；AI 关闭下 `POST /summaries/generate` 返回 200（昨日、TEMPLATE、10 项 metrics）；重复生成不重复通知；通知 mine 接口返回「经营日报｜昨日」未读 1 条；预审接口 200（空库返回 `[]`）；8082 冒烟后进程已停止 |
| 已知限制 | ① 宿主库业务表为空，日报数字仅覆盖「无数据」口径，真实金额需在有数据环境复测；② 越权 403 未实测（种子仅 admin/SYS_ADMIN，未新建 STAFF 账号）；③ 小程序仅 typecheck，未真机验证 |

实施期修复：① 润色数字护栏由单向升级为双向（拦截模型新增数字）；② 历史补录默认不发通知，避免首次部署通知轰炸；③ 改为先落库摘要再建通知，避免孤立通知。

### M4（2026-11-07，`dev`）

| 任务 | 结果 |
|---|---|
| T13 后端 | `GET /api/v1/bank/statements/{id}/match-candidates`（`hasAnyRole('SYS_ADMIN','FINANCE')`；`daysTolerance` 默认 3 / 范围 0..30，`limit` 默认 5 / 范围 1..10）；`ReconciliationService.matchCandidates` 为纯只读：候选池 = 同账户付款交易 + `accountName` 对齐的总账，且 `abs(日期差) <= daysTolerance`；评分 = 金额（一致 50 / ≤1% 30 / ≤5% 15）+ 日期（同日 25，否则 max(0, 15-5*diff)）+ 摘要（完全相同 15 / 包含 10 / 均空 5）；排序 score↓→dateDiff↑→sourceId↑，返回 `candidateCount`/`truncated`；总账账号掩码 `****+末 4`；`match()` 语义未改动，全程不写库 |
| T13 AI 工具 | 新增第 9 个只读工具 `queryBankMatchCandidates`（仅 FINANCE/SYS_ADMIN，STAFF 直接拒绝；流水不存在返回空结果），首行 summary + 候选行，`reasons` 以「；」拼接；`ToolRegistry.ALLOWED_TOOL_NAMES` 扩至 9，启动校验与 `ToolRegistryTest` 同步通过 |
| T14 后端 | `GET /api/v1/ai/contracts/{id}/risk-hints`（登录用户）；ai 模块零 SQL/零 Mapper，经 `SubcontractorContractService.detail` 取数；输出 10 项 `clausePoints` 与 8 条确定性规则：OVERPAID / EXPIRED_ACTIVE / ENDING_SOON / MISSING_END_DATE / MISSING_ATTACHMENT / HIGH_PAYMENT_RATIO / ZERO_AMOUNT / MISSING_DESCRIPTION；等级降序、`riskLevel` 取最高（无风险 NONE）、固定免责声明「AI 建议，请核实；仅提示，不修改合同」 |
| T14 AI 复述 | 仅当 `ceams.ai.enabled=true` 且 `ceams.ai.contract-risk.ai-note-enabled=true`（默认 true）时调用；强制双向数字保全校验（剔除日期、去千分位逗号），失败/异常/超时一律 `aiNote=null` 并 warn，不阻断接口 |
| T13/T14 前端 | `packages/types` 新增 bank/ai 两组类型；`packages/api-client` 新增 `statementMatchCandidates`、`contractRiskHints`；银行流水列表新增「匹配建议」弹窗（流水摘要 + 免责声明 + 候选表：来源/日期/金额/差异/分数/理由标签、空候选与截断提示）；分包合同列表新增「风险提示」弹窗（条款要点 + 风险等级标签 + AI 说明 + 免责声明） |
| 数据表 | 无新增迁移：两项能力均为实时只读计算，不落库、不缓存 |
| 单测 | 153 例全绿（M4 新增 23 例：匹配候选 8 + 工具 4 + 合同风险 11） |
| 格式 | `mvn spotless:check` 仅 file 模块历史 4 处，本次代码零违规 |
| 前端验证 | types / api-client / web typecheck 通过；web build 通过；`eslint --quiet` 0 error |
| 实测 | 8082 冒烟：`/api/v1/ai/status` 返回 `enabled=false`/`reachable=false`；`GET /bank/statements/1/match-candidates` 与 `GET /ai/contracts/1/risk-hints` 路由与权限生效（空库无记录返回 `404001 BIZ_NOT_FOUND`）；既有列表接口正常 |
| 已知限制 | ① 宿主库业务表为空，候选评分与合同风险的成功路径仅由单测覆盖，未做有数据端到端；② 越权 403 未实测（种子仅 admin/SYS_ADMIN）；③ 合同无正文/附件，未做文档解析（见 §7 M4）；④ 合同风险接口按登录用户开放，与既有分包合同查询口径一致（未做数据范围过滤） |

实施期注意：AI 复述采用严格字面双向数字比对，事实为 `90.00%` 而模型输出 `90%` 时会判不一致并回退 `aiNote=null`，属「宁缺勿编造」预期行为。



