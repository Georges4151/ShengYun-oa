# CEAMS AI 智能化升级方案（本地 Ollama）

> 版本：v1.0 ｜ 生成时间：2026-11-06 ｜ 适用分支：`dev` ｜ 状态：待评审

---

## 0. 结论摘要

- **定位**：AI 只做「理解、检索、生成建议」，**不做任何写操作**；审批、付款、盖章必须人工确认后由既有接口执行。
- **模型**：本机 Ollama + `qwen2.5:7b-instruct`（q4_K_M），纯 CPU/8GB 显存均可运行。
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
| 纯 CPU + 16GB 内存 | `qwen2.5:7b-instruct` q4_K_M | 首字 3~6s，生成约 4~6 tok/s |
| 8GB 显存（如 RTX 3060/4060） | 同上 | 首字 <1s，20~30 tok/s |
| 16GB 显存 | `qwen2.5:14b-instruct` | 效果更好，速度减半 |

### 2.2 模型清单

| 模型 | 用途 | 何时引入 |
|---|---|---|
| `qwen2.5:7b-instruct` | 问答、摘要、审批意见草稿 | **M1 即引入** |
| `qwen2.5:14b-instruct` | 对账分析、风险归因 | M3（显存 ≥16GB 时） |
| `qwen2.5-vl:7b` | 票据/合同图像理解（替代 OCR 链路） | M4 单独评估 |
| `bge-m3` | 知识库检索向量化 | 仅在引入规章制度知识库时 |

### 2.3 Ollama 部署（Windows）

```powershell
# 安装（任选其一）
winget install Ollama.Ollama
# 或从 https://ollama.com/download 下载安装包

# 拉取模型
ollama pull qwen2.5:7b-instruct

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
| GET | `/api/v1/ai/conversations` | 我的会话列表（分页） | 本人 |
| GET | `/api/v1/ai/conversations/{id}/messages` | 会话消息 | 本人 |
| POST | `/api/v1/ai/feedback` | 提交评价 | 本人 |

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
data: {"conversationId":"...","model":"qwen2.5:7b-instruct"}

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

- 日报/周报摘要：定时任务 + 人工确认发送
- 报销预审：金额比对、重复报销检测、要件缺失提示（不做自动驳回）

### M4 对账与合同（按需）

- 银行流水匹配候选 + 理由
- 合同条款抽取与风险提示

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
    model: ${OLLAMA_MODEL:qwen2.5:7b-instruct}
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

降级策略：连续 3 次探活失败 → 状态缓存标记不可用 60s，前端入口提示「AI 服务暂不可用」，主流程不受影响。

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

1. **硬件**：本机是否有独立显卡/显存容量？决定 M1 就上 7B 还是直接 14B。
2. **首批场景**：是否认可 P0 只做「只读问答」，P2 报销预审放到 M3。
3. **可见范围**：STAFF 角色是否只能问本人数据（当前方案按此设计）。
4. **审计保留期**：180 天是否满足财务要求。
