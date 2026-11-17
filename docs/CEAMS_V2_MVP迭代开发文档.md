# CEAMS V2.0 一期 MVP 迭代开发文档

> 文档版本：V1.0（MVP 重写版）
> 生成日期：2026-11-08
> 基线版本：`docs/系统功能清单.md` v1.0
> 部署方式：沿用现有 Docker 与 Flyway 增量迁移，不重建数据库
> 定位：本期只交付"统一审批内核 + 统一待办 + 两个存量业务迁移"，其余业务按 P1/P2 排期

## 变更记录

| 版本 | 日期 | 变更说明 |
|---|---|---|
| V1.0 | 2026-11-08 | 按 MVP 方案重写：范围收敛为 BPM 内核、统一待办、报销与项目变更迁移；拖拽设计器、HR、行政后勤、知识库/OnlyOffice、企微、动态表单引擎移出本期 |

---

## 一、MVP 目标

用最小范围验证一件事：**把分散在各业务里的审批，收口到一套自研流程内核和一个统一待办中心**，并且让报销、项目变更这两条最常用的审批链完整跑通。

MVP 不追求功能大而全，只要求三件事成立：

1. 新发起的报销、项目变更 100% 走 BPM，审批状态只有一个来源；
2. Web 与小程序看到同一份待办，不存在重复待办与双审批入口；
3. 内核具备会签、或签、驳回、撤回、超时提醒能力，且并发安全。

达到后再按依赖顺序扩展 HR、行政、知识库，避免在流程底座未验证前堆业务。

## 二、范围划分

### 2.1 本期做（MVP）

| 模块 | 本期交付内容 | 交付理由 |
|---|---|---|
| BPM 内核 | 模板 JSON 存储与版本、实例与节点执行、会签/或签/抄送、驳回（发起人/上一级）、撤回、管理员终止、超时提醒 | 底座，必须先验证 |
| 流程配置 | Web 端表单化配置器（节点列表、审批人来源、条件行），产出 JSON；**不做拖拽画布** | 满足配置需求且规避设计器高成本 |
| 统一待办 | 我的待办 / 我发起 / 我已办 / 抄送我的，Web + 小程序同源 `bpm_task` | 直接替代分散审批入口 |
| 存量迁移 | 报销申请、项目变更单迁移 BPM，业务表新增 `bpm_instance_id` | 覆盖面最大、规则最清晰 |
| 通知对接 | 待办生成/流转/催办写入现有通知中心，小程序同步 | 不改造通知底座，低成本 |
| 权限与审计 | 新增 BPM 权限点、数据范围（本人/部门/全部）、审批事件与业务变更快照表 | 安全前置条件 |
| 报表 | 仪表盘新增"我的待办数、待办平均处理时长、超时任务数" | 验证内核运行效果 |

### 2.2 本期不做（移出 MVP）

| 模块 | 处理 | 移出理由 |
|---|---|---|
| 可视化拖拽设计器 | P1 | 画布校验、连线合法性、版本对比成本高，先用配置器验证模型 |
| HR 人事全模块 | P1 做档案/合同/入转调离；P2 做排班打卡 | 新领域，与审批底座无强依赖 |
| 薪资、绩效、工资条 | P2 | 核算规则、合规、数据权限均未定稿，风险最高 |
| 行政后勤（资产/耗材/车辆/会议室/访客/报修） | P2 | 六个子系统，独立性强，可后置 |
| 文档知识库 + OnlyOffice | P2 | 需独立容器、回调链路、版本与权限模型，且现网文件服务仅支持单文件 20MB 上传，改造量大 |
| 企业微信集成 | P1 消息推送；P2 组织同步与扫码登录 | 免费 API 有频率限制与权限申请周期，不影响审批闭环 |
| 小程序 JSON-Schema 动态表单引擎 | P1 | 现有 Web 表单并非 Schema 驱动，本期用原生页面覆盖两个迁移场景即可 |
| AI 新增只读工具 | P1 | 本期无新业务域，且需先明确敏感数据隔离红线 |
| 电子印章改造 | P2 | 按业务决策本期保持现状，仅做回归验证，不新增表、不改逻辑 |

## 三、BPM 内核设计

### 3.1 数据表（新增，Flyway V14 起）

| 表 | 关键字段 | 说明 |
|---|---|---|
| `bpm_template` | code、name、biz_type、current_version_id、status | 一种业务对应一个模板 |
| `bpm_template_version` | template_id、version_no、schema_json、status、effective_time | 历史实例沿用创建时版本 |
| `bpm_instance` | template_version_id、biz_type、biz_id、initiator_id、status、started_at、ended_at、version | 一次发起一条 |
| `bpm_node` | instance_id、node_key、node_type、seq_no、status、arrived_at、completed_at、version | 节点实例 |
| `bpm_task` | instance_id、node_id、assignee_id、source_type、task_type、status、due_at、version | 全系统唯一待办来源 |
| `bpm_assignee_snapshot` | node_id、assignee_id、source_type、resolved_at | 固化审批人，防止人员变动导致待办漂移 |
| `bpm_cc_record` | instance_id、node_id、receiver_id、read_status | 抄送 |
| `bpm_variable` | instance_id、var_key、var_value | 业务 ID 与条件变量 |
| `biz_audit_snapshot` | target_type、target_id、action、before_json、after_json、masked_fields、operator_id、trace_id | 现有操作日志无前后数据，必须新增 |

索引要求：`bpm_task(assignee_id, status, due_at)`、`bpm_task(instance_id)`、`bpm_instance(biz_type, biz_id)`、`bpm_node(instance_id, seq_no)`；超时扫描按 `status + due_at` 走索引分片，禁止全表扫描。

### 3.2 流程动作（本期）

| 动作 | 规则 |
|---|---|
| 发起 | 校验模板已启用、发起人有对应业务权限，写实例、首节点、待办与变量 |
| 同意 | 校验任务归属与状态；会签需全部同意才流转，或签任一同意即流转 |
| 驳回 | 支持驳回发起人、驳回上一级；驳回后实例回到目标节点，已办任务归档，原待办关闭 |
| 撤回 | 仅发起人，且仅首节点未处理时可撤回，实例置 WITHDRAWN |
| 终止 | 仅管理员权限，实例置 TERMINATED，关闭所有在途任务 |
| 改派 | 本期由管理员改派，用户自助代理放 P1 |
| 超时 | 定时任务分批扫描，仅发送提醒，不自动流转；自动流转放 P1 |

### 3.3 审批人解析

支持四种来源：指定用户、指定角色、部门主管、发起人直属上级。解析发生在节点到达时，**解析结果写入 `bpm_assignee_snapshot` 并冗余到 `bpm_task`**；解析为空时任务进入 `PENDING_ASSIGNEE` 并通知管理员，不允许生成无人待办。

组织取数口径（V17 起）：

- 部门主管：优先取发起人 `sys_user.dept_id` 对应 `sys_dept.leader_id`；部门主管为空时回退发起人 `sys_user.leader_id`；
- 发起人直属上级：直接取发起人 `sys_user.leader_id`；
- 两者均未维护时解析为空，任务落 `PENDING_ASSIGNEE` 并记录告警。

### 3.4 条件分支

已实现，服务端求值，**不支持脚本**：

- 求值上下文：`bpm_variable` 表单变量 + 发起人角色（`sys_user_role`/`sys_role`）+ 发起人部门（含子部门，`sys_dept.parent_id` 递归）；
- 数值比较：`>=、>、<=、<、==`（兼容 `GE/GT/LE/LT/EQ`，按 `BigDecimal` 比较）；
- 字符串相等：`==、!=`（两侧均可解析为数值时按数值比较）；
- 角色匹配：字段 `initiatorRole/role`，忽略大小写与 `ROLE_` 前缀；
- 部门匹配：字段 `initiatorDept/dept`，支持部门 ID 或部门名称；
- 条件节点按配置顺序取第一个命中分支，全部不命中走 `defaultNext`；普通节点优先按 `nextNodeKey` 路由，未配置时按 `seq_no` 回退；
- 变量或组织数据缺失时降级为“不命中并告警”，不抛系统异常；条件节点缺失默认出口按模板错误（`BPM_SCHEMA_INVALID`）处理，避免静默结束。

### 3.5 并发与幂等

审批动作全部要求客户端传 `taskVersion`，服务端以 `version` 乐观锁更新，冲突返回 `BPM_TASK_CONFLICT`。为避免前端二次查询，待办 / 已办列表接口（`GET /api/v1/bpm/tasks/pending`、`/done`）必须返回任务当前 `taskVersion`（取自 `bpm_task.version`）。同一任务重复提交同一动作返回首次结果，不重复流转。会签场景下"最后一人同意"与"他人驳回"并发时，以先落库的终态为准，其余任务统一关闭。

### 3.6 模板配置器（本期形态）

Web 端表单化配置：节点增删排序、节点类型、审批人来源、驳回策略、超时时长与提醒方式、条件行。保存时做连通性与必填校验，输出 JSON 存 `schema_json`。发布新版本后仅对新实例生效，历史实例锁定原版本。

## 四、存量业务迁移

### 4.1 迁移原则

业务表只保留业务数据，新增 `bpm_instance_id`；**审批状态改为读取 BPM 实例**。为兼容存量，旧状态字段在灰度期内保留写入，但只作为冗余快照，不作为判断依据。

### 4.2 迁移步骤

1. DDL：业务表新增 `bpm_instance_id`、`bpm_migrated` 字段与索引，不动原状态字段；
2. 模板：为报销、项目变更各建一套模板版本并发布；
3. 开关：新增配置 `ceams.bpm.migration.biz-types`，按业务类型灰度；
4. 新单：开关打开后，创建接口只写业务表并发起 BPM 实例，不再走旧审批接口；
5. 存量在途：旧审批接口保留只读与完结能力，结束后写一次 `bpm_instance_id = NULL` 的归档标记，不生成 BPM 实例；
6. 历史已完结：不迁移运行实例，仅保留原记录，列表页按来源统一展示。

### 4.3 一致性校验与回滚

每日对账任务校验"业务表状态快照 = BPM 实例状态"，差异写告警日志；发现异常可关闭业务开关回到旧流程，BPM 侧实例标记作废，不回滚业务数据。

### 4.4 业务副作用

审批终态后的下游动作（报销入账、变更生效调预算）通过 BPM 终态事件触发，采用本地事件表 + 定时投递（outbox），保证业务事务与事件写入同库同事务，避免跨模块双写不一致。

## 五、统一待办中心

接口统一前缀 `/api/v1/bpm`，四个视图共用一套查询：

| 视图 | 数据来源 | 关键规则 |
|---|---|---|
| 我的待办 | `bpm_task` 中 assignee=我 且状态为待处理 | 支持业务类型、发起人、超时状态筛选 |
| 我发起 | `bpm_instance` 中 initiator=我 | 展示当前节点与处理人 |
| 我已办 | 已完成且处理人=我的任务 | 只读，可跳转业务详情 |
| 抄送我的 | `bpm_cc_record` | 未读标记与批量已读 |

待办产生、流转、催办时写入现有通知中心；通知点击深链到待办详情。通知与待办共用未读角标，避免两套红点。

## 六、权限与数据范围

### 6.1 新增权限点（沿用现有 ASCII 命名）

```
bpm:template:view
bpm:template:manage
bpm:instance:view
bpm:task:operate
bpm:task:delegate
bpm:report:view
```

### 6.2 数据范围

在角色上新增 `data_scope`：SELF / DEPT / ALL。本期仅作用于待办、我发起、我已办与审批详情查询；业务表本身的列表权限沿用现有 RBAC，不做全量改造。

本期落地口径（`V18__role_data_scope.sql`）：由角色 `sys_role.data_scope` 控制，读取当前用户全部角色并取最宽范围（ALL > DEPT > SELF）；无角色、空值或非法值一律回退 SELF。DEPT 范围为**同部门及子部门**（按 `sys_dept.parent_id` 递归，无子部门时即同部门）；角色为 SELF 的普通员工即使维护了 `dept_id` 也只能看到本人数据。组织数据缺失时按仅本人兜底，不越权展示。

### 6.3 审计

现有 `sys_operation_log` 不含变更前后数据，本期新增 `biz_audit_snapshot` 并在以下动作写入：发起、同意、驳回、撤回、终止、改派、模板发布。快照字段支持 `masked_fields` 脱敏清单，为后续薪资等敏感模块预留，但**本期不接入任何薪资数据**。

### 6.4 高危接口

BPM 终止、改派、模板发布纳入现有 HMAC 签名保护范围；审批动作走 JWT 鉴权 + 任务归属校验 + 乐观锁三重防护。

## 七、三端改造

| 端 | 本期改动 |
|---|---|
| Web | 新增待办中心页、流程配置页、审批详情抽屉；报销与项目变更列表接入 BPM 状态；仪表盘增加待办指标 |
| 小程序 | 新增待办列表/详情与同意、驳回、撤回；报销、项目变更改为原生表单发起；保留项目查看、通知、AI 助手 |
| Electron | 无业务改动，但因新增页面与下载/跳转入口，需回归外链拦截、下载路径与 USB 印章逻辑；版本随 Web 同步发版 |

## 八、部署与数据变更

1. 新增 Flyway 增量迁移 `V14__bpm_core.sql` ~ `V21__bpm_default_templates.sql`，均为增量、不重建库；`V19__biz_bpm_instance_id.sql` 为业务表加 `bpm_instance_id`；
2. **一次性迁移**（已确认决策）：不保留灰度开关、双写与对账任务，旧审批/核验接口与代码路径全部删除；
3. `V21` 播种默认模板：`EXPENSE`（部门审批 → 财务核验 → 结束）与 `CHANGE_REQUEST`（部门审批 → 结束），并补齐根部门、财务角色与所需权限点；
4. 无新增容器，仅复用现有主服务；定时任务使用独立线程池，配置化分片大小；
5. 密钥仍然只走环境变量，新增配置项全部提供默认值与启动校验。

## 九、里程碑（按顺序，不含工期承诺）

| 里程碑 | 退出标准 |
|---|---|
| M0 设计定稿 | 表结构、模板 JSON Schema、接口契约、权限点评审通过 |
| M1 内核可用 | 会签/或签/驳回/撤回/终止/超时提醒单测与并发测试通过 |
| M2 业务迁移 | 报销、项目变更新单全量走 BPM，旧审批接口已下线（未上线，无存量在途） |
| M3 三端联调 | Web 与小程序待办一致，无重复待办，通知深链可用 |
| M4 上线验收 | 真实库迁移演练通过、冒烟用例通过，一次性切换，无灰度与回滚开关 |

## 十、验收标准

| 维度 | 指标 |
|---|---|
| 覆盖率 | 新发起的报销、项目变更 100% 走 BPM；旧 `approve/reject/verify` 接口已删除 |
| 正确性 | 会签/或签/驳回/撤回/终止用例全通过；业务状态由 `BpmBizCallback` 从实例状态派生，无双状态 |
| 并发 | 50 并发处理同一会签节点无脏状态、无重复流转、无丢任务 |
| 幂等 | 同一任务重复提交返回首次结果，实例状态不变 |
| 性能 | 10 万级 `bpm_task` 下待办列表 P95 < 300ms；超时扫描单批耗时可观测 |
| 一致性 | 待办、通知、业务详情三处状态一致；无重复待办 |
| 审计 | 全部流程动作 100% 落 `biz_audit_snapshot` |
| 安全 | 越权查看他人待办返回 403；终止/改派/模板发布校验 HMAC |

## 十一、风险与应对

| 风险 | 应对 |
|---|---|
| 自研内核边界不清 | 本期动作集合写死，P1 才扩代理与自动流转；超出范围的需求不接 |
| 审批人解析为空 | `PENDING_ASSIGNEE` 状态 + 管理员告警，不生成无人待办 |
| 存量迁移出现双状态 | 不适用：未上线，无存量在途单；旧审批代码路径已删除，审批状态唯一来源为 `bpm_instance` |
| 业务副作用不一致 | 同库 outbox 事件表 + 定时投递，失败重试与告警 |
| 小程序表单重复建设 | 本期只覆盖两个场景的原生表单，P1 再抽 Schema 引擎 |
| 电子印章带病运行 | 本期不触碰逻辑，仅回归；已知问题列入 P2 必改清单并向业务确认风险接受 |

## 十二、后续路线（不在本期开发）

| 阶段 | 内容 |
|---|---|
| P1 | 拖拽设计器、用户自助代理与自动流转、小程序 JSON-Schema 表单引擎、企业微信消息推送、HR 员工档案/劳动合同/入转调离、AI 待办摘要 |
| P2 | HR 排班打卡与薪资绩效工资条、行政后勤六模块、知识库与 OnlyOffice、企业微信组织同步与扫码登录、电子印章迁移 BPM 与漏洞修复 |
| P3 | 培训考试、CRM、采购询价、自定义 BI 报表、硬件考勤机 |

## 十三、已确认决策

| # | 问题 | 结论 |
|---|---|---|
| 1 | 审批链是否按部门自定义 | 一期使用 `V21` 播种的全公司统一模板，自定义留到 P1 设计器 |
| 2 | 存量在途单据如何处理 | 未上线、无存量；采用**一次性迁移**，不保留旧接口与灰度开关 |
| 3 | 超时提醒默认时长与频次 | 模板默认 24h，每任务仅提醒一次（`reminded_at` 标记） |
| 4 | 数据范围 DEPT 口径 | 以 IAM 组织树为准（同部门及子部门） |
| 5 | 报销与财务核验是否合并 | **二者合并**为一条流程：部门审批 → 财务核验 → 结束 |

## 十四、运维手册（P4）

### 14.1 启动前置

| 项 | 要求 |
|---|---|
| JDK | 17+（本机以 `E:\Program Files\Java\jdk-19` 验证；`java` 默认可能为 11，需显式指定） |
| MySQL | 8.0，库 `ceams`；本机可直连 `127.0.0.1:3306`（root/123456），或 Docker 映射 `13306` |
| 环境变量 | `MYSQL_HOST/MYSQL_PORT/MYSQL_DATABASE/MYSQL_USER/MYSQL_PASSWORD`、`AI_ENABLED=false` |
| 构建 | 离线仓库：`mvn -o -DskipTests package`，产物 `target/ceams-2.0.0-SNAPSHOT.jar` |

### 14.2 迁移检查（上线前必做）

1. 启动日志应出现 `Migrating schema ... to version "21 - bpm default templates"` 与 `Successfully applied ... now at version v21`；
2. `SELECT version FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 1;` 结果为 `21`；
3. `SELECT code,name,biz_type,status FROM bpm_template;` 应存在 `EXPENSE`、`CHANGE_REQUEST` 两条且为 `ENABLED`，对应版本 `PUBLISHED`；
4. `SELECT id,dept_id FROM sys_user WHERE username='admin';` 应有归属部门；`sys_user_role` 中 admin 绑定 `SYS_ADMIN` 与 `FINANCE`（单管理员兜底，保证财务核验节点可解析）。

### 14.3 冒烟脚本（已在本机 v13→v21 迁移后实测通过）

| 步骤 | 接口 | 预期 |
|---|---|---|
| 登录 | `POST /api/v1/auth/login`（admin/admin123） | 返回 accessToken，角色含 `SYS_ADMIN`、`FINANCE` |
| 建单 | `POST /api/v1/expenses` | 返回单据 ID |
| 提交 | `POST /api/v1/expenses/{id}/submit` | `approvalStatus=PENDING`，写入 `bpmInstanceId` |
| 部门审批 | `GET /api/v1/bpm/tasks/pending` → `POST /api/v1/bpm/tasks/{taskId}/approve` | 节点 `部门审批` 通过，单据仍 `PENDING` |
| 财务核验 | 同一任务接口再审批一次（`财务核验` 节点） | 单据 `APPROVED` 且 `verificationStatus=VERIFIED` |
| 项目变更 | `POST /api/v1/change-requests` → `/{id}/submit` → 审批一次 | `status=APPROVED`，`approvedBy/approvedAt` 回写 |

> 审批请求体必须携带列表返回的 `taskVersion`（乐观锁），缺失或过期分别返回 `BPM_TASK_CONFLICT`。

### 14.4 通知与超时排查

1. 待办生成/改派/超时/抄送/结束均写 `biz_notification`，`link_url` 形如 `ceams://todo?tab=pending&taskId=..&instanceId=..`；
2. Web 通知列表点击“查看详情”按深链跳 `/todo` 并自动切换页签、打开流程抽屉；
3. 桌面端通过 `ceams://` 协议深链唤起单实例窗口并跳转（主进程 `nav:deep-link` → 渲染层路由）；
4. 超时未提醒：检查 `bpm_task.reminded_at` 是否已置位（每任务只提醒一次）、`BpmTimeoutScheduler` 日志“BPM 超时提醒已发送”。

### 14.5 常见故障

| 现象 | 排查方向 |
|---|---|
| 启动报 `Communications link failure` | MySQL 未启动 / 端口不符（3306 本机、13306 Docker） |
| 启动报 `UnsupportedClassVersionError` | 使用了低版本 `java`，改用 JDK 17+ 全路径启动 |
| 提交审批报 `PENDING_ASSIGNEE` | 发起人无部门主管或财务角色无人，检查 `sys_dept.leader_id`、`sys_user_role` 的 `FINANCE` 绑定 |
| 审批报 `BPM_TASK_CONFLICT` | `taskVersion` 未回传或已被他人处理，刷新列表重试 |
| 待办为空但单据 PENDING | 检查 `bpm_task.assignee_id` 与当前用户 ID 是否一致、`bpm_task.status` 是否 `PENDING` |

## 十五、执行版：Docker 环境落地步骤

> 本章为可逐条执行的版本，区别于第十四章的原则性说明。命令均在 `e:\sxassist` 下以 PowerShell 执行。

### 15.1 环境现状（已实测）

| 组件 | 实测状态 | 连接信息 |
|---|---|---|
| `ceams-mysql` | `mysql:8.0`，`0.0.0.0:13306->3306` | 库 `ceams`，账号 `root/root` |
| `ceams-redis` | `redis:7-alpine`，`0.0.0.0:6379->6379` | 无密码 |
| `sxassist-redis-1` | 已清理（原 compose 遗留的孤儿容器与 `sxassist_default` 网络均已删除） | 现仅保留 `ceams-redis` 一套 |
| 库内 Flyway | 执行前仅 V1、V2；执行后 **v21** | 由 `app` 容器启动时增量迁移 |

### 15.2 路径选择（二选一，不并行）

| 路径 | 适用 | 关键点 |
|---|---|---|
| A. 宿主机直跑 jar | 本次迁移演练（推荐） | 连 `127.0.0.1:13306`，改配置即可生效，排障快 |
| B. compose 容器化运行 | 交付/联调环境 | 需把 `app` 的 `MYSQL_HOST` 改指向 `ceams-mysql`，否则会连到宿主机 3306 |

> 现状差异：`docker-compose.yml` 中 `app` 当前为 `MYSQL_HOST=host.docker.internal`、`MYSQL_PORT=3306`、密码 `123456`，指向宿主机 MySQL；`ceams-mysql` 是独立容器（端口 13306），两者不是同一数据源，执行前必须先统一。

### 15.3 步骤 1：构建产物

```powershell
mvn -o -DskipTests package
Get-ChildItem e:\sxassist\target\*.jar
```

预期：生成 `target/ceams-2.0.0-SNAPSHOT.jar`。离线仓库缺失时报错需先联网补依赖。

### 15.4 步骤 2：以 Docker MySQL 启动

```powershell
$env:MYSQL_HOST='127.0.0.1'; $env:MYSQL_PORT='13306'
$env:MYSQL_DATABASE='ceams'; $env:MYSQL_USER='root'; $env:MYSQL_PASSWORD='root'
$env:AI_ENABLED='false'
& "E:\Program Files\Java\jdk-19\bin\java.exe" -jar e:\sxassist\target\ceams-2.0.0-SNAPSHOT.jar
```

必须显式使用 JDK 17+ 全路径；默认 `java` 若为 11 会报 `UnsupportedClassVersionError`。

### 15.5 步骤 3：核对迁移

```powershell
docker exec ceams-mysql mysql -uroot -proot -D ceams -e "SELECT version,description,success FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 5;"
docker exec ceams-mysql mysql -uroot -proot -D ceams -e "SELECT code,name,biz_type,status FROM bpm_template;"
```

预期：最新版本为 `21`；`EXPENSE`、`CHANGE_REQUEST` 两条模板均为 `ENABLED`，对应版本 `PUBLISHED`。日志应出现 `Successfully applied ... now at version v21`。

### 15.6 步骤 4：接口冒烟

| 顺序 | 请求 | 通过标准 |
|---|---|---|
| 1 | `POST /api/v1/auth/login`（admin/admin123） | 返回 accessToken，角色含 `SYS_ADMIN`、`FINANCE` |
| 2 | `POST /api/v1/expenses` | 返回单据 ID |
| 3 | `POST /api/v1/expenses/{id}/submit` | `approvalStatus=PENDING`，`bpmInstanceId` 非空 |
| 4 | `GET /api/v1/bpm/tasks/pending` | 出现 `部门审批` 节点待办，含 `taskVersion` |
| 5 | `POST /api/v1/bpm/tasks/{taskId}/approve` | 节点通过，单据仍 `PENDING` |
| 6 | 再次审批 `财务核验` 节点 | 单据 `APPROVED` 且 `verificationStatus=VERIFIED` |
| 7 | `POST /api/v1/change-requests` → `/{id}/submit` → 审批 | `status=APPROVED`，`approvedBy/approvedAt` 回写 |

审批请求体示例（`taskVersion` 必传，取自上一步列表返回）：

```json
{ "taskVersion": 0, "comment": "同意" }
```

### 15.7 步骤 5：容器化运行（路径 B，已实施）

数据源统一为**已运行的 `ceams-mysql` / `ceams-redis`**，不新增数据库容器、不改动其数据卷：

1. `docker-compose.yml` 中 `app` 的环境变量已改为容器名直连：

| 变量 | 值 |
|---|---|
| `MYSQL_HOST` / `MYSQL_PORT` | `ceams-mysql` / `3306`（容器内部端口） |
| `MYSQL_DATABASE` / `MYSQL_USER` / `MYSQL_PASSWORD` | `ceams` / `root` / `root` |
| `REDIS_HOST` / `REDIS_PORT` | `ceams-redis` / `6379` |

2. 三个容器同处外部网络 `ceams-net`（一次性执行）：

```powershell
docker network create ceams-net
docker network connect ceams-net ceams-mysql
docker network connect ceams-net ceams-redis
```

3. 构建并启动：

```powershell
docker compose up -d --build app
docker compose logs -f app
```

4. 校验地址为 `http://127.0.0.1:9095/api/v1`（容器内 `SERVER_PORT=9095`）；
5. 原 compose 中的 `redis` 服务已移除，避免再起一个重复 Redis 容器抢占 6379。

### 15.8 执行记录（路径 B 实测结果，2026-11-09）

| # | 事项 | 结果 |
|---|---|---|
| 1 | jar / 镜像构建成功 | ✅ `sxassist-app:latest` |
| 2 | 应用连 `ceams-mysql` 启动成功 | ✅ `Started CeamsApplication in 8.868 seconds` |
| 3 | Flyway 到 V21 | ✅ `Successfully applied 18 migrations ... now at version v21` |
| 4 | 模板 `EXPENSE` / `CHANGE_REQUEST` 均为 ENABLED | ✅ |
| 5 | 报销：部门审批 → 财务核验 → APPROVED/VERIFIED | ✅ `PENDING → PENDING → APPROVED / VERIFIED` |
| 6 | 项目变更：审批 → APPROVED 并回写审批人 | ✅ `approvedBy=1`、`approvedAt=2026-11-09T20:04:04` |
| 7 | 通知 `link_url` 为 `ceams://todo?...` | ✅ `ceams://todo?tab=pending&taskId=..&instanceId=..` |
| 8 | Web/小程序/桌面端三端可打开待办 | 待人工联调（服务端已就绪） |

### 15.9 环境清理记录（2026-11-09，执行完毕）

| 阶段 | 对象 | 处理 | 说明 |
|---|---|---|---|
| 1 | 容器 `sxassist-redis-1` | 已删除 | 原 compose 遗留孤儿容器，`docker compose up -d --remove-orphans` |
| 1 | 网络 `sxassist_default` | 已删除 | 无容器占用 |
| 2 | 卷 `sxassist_ceams-redis-data` | 已删除 | 无容器挂载 |
| 3 | 容器 `ceams-a1-app` | 已删除 | `Exited (255)` 遗留应用容器 |
| 3 | 容器 `practical_shirley` | 已删除 | 5 个月前残留 mysql 容器 |
| 3 | Build Cache | 已回收 4.338GB | `docker builder prune -f` |
| 4 | 卷 `0d701e1f...` | 已删除 | `practical_shirley` 的匿名 mysql 数据卷 |
| 4 | 卷 `sxassist_ceams-backup` | 已删除 | 无容器挂载 |
| 4 | 卷 `sxassist_ceams-mysql-data` | 已删除 | 旧 compose 遗留库数据，与当前 `ceams-mysql` 无关 |
| 4 | 卷 `sxassist_ceams-receipts` | 已删除 | 无容器挂载 |

清理前后对比：

| 类型 | 清理前 | 清理后 |
|---|---|---|
| 容器 | 5（含 2 个已退出、1 个孤儿） | 3（全部运行中） |
| 镜像 | 5.45GB / 5 个 | 2.02GB / 5 个（**未删镜像**） |
| 构建缓存 | 5.07GB | 0.73GB |
| 数据卷 | 8 个 / 429.2MB | 4 个 / 230.8MB（可回收 0） |

清理后运行态：`sxassist-app-1`(9095)、`ceams-mysql`(13306)、`ceams-redis`(6379) 均正常，`POST /api/v1/auth/login` 复检通过；剩余卷 `sxassist_ceams-files`、`sxassist_ceams-logs` 及 `ceams-mysql`、`ceams-redis` 的数据卷均在用。

> 镜像按要求保留，如需释放可后续执行 `docker image prune -a`（会删除非运行中镜像，需评估后再做）。

---

## 十六、P1 能力增强：用户自助代理 + 节点自动通过（2026-11-09 完成）

### 16.1 需求与设计

| 能力 | 设计要点 |
|---|---|
| 用户自助代理 | 委托人设置代理时段与适用范围；任务生成时若审批人处于代理时段，待办自动转交代理人，原审批人保留在 `owner_assignee_id` |
| 节点自动通过 | 模板节点支持 `autoApproveIfInitiator`（审批人仅含发起人时自动通过）与 `autoApproveWhen`（满足条件自动通过），命中后不生成待办，节点直接完成并留痕 |

关键约束：

1. 代理只影响**待处理**任务；已生成的历史任务不追溯改派。
2. 代理人处理后，任务记录仍归属原审批人（`owner_assignee_id`），前端展示“代 XXX”。
3. 自动通过必须存在可解析的审批人，解析为空仍按原逻辑落 `PENDING_ASSIGNEE`，避免吞单。
4. 自动通过任务记为 `APPROVED` 并写审批快照，不发送待办通知；审批状态仍以 `bpm_instance` 为唯一来源。

### 16.2 数据与接口

迁移 `V22__bpm_delegate_and_auto_flow.sql`：

- 新建 `bpm_delegate`（`user_id`、`delegate_id`、`biz_type`、`start_time`、`end_time`、`status`、`reason`）。
- `bpm_task` 新增 `owner_assignee_id`；`bpm_assignee_source` 增加 `PROXY`。

接口（均要求登录，只操作本人代理）：

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/v1/bpm/delegates` | 我的代理记录（含已停用/已过期） |
| GET | `/api/v1/bpm/delegates/candidates?keyword=&limit=` | 代理人候选（启用用户） |
| POST | `/api/v1/bpm/delegates` | 新建代理 |
| POST | `/api/v1/bpm/delegates/{id}/cancel` | 停用代理 |

校验规则：结束时间必须晚于开始时间；不能代理给自己；代理人必须为启用状态；同一委托人重叠时段（范围为全部或同一业务）不可重复设置。

### 16.3 模板配置示例

```json
{
  "nodeKey": "dept_approve",
  "nodeName": "部门审批",
  "nodeType": "APPROVAL",
  "approveMode": "ANY",
  "assignees": [{ "type": "DEPT_LEADER" }],
  "dueHours": 24,
  "autoApproveIfInitiator": true,
  "autoApproveWhen": [
    { "field": "amount", "operator": "LE", "value": "500" }
  ]
}
```

- `autoApproveIfInitiator`：解析出的审批人**全部**为发起人时自动通过（避免自审）。
- `autoApproveWhen`：数组内任一条件命中即自动通过，字段取值来自实例变量（如 `amount`、`receiptType`）。
- 两个配置可同时存在，先判断发起人规则，再判断条件规则。

### 16.4 三端改造

| 端 | 改动 |
|---|---|
| Web | 待办中心新增“代理设置”页签（列表/新建/停用，代理人远程搜索）；待办列表新增“代理”列 |
| 小程序 | 无需改动，待办默认返回代理后的任务 |
| 桌面端 | 无需改动 |

### 16.5 验证结果

| 项 | 结果 |
|---|---|
| 单测 | 257 个全部通过（新增代理转交、仅发起人自动通过、条件自动通过、模板解析共 5 个用例） |
| Flyway | 容器启动自动迁移 `v21 → v22`，`Successfully applied 1 migration` |
| 代理端到端 | admin 设置代理给张三后提交报销：两张待办均落到代理人，`ownerAssigneeId=1`、`sourceType=PROXY`；代理人连续审批后单据 `APPROVED` + `VERIFIED`，节点流转正常 |
| 权限校验 | 非任务处理人调用审批接口返回 403（代理任务仅代理人可处理） |
| 前端门禁 | `@ceams/web` typecheck 通过 |

> 冒烟数据说明：验证用代理记录已停用（`DISABLED`），测试用户 `zhangsan` 与报销单 `TEST-DELEGATE-*` 保留在开发库中。

## 十七、流程拖拽设计器（P1 交付）

### 17.1 交付范围

原 P1 项「可视化拖拽设计器」已落地，替换模板发布弹窗内的纯 JSON 文本编辑：

| 能力 | 说明 |
|---|---|
| 可视化画布 | 纵向节点链，固定「开始 / 结束」端点，中间节点可拖拽排序、上移下移、删除 |
| 节点调色板 | 单人审批、会签、或签、抄送、自动节点；支持拖入画布指定位置，或点击追加到末尾 |
| 属性面板 | 节点名称、节点类型、审批人来源（多行）、处理时限、发起人自动通过开关、条件自动通过条件行 |
| 双模式切换 | 「可视化设计 / JSON 编辑」切换；JSON 是唯一数据源，双向同步 |
| 兼容保护 | 模板含条件分支、自定义跳转或不支持的节点类型时，可视化模式转只读并提示改用 JSON 编辑，避免误存丢失配置 |

### 17.2 组件与改造点

| 文件 | 改动 |
|---|---|
| `apps/web/src/views/bpm/components/FlowDesigner.vue` | 新增设计器组件：调色板 + 画布 + 属性面板，内部维护节点模型并生成 `schema_json` |
| `apps/web/src/views/bpm/BpmView.vue` | 发布弹窗改为双模式，弹窗宽度 1100px；打开弹窗与切回可视化时调用 `load()` 重新解析 |
| `apps/web/src/utils/labels.ts` | 新增 `bpmAssigneeSourceLabels`、`bpmConditionOperatorLabels` |

生成规则：

1. 固定首节点 `start`（START）、末节点 `end`（END），中间节点按画布顺序重排 `seqNo`；
2. 新节点编码自动生成（`node_n`）且只读，历史节点保留原 `nodeKey`；
3. 仅可分配审批人的节点（SINGLE / COUNTERSIGN / OR_SIGN / CC）输出 `assignees`；AUTO 不输出；
4. `autoApproveIfInitiator` 仅在为真时输出，`autoApproveWhen` 仅输出 field/operator 均填写的条件；
5. 审批人来源仅提供解析器已实现的分支：`USER / ROLE / DEPT_LEADER / SUPERIOR`，避免配置出无人待办。

### 17.3 验证结果

| 项 | 结果 |
|---|---|
| 类型检查 | `pnpm --filter @ceams/web typecheck` 通过 |
| 构建 | `pnpm --filter @ceams/web build` 通过（1813 modules，BpmView chunk 40.3 kB） |
| 浏览器验收 | admin 登录 → 流程模板 → EXPENSE 发布新版本：画布正确渲染「发起 / 部门审批 / 财务核验 / 结束」；点击调色板「会签」新增节点；切到 JSON 模式可见 `nodeType: COUNTERSIGN` |
| 存量兼容 | V21 播种模板使用 `APPROVAL + approveMode=ANY`，可视化解析为 `OR_SIGN`，重新发布后语义不变 |

> 说明：设计器仅覆盖线性审批链；条件分支仍走 JSON 编辑，与 3.4 节服务端条件求值能力保持一致。

## 十八、小程序 JSON-Schema 表单引擎（P1 交付）

### 18.1 交付范围

原 P1 项「小程序 JSON-Schema 动态表单引擎」已落地，报销申请与项目变更两张原生表单改为 schema 驱动渲染：

| 能力 | 说明 |
|---|---|
| Schema 渲染 | 按 `sections[].fields[]` 逐字段渲染卡片与控件，页面不再重复编写字段模板 |
| 控件类型 | 单行文本、数字/整数输入、多行文本、日期、枚举下拉、附件上传（拍照/相册 + 文件） |
| 校验 | `required`、`maxLength`、`minimum`/`maximum`、数字/整数格式，返回首条错误文案，由页面 toast 提示 |
| 初始值 | `createFormModel()` 按 `default` 生成表单模型，页面只补充运行时默认值（如报销日期取当天） |
| 附件 | 组件内置选择、上传、移除与地址复制，上传鉴权复用 `platform/upload` 与登录 token |

### 18.2 Schema 结构

Schema 采用 JSON Schema 子集，`type` 描述数据类型，`format` 指定控件形态，`enum` + `enumNames` 描述下拉选项：

```json
{
  "bizType": "EXPENSE",
  "sections": [
    {
      "title": "报销信息",
      "fields": [
        { "key": "amount", "label": "报销金额", "type": "number", "required": true, "minimum": 0.01 },
        {
          "key": "type",
          "label": "票据类型",
          "type": "string",
          "enum": ["TRANSPORTATION", "ACCOMMODATION"],
          "enumNames": ["交通", "住宿"],
          "default": "TRANSPORTATION"
        },
        { "key": "receiptDate", "label": "报销日期", "type": "string", "format": "date", "required": true },
        { "key": "description", "label": "说明", "type": "string", "format": "textarea", "maxLength": 500 }
      ]
    }
  ]
}
```

- `format`：`date` / `textarea` / `attachment`，缺省按 `type` 推断为文本或数字输入；
- `enum` 存在即渲染下拉，`enumNames` 与之一一对应；
- `help` 用于字段下方说明（如附件大小限制）；
- 校验只做前端格式与必填，业务规则仍以服务端为准。

### 18.3 组件与页面改造

| 文件 | 改动 |
|---|---|
| `apps/miniprogram/src/schemas/form-schema.ts` | 新增 schema 类型、注册表 `getFormSchema(bizType)` 与 `createFormModel()` |
| `apps/miniprogram/src/schemas/expense.json` | 报销表单 schema（字段与原页面一致，附件单独分组） |
| `apps/miniprogram/src/schemas/change-request.json` | 项目变更表单 schema |
| `apps/miniprogram/src/components/schema-form/schema-form.vue` | 新增渲染引擎：控件渲染、附件上传、`validate()` |
| `apps/miniprogram/src/pages/expense/create.vue` | 仅保留 schema 装配、模型取值与提交逻辑，字段模板与样式移入引擎 |
| `apps/miniprogram/src/pages/project/change-create.vue` | 同上；保留项目 ID 带入与「保存草稿 / 保存并提交」双动作 |

新增业务表单只需两步：新增 `src/schemas/<biz>.json` 并在 `form-schema.ts` 注册，页面用 `SchemaForm` + `validate()` 即可，无需再写字段模板。

### 18.4 验证结果

| 项 | 结果 |
|---|---|
| 类型检查 | `pnpm --filter @ceams/miniprogram typecheck` 通过 |
| 构建 | `pnpm --filter @ceams/miniprogram build:mp-weixin` 通过，产物含 `components/schema-form/*` 与 `schemas/form-schema.js` |
| 编译产物 | `pages/expense/create`、`pages/project/change-create` 的 json 已注册组件，wxml 引用生效 |
| 行为一致性 | 报销字段、默认值（票据类型取首项、日期取当天）、附件上传与复制地址逻辑与原页面一致；校验从"按钮置灰"改为"点击后提示具体原因" |

> 说明：schema 目前随小程序代码版本管理，未下沉到服务端；后续如需运营在线调整表单，可在保留 `FormSchema` 结构不变的前提下增加模板接口，页面无需改动。

## 十九、企业微信推送与 AI 待办摘要（P1 交付）

### 19.1 交付范围

| 能力 | 说明 |
|---|---|
| 企业微信推送 | 群机器人 webhook + markdown 消息，覆盖待办生成、改派、超时提醒、抄送、实例结束等 BPM 通知；默认关闭，配置后开启 |
| 安全降级 | 未启用、webhook 为空/非法、网络异常、企业微信返回非 0 errcode 时仅记录日志，不抛出、不阻塞审批事务 |
| AI 待办摘要 | 只读汇总当前数据范围内的待办：总数、超时数、今日到期数、业务类型分布、最近到期清单 |
| 摘要生成 | 规则模板兜底；AI 仅做可选润色，且必须通过数字保全校验，否则自动回退模板 |
| 展示入口 | Web AI 助手抽屉「待办摘要」视图；小程序 AI 助手页顶部待办摘要卡片 |

本项不新增数据库迁移，不扩展通知偏好表；企业微信以群为投递目标，不按用户路由。

### 19.2 企业微信配置与降级策略

配置前缀 `ceams.notification.wecom`，均可通过环境变量覆盖：

| 配置项 | 环境变量 | 默认值 | 说明 |
|---|---|---|---|
| `ceams.notification.wecom.enabled` | `WECOM_ENABLED` | `false` | 推送总开关 |
| `ceams.notification.wecom.webhook-url` | `WECOM_WEBHOOK_URL` | 空 | 群机器人地址，仅接受 http/https |
| `ceams.notification.wecom.timeout-millis` | `WECOM_TIMEOUT_MILLIS` | `3000` | 连接/读取超时 |
| `ceams.notification.wecom.max-content-length` | `WECOM_MAX_CONTENT_LENGTH` | `800` | 消息正文最大长度，超出截断 |

实现要点：

1. `WeComPushClient` 使用独立线程池（core=1、max=2、queue=100），避免 webhook 响应慢阻塞审批线程；队列满时拒绝并记录 warn。
2. 消息体固定为 `{"msgtype":"markdown","markdown":{"content":"### 标题\n> 正文\n[查看详情](链接)"}}`；仅 http/https 链接追加「查看详情」。
3. `BpmNotificationPublisher.send(...)` 在站内通知之后调用推送，二者相互独立；站内通知失败也不影响企业微信推送尝试。
4. 生产接入只需提供群机器人 webhook 并设置 `WECOM_ENABLED=true`，无需改动业务代码。

### 19.3 AI 待办摘要接口

`GET /api/v1/ai/todo-summary`，登录即可访问，返回统一 `Result<T>`。

数据口径：

| 项 | 规则 |
|---|---|
| 任务状态 | 仅 `PENDING`，与待办列表一致 |
| 数据范围 | 复用 `BpmSecurityGuard`：ALL 不限、DEPT 同部门及子部门、SELF 仅本人；组织数据缺失时按本人兜底 |
| 超时 | `due_at < 当前时间` |
| 今日到期 | `due_at ∈ [今日 00:00, 明日 00:00)` |
| 业务类型 | `EXPENSE=报销`、`CHANGE_REQUEST=项目变更`，未知编码原样展示 |
| 最近到期 | 按 `due_at` 升序取前 N 条，`N` 由 `ceams.ai.todo-summary.upcoming-limit` 控制（默认 5，最大 20） |

响应字段：

| 字段 | 类型 | 说明 |
|---|---|---|
| `generatedAt` | datetime | 生成时间 |
| `totalPending` | number | 待办总数 |
| `overdue` | number | 已超时数 |
| `dueToday` | number | 今日到期数 |
| `byBizType[]` | array | `bizType` / `label` / `count`，按报销、项目变更、其他的固定顺序 |
| `upcoming[]` | array | `taskId` / `instanceId` / `bizType` / `label` / `initiatorName` / `dueAt` / `overdue` |
| `content` | string | 摘要正文（模板或 AI 润色） |
| `model` | string | `TEMPLATE` 或实际模型名 |
| `aiEnabled` | boolean | AI 能力是否开启 |
| `disclaimer` | string | 固定免责声明 |

AI 润色配置：`ceams.ai.todo-summary.polish-enabled`（环境变量 `AI_TODO_SUMMARY_POLISH_ENABLED`，默认 `true`），仍需 `ceams.ai.enabled=true`；润色异常或数字保全失败均回退模板，`model` 返回 `TEMPLATE`。摘要全过程只读，不落库、不发通知。

### 19.4 前端入口

| 端 | 文件 | 改动 |
|---|---|---|
| 共享类型 | `packages/types/src/ai.ts` | 新增 `AiTodoBizTypeCount`、`AiTodoUpcomingItem`、`AiTodoSummaryVO` |
| 共享 API | `packages/api-client/src/domains/ai.ts` | 新增 `ai.todoSummary()`，映射 `GET /api/v1/ai/todo-summary` |
| Web | `apps/web/src/components/AiAssistant.vue` | 视图扩展为「对话 / 日报摘要 / 待办摘要」；待办视图展示三项统计、业务类型标签、最近到期列表、摘要正文与免责声明 |
| 小程序 | `apps/miniprogram/src/pages/ai/assistant.vue` | 顶部新增待办摘要卡片：默认展示三项统计与业务类型，展开后展示最近到期与正文；加载失败提供重试，不影响对话 |

### 19.5 验证结果

| 项 | 结果 |
|---|---|
| 后端单测 | 全量 269 个通过（新增 `WeComPushClientTest` 6 个、`AiTodoSummaryServiceImplTest` 6 个），0 失败 |
| 推送降级 | 单测覆盖：未启用不发请求、webhook 为空/非 http 跳过、超长截断、errcode 非 0 返回 false、网络异常不抛出 |
| Web | `pnpm --filter @ceams/web typecheck` 通过；`build` 通过（1813 modules） |
| 小程序 | `pnpm --filter @ceams/miniprogram typecheck` 通过；`build:mp-weixin` 通过 |
| 接口冒烟 | 容器重建后登录 admin，`GET /api/v1/ai/todo-summary` 返回 200；`totalPending=0` 与数据库 `bpm_task` 中 `PENDING` 数量一致；`model=TEMPLATE`、`aiEnabled=false`（本地未启用 Ollama） |
| 浏览器验收 | Web 登录后打开 AI 助手 → 待办摘要：三项统计显示 `0 / 0 / 0`，正文以「CEAMS 待办摘要（2026-11-10）」开头，规则汇总展示正常 |

> 说明：企业微信推送默认关闭，本地与测试环境不依赖外网；生产只需配置 `WECOM_WEBHOOK_URL` 并打开 `WECOM_ENABLED`。AI 润色依赖本地 Ollama，不可用时自动回退规则模板，摘要接口始终可用。

