# CEAMS V2.0 一期剩余范围开发文档

> 文档版本：V1.0  
> 编写日期：2026-11-10  
> 依据文档：《CEAMS V2.0 一期迭代开发文档》V1.1、《CEAMS_V2_MVP迭代开发文档》第十八/十九章  
> 当前基线：BPM 内核、报销/项目变更迁移、Web 拖拽设计器、小程序 JSON-Schema 表单、企业微信群机器人推送、AI 待办摘要已完成  
> 目标：补齐一期剩余 HR、行政后勤、知识库、企业微信完整集成、小程序与存量适配

---

## 一、背景与现状

### 1.1 已完成能力

| 模块 | 状态 | 关键产物 |
|---|---|---|
| BPM 内核 | 已交付 | 模板/版本/实例/节点/任务/抄送/变量表，会签、或签、抄送、条件、代理、超时、乐观锁 |
| 存量业务迁移 | 已交付 | 报销、项目变更通过 `bpm_instance_id` 接入；`ExpenseBpmCallback`、`ChangeRequestBpmCallback` |
| Web 流程设计器 | 已交付 | `apps/web/src/views/bpm/components/FlowDesigner.vue` |
| 统一待办中心 | Web 已交付 | `apps/web/src/views/bpm/BpmView.vue` 四类页签 |
| 小程序表单引擎 | 已交付 | `packages` 共享类型 + `components/schema-form`，报销/项目变更两套 schema |
| 企业微信通知 | 最小可用 | 群机器人 markdown 推送，可配置、可关闭、异常降级 |
| AI 待办摘要 | 已交付 | `GET /api/v1/ai/todo-summary`，规则模板 + 可选 Ollama 润色 |

### 1.2 剩余范围（本文档范围）

1. HR 人事：员工档案、岗位、合同、假期、排班、打卡、薪资、绩效、工资核算与工资条。
2. 行政后勤：固定资产、低值易耗品、车辆、会议室、访客、报修。
3. 文档知识库：目录树、分片上传、版本、细粒度权限、OnlyOffice 在线编辑。
4. 企业微信完整集成：组织单向同步、扫码登录、应用消息 + 失败重试队列。
5. 小程序补齐：四类待办、请假/资产领用等动态表单、打卡、工资条、绩效自评。
6. 存量适配：AI 只读工具、报表指标、通知扩展、权限点补齐。

### 1.3 不在一期范围

- 电子印章全套改造、用印申请迁移 BPM（二期）。
- 培训考试、CRM、采购询价、BI 自定义报表（二期）。
- 硬件考勤机实际对接（仅预留接口）。
- BPM 脚本条件、任意节点跳转（二期）。

---

## 二、工程约定

### 2.1 后端结构

- 包结构：`com.ceams.module.<域>`，新增域：`hr`、`admin`、`doc`、`integration`。
- 子包：`controller / service / service.impl / entity / mapper / dto / vo / enums / support / config`。
- 返回：统一 `Result<T>`；分页统一 `PageResult<T>`；错误使用 `ErrorCode`。
- 主键：雪花 ID（`BIGINT`），接口序列化为字符串。
- 审计字段：所有业务表继承 `BaseEntity` 约定：
  `create_time / update_time / create_by / update_by / deleted / version`。
- 枚举字段：`VARCHAR` 存枚举 `name`，与现有模块一致。
- 并发：涉及余额、状态、库存、工资台账的表必须加 `version` 乐观锁。
- 逻辑删除：`deleted` 统一由 MyBatis-Plus 处理；库存、台账类查询显式带 `deleted=0`。
- 迁移：只做增量 Flyway，编号从 `V23` 起（M1 实际落地为 `V23`～`V26`）；不重建库、不修改已执行脚本。
- 敏感字段：复用 `V9__sensitive_encrypt.sql` 的加密 TypeHandler，禁止明文薪资/身份证/银行卡。

### 2.2 前端结构

- Web 路由：`apps/web/src/router/routes.ts` 增加一级路由；菜单可见性由后端 `sys_permission` 控制。
- Web 页面：`apps/web/src/views/<域>/`，复杂页面拆分 `components/`。
- 小程序：`apps/miniprogram/src/pages/<域>/`，统一经 `platform/http.ts` 的 `apiClient` 访问接口。
- 共享类型：`packages/types/src/<域>.ts`，并在 `index.ts` 导出。
- 共享 API：`packages/api-client/src/domains/<域>.ts`，并在 `client.ts` 挂载。

### 2.3 BPM 接入模式

业务接入统一遵循现有报销/项目变更模式：

1. 业务表增加 `bpm_instance_id BIGINT NULL` 与索引。
2. 业务服务发起审批：`BpmInstanceService.start(...)`，回写 `bpm_instance_id`。
3. 业务回调实现 `BpmBizCallback`：
   - `onInstanceFinished`：按 `APPROVED/REJECTED/WITHDRAWN/TERMINATED` 派生业务状态；
   - `onNodeCompleted`：处理节点级副作用（如财务核验、绩效打分汇总）。
4. 新增业务类型写入 `BpmBizType` 枚举，并补充模板种子。
5. 业务状态只做“派生快照”，审批真相以 `bpm_instance.status` 为准。

### 2.4 权限与数据范围

- 菜单/权限点统一写 `sys_permission`，权限码格式 `<域>:<资源>:<动作>`。
- 落地时以库中实际最大值为准：M1 已占用权限 157～163（V20 另占 150/151）、`sys_role_permission` 至 1095；M2 从 164 / 1096 起递增。
- 数据范围：岗位/部门/本人三级；薪资、工资条、绩效评分仅本人或 HR 授权角色可见。
- 所有薪资变更、工资确认、工资条发布写全局操作审计快照（复用 `BizAuditSnapshot`）。

---

## 三、总体里程碑

| 里程碑 | 内容 | 交付标志 | 状态 |
|---|---|---|---|
| M1 | HR 基础：员工、岗位、合同、假期、排班、打卡 | 员工档案可维护，小程序可打卡，补卡可走 BPM | 已完成（V23～V26；后端 294 用例、Web/小程序构建、接口冒烟均通过） |
| M2 | HR 薪资绩效：薪资台账、变动审批、绩效、工资核算、工资条 | 月度工资可核算、确认、发布，员工可查看本人工资条 | 已完成（V27～V32；349 用例；Web/小程序构建与冒烟通过） |
| M3 | 行政后勤：资产、易耗品、车辆、会议室、访客、报修 | 六类业务可闭环，领用/用车/预定/报修接入 BPM | 已完成（V33～V37；11 表、权限 170～177、模板 9011～9015；419 用例；六条全链路冒烟全绿） |
| M4 | 文档知识库 + OnlyOffice | 目录/文件/版本/权限可用，Office 文档可在线编辑回写 | 已完成（V38～V39；464 用例；OnlyOffice 8.1 容器化；六步冒烟与开/关双跑通过） |
| M5 | 企业微信完整集成 | 组织同步、扫码登录、应用消息重试可用 | 已完成（V40～V41；490 用例；Web/小程序类型检查与构建通过；未启用态接口冒烟通过；启用态凭证与真机扫码待上线前补验） |
| M6 | 小程序与存量适配 | 四类待办、HR 自助、AI 工具、报表指标补齐 | 未开始 |

实施顺序：M1 → M2 → M3 → M4 → M5 → M6。M6 中的小程序页面随各里程碑同步开发，最后统一联调。

### 3.1 当前进度与交接（2026-11-17）

**已完成**：M1～M5。后端单测 **490 例 0 失败**；Web `build`（1935 modules）与 `typecheck`、小程序 `typecheck` 均通过；Flyway 已执行到 **v41**。

**M5 已落地清单**

| 项 | 内容 |
|---|---|
| 迁移 | `V40__integration_wecom.sql`（`integration_wecom_sync`、`integration_wecom_message` 及唯一键/索引；`sys_user.wecom_userid`、`sys_dept.wecom_dept_id`）；`V41__wecom_permission_seed.sql`（权限 184 `integration:wecom:view`、185 `integration:wecom:sync`，角色权限 1122/1123 授 SYS_ADMIN） |
| 后端 | `com.ceams.module.integration`：`WeComAppProperties`/`WeComAppConfiguration`/`WeComApiClient`/`WeComTokenCache`/`WeComSyncRunner`/`WeComSyncService(Impl)`/`WeComMessageOutbox`/`WeComMessageDispatcher`/`WeComMessageService(Impl)`/`WeComLoginService(Impl)` + 3 个 Controller；`AuthService.loginByWeCom(code,state,deviceId,deviceName,request)` 复用私有 `issueTokens`；`AuditAction` 新增 `WECOM_SYNC`、`WECOM_MESSAGE_RETRY` |
| 接口 | `POST /api/v1/integration/wecom/sync`、`GET /sync/logs`、`GET /messages`、`POST /messages/{id}/retry`、`GET /api/v1/auth/wecom/qrcode`、`POST /api/v1/auth/wecom/login` |
| 前端 | 类型与 API 域 `packages/types/src/integration.ts`、`packages/api-client/src/domains/integration.ts`；页面 `views/integration/WeComView.vue`、`views/auth/WeComCallbackView.vue`；登录页扫码入口；路由 `/integration/wecom`、`/login/wecom`（白名单匿名）；`usePermission` 登记 184/185 |
| 部署 | `docker-compose.yml` 增加 `WECOM_APP_ENABLED/CORP_ID/AGENT_ID/SECRET/REDIRECT_URI`（默认关闭） |

**待补验（不阻塞 M6）**：①`WECOM_APP_ENABLED=true` + 无效凭证 → `POST /sync` 批次应为 `FAILED` 且 `error_msg` 明确；②真机扫码登录需可信域名与回调白名单。当前 Docker Desktop 守护进程已停止，`sxassist-app-1` 处于停止状态，需先启动 Docker 再补跑。

**下一步（M6）**：第九节目前仍是简版（9.1 小程序补齐、9.2 AI 只读工具、9.3 报表指标、9.4 通知扩展、9.5 验收），需按第八节颗粒度扩写为 9.1～9.11 后再分 P1～P5 实施。M6 若需迁移从 **V42** 起，权限编号从 **186** 起、`sys_role_permission` 从 **1124** 起（以实际库为准）。

**固定约束**：只做增量 Flyway、不改已执行脚本（V23～V41）、不重建库、不 git 提交、中文注释、金额用 `BigDecimal`、薪资/手机号密文存储（`v1:` 前缀）、单测不用 `LambdaQueryWrapper.select(lambda)`、置空用 `UpdateWrapper.set(col, null)`、新建 Mapper 必须带 `@Mapper`。

---

## 四、M1 HR 基础

### 4.1 数据表与权限/模板种子（V23～V26）

**V23__hr_employee_contract.sql**

1. `hr_position` 岗位职级

| 字段 | 类型 | 说明 |
|---|---|---|
| id | BIGINT | 雪花 ID |
| code | VARCHAR(64) | 岗位编码，唯一 |
| name | VARCHAR(128) | 岗位名称 |
| level | VARCHAR(32) | 职级，如 P5/M2 |
| job_family | VARCHAR(64) | 序列：技术/职能/管理 |
| sort_order | INT | 排序 |
| status | VARCHAR(20) | ENABLED/DISABLED |

2. `hr_employee` 员工档案

| 字段 | 类型 | 说明 |
|---|---|---|
| id | BIGINT | 主键 |
| user_id | BIGINT | 绑定 `sys_user.id`，唯一 |
| employee_no | VARCHAR(64) | 工号，唯一 |
| name | VARCHAR(64) | 姓名 |
| gender | VARCHAR(16) | MALE/FEMALE/UNKNOWN |
| birth_date | DATE | 出生日期 |
| id_card_enc | VARCHAR(255) | 身份证密文 |
| phone_enc | VARCHAR(255) | 手机号密文 |
| email | VARCHAR(128) | 邮箱 |
| dept_id | BIGINT | 部门 |
| position_id | BIGINT | 岗位 |
| employment_type | VARCHAR(20) | FULL_TIME/PART_TIME/INTERN |
| hire_date | DATE | 入职日期 |
| probation_end_date | DATE | 试用期结束 |
| regular_date | DATE | 转正日期 |
| leave_date | DATE | 离职日期 |
| status | VARCHAR(20) | PROBATION/ACTIVE/LEAVE/TERMINATED |
| emergency_contact | VARCHAR(64) | 紧急联系人 |
| emergency_phone_enc | VARCHAR(255) | 紧急联系电话密文 |
| id_card_file_id | BIGINT | 证件附件 |
| remark | VARCHAR(500) | 备注 |

3. `hr_contract` 劳动合同台账

| 字段 | 类型 | 说明 |
|---|---|---|
| id | BIGINT | 主键 |
| employee_id | BIGINT | 员工 |
| contract_no | VARCHAR(64) | 合同编号，唯一 |
| contract_type | VARCHAR(20) | FIXED/UNFIXED/INTERN |
| start_date / end_date | DATE | 起止日期 |
| sign_date | DATE | 签订日期 |
| file_id | BIGINT | 合同附件 |
| status | VARCHAR(20) | ACTIVE/EXPIRED/TERMINATED |
| renewal_remind_date | DATE | 续签提醒日期 |
| remark | VARCHAR(500) | 备注 |

**V24__hr_attendance.sql**

4. `hr_leave_quota`：`employee_id / year / leave_type(ANNUAL/SICK/PERSONAL) / total_days / used_days / remaining_days / expire_date / version`，唯一键 `(employee_id, year, leave_type)`。

5. `hr_schedule`：`employee_id / work_date / shift_code / start_time / end_time / schedule_type(FIXED/TEMP) / remark`，索引 `(employee_id, work_date)`。

6. `hr_checkin`：`employee_id / checkin_date / checkin_time / checkin_type(IN/OUT) / latitude / longitude / wifi_ssid / wifi_bssid / address / source(MINI/DEVICE/MANUAL) / status(NORMAL/LATE/EARLY/ABSENT) / remark`，索引 `(employee_id, checkin_date)`。

**V25__hr_permission_seed.sql**

7. HR 菜单与权限点种子：`hr`（157）、`hr:employee:view`（158）、`hr:employee:edit`（159）、`hr:contract:manage`（160）、`hr:schedule:manage`（161）、`hr:attendance:view`（162）、`hr:leave:manage`（163）；仅授予 `SYS_ADMIN`（`sys_role_permission` 1089～1095）。均为 `INSERT ... WHERE NOT EXISTS` 幂等写法。

**V26__bpm_hr_templates.sql**

8. HR 审批模板种子（当前版本随模板一并发布）：
   - `HR_REGULAR` 员工转正（9003 / 9103）：上级审批 → HR 复核。
   - `HR_TRANSFER` 员工调岗（9004 / 9104）：上级审批 → 部门负责人 → HR 复核。
   - `HR_RESIGN` 员工离职（9005 / 9105）：上级审批 → HR 复核。
   - `HR_LEAVE` 员工请假（9006 / 9106）：上级审批 → 天数判断（`days GT 3` 走 HR 复核）。
   - `HR_CHECKIN_SUPPLEMENT` 补卡（9007 / 9107）：上级审批 → HR 复核。

### 4.2 接口

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/v1/hr/employees` | 员工分页，支持部门/状态/关键字 |
| GET | `/api/v1/hr/employees/{id}` | 员工详情 |
| GET | `/api/v1/hr/employees/me` | 当前员工档案 |
| POST/PUT | `/api/v1/hr/employees` | 新增/维护员工（`hr:employee:edit`） |
| POST | `/api/v1/hr/employees/{id}/regular` | 发起转正 BPM |
| POST | `/api/v1/hr/employees/{id}/transfer` | 发起调岗 BPM |
| POST | `/api/v1/hr/employees/{id}/resign` | 发起离职 BPM |
| GET/POST/PUT | `/api/v1/hr/positions` | 岗位维护 |
| GET/POST/PUT | `/api/v1/hr/contracts` | 合同台账；`GET /contracts/expiring` 到期预警 |
| GET | `/api/v1/hr/leave-quotas` | 假期额度查询 |
| PUT | `/api/v1/hr/leave-quotas/{id}` | HR 调整额度 |
| POST | `/api/v1/hr/leave-requests` | 请假申请，发起 BPM |
| GET/POST/PUT | `/api/v1/hr/schedules` | 排班维护/批量排班 |
| POST | `/api/v1/hr/checkins/clock` | 小程序打卡（定位 + WiFi 校验） |
| GET | `/api/v1/hr/checkins/my` | 本人打卡记录 |
| GET | `/api/v1/hr/checkins` | HR 查询全员打卡 |
| POST | `/api/v1/hr/checkins/supplement` | 补卡申请，发起 BPM |

### 4.3 页面

| 端 | 页面 | 功能 |
|---|---|---|
| Web | `views/hr/EmployeeView.vue` | 员工列表、档案抽屉、入转调离入口 |
| Web | `views/hr/ContractView.vue` | 合同台账、到期预警 |
| Web | `views/hr/AttendanceView.vue` | 排班、打卡记录、补卡审批 |
| 小程序 | `pages/hr/checkin.vue` | 定位 + WiFi 打卡、当月记录 |
| 小程序 | `pages/hr/leave-create.vue` | JSON-Schema 请假表单 |
| 小程序 | `pages/hr/profile.vue` | 本人档案、合同、假期额度 |

### 4.4 BPM 集成

新增业务类型与模板：

- `HR_REGULAR`：转正申请，员工提交 → 直属上级 → HR 复核。
- `HR_TRANSFER`：调岗，员工/HR 发起 → 原部门主管 → 新部门主管 → HR。
- `HR_RESIGN`：离职，员工发起 → 直属上级 → HR → 资产归还校验 → 账号回收。
- `HR_LEAVE`：请假，员工发起 → 直属上级（>3 天加 HR）。
- `HR_CHECKIN_SUPPLEMENT`：补卡，员工发起 → 直属上级 → HR。

回调 `HrEmployeeBpmCallback`：

- 离职通过：`hr_employee.status=TERMINATED`，校验 `admin_asset_borrow` 未归还记录；无未归还后调用 IAM 禁用账号。
- 转正通过：`status=ACTIVE`、写 `regular_date`。
- 调岗通过：更新 `dept_id/position_id`，写 IAM 用户部门。

### 4.5 权限点

| 权限码 | 名称 |
|---|---|
| `hr` | HR 人事菜单 |
| `hr:employee:view` | 员工档案查看 |
| `hr:employee:edit` | 员工档案维护 |
| `hr:contract:manage` | 劳动合同管理 |
| `hr:schedule:manage` | 排班管理 |
| `hr:attendance:view` | 考勤查看 |
| `hr:leave:manage` | 假期额度管理 |

### 4.6 验收标准

1. 员工档案与 IAM 用户一一绑定，手机号/身份证接口脱敏、库内密文。
2. 入转调离均可发起 BPM，实例结束后业务状态正确派生。
3. 离职流程存在未归还资产时阻断完成。
4. 小程序打卡记录定位、WiFi、设备来源；非排班日/非工作时段按规则标记异常。
5. 补卡申请可走 BPM 并在通过后补生成打卡记录。

---

## 五、M2 HR 薪资绩效

### 5.1 迁移与表结构（V27～V32）

**V27__hr_salary.sql**

1. `hr_salary` 薪资主台账

| 字段 | 类型 | 说明 |
|---|---|---|
| id | BIGINT | 主键 |
| employee_id | BIGINT | 员工，非空 |
| effective_date | DATE | 生效日期，非空 |
| base_salary_enc | VARCHAR(255) | 基本工资（`SensitiveTypeHandler` 加密） |
| post_salary_enc | VARCHAR(255) | 岗位工资（加密） |
| performance_base_enc | VARCHAR(255) | 绩效基数（加密） |
| allowance_enc | VARCHAR(255) | 固定补贴（加密） |
| status | VARCHAR(20) | ACTIVE/HISTORY，默认 ACTIVE |
| change_apply_id | BIGINT | 来源变动申请 |
| remark | VARCHAR(500) | 备注 |
| active_flag | TINYINT | 生成列：`CASE WHEN status='ACTIVE' THEN 1 ELSE NULL END STORED` |
| 审计字段 | — | `create_time/update_time/create_by/update_by/deleted/version` |

索引：唯一键 `uk_hr_salary_active(employee_id, active_flag)`（`active_flag` 为 NULL 时不参与唯一约束，从而保证「每人仅一条 ACTIVE、可保留任意多条 HISTORY」）；普通索引 `idx_hr_salary_emp(employee_id, effective_date)`。
说明：MySQL 不支持部分唯一索引，故用「可空生成列 + 唯一键」替代原文档的 `uk(employee_id, status)` 方案。

2. `hr_salary_change_apply` 薪资变动申请

| 字段 | 类型 | 说明 |
|---|---|---|
| id | BIGINT | 主键 |
| employee_id | BIGINT | 目标员工 |
| applicant_id | BIGINT | 发起人（`sys_user.id`） |
| change_type | VARCHAR(20) | ADJUST/PROMOTION/CORRECTION |
| before_snapshot | JSON | 变更前快照（加密后的字段值不落 JSON，仅存业务金额明文由服务端脱敏后写入） |
| after_snapshot | JSON | 变更后快照 |
| effective_date | DATE | 期望生效日期 |
| reason | VARCHAR(500) | 申请原因 |
| bpm_instance_id | BIGINT | BPM 实例 |
| status | VARCHAR(20) | DRAFT/APPROVING/APPROVED/REJECTED |
| 审计字段 | — | 同上 |

索引：`idx_hr_salary_change_emp(employee_id, status)`、`idx_hr_salary_change_bpm(bpm_instance_id)`。

**V28__hr_performance.sql**

3. `hr_performance_template` 绩效模板

| 字段 | 类型 | 说明 |
|---|---|---|
| id | BIGINT | 主键 |
| code | VARCHAR(64) | 模板编码，唯一 |
| name | VARCHAR(128) | 模板名称 |
| position_id | BIGINT | 适用岗位，NULL 表示通用 |
| period_type | VARCHAR(20) | MONTH/QUARTER |
| items_json | JSON | `[{name,weight,minScore,maxScore,description}]`，`SUM(weight)=100` |
| score_rules_json | JSON | `[{minScore,maxScore,coefficient}]`，区间需连续且不重叠 |
| total_weight | INT | 冗余校验，写入时校验等于 100 |
| status | VARCHAR(20) | ENABLED/DISABLED |
| 审计字段 | — | 同上 |

索引：唯一键 `uk_hr_perf_tpl_code(code)`；普通索引 `idx_hr_perf_tpl_position(position_id)`。

4. `hr_performance_review` 绩效考评单

| 字段 | 类型 | 说明 |
|---|---|---|
| id | BIGINT | 主键 |
| employee_id | BIGINT | 被考评人 |
| template_id | BIGINT | 模板 |
| period | VARCHAR(16) | 周期，月 `2026-11`、季 `2026-Q4` |
| leader_id | BIGINT | 直属上级（发起时快照，避免组织变更后错位） |
| self_score | DECIMAL(6,2) | 自评总分 |
| self_comment | VARCHAR(1000) | 自评说明 |
| self_submitted_at | DATETIME | 自评提交时间 |
| leader_score | DECIMAL(6,2) | 上级打分 |
| leader_comment | VARCHAR(1000) | 上级评语 |
| final_score | DECIMAL(6,2) | 定稿分数 |
| coefficient | DECIMAL(6,4) | 由 `score_rules_json` 映射的系数 |
| status | VARCHAR(20) | DRAFT/SELF_SUBMITTED/LEADER_DONE/APPROVED/REJECTED |
| bpm_instance_id | BIGINT | BPM 实例 |
| 审计字段 | — | 同上 |

索引：唯一键 `uk_hr_perf_review(employee_id, template_id, period)`；普通索引 `idx_hr_perf_review_status(status)`、`idx_hr_perf_review_bpm(bpm_instance_id)`。

**V29__hr_payroll.sql**

5. `hr_salary_calc` 月度核算台账

| 字段 | 类型 | 说明 |
|---|---|---|
| id | BIGINT | 主键 |
| employee_id | BIGINT | 员工 |
| calc_month | VARCHAR(7) | 核算月，如 `2026-11` |
| base_salary_enc / post_salary_enc | VARCHAR(255) | 取自生效薪资（加密） |
| attendance_days | DECIMAL(5,2) | 应出勤天数 |
| absent_days | DECIMAL(5,2) | 缺勤天数 |
| late_count | INT | 迟到/早退次数 |
| attendance_deduction_enc | VARCHAR(255) | 考勤扣款（加密） |
| performance_coefficient | DECIMAL(6,4) | 绩效系数，默认 1.0 |
| performance_amount_enc | VARCHAR(255) | 绩效工资（加密） |
| allowance_enc | VARCHAR(255) | 补贴（加密） |
| social_insurance_enc / housing_fund_enc / tax_enc | VARCHAR(255) | 社保、公积金、个税（加密） |
| gross_pay_enc / net_pay_enc | VARCHAR(255) | 应发、实发（加密） |
| status | VARCHAR(20) | DRAFT/CONFIRMED/LOCKED |
| confirmed_by / confirmed_at | BIGINT / DATETIME | 确认人/时间 |
| 审计字段 | — | 同上 |

索引：唯一键 `uk_hr_calc(employee_id, calc_month)`；普通索引 `idx_hr_calc_month(calc_month, status)`。

6. `hr_salary_slip` 工资条

| 字段 | 类型 | 说明 |
|---|---|---|
| id | BIGINT | 主键 |
| employee_id | BIGINT | 员工 |
| calc_id | BIGINT | 来源核算记录 |
| calc_month | VARCHAR(7) | 核算月 |
| content_json | TEXT | 工资条明细（金额字段加密后再序列化，避免明细泄露） |
| published_at | DATETIME | 发布时间 |
| read_status | VARCHAR(20) | PENDING/READ |
| read_at | DATETIME | 阅读时间 |
| 审计字段 | — | 同上 |

索引：唯一键 `uk_hr_slip(employee_id, calc_month)`；普通索引 `idx_hr_slip_month(calc_month)`。

**V30__hr_salary_permission_seed.sql**：M2 权限点种子，内容见 5.7 节。

**V31__bpm_hr_salary_templates.sql**：薪资变动/更正审批模板（9008/9108、9009/9109），写法见 5.6 节。

**V32__hr_performance_items.sql**：为 `hr_performance_review` 增量补列 `self_items_json`、`leader_items_json`（明细打分），并新增绩效审批模板（9010/9110）。

**公共约束**：全部金额用 `DECIMAL(18,2)` 计算、`HALF_UP` 保留 2 位，落库前经 `SensitiveTypeHandler` 加密；系数用 `DECIMAL(6,4)`；所有表继承 `BaseEntity` 审计字段；余额、状态、核算类表带 `version` 乐观锁。

### 5.2 后端类清单

包根：`com.ceams.module.hr`，沿用 M1 的 `controller / service / service.impl / entity / mapper / dto / vo / converter / enums / support` 结构。

| 层 | 新增类 |
|---|---|
| enums | `SalaryStatus`、`SalaryChangeType`、`SalaryChangeStatus`、`PeriodType`、`PerformanceReviewStatus`、`PayrollStatus`、`SlipReadStatus` |
| entity | `HrSalary`、`HrSalaryChangeApply`、`HrPerformanceTemplate`、`HrPerformanceReview`、`HrSalaryCalc`、`HrSalarySlip` |
| mapper | 6 个对应 Mapper（`BaseMapper`，复杂查询用注解 SQL） |
| dto | `SalaryCreateRequest`、`SalaryChangeApplyRequest`、`PerformanceTemplateSaveRequest`、`PerformanceSelfRequest`、`PerformanceLeaderRequest`、`PerformanceReviewBatchRequest`、`PayrollCalcRequest`、`PayrollAdjustRequest`、`PayrollConfirmRequest`、`SlipPublishRequest` |
| vo | `SalaryVO`、`SalaryChangeVO`、`PerformanceTemplateVO`、`PerformanceReviewVO`、`SalaryCalcVO`、`SalarySlipVO`、`PayrollCalcResultVO`（含异常员工清单） |
| controller | `HrSalaryController`、`HrPerformanceController`、`HrPayrollController` |
| support | `PayrollCalculator`（纯函数式核算，便于单测）、`SocialInsurancePolicy`（社保/公积金/个税策略，读配置） |
| callback | `HrSalaryBpmCallback`、`HrPerformanceBpmCallback`（实现 `BpmBizCallback`） |

复用项：`BpmInstanceService`、`BpmBizType`（新增 3 个枚举值）、`BpmSecurityGuard`（补 case）、`HrEmployeeService`（取员工/直属上级）、`BizAuditSnapshot`、`SensitiveTypeHandler`、`SensitiveMaskUtil`。

### 5.3 核算规则

**初始化**

1. 入参：`calcMonth`（必填，`yyyy-MM`）、可选 `deptId`、`employeeIds`。
2. 取薪资：`status=ACTIVE` 且 `effective_date <= 当月最后一天`；每员工取 `effective_date` 最大的一条。
3. 幂等：已存在 `DRAFT` 记录则整体重算覆盖；存在 `CONFIRMED/LOCKED` 则跳过并记入 `lockedEmployees`；无生效薪资记入 `skippedEmployees`。返回 `PayrollCalcResultVO`：`total / created / recalculated / skipped / locked / warnings`。

**考勤扣款**

4. 日工资 = `(baseSalary + postSalary) / workingDays`，`workingDays` 默认 21.75，可配。
5. 缺勤扣款 = 日工资 × `absentDays`；`absentDays` 取当月 `hr_checkin.status=ABSENT` 的去重天数。
6. 事假扣款：`hr_leave_request` 中 `leaveType=PERSONAL` 且审批通过的天数 × 日工资。
7. 病假扣款：病假天数 × 日工资 × `sickDeductionRate`（默认 0.4，可配）；年假不扣。
8. 迟到/早退：`late_count` × `lateDeduction`（默认 50 元/次），可配。
9. `attendanceDeduction = 5~8 之和`，上限不超过当月应发工资，超出时截断并记 `warnings`。

**绩效与社保**

10. 绩效系数：取同月 `hr_performance_review.status=APPROVED` 的 `coefficient`；无记录取 1.0；一员工同月多条时取最新周期。
11. 绩效工资 = `performanceBase × coefficient`。
12. 社保 = `clamp(socialBase, socialBaseMin, socialBaseMax) × socialInsuranceRate`，`socialBase` 暂取 `baseSalary + postSalary`，预留独立字段；公积金同理用 `housingFundRate`。

**个税与实发**

13. 应纳税所得额 = `grossPay - socialInsurance - housingFund - taxThreshold`，小于 0 按 0。
14. 个税按内置七级累进表计算，可被 `ceams.hr.payroll.tax-brackets` 覆盖：`0~3000:3%`、`~12000:10%-210`、`~25000:20%-1410`、`~35000:25%-2660`、`~55000:30%-4410`、`~80000:35%-7160`、`>80000:45%-15160`。
15. 应发 `grossPay = baseSalary + postSalary + performanceAmount + allowance`。
16. 实发 `netPay = grossPay - attendanceDeduction - socialInsurance - housingFund - tax`；为负时不置零，记 `warnings`。

**精度与状态**

17. 全链路 `BigDecimal`，中间 4 位、落库 2 位，`RoundingMode.HALF_UP`；禁止 `double`/`float`。
18. 状态机：`DRAFT → CONFIRMED → LOCKED`；`CONFIRMED` 后禁止直接改，仅允许 `CORRECTION` 流程，通过后回到 `DRAFT` 重新确认；`LOCKED` 为月结后状态，M2 不提供解锁接口。
19. 并发：唯一键 `uk_hr_calc(employee_id, calc_month)` 兜底，更新带 `version`；批量核算按 200 条/事务分片。
20. 审计：确认、调整、工资条发布均写 `BizAuditSnapshot`。

配置项（`application.yml` 新增，M1 的 `ceams.hr.checkin` 之后并列）：

```yaml
ceams:
  hr:
    payroll:
      working-days: 21.75
      social-insurance-rate: ${HR_SOCIAL_INSURANCE_RATE:0.105}
      housing-fund-rate: ${HR_HOUSING_FUND_RATE:0.07}
      social-base-min: ${HR_SOCIAL_BASE_MIN:0}
      social-base-max: ${HR_SOCIAL_BASE_MAX:999999}
      tax-threshold: ${HR_TAX_THRESHOLD:5000}
      late-deduction: ${HR_LATE_DEDUCTION:50}
      sick-deduction-rate: ${HR_SICK_DEDUCTION_RATE:0.4}
```

### 5.4 接口

| 方法 | 路径 | 权限 | 说明 |
|---|---|---|---|
| GET | `/api/v1/hr/salaries/my` | 登录 | 本人当前薪资（脱敏） |
| GET | `/api/v1/hr/salaries` | `hr:salary:view` | 薪资台账分页（支持员工/状态/生效月） |
| POST | `/api/v1/hr/salaries` | `hr:salary:change` | 首次建档（仅无 ACTIVE 记录时允许） |
| POST | `/api/v1/hr/salary-changes` | `hr:salary:change` | 发起建/调/更正申请并启动 BPM |
| GET | `/api/v1/hr/salary-changes` | `hr:salary:view` | 变动申请分页 |
| GET | `/api/v1/hr/salary-changes/my` | 登录 | 本人发起记录 |
| GET/POST/PUT | `/api/v1/hr/performance/templates` | `hr:performance:template` | 模板查询/新建/修改 |
| POST | `/api/v1/hr/performance/reviews/batch` | `hr:performance:operate` | 按部门+周期批量生成考评单 |
| GET | `/api/v1/hr/performance/reviews` | `hr:performance:operate` | 考评单分页 |
| GET | `/api/v1/hr/performance/reviews/my` | 登录 | 本人考评单 |
| POST | `/api/v1/hr/performance/reviews/{id}/self` | 登录 | 提交自评并启动 BPM |
| POST | `/api/v1/hr/performance/reviews/{id}/leader` | 登录 | 上级打分（节点任务附带） |
| POST | `/api/v1/hr/performance/reviews/{id}/review` | `hr:performance:operate` | HR 复核定稿 |
| POST | `/api/v1/hr/payroll/calc` | `hr:salary:confirm` | 批量初始化/重算当月核算 |
| GET | `/api/v1/hr/payroll/calc` | `hr:salary:view` | 核算明细分页 |
| PUT | `/api/v1/hr/payroll/calc/{id}` | `hr:salary:confirm` | 手工调整单项（仅 DRAFT） |
| POST | `/api/v1/hr/payroll/confirm` | `hr:salary:confirm` | 按月批量确认（`month` + 可选 `employeeIds`） |
| POST | `/api/v1/hr/payroll/slips/publish` | `hr:salary-slip:export` | 按月生成并发布工资条 |
| GET | `/api/v1/hr/payroll/slips/my` | 登录 | 本人工资条列表（自动置 READ） |
| GET | `/api/v1/hr/payroll/slips/export` | `hr:salary-slip:export` | 导出当月工资台账（xlsx 流） |

分页统一 `PageResult<T>`；错误统一 `BusinessException.of(ErrorCode.…)`，越权由 `@PreAuthorize` 返回 403。

### 5.5 前端

Web 不新增一级路由，在 `apps/web/src/views/hr/HrView.vue` 的页签中追加「薪酬台账」「绩效管理」「工资核算」，与 M1 的员工/合同/考勤页签并列。

| 文件 | 内容 |
|---|---|
| `views/hr/components/SalaryList.vue` | 薪资台账分页、员工筛选、变动申请抽屉（建/调/更正，提交即启动 BPM）、变动记录时间线 |
| `views/hr/components/PerformancePanel.vue` | 模板列表与编辑（指标权重实时校验和为 100、系数区间校验）、按部门+周期批量生成考评单、HR 复核定稿 |
| `views/hr/components/PayrollPanel.vue` | 按月核算（展示 `PayrollCalcResultVO` 的跳过/锁定/告警清单）、明细表格行内调整（仅 DRAFT）、批量确认、工资条发布、导出 xlsx |
| `packages/types/src/hr.ts` | 追加 `SalaryVO`、`SalaryChangeVO`、`PerformanceTemplateVO`、`PerformanceReviewVO`、`SalaryCalcVO`、`SalarySlipVO`、`PayrollCalcResultVO` |
| `packages/api-client/src/domains/hr.ts` | 追加 `salaries`、`salaryChanges`、`performance`、`payroll` 四组方法，沿用现有 `client.hr.*` 命名 |

按钮级显隐用 `v-if="hasPermission('hr:salary:confirm')"` 等，不新增菜单项（`hr` 菜单已由 V25 提供）。

小程序新增两个页面：

| 文件 | 内容 |
|---|---|
| `pages/hr/salary-slip.vue` | 工资条列表与详情，进入列表即调用 `slips/my` 将 `read_status` 置 READ |
| `pages/hr/performance-self.vue` | 绩效自评表单（按模板 `items_json` 渲染打分项、评语），提交后启动 BPM |
| `pages.json` / `pages/index/index.vue` | 注册路由并在首页增加「工资条」「绩效自评」入口 |

### 5.6 BPM 集成（模板 9008～9010 / 版本 9108～9110，脚本 V31）

| 业务类型 | 模板/版本 id | 流程节点 |
|---|---|---|
| `HR_SALARY_CHANGE` | 9008 / 9108 | 发起 → 财务复核（ROLE=FINANCE）→ 总经理审批（ROLE=SYS_ADMIN）→ 结束 |
| `HR_SALARY_CORRECTION` | 9009 / 9109 | 发起 → 财务复核（ROLE=FINANCE）→ 结束 |
| `HR_PERFORMANCE` | 9010 / 9110 | 发起 → 直属上级打分（SUPERIOR）→ HR 复核（ROLE=SYS_ADMIN）→ 结束 |

约定：

1. 模板均 `approveMode=ANY`、`dueHours=24`，与 M1 一致；种子脚本幂等（按 `code` 与 `(template_id, version_no)` 判重）。
2. 发起即写入业务表（自评分数、薪资快照），BPM 只承载审批流转；`BpmBizType` 新增 3 个枚举值，`BpmSecurityGuard` 补对应 case。
3. 流程变量：`employeeId`、`changeType`、`beforeSalary`、`afterSalary`、`period`、`finalScore`、`coefficient`。

回调：

| 回调 | 通过（APPROVED） | 驳回/撤回/终止 |
|---|---|---|
| `HrSalaryBpmCallback`（建/调） | 旧 `hr_salary.status=HISTORY` → 插入新 `ACTIVE`（回填 `change_apply_id`）→ 申请置 `APPROVED` → 写 `BizAuditSnapshot` | 申请置 `REJECTED`，不动薪资台账 |
| `HrSalaryBpmCallback`（更正） | 更新对应 `hr_salary_calc` 字段，状态回到 `DRAFT`，写审计 | 申请置 `REJECTED`，核算记录不变 |
| `HrPerformanceBpmCallback` | 写入 `final_score / coefficient`，`status=APPROVED` | `status=REJECTED`，允许重新发起 |

### 5.7 权限点（V30__hr_salary_permission_seed.sql）

沿用 V25 的显式 ID 与幂等写法；`sys_permission` 从 164 起（M1 已占用至 163），`sys_role_permission` 从 1096 起（已占用至 1095）。

| ID | 权限码 | 类型 | 授予角色 |
|---|---|---|---|
| 164 | `hr:salary:view` | API | SYS_ADMIN、FINANCE |
| 165 | `hr:salary:change` | BUTTON | SYS_ADMIN |
| 166 | `hr:salary:confirm` | BUTTON | SYS_ADMIN |
| 167 | `hr:performance:template` | BUTTON | SYS_ADMIN |
| 168 | `hr:performance:operate` | BUTTON | SYS_ADMIN |
| 169 | `hr:salary-slip:export` | BUTTON | SYS_ADMIN |

`sys_role_permission`：1096(SYS_ADMIN,164)、1097(1,165)、1098(1,166)、1099(1,167)、1100(1,168)、1101(1,169)、1102(FINANCE=2,164)。

说明：`hr:salary-slip:my` 与本人查看类接口不做权限点，仅要求登录，避免普通员工被菜单权限拦住。

### 5.8 测试与验收

单测（新增约 20 例，目标总数 ≥ 314）：

1. `PayrollCalculator`：正常月、缺勤、事假、病假、迟到、绩效系数 0.8/1.2、个税跨档、实发为负告警。
2. 薪资台账：首次建档、重复建档拦截、变动通过后旧记录转 HISTORY、唯一 ACTIVE 约束。
3. 绩效：模板权重和 ≠ 100 拦截、系数区间不连续拦截、自评→上级→HR 全链路状态流转。
4. 核算：重复初始化幂等、CONFIRMED 不可改、LOCKED 跳过、唯一键冲突回滚。
5. 工资条：发布幂等、本人可见、已读回写、越权访问他人返回 403。

接口冒烟（沿用 M1 脚本方式）：登录 → 建薪资 → 发起变动 → 查申请 → 建绩效模板 → 批量考评 → 核算 → 确认 → 发布工资条 → 本人读取。

验收标准：

1. 普通员工只能读取本人薪资、工资条、绩效，越权返回 403。
2. 薪资禁止直接编辑，所有变更均有 BPM 实例与审计快照。
3. 核算结果与手工样例一致；确认后不可改，更正必须走 BPM。
4. 工资条发布后小程序可见，已读状态可回写。
5. 薪资字段库内密文、接口脱敏（复用 M1 已验证的 `SensitiveTypeHandler` + `SensitiveMaskUtil`）。

### 5.9 实施步骤

| 任务包 | 内容 | 完成标准 | 状态 |
|---|---|---|---|
| P1 迁移 | V27/V28/V29 建表 + V30 权限种子 | `flyway migrate` 成功，表与索引齐全 | 已完成（v30） |
| P2 薪资台账 | 实体/Mapper/枚举/DTO/VO + 台账与建档接口 + 变动 BPM 发起 + 回调 + V31 模板 | 建档、变动通过与驳回三态单测通过 | 已完成（v31，303 用例） |
| P3 绩效 | 模板校验、批量生成考评单、自评/上级/复核、回调 + V32 | 全链路状态流转单测通过 | 已完成（v32，320 用例） |
| P4 工资核算 | `PayrollCalculator` + 初始化/调整/确认 | 计算样例与手算一致，幂等成立 | 已完成（338 用例；手工调整后个税自动重算） |
| P5 工资条 | 生成发布、已读回写、xlsx 导出 | 发布/读取/导出冒烟通过 | 已完成（349 用例；零依赖 xlsx） |
| P6 前端与联调 | Web 三个组件 + 共享类型/API + 小程序两页 | typecheck/build 通过，冒烟全绿 | 已完成（349 用例；Web 三页签 + 小程序工资条/绩效自评；VO 增加 templateItems 解决自评取指标） |

### 5.10 风险与待确认

| 项 | 说明 | 建议 |
|---|---|---|
| 社保/公积金口径 | 各城市基数与比例不同，且需年度调整 | M2 先按配置全局比例，个人差异走手工调整 |
| 个税 | 专项附加扣除、累计预扣未覆盖 | 内置简易累进表，差额由 HR 调整，二期接完整算法 |
| 财务角色 | 流程依赖 `FINANCE` 角色（id=2） | 已存在，需确认有在职用户绑定该角色，否则任务落 PENDING_ASSIGNEE |
| 工资条明细加密 | `content_json` 内含全部金额 | 先按「序列化后整体加密」实现，前端解密依赖接口返回而非直读库 |
| 批量规模 | 千人级核算耗时 | 200 条/事务分片，接口返回汇总结果，避免长事务 |
| 调岗与薪资联动 | 调岗通过后是否自动触发调薪 | M2 不自动联动，由 HR 另起 `PROMOTION` 申请 |

---

## 六、M3 行政后勤

### 6.1 迁移与表结构（V33～V37）

> 编号修正：行政流程模板由原计划的 `V41` 提前为 `V37`。原因：Flyway 默认 `outOfOrder=false`，若 `V41` 先执行，后续 M4/M5 的 `V37～V40` 会因版本号小于当前版本被拒绝。M4 顺延为 `V38～V39`、M5 顺延为 `V40～V41`，历史编号一律不动。

**V33__admin_asset_material.sql**

1. `admin_asset` 资产台账

| 字段 | 类型 | 说明 |
|---|---|---|
| id | BIGINT | 主键 |
| asset_no | VARCHAR(64) | 资产编号，唯一，服务端按 `AST-yyyyMM-####` 生成 |
| name | VARCHAR(128) | 资产名称，非空 |
| category | VARCHAR(32) | 分类：ELECTRONIC/FURNITURE/VEHICLE_TOOL/OTHER |
| specification | VARCHAR(255) | 规格型号 |
| purchase_date | DATE | 购置日期 |
| purchase_price | DECIMAL(14,2) | 购置金额 |
| dept_id | BIGINT | 归属部门（逻辑引用 `sys_dept.id`） |
| keeper_id | BIGINT | 保管人（`sys_user.id`） |
| location | VARCHAR(128) | 存放位置 |
| status | VARCHAR(20) | IN_STOCK/BORROWED/REPAIRING/SCRAPPED |
| depreciation_months | INT | 折旧月数 |
| remark | VARCHAR(500) | 备注 |
| 审计字段 | — | `create_time/update_time/create_by/update_by/deleted/version` |

索引：唯一键 `uk_admin_asset_no(asset_no)`；普通索引 `idx_admin_asset_status(status)`、`idx_admin_asset_dept(dept_id)`、`idx_admin_asset_keeper(keeper_id)`。

2. `admin_asset_borrow` 资产领用/借还记录

| 字段 | 类型 | 说明 |
|---|---|---|
| id | BIGINT | 主键 |
| asset_id | BIGINT | 资产 |
| borrower_id | BIGINT | 借用人（`sys_user.id`） |
| borrow_date | DATE | 领用日期，非空 |
| expected_return_date | DATE | 预计归还日期 |
| actual_return_date | DATE | 实际归还日期 |
| bpm_instance_id | BIGINT | BPM 实例 |
| status | VARCHAR(20) | APPROVING/BORROWING/RETURNED/REJECTED（提交即 APPROVING，回调推进） |
| remark | VARCHAR(500) | 备注 |
| 审计字段 | — | 同上 |

索引：`idx_admin_asset_borrow_asset(asset_id, status)`、`idx_admin_asset_borrow_user(borrower_id)`、`idx_admin_asset_borrow_bpm(bpm_instance_id)`。

3. `admin_material` 易耗品

| 字段 | 类型 | 说明 |
|---|---|---|
| id | BIGINT | 主键 |
| code | VARCHAR(64) | 物料编码，唯一 |
| name | VARCHAR(128) | 名称 |
| specification | VARCHAR(255) | 规格 |
| unit | VARCHAR(16) | 单位（个/盒/包） |
| stock_qty | DECIMAL(12,2) | 当前库存，非空默认 0，禁止为负 |
| safety_stock | DECIMAL(12,2) | 安全库存，默认 0 |
| unit_price | DECIMAL(14,2) | 参考单价 |
| status | VARCHAR(20) | ENABLED/DISABLED |
| 审计字段 | — | 同上 |

索引：唯一键 `uk_admin_material_code(code)`；普通索引 `idx_admin_material_status(status)`。

4. `admin_material_apply` 易耗品申领单

| 字段 | 类型 | 说明 |
|---|---|---|
| id | BIGINT | 主键 |
| material_id | BIGINT | 物料 |
| applicant_id | BIGINT | 申请人（`sys_user.id`） |
| quantity | DECIMAL(12,2) | 申领数量，>0 |
| reason | VARCHAR(500) | 申领事由 |
| bpm_instance_id | BIGINT | BPM 实例 |
| status | VARCHAR(20) | DRAFT/APPROVING/APPROVED/REJECTED/ISSUED |
| issued_at | DATETIME | 发放时间 |
| issued_by | BIGINT | 发放人 |
| 审计字段 | — | 同上 |

索引：`idx_admin_material_apply_material(material_id, status)`、`idx_admin_material_apply_user(applicant_id)`、`idx_admin_material_apply_bpm(bpm_instance_id)`。

**V34__admin_vehicle_meeting.sql**

5. `admin_vehicle` 车辆台账

| 字段 | 类型 | 说明 |
|---|---|---|
| id | BIGINT | 主键 |
| plate_no | VARCHAR(32) | 车牌号，唯一 |
| brand_model | VARCHAR(128) | 品牌型号 |
| seats | INT | 座位数 |
| purchase_date | DATE | 购置日期 |
| insurance_expire | DATE | 保险到期日 |
| inspection_expire | DATE | 年检到期日 |
| status | VARCHAR(20) | IDLE/USING/MAINTENANCE |
| remark | VARCHAR(500) | 备注 |
| 审计字段 | — | 同上 |

索引：唯一键 `uk_admin_vehicle_plate(plate_no)`；普通索引 `idx_admin_vehicle_status(status)`、`idx_admin_vehicle_insurance(insurance_expire)`、`idx_admin_vehicle_inspection(inspection_expire)`。

6. `admin_vehicle_apply` 用车申请

| 字段 | 类型 | 说明 |
|---|---|---|
| id | BIGINT | 主键 |
| vehicle_id | BIGINT | 车辆 |
| applicant_id | BIGINT | 申请人 |
| purpose | VARCHAR(255) | 用车事由 |
| destination | VARCHAR(255) | 目的地 |
| start_time | DATETIME | 开始时间 |
| end_time | DATETIME | 结束时间 |
| bpm_instance_id | BIGINT | BPM 实例 |
| status | VARCHAR(20) | DRAFT/APPROVING/APPROVED/REJECTED/USING/RETURNED/CANCELLED |
| return_time | DATETIME | 实际还车时间 |
| mileage | DECIMAL(10,1) | 还车里程（公里） |
| 审计字段 | — | 同上 |

索引：`idx_admin_vehicle_apply_vehicle(vehicle_id, status, start_time)`、`idx_admin_vehicle_apply_user(applicant_id)`、`idx_admin_vehicle_apply_bpm(bpm_instance_id)`。

7. `admin_vehicle_expense` 车辆费用

| 字段 | 类型 | 说明 |
|---|---|---|
| id | BIGINT | 主键 |
| vehicle_id | BIGINT | 车辆 |
| apply_id | BIGINT | 关联用车单，可空 |
| expense_type | VARCHAR(20) | MAINTENANCE/VIOLATION/FUEL |
| amount | DECIMAL(14,2) | 金额 |
| occur_date | DATE | 发生日期 |
| attachment_name | VARCHAR(255) | 附件（`/api/v1/files/upload` 返回的服务端文件名） |
| remark | VARCHAR(500) | 备注 |
| 审计字段 | — | 同上 |

索引：`idx_admin_vehicle_expense_vehicle(vehicle_id, occur_date)`、`idx_admin_vehicle_expense_apply(apply_id)`。
说明：原计划的 `attachment_id BIGINT` 改为 `attachment_name VARCHAR(255)`，与现有 file 模块（无实体、按服务端文件名存取）对齐。

8. `admin_meeting_room` 会议室

| 字段 | 类型 | 说明 |
|---|---|---|
| id | BIGINT | 主键 |
| name | VARCHAR(128) | 名称，唯一 |
| location | VARCHAR(128) | 位置 |
| capacity | INT | 容纳人数 |
| equipment | VARCHAR(500) | 设备（投影/白板等，逗号分隔） |
| status | VARCHAR(20) | ENABLED/DISABLED |
| 审计字段 | — | 同上 |

索引：唯一键 `uk_admin_meeting_room_name(name)`；普通索引 `idx_admin_meeting_room_status(status)`。

9. `admin_meeting_apply` 会议室预定

| 字段 | 类型 | 说明 |
|---|---|---|
| id | BIGINT | 主键 |
| room_id | BIGINT | 会议室 |
| applicant_id | BIGINT | 申请人 |
| subject | VARCHAR(255) | 会议主题 |
| start_time | DATETIME | 开始时间 |
| end_time | DATETIME | 结束时间 |
| attendee_ids | VARCHAR(1000) | 参会人 ID，逗号分隔 |
| bpm_instance_id | BIGINT | BPM 实例 |
| status | VARCHAR(20) | DRAFT/APPROVING/APPROVED/REJECTED/CANCELLED |
| 审计字段 | — | 同上 |

索引：`idx_admin_meeting_apply_room(room_id, start_time, end_time)`（冲突校验）、`idx_admin_meeting_apply_user(applicant_id)`、`idx_admin_meeting_apply_bpm(bpm_instance_id)`。
说明：冲突判定为 `existing.start_time < new.end_time AND existing.end_time > new.start_time`，只统计 `APPROVING/APPROVED` 状态。

**V35__admin_visitor_repair.sql**

10. `admin_visitor` 访客登记

| 字段 | 类型 | 说明 |
|---|---|---|
| id | BIGINT | 主键 |
| visitor_name | VARCHAR(64) | 访客姓名 |
| phone_enc | VARCHAR(255) | 手机号（`SensitiveTypeHandler` 加密，接口脱敏） |
| company | VARCHAR(128) | 来访单位 |
| host_user_id | BIGINT | 被访人（`sys_user.id`） |
| visit_date | DATE | 来访日期 |
| purpose | VARCHAR(255) | 来访事由 |
| qr_token | VARCHAR(64) | 一次性核销令牌，唯一，核销后置 NULL |
| status | VARCHAR(20) | PENDING/CHECKED_IN/CHECKED_OUT/CANCELLED |
| checkin_time | DATETIME | 进入时间 |
| checkout_time | DATETIME | 离开时间 |
| 审计字段 | — | 同上 |

索引：唯一键 `uk_admin_visitor_token(qr_token)`（MySQL 唯一键允许多个 NULL，核销后置 NULL 不影响唯一性）；普通索引 `idx_admin_visitor_date(visit_date, status)`、`idx_admin_visitor_host(host_user_id)`。

11. `admin_repair` 报修工单

| 字段 | 类型 | 说明 |
|---|---|---|
| id | BIGINT | 主键 |
| repair_no | VARCHAR(64) | 工单号，唯一，`RP-yyyyMMdd-####` |
| reporter_id | BIGINT | 报修人 |
| dept_id | BIGINT | 报修人部门快照 |
| location | VARCHAR(128) | 报修位置 |
| category | VARCHAR(32) | ELECTRIC/WATER/FURNITURE/IT/OTHER |
| description | VARCHAR(1000) | 故障描述 |
| images_json | JSON | 图片文件名数组 |
| assignee_id | BIGINT | 维修人 |
| bpm_instance_id | BIGINT | BPM 实例 |
| status | VARCHAR(20) | SUBMITTED/ASSIGNED/PROCESSING/DONE/ACCEPTED/REJECTED |
| assigned_at / processed_at / finished_at / accepted_at | DATETIME | 各节点时间 |
| remark | VARCHAR(500) | 处理说明 |
| 审计字段 | — | 同上 |

索引：唯一键 `uk_admin_repair_no(repair_no)`；普通索引 `idx_admin_repair_status(status)`、`idx_admin_repair_assignee(assignee_id, status)`、`idx_admin_repair_bpm(bpm_instance_id)`。
说明：原状态机缺少「驳回」态，补 `REJECTED`，由审批驳回回调写入。

**V36__admin_permission_seed.sql**

权限点 170～177、角色-权限 1103～1110，详见 6.7；沿用 `INSERT ... SELECT ... WHERE NOT EXISTS` 幂等写法。

**V37__bpm_admin_templates.sql**

模板 9011～9015 / 版本 9111～9115，详见 6.6；沿用 V31/V32 幂等写法（模板按 `code` 判重、版本按 `(template_id, version_no)` 判重）。

### 6.2 后端类清单（`com.ceams.module.admin`）

| 子包 | 类 |
|---|---|
| entity | `AdminAsset`、`AdminAssetBorrow`、`AdminMaterial`、`AdminMaterialApply`、`AdminVehicle`、`AdminVehicleApply`、`AdminVehicleExpense`、`AdminMeetingRoom`、`AdminMeetingApply`、`AdminVisitor`、`AdminRepair` |
| enums | `AssetStatus`、`AssetBorrowStatus`、`AssetCategory`、`MaterialStatus`、`MaterialApplyStatus`、`VehicleStatus`、`VehicleApplyStatus`、`VehicleExpenseType`、`MeetingApplyStatus`、`VisitorStatus`、`RepairStatus`、`RepairCategory` |
| mapper | 与 entity 一一对应的 11 个 `BaseMapper` |
| dto | `AssetCreateRequest/AssetUpdateRequest/AssetBorrowRequest/AssetScrapRequest/AssetInventoryRequest`、`MaterialCreateRequest/MaterialUpdateRequest/MaterialStockInRequest/MaterialApplyRequest`、`VehicleCreateRequest/VehicleUpdateRequest/VehicleApplyRequest/VehicleReturnRequest/VehicleExpenseRequest`、`MeetingRoomCreateRequest/MeetingRoomUpdateRequest/MeetingApplyRequest`、`VisitorCreateRequest/VisitorCheckinRequest`、`RepairCreateRequest/RepairAssignRequest/RepairProcessRequest` |
| vo | `AssetVO`、`AssetBorrowVO`、`MaterialVO`、`MaterialApplyVO`、`VehicleVO`、`VehicleApplyVO`、`VehicleExpenseVO`、`MeetingRoomVO`、`MeetingApplyVO`、`VisitorVO`、`VisitorQrVO`、`RepairVO` |
| service / impl | `AdminAssetService`、`AdminMaterialService`、`AdminVehicleService`、`AdminMeetingService`、`AdminVisitorService`、`AdminRepairService` |
| support | `AdminAssetBpmCallback`、`AdminMaterialBpmCallback`、`AdminVehicleBpmCallback`、`AdminMeetingBpmCallback`、`AdminRepairBpmCallback`、`AdminConflictGuard`（会议室/用车时段冲突）、`AdminNoGenerator`（资产/工单编号）、`AdminConverter`（实体→VO + 手机号脱敏） |
| controller | `AdminAssetController`、`AdminMaterialController`、`AdminVehicleController`、`AdminMeetingController`、`AdminVisitorController`、`AdminRepairController` |
| config | `AdminProperties`（`qr-checkin-window-hours` 默认 24、`alert-days` 默认 30） |

复用：`BaseEntity`、`ApiResponse`、`PageResult`、`ErrorCode`、`SensitiveTypeHandler`、`SensitiveMaskUtil`、`BpmInstanceService`、`BpmAuditService`；分页复用 `HrPageUtil` 的边界处理逻辑（上移至 `common` 或 admin 侧复制 5 行，实施时二选一，优先上移）。

### 6.3 业务规则

**资产**

1. `asset_no` 由服务端生成 `AST-yyyyMM-####`（按创建当月序号），入库前判重。
2. 领用前置：资产必须为 `IN_STOCK`；同一资产同时只允许一条 `APPROVING/BORROWING` 记录。
3. 领用走 BPM，提交即落 `APPROVING`；通过回调：资产置 `BORROWED`，借还记录置 `BORROWING`；驳回/撤回/终止：记录置 `REJECTED`，资产不变。
4. 归还：仅 `BORROWING` 可归还，回写 `actual_return_date`、记录 `RETURNED`、资产恢复 `IN_STOCK`。
5. 报废：仅 `IN_STOCK` 且无 `APPROVING/BORROWING` 记录可报废，置 `SCRAPPED`；报废不可逆。
6. 盘点：批量提交 `[{assetId, location, keeperId, status}]`，逐条更新并返回实际变更条数；状态只允许 `IN_STOCK/REPAIRING`，避免盘点覆盖借出状态。
7. 领用回调幂等：记录已是 `BORROWING` 直接返回；资产状态以记录派生，不重复写。

**易耗品**

8. 入库：`quantity > 0`，`stock_qty = stock_qty + quantity`；出库/申领扣减统一用条件更新 `UPDATE ... SET stock_qty = stock_qty - ? WHERE id = ? AND stock_qty >= ?`，影响行数为 0 即库存不足回滚。
9. 申领前置：物料 `ENABLED`、`quantity > 0`、`quantity <= stock_qty`；同一物料存在 `APPROVING` 申领时允许再次提交（不同人员），不做全局互斥。
10. 申领走 BPM；通过回调：扣减库存并将申领单置 `APPROVED`；扣减失败抛 `BIZ_STATE_CONFLICT`，审批事务回滚，任务保持待办。
11. 行政确认发放：`APPROVED → ISSUED`，回写 `issued_at/issued_by`；重复发放报状态冲突。
12. 预警：`stock_qty <= safety_stock` 且 `status=ENABLED` 的物料列表，按缺口比例降序。

**车辆**

13. 用车申请时段校验：同一车辆 `APPROVING/APPROVED/USING` 记录不得与新时段重叠（与会议室同口径）。
14. 通过回调：车辆置 `USING`，申请置 `APPROVED`；同一车辆已有 `USING` 时抛冲突（乐观锁兜底）。
15. 还车：仅 `USING/APPROVED` 可还车，登记 `return_time` 与 `mileage`（≥0），申请置 `RETURNED`，车辆恢复 `IDLE`。
16. 费用登记：金额 > 0，类型限 `MAINTENANCE/VIOLATION/FUEL`；附件可选，存 file 模块文件名。
17. 预警：`insurance_expire` 或 `inspection_expire` 非空且 `<= today+30天`（含已过期）的车辆列表，VO 返回剩余天数（可为负）。

**会议室**

18. 提交预定与审批通过两处都执行冲突校验；通过时若已被占用，抛 `BIZ_STATE_CONFLICT`（提示改期或驳回），审批事务回滚。
19. 时间校验：`start_time < end_time`；参会人数（`attendee_ids` 去重计数）不得超过 `capacity`。
20. `availability` 查询返回指定会议室指定日期的已占用区间（`APPROVING/APPROVED`），供前端日历置灰。

**访客**

21. 登记时生成 `qr_token`（32 位无横线 UUID），有效期 `qr-checkin-window-hours`（默认 24 小时）；过期或提前登记时可调 `GET /visitors/{id}/qr` 重新签发（仅 `PENDING` 且 `visit_date` 当天）。
22. 核销：token 命中 `PENDING`、`visit_date = 今天` 且未过期，置 `CHECKED_IN` 并清空 `qr_token`（一次性）；token 不存在/已用/过期统一返回「二维码无效或已使用」。
23. 离场：`CHECKED_IN → CHECKED_OUT`，回写 `checkout_time`；重复离场报状态冲突。
24. 取消：仅 `PENDING` 可取消；手机号入库加密、出参统一 `SensitiveMaskUtil.maskPhone`。

**报修**

25. 工单号 `RP-yyyyMMdd-####`；提交即走 BPM，状态 `SUBMITTED`；通过回调置 `ASSIGNED`，驳回置 `REJECTED`。
26. 派单：`ASSIGNED → ASSIGNED`（回写 `assignee_id/assigned_at`），可重复改派直至处理开始。
27. 处理：`ASSIGNED → PROCESSING`（回写 `processed_at`）；完工：`PROCESSING → DONE`（回写 `finished_at`，可填处理说明）。
28. 验收：`DONE → ACCEPTED`，仅报修人本人或行政可验收；验收后不可再改。
29. 图片：`images_json` 存 file 模块文件名数组，最多 6 张；提交时逐张校验是否存在（文件模块仅能按名下载，不做内容校验）。

**通用**

30. 所有 `manage` 接口 `@PreAuthorize("hasAnyRole('SYS_ADMIN')")`；本人申请/查询接口 `isAuthenticated()` 且服务端强制 `applicant_id = 当前用户`，禁止传参越权。
31. 写操作统一带 `version` 乐观锁；状态流转先用「当前状态 + 期望状态」条件更新，失败抛 `BIZ_STATE_CONFLICT`。
32. 资产报废、盘点、库存入库/扣减写 `BizAuditSnapshot`；`AuditAction` 追加 `SCRAP`、`STOCK_IN`、`INVENTORY` 三个取值（`VARCHAR` 落库，无需迁移）。

### 6.4 接口（统一前缀 `/api/v1/admin`）

| 模块 | 方法与路径 | 权限 | 说明 |
|---|---|---|---|
| 资产 | `GET /assets` | `admin:asset:view` | 分页，支持 keyword/status/deptId |
| | `GET /assets/{id}` | `admin:asset:view` | 详情 |
| | `POST /assets`、`PUT /assets/{id}` | `admin:asset:manage` | 建档/编辑 |
| | `POST /assets/{id}/borrow` | 登录 | 发起领用（BPM），回写 `bpm_instance_id` |
| | `GET /assets/borrows/my` | 登录 | 本人领用记录 |
| | `POST /assets/{id}/return` | `admin:asset:manage` | 归还登记 |
| | `POST /assets/{id}/scrap` | `admin:asset:manage` | 报废 |
| | `POST /assets/inventory` | `admin:asset:manage` | 批量盘点 |
| 易耗品 | `GET /materials`、`POST /materials`、`PUT /materials/{id}` | `admin:material:manage` | 台账 |
| | `POST /materials/{id}/stock-in` | `admin:material:manage` | 入库 |
| | `POST /materials/apply` | 登录 | 申领（BPM） |
| | `GET /materials/applies/my` | 登录 | 本人申领记录 |
| | `PUT /materials/applies/{id}/issue` | `admin:material:manage` | 确认发放 |
| | `GET /materials/alerts` | `admin:material:manage` | 低于安全库存 |
| 车辆 | `GET /vehicles`、`POST /vehicles`、`PUT /vehicles/{id}` | `admin:vehicle:manage` | 台账 |
| | `POST /vehicles/apply` | 登录 | 用车申请（BPM） |
| | `GET /vehicles/applies/my` | 登录 | 本人用车记录 |
| | `POST /vehicles/applies/{id}/return` | 登录（本人） | 还车登记里程 |
| | `POST /vehicles/{id}/expenses` | `admin:vehicle:manage` | 费用登记 |
| | `GET /vehicles/alerts` | `admin:vehicle:manage` | 保险/年检 30 天预警 |
| 会议室 | `GET /meeting-rooms`、`POST /meeting-rooms`、`PUT /meeting-rooms/{id}` | `admin:meeting:manage` | 台账 |
| | `POST /meeting-rooms/apply` | 登录 | 预定（BPM） |
| | `GET /meeting-rooms/applies/my` | 登录 | 本人预定 |
| | `GET /meeting-rooms/availability?roomId&date` | 登录 | 已占用区间 |
| 访客 | `POST /visitors` | 登录 | 访客登记，返回 `VisitorQrVO` |
| | `GET /visitors?date&status` | `admin:visitor:manage` | 当日访客列表 |
| | `GET /visitors/{id}/qr` | 登录（被访人/行政） | 重新获取二维码内容 |
| | `POST /visitors/checkin` | 登录 | 按 `{token}` 核销 |
| | `POST /visitors/{id}/checkout` | 登录 | 离场 |
| | `POST /visitors/{id}/cancel` | 登录（被访人/行政） | 取消 |
| 报修 | `POST /repairs` | 登录 | 提交（BPM） |
| | `GET /repairs` | `admin:repair:manage` | 分页，支持 status/assigneeId |
| | `GET /repairs/my` | 登录 | 本人报修 |
| | `PUT /repairs/{id}/assign` | `admin:repair:manage` | 派单/改派 |
| | `PUT /repairs/{id}/process` | 登录（维修人） | 开始处理 |
| | `PUT /repairs/{id}/finish` | 登录（维修人） | 完工 |
| | `PUT /repairs/{id}/accept` | 登录（报修人） | 验收 |

说明：备注、处理说明等字段随对应业务动作接口一并提交，不单独开接口；所有分页返回 `PageResult<T>`，主键序列化为字符串。

### 6.5 前端

**Web（`apps/web`）**

| 文件 | 内容 |
|---|---|
| `views/admin/AdminView.vue` | 六个页签：资产台账 / 易耗品 / 车辆 / 会议室 / 访客 / 报修 |
| `views/admin/components/AssetPanel.vue` | 资产列表、建档、领用、归还、报废、盘点 |
| `views/admin/components/MaterialPanel.vue` | 物料台账、入库、申领审核、发放、低库存预警 |
| `views/admin/components/VehiclePanel.vue` | 车辆台账、用车记录、还车、费用、到期预警 |
| `views/admin/components/MeetingPanel.vue` | 会议室台账、预定、当日占用时间轴 |
| `views/admin/components/VisitorPanel.vue` | 访客登记、列表、二维码弹窗（`qrcode` 渲染）、核销/离场 |
| `views/admin/components/RepairPanel.vue` | 工单列表、派单、处理、验收 |
| `router/routes.ts` | 新增 `/admin` 一级路由 |
| `composables/usePermission.ts` | 追加 170～177 权限码 → 角色映射 |
| `utils/labels.ts` | 资产/领用/物料/申领/车辆/用车/费用/会议室/访客/报修状态中文 |
| `package.json` | 新增 `qrcode` 与 `@types/qrcode`（仅 Web 端渲染访客二维码） |

**共享包**

- `packages/types/src/admin.ts`：11 个 VO、各 Request、分页查询类型，并在 `index.ts` 导出。
- `packages/api-client/src/domains/admin.ts`：`createAdminApi(client)`，按 6.4 全量方法，挂载到 `client.admin`。

**小程序（`apps/miniprogram`）**

| 页面 | 内容 |
|---|---|
| `pages/admin/asset-apply.vue` | 可领用资产列表 + 领用申请 |
| `pages/admin/vehicle-apply.vue` | 车辆列表 + 用车申请（时间区间） |
| `pages/admin/meeting-apply.vue` | 会议室 + 日期时段选择 + 冲突提示 |
| `pages/admin/repair-create.vue` | 报修提交（拍照上传、分类、位置） |
| `pages/admin/visitor-checkin.vue` | `uni.scanCode` 扫码核销 + 手动输入 token 兜底 |

`pages.json` 追加 5 个页面；`pages/index/index.vue` 增加「行政服务」入口分组。

### 6.6 BPM 集成（V37__bpm_admin_templates.sql）

| 业务类型 | 模板/版本 | 节点 |
|---|---|---|
| `ADMIN_ASSET_BORROW` | 9011 / 9111 | start → `dept_leader`（DEPT_LEADER）→ `admin_review`（ROLE SYS_ADMIN）→ end |
| `ADMIN_VEHICLE_APPLY` | 9012 / 9112 | 同上 |
| `ADMIN_MEETING_APPLY` | 9013 / 9113 | 同上 |
| `ADMIN_REPAIR` | 9014 / 9114 | 同上 |
| `ADMIN_MATERIAL_APPLY` | 9015 / 9115 | 同上 |

- 模板编码与 `biz_type` 同名，`BpmBizType` 追加 5 个枚举值；`BpmInstanceService.start` 时 `templateCode = bizType.name()`、`bizId = 业务单据 ID`。
- 所有节点 `approveMode=ANY`、`dueHours=24`，与 M1/M2 一致；`DEPT_LEADER` 解析为空时任务落 `PENDING_ASSIGNEE`，由行政在待办中心改派（沿用现有机制，不新增兜底逻辑）。
- 回调实现 `BpmBizCallback`，统一模式：`onInstanceFinished(APPROVED)` 推进业务状态；`REJECTED/WITHDRAWN/TERMINATED` 置 `REJECTED`；回调内先判幂等（已是目标状态直接返回）。
- 关键二次校验：`AdminMeetingBpmCallback` 通过时重新执行冲突校验，冲突抛 `BIZ_STATE_CONFLICT` 回滚审批；`AdminVehicleBpmCallback` 通过时用条件更新 `status IDLE → USING`，失败即冲突。
- 会议室/车辆/资产的状态只由回调写，前端不提供直接改状态接口（报废除外）。

### 6.7 权限点（V36__admin_permission_seed.sql）

| id | code | 名称 | 类型 |
|---|---|---|---|
| 170 | `admin` | 行政后勤 | MENU（`/admin`，icon `Box`，sort 64） |
| 171 | `admin:asset:view` | 查看资产 | API |
| 172 | `admin:asset:manage` | 管理资产 | BUTTON |
| 173 | `admin:material:manage` | 管理易耗品 | BUTTON |
| 174 | `admin:vehicle:manage` | 管理车辆 | BUTTON |
| 175 | `admin:meeting:manage` | 管理会议室 | BUTTON |
| 176 | `admin:visitor:manage` | 管理访客 | BUTTON |
| 177 | `admin:repair:manage` | 管理报修 | BUTTON |

- 角色-权限 1103～1110：仅授予 SYS_ADMIN（`role_id=1`）；STAFF 不授菜单，走小程序自助入口（仅登录校验）。
- 后续若新增「行政专员」角色，只需补 `sys_role_permission`，不改代码。

### 6.8 测试与验收

单测（新增约 45～55 用例，目标总数 ≥ 395）：

1. 资产：编号生成与判重、非 `IN_STOCK` 领用拒绝、领用通过/驳回、归还、报废前置、盘点过滤借出。
2. 易耗品：入库累加、条件扣减成功/库存不足回滚、申领通过扣减、发放幂等、低库存排序。
3. 车辆：时段重叠拒绝、通过置 `USING`、重复占用冲突、还车里程校验、30 天预警边界（第 30 天含）。
4. 会议室：提交冲突、通过时二次冲突回滚、`start >= end` 拒绝、人数超容拒绝。
5. 访客：token 核销一次性、过期/跨日拒绝、离场与取消状态机、手机号脱敏输出。
6. 报修：回调置 `ASSIGNED`、派单/处理/完工/验收顺序校验、非法跳转拒绝。
7. 通用：`version` 冲突返回 `BIZ_STATE_CONFLICT`；本人接口越权传参被强制覆盖。

验收标准（原 6.5 五条，全部保留）：

1. 资产领用/归还、用车、会议室、报修、易耗品申领全部可走 BPM 并正确回写业务状态。
2. 会议室同一时段不可重复占用，提交与审批通过两处均校验。
3. 易耗品低于安全库存产生预警，申领通过后扣减库存且并发下不出现负库存。
4. 车辆保险/年检到期前 30 天预警。
5. 访客二维码一次性有效（核销后立即失效），进入/离开时间可追溯。

冒烟脚本（PowerShell，账号 admin）：

1. 建资产 → 领用 → 审批通过 → 查 `BORROWED` → 归还 → 查 `IN_STOCK`。
2. 建物料（库存 10、安全库存 5）→ 申领 3 → 通过 → 查 `stock_qty=7` → 再申领 8 → 期望库存不足。
3. 建车辆 + 会议室 → 提交两个重叠预定 → 第二个 409。
4. 登记访客 → 取 token → 核销 → 再核销同一 token → 期望 400/409。
5. 提交报修 → 通过 → 派单 → 处理 → 完工 → 验收。

### 6.9 实施步骤

| 任务包 | 内容 | 完成标准 | 状态 |
|---|---|---|---|
| P1 迁移 | V33/V34/V35 建表 + V36 权限 + V37 模板 | `flyway migrate` 到 v37，表/索引/权限/模板齐全 | 已完成（11 表、权限 170～177/1103～1110、模板 9011～9015/9111～9115，v37 已执行） |
| P2 资产与易耗品 | 实体/Mapper/枚举/DTO/VO + 台账接口 + 领用/申领 BPM + 回调 | 领用、申领、库存扣减单测通过 | 已完成（365 用例；建档 AST-202611-0001、领用 APPROVING、入库 10→15、低库存预警均冒烟通过） |
| P3 车辆与会议室 | 台账 + 申请/还车/费用 + 冲突校验 + 回调 | 冲突与状态流转单测通过 | 已完成（389 用例；车辆重叠 409、会议室重叠 409/超员 400、availability 返回占用区间均冒烟通过） |
| P4 访客与报修 | 二维码 token、核销/离场 + 工单流转 | token 一次性、工单状态机单测通过 | 已完成（419 用例；访客二次核销 409、手机号密文 v1: 且出参脱敏、报修 SUBMITTED→ASSIGNED→PROCESSING→DONE→ACCEPTED 全链路冒烟通过） |
| P5 前端 | Web 六页签 + 共享类型/API + 小程序五页 | typecheck/build 通过 | 已完成（web 1908 modules 8.90s、mp DONE；types/admin.ts + api-client/admin.ts、权限 171～177、/admin 路由、qrcode 二维码均已接入） |
| P6 联调 | 冒烟脚本、单测全量、构建、文档回写 | 冒烟全绿，用例 ≥395 | 已完成（419 用例；容器重建 8.095s；资产/易耗品/用车/会议室/访客/报修六条全链路冒烟全绿） |

### 6.10 风险与待确认

| 项 | 说明 | 建议 |
|---|---|---|
| 二维码依赖 | Web 需新增 `qrcode` 依赖，小程序不引入二维码库，只负责扫码 | 仅 Web 端使用；若后续需要访客自助展示，再评估小程序端方案 |
| 审批人缺失 | `DEPT_LEADER` 未维护时任务落 `PENDING_ASSIGNEE` | 上线前确认 `sys_dept.leader_id` 已补齐，或在待办中心改派 |
| 附件鉴权 | file 模块按文件名下载，无归属校验 | 本期接受，M4 知识库时统一补文件权限 |
| 访客隐私 | 手机号加密存储、列表脱敏；二维码 token 不含手机号 | 二维码只编码 `token`，不落任何个人信息 |
| 时区 | 会议室/用车时间按 `Asia/Shanghai` 处理 | 统一使用 `LocalDateTime`，服务端不做 UTC 转换 |
| 库存并发 | 条件更新 + 事务已防负库存，但高并发下会出现重试失败 | 前端提示「库存变动，请重试」，不自动重试 |

---

## 七、M4 文档知识库 + OnlyOffice

### 7.1 迁移与表结构（V38～V39）

`V38__doc_knowledge_base.sql` 建 5 张表，`V39__doc_permission_seed.sql` 建权限点（见 7.7）。所有表沿用标准列 `create_time / update_time / create_by / update_by / deleted / version`。

1. `doc_directory`（目录）

| 字段 | 类型 | 说明 |
|---|---|---|
| id | BIGINT | 主键（雪花） |
| name | VARCHAR(200) | 目录名，同级不重名（业务校验，排除 deleted=1） |
| parent_id | BIGINT NULL | 父目录，NULL 为库根 |
| library_type | VARCHAR(20) | PERSONAL / DEPT / PROJECT / PUBLIC |
| owner_user_id | BIGINT NULL | PERSONAL 必填 |
| owner_dept_id | BIGINT NULL | DEPT 必填 |
| project_id | BIGINT NULL | PROJECT 必填（引用 biz_project.id） |
| path | VARCHAR(500) | `/id/id/` 形式，支撑子树查询与移动校验 |
| sort_order | INT | 排序，默认 0 |
| status | VARCHAR(20) | NORMAL / ARCHIVED |

索引：`idx_doc_dir_parent(parent_id)`、`idx_doc_dir_lib(library_type, owner_user_id, owner_dept_id, project_id)`、`idx_doc_dir_path(path)`。不加同级唯一键（软删除后重建同名会冲突），改由业务层校验。

2. `doc_file`（文件，仅存元数据）

| 字段 | 类型 | 说明 |
|---|---|---|
| id | BIGINT | 主键 |
| dir_id | BIGINT | 所属目录 |
| name | VARCHAR(255) | 含扩展名的文件名 |
| ext | VARCHAR(20) | 小写扩展名 |
| size | BIGINT | 字节数 |
| storage_key | VARCHAR(500) | 相对 `upload-dir` 的路径 |
| checksum | CHAR(64) | SHA-256，用于秒传与变更判定 |
| current_version_no | INT | 当前版本号，从 1 起 |
| status | VARCHAR(20) | NORMAL / LOCKED（LOCKED 仅用于列表展示） |
| lock_user_id | BIGINT NULL | 编辑锁持有人（与 Redis 冗余） |
| lock_time | DATETIME NULL | 加锁时间 |
| uploaded_by | BIGINT | 上传人 |
| download_count | INT | 默认 0 |

索引：`idx_doc_file_dir(dir_id, deleted)`、`idx_doc_file_checksum(checksum)`、`idx_doc_file_name(name)`。

3. `doc_file_version`

`file_id / version_no / storage_key / size / checksum / uploaded_by / change_note VARCHAR(500) / source VARCHAR(20)`，`source` 取 `UPLOAD/ONLINE_EDIT/ROLLBACK`，唯一键 `uk_doc_ver(file_id, version_no)`。

4. `doc_dir_permission`

`dir_id / subject_type(USER/ROLE/DEPT) / subject_id / actions JSON / granted_by`，唯一键 `uk_doc_dir_perm(dir_id, subject_type, subject_id)`。

5. `doc_file_permission`

`file_id / subject_type / subject_id / actions JSON / granted_by`，唯一键 `uk_doc_file_perm(file_id, subject_type, subject_id)`。

`actions` 为 JSON 数组，取值 `PREVIEW / DOWNLOAD / EDIT / DELETE`。

### 7.2 后端类清单（`com.ceams.module.doc`）

| 分层 | 类 |
|---|---|
| 枚举 | `LibraryType`、`DocFileStatus`、`DocSubjectType`、`DocAction`、`DocVersionSource` |
| 实体 | `DocDirectory`、`DocFile`、`DocFileVersion`、`DocDirPermission`、`DocFilePermission`（Mapper 全部 `@Mapper`） |
| DTO | `DirectoryCreateRequest`、`DirectoryUpdateRequest`、`DirectoryMoveRequest`、`UploadInitRequest`、`UploadCompleteRequest`、`FileRenameRequest`、`FileMoveRequest`、`PermissionGrantRequest` |
| VO | `DirectoryNodeVO`、`DocFileVO`、`DocVersionVO`、`UploadInitVO`、`EditorConfigVO`、`PermissionVO` |
| Service | `DocDirectoryService`、`DocFileService`、`DocUploadService`、`DocVersionService`、`DocPermissionService`、`DocLockService`、`OnlyOfficeService` |
| 支撑 | `DocAccessResolver`（权限合并）、`DocPathUtil`（路径与循环校验）、`DocStorageService`（落盘/校验/引用计数）、`OnlyOfficeJwtSupport`、`DocProperties`、`OnlyOfficeProperties` |
| Controller | `DocDirectoryController`、`DocFileController`、`DocUploadController`、`DocVersionController`、`DocPermissionController`、`OnlyOfficeController` |

追加既有枚举 `AuditAction`：`DOC_UPLOAD / DOC_EDIT / DOC_DELETE / DOC_GRANT / DOC_ROLLBACK`（仅追加，不动既有取值）。

### 7.3 业务规则

1. 库默认可见性：PERSONAL 仅 `owner_user_id`；DEPT 为 `owner_dept_id` 本部门及子部门；PROJECT 为 `biz_project.manager_id` 与显式授权；PUBLIC 所有登录用户。系统无项目成员表，PROJECT 库成员仅靠 `doc_dir_permission` 显式授权（避免臆造表结构）。
2. 权限合并：沿目录链自根到叶对 `actions` 取并集，再与文件级条目取并集；不建模显式 deny。`SYS_ADMIN` 全权。
3. 同级目录重名拒绝（`deleted=0` 范围内）；移动不得指向自身或子树，不得跨库（`library_type` 与 owner 必须一致）。
4. 删除目录：存在未删除子目录或文件时拒绝，提示先清空。
5. 上传限制：单文件 ≤ 200MB（`ceams.doc.max-file-size`），分片 5MB，分片数 ≤ 1000；扩展名白名单 `doc/docx/xls/xlsx/ppt/pptx/pdf/txt/md/csv/zip/png/jpg/jpeg/webp`。
6. 文件名过滤 `/ \ .. : * ? " < > |` 与控制字符；仅保留基础名，不信任前端路径。
7. 分片合并按序拼接后计算 SHA-256，与 init 传入 `checksum` 不一致则拒绝并清理临时目录；临时目录 `{upload-dir}/tmp/chunks/{uploadId}/`，Redis `doc:upload:{uploadId}` 记录 meta 与已上传序号，TTL 24h。
8. 秒传：`checksum + size` 命中既有 `doc_file` 时直接建新记录复用 `storage_key`，物理文件不复制；物理删除仅当该 `storage_key` 无其他未删除引用。
9. 版本：新上传与在线保存均 `current_version_no + 1` 并写 `doc_file_version`，唯一键冲突重试 3 次。
10. 编辑锁：Redis `doc:lock:{fileId}`（值=userId，TTL 30min，`setIfAbsent`）；持有人可续期/释放；他人加锁或释放返回 409「文件正在被他人编辑」；锁过期后自动可用，读取时校准数据库 `status`。
11. 下载/预览鉴权走 `DocAccessResolver`；下载签发一次性短时 token（jwt，300s，claim 含 fileId/userId/versionNo），Redis 记 nonce 保证一次性。
12. OnlyOffice 仅对 `doc/docx/xls/xlsx/ppt/pptx` 返回编辑器配置；其他类型走下载或浏览器预览。无 EDIT 权时 `mode=view`。
13. 回调安全：校验 JWT 签名（`Authorization` 或 body.token）、`fileId` 与路径一致、`url` 域名属于配置的 `document-server-url`；非法返回 403。
14. 回调幂等：以 `(fileId, status, checksum, key)` 作 Redis 幂等键 10min；`status=2/6` 且下载内容 checksum 与当前版本一致时只释放锁、不建版本。
15. 回调保存：从 `url` 下载到临时文件（30s 超时，3 次退避）→ 计算 SHA-256 → 建新版本（`source=ONLINE_EDIT`）→ 更新 `size/checksum/current_version_no` → 释放锁 → 审计 `DOC_EDIT`。
16. 回滚：不删除历史，复制目标版本内容生成新版本（`source=ROLLBACK`，`change_note=回滚到 vN`）；目标版本不存在返回 400。
17. 删除文件：软删除元数据；物理文件按引用计数判定；审计 `DOC_DELETE`。
18. 审计：上传/编辑/删除/授权/回滚均写 `bpm_audit`，`targetType=DOC_FILE`，内容不入库、`maskedFields=null`。
19. OnlyOffice 降级：`enabled=false` 或 `/healthcheck` 2s 探活失败时，`editor-config` 返回 503「在线编辑暂不可用」，前端提示改为下载；上传/下载/版本不受影响。
20. 中文文件名：入库原样保存，URL 侧统一 `encode`，响应头 `filename*=UTF-8''`。

### 7.4 接口（统一前缀 `/api/v1/doc`）

| 方法 | 路径 | 权限 | 说明 |
|---|---|---|---|
| GET | `/directories/tree?libraryType&ownerId` | 登录 | 目录树（按可见性过滤） |
| POST | `/directories` | `doc:dir:manage` | 新建目录 |
| PUT | `/directories/{id}` | `doc:dir:manage` | 改名/排序 |
| PUT | `/directories/{id}/move` | `doc:dir:manage` | 移动 |
| DELETE | `/directories/{id}` | `doc:dir:manage` | 删除（非空拒绝） |
| GET | `/files?dirId&keyword&page&size` | 登录 | 文件列表 |
| POST | `/files/upload/init` | `doc:file:upload` | 初始化分片（返回 uploadId/已上传分片/是否秒传） |
| PUT | `/files/upload/chunk` | `doc:file:upload` | 上传分片（multipart） |
| POST | `/files/upload/complete` | `doc:file:upload` | 合并落库 |
| GET | `/files/{id}/preview` | 登录 + 数据权限 | 预览（图片/PDF 直出，Office 走 editor-config） |
| POST | `/files/{id}/download-token` | 登录 + 数据权限 | 签发一次性下载 token |
| GET | `/files/{id}/download?token=` | token | 下载 |
| GET | `/onlyoffice/file?token=` | token | 供 Document Server 拉取原件（无用户上下文） |
| PUT | `/files/{id}/rename` | `doc:file:edit` | 重命名 |
| PUT | `/files/{id}/move` | `doc:file:edit` | 移动 |
| DELETE | `/files/{id}` | `doc:file:delete` | 删除 |
| POST | `/files/{id}/lock` / `/unlock` | `doc:file:edit` | 加锁/释放 |
| POST | `/files/{id}/lock/renew` | `doc:file:edit` | 续期 |
| GET | `/files/{id}/versions` | 登录 + 数据权限 | 版本列表 |
| POST | `/files/{id}/versions/{no}/rollback` | `doc:file:edit` | 回滚 |
| GET | `/files/{id}/permissions` | `doc:permission:manage` | 授权列表 |
| POST | `/files/{id}/permissions` | `doc:permission:manage` | 授权（用户/角色/部门 + 四类动作） |
| DELETE | `/files/{id}/permissions/{permId}` | `doc:permission:manage` | 取消授权 |
| GET | `/files/{id}/editor-config` | 登录 + EDIT/PREVIEW | OnlyOffice 配置与 JWT |
| POST | `/onlyoffice/callback` | JWT | 保存回调 |

### 7.5 前端

共享包：

- `packages/types/src/doc.ts`：`LibraryType`、`DocAction` 联合类型，`DirectoryNodeVO`、`DocFileVO`、`DocVersionVO`、`UploadInitVO`、`EditorConfigVO`、`PermissionVO` 及全部 Request；`size` 用 `string`（后端 Long 序列化为字符串）。
- `packages/api-client/src/domains/doc.ts`：`directories / files / upload / versions / permissions / editorConfig` 六组方法，分片上传用 `FormData`，支持已上传分片跳过。

Web：

| 文件 | 内容 |
|---|---|
| `views/doc/DocView.vue` | 左侧库切换 + 目录树，右侧面包屑 + 文件表格 + 搜索 + 批量删除 |
| `components/DirectoryTree.vue` | 树渲染、右键菜单（新建/改名/移动/删除/授权） |
| `components/FileTable.vue` | 名称/大小/版本/上传人/更新时间、行内操作（预览/下载/编辑/版本/授权/删除） |
| `components/UploadDialog.vue` | 分片上传、进度、断点续传、秒传提示 |
| `components/PermissionDrawer.vue` | 主体类型切换（用户/角色/部门）、四类动作勾选、已有授权列表 |
| `components/DocEditor.vue` | 动态加载 `{documentServerUrl}/web-apps/apps/api/documents/api.js`，容器 `div` + `DocsAPI.DocEditor`，销毁时 `destroyEditor` |
| `router/routes.ts`、`AppLayout.vue` | `/doc` 路由与菜单 |

小程序：M4 不新增页面（在线编辑仅 Web），只读列表与下载留待 M6。

### 7.6 OnlyOffice 集成

`docker-compose.yml` 增加独立服务（保留原方案）：

```yaml
onlyoffice:
  image: onlyoffice/documentserver:8.1
  ports:
    - "8081:80"
  environment:
    JWT_ENABLED: "true"
    JWT_SECRET: ${ONLYOFFICE_JWT_SECRET}
  volumes:
    - onlyoffice-data:/var/www/onlyoffice/Data
  networks:
    - ceams-net
```

CEAMS 配置：

```yaml
ceams:
  doc:
    max-file-size: 209715200
    chunk-size: 5242880
    lock-ttl-minutes: 30
    download-token-ttl-seconds: 300
    onlyoffice:
      enabled: ${ONLYOFFICE_ENABLED:false}
      document-server-url: ${ONLYOFFICE_URL:http://onlyoffice}
      public-base-url: ${CEAMS_PUBLIC_URL:http://app:9095}
      jwt-secret: ${ONLYOFFICE_JWT_SECRET:}
      callback-path: /api/v1/doc/onlyoffice/callback
```

可达性要点（关键，易踩坑）：

1. Document Server 在容器内通过 `ceams-net` 访问 `http://app:9095`，因此 `public-base-url` 必须填容器网络可达地址（本地 compose 即 `http://app:9095`），不能填 `localhost`。
2. `document.url` 指向 `{public-base-url}/api/v1/doc/onlyoffice/file?token=...`，token 为 300s 短时 JWT（scope=`onlyoffice-file`），绕过用户 Authorization。
3. 回调由 Document Server 发起，走 `callback-path`，JWT 用同一 secret 校验。
4. `app` 容器已有 `ceams-files:/app/data/files` 卷，Document Server 不需要挂载该卷（只经 HTTP 取文件）。
5. 中文字体：若出现方框字，Document Server 需安装 `fonts-noto-cjk`（在镜像内 `apt-get install` 或自定义 Dockerfile），写入部署说明。

### 7.7 权限点（V39__doc_permission_seed.sql）

ID 续接 V36：`sys_permission` 178～183，`sys_role_permission` 1111～1116；脚本用 `INSERT ... SELECT ... WHERE NOT EXISTS` 幂等，子权限挂 `parent_id=178`。

| id | code | 名称 | 类型 | 授权角色 |
|---|---|---|---|---|
| 178 | `doc` | 文档知识库 | MENU | SYS_ADMIN、STAFF、FINANCE |
| 179 | `doc:dir:manage` | 管理目录 | BUTTON | SYS_ADMIN |
| 180 | `doc:file:upload` | 上传文件 | BUTTON | SYS_ADMIN、STAFF、FINANCE |
| 181 | `doc:file:edit` | 编辑/重命名/锁 | BUTTON | SYS_ADMIN、STAFF |
| 182 | `doc:file:delete` | 删除文件 | BUTTON | SYS_ADMIN |
| 183 | `doc:permission:manage` | 授权管理 | BUTTON | SYS_ADMIN |

下载/预览不设独立权限点，由登录 + 数据权限（`DocAccessResolver`）判定。

建库入口：PUBLIC 库目录仅 SYS_ADMIN（`doc:dir:manage`）可创建；PERSONAL 与 DEPT 库目录由用户自助建，PROJECT 库按项目经理判定。

### 7.8 测试与验收

单测 ≥30 例（基线 419 → 目标 ≥449）：

- `DocAccessResolverTest`（7）：个人库仅本人、部门库含子部门、项目库经理、公开库、文件级覆盖、跨部门拒绝、SYS_ADMIN 全权。
- `DocPathUtilTest`（4）：路径生成、子树判定、循环移动拒绝、跨库移动拒绝。
- `DocDirectoryServiceImplTest`（3）：同级重名、非空删除拒绝、移动成功。
- `DocUploadServiceImplTest`（5）：分片缺失、checksum 不符、秒传命中、超限拒绝、扩展名/文件名过滤。
- `DocFileServiceImplTest`（3）：版本递增、引用计数物理删除、非法重命名。
- `DocLockServiceImplTest`（4）：加锁、续期、他人加锁 409、过期后可用。
- `OnlyOfficeServiceImplTest`（7）：view/edit 模式、JWT 签发、非法 JWT 403、回调幂等、新版本生成、同 checksum 跳过、回滚生成新版本。

验收标准：

1. 个人库仅本人可见；部门库按部门子树；项目库按项目经理与显式授权；公开库全员。
2. 目录权限与文件权限取并集生效，SYS_ADMIN 全权。
3. 编辑锁生效，第二人只能预览；锁超时自动释放。
4. OnlyOffice 可打开、编辑、保存，保存后生成新版本，可回滚。
5. 上传/下载/编辑/删除/授权/回滚全部有审计记录。
6. OnlyOffice 不可用时上传/下载/版本功能不中断，仅在线编辑降级。

冒烟剧本（6 步）：建目录 → 分片上传 docx（校验版本 v1）→ 同 checksum 秒传 → 加锁后他人加锁 409 → editor-config 返回 JWT 与 url → 模拟回调写入 v2 → 回滚到 v1 生成 v3。

### 7.9 实施步骤

| 步骤 | 任务包 | 完成标准 | 状态 |
|---|---|---|---|
| P1 | V38 建表 + V39 权限种子，执行 flyway | v38/v39 success，表与权限幂等可重跑 | 已完成（5 表、权限 178～183 / 角色权限 1111～1121；v39 二次执行计数不变） |
| P2 | 目录 + 文件元数据 + `DocAccessResolver` + 单测 | 权限解析与目录规则单测通过 | 已完成（436 用例，新增 17；目录 CRUD 与 8 条冒烟通过，含同名/循环移动/非空删除 400） |
| P3 | 分片上传/秒传/版本/下载 token/编辑锁 + 单测 | 上传与锁单测通过 | 已完成（451 用例，新增 15；12MB 三分片上传 437ms、秒传命中、token 二次使用 401、同用户重复加锁幂等） |
| P4 | OnlyOffice（compose 服务、editor-config、回调、降级）+ 单测 | 回调与降级单测通过 | 已完成（463 用例，新增 12；镜像 5.13GB 已就绪，healthcheck=true，取件 SHA-256 与原文件一致，回调后释放锁，停服 503 降级） |
| P5 | 前端 Web（树/表格/上传/权限抽屉/编辑器）+ 共享类型/API | typecheck/build 通过 | 已完成（typecheck 0；build 1927 modules / 9.36s；新增 `browserDocumentServerUrl` 解决浏览器加载 api.js；503 降级到下载） |
| P6 | 联调（单测全量、容器重建、OnlyOffice 开/关双跑、冒烟、文档回写） | 冒烟全绿，用例 ≥449 | 已完成（464 用例；六步冒烟全绿；顺带修复「删除文件未释放编辑锁」，真机验证 Redis key 已清、DB 锁字段归零） |

### 7.10 风险与待确认

| 项 | 说明 | 建议 |
|---|---|---|
| 镜像体积 | `onlyoffice/documentserver:8.1` 约 2GB+，首次拉取受网络影响 | P1～P3 不依赖镜像，P4 开始前再拉取；离线环境用 `docker save/load` 分发 |
| 字体 | 缺中文字体会显示方框 | 镜像内装 `fonts-noto-cjk`，写入部署说明 |
| 回调可达性 | `public-base-url` 必须是 Document Server 可达地址 | 本地用 `http://app:9095`，生产用内网域名 |
| 磁盘容量 | 分片临时目录 + 版本副本放大占用 | 定期清理 TTL 临时目录，监控 `data/files` |
| 秒传共享 | 多文件共用 `storage_key` | 物理删除按引用计数，禁止直接删文件 |
| 权限模型 | 只做并集，不支持显式 deny | 如需拒绝，后续单列 `deny` 字段 |
| Redis 依赖 | 编辑锁 fail-closed | Redis 不可用时禁止加锁编辑，可预览下载 |
| JWT 密钥 | jjwt 要求 HS256 密钥 ≥256 位；开发默认 `ceams-dev-secret` 仅 16 字节 | 生产必须配置 ≥32 字节的 `ONLYOFFICE_JWT_SECRET`；短密钥由兼容 HMAC 路径处理，仅供本地开发 |
| 文件预览 | Office 非编辑预览依赖 Document Server | 不可用时降级为下载 |

---

## 八、M5 企业微信完整集成

### 8.1 迁移与数据表（V40～V41）

**V40__integration_wecom.sql**

1. `integration_wecom_sync`（同步批次）：
   `id BIGINT PK / sync_batch_no VARCHAR(32) UNIQUE / sync_type VARCHAR(16) / total INT / success_count INT / failed_count INT / status VARCHAR(16) / error_msg VARCHAR(1000) / started_at DATETIME / finished_at DATETIME` + 审计列（`create_time/update_time/create_by/update_by/deleted`）；索引 `idx_status_started(status, started_at)`。
2. `integration_wecom_message`（应用消息发件箱）：
   `id / notification_id BIGINT / target_user_id BIGINT / wecom_userid VARCHAR(64) / msg_type VARCHAR(16) / payload_json JSON / status VARCHAR(16) / retry_count INT DEFAULT 0 / next_retry_at DATETIME / error_msg VARCHAR(500)` + 审计列；唯一键 `uk_notification_user(notification_id, target_user_id)`（防重复入队）；索引 `idx_status_retry(status, next_retry_at)`。
3. `ALTER TABLE sys_user ADD COLUMN wecom_userid VARCHAR(64) NULL` + `UNIQUE KEY uk_sys_user_wecom(wecom_userid)`（MySQL 唯一索引允许多个 NULL）。
4. `ALTER TABLE sys_dept ADD COLUMN wecom_dept_id VARCHAR(64) NULL` + `UNIQUE KEY uk_sys_dept_wecom(wecom_dept_id)`。

**V41__wecom_permission_seed.sql**：权限 184 `integration:wecom:view`（`MENU`，页面入口 `/integration/wecom`，排序 78）、185 `integration:wecom:sync`（`BUTTON`，挂 184 下，排序 79）；角色权限 1122～1123 授予 SYS_ADMIN；脚本幂等（`WHERE NOT EXISTS`）。

> P1 已落地：Flyway 由 v39 升至 v41，两张表与 4 个索引、`sys_user.wecom_userid`/`sys_dept.wecom_dept_id` 及其唯一键、权限 184/185 与角色权限 1122/1123 均已校验。

### 8.2 工程结构（`com.ceams.module.integration`）

| 层 | 类 |
|---|---|
| 配置 | `WeComAppProperties`（`ceams.wecom.*`）、`WeComAppConfiguration`（专用 `RestClient` + 调度线程池） |
| 客户端 | `WeComApiClient`（`getAccessToken` / `listDept` / `listDeptUsers` / `getUser` / `getUserIdByCode` / `sendAppMessage`） |
| 支撑 | `WeComTokenCache`（Redis `wecom:token`，提前 300s 刷新，双重检查）、`WeComSyncRunner`（部门/成员 upsert 纯逻辑） |
| 实体/Mapper | `IntegrationWecomSync`、`IntegrationWecomMessage` + 对应 Mapper（`@Mapper`） |
| 服务 | `WeComSyncService`（手动/定时同步、日志分页）、`WeComMessageOutbox`（入队）、`WeComMessageDispatcher`（`@Scheduled` 消费）、`WeComLoginService`（state + code 换身份 + 绑定） |
| 接口 | `WeComSyncController`、`WeComLoginController` |

`AuthService` 增加 `LoginResponse loginByWeCom(String code, String state, String deviceId, String deviceName, HttpServletRequest http)`（P3 落地为 5 参数，deviceId 优先取调用方、其次取 state 记录值、最后回落 `wecom-{userId}`），内部复用 `AuthServiceImpl.issueTokens` 与 `bindDevice`（`issueTokens` 保持 private，不复制 JWT 逻辑）。

### 8.3 配置

```yaml
ceams:
  wecom:
    # 注意：群机器人已占用 WECOM_ENABLED / WECOM_TIMEOUT_MILLIS，
    # 应用集成统一使用 WECOM_APP_* 前缀，避免与 ceams.notification.wecom.* 冲突。
    enabled: ${WECOM_APP_ENABLED:false}
    corp-id: ${WECOM_APP_CORP_ID:}
    agent-id: ${WECOM_APP_AGENT_ID:}
    secret: ${WECOM_APP_SECRET:}
    api-base-url: ${WECOM_APP_API_BASE_URL:https://qyapi.weixin.qq.com}
    login-base-url: ${WECOM_APP_LOGIN_BASE_URL:https://login.work.weixin.qq.com}
    redirect-uri: ${WECOM_APP_REDIRECT_URI:http://localhost:5173/login/wecom}
    sync-cron: "${WECOM_APP_SYNC_CRON:0 0 2 * * ?}"
    dispatch-interval-ms: ${WECOM_APP_DISPATCH_INTERVAL_MS:30000}
    message-retry-max: ${WECOM_APP_MESSAGE_RETRY_MAX:5}
    timeout-millis: ${WECOM_APP_TIMEOUT_MILLIS:5000}
```

群机器人推送（`ceams.notification.wecom.*`）保持不变，两者独立开关；`enabled=false` 时同步/登录/发件箱全部短路，不影响既有功能。

### 8.4 组织单向同步规则

1. **入口**：`POST /integration/wecom/sync`（手动，需 `integration:wecom:sync`）与 `@Scheduled(cron=ceams.wecom.sync-cron)` 共用同一 Service；Redis 锁 `wecom:sync:lock`（`SET NX EX 600`）保证同一时刻只跑一批，抢锁失败返回 409。
2. **部门**：`GET /cgi-bin/department/list` 全量拉取，按 `wecom_dept_id` upsert `sys_dept`（`name`、`sort_order` 取 `order`）；`parent_id` 用「先建后修」：第一轮写入/更新所有部门并建立 `wecom_dept_id → sys_dept.id` 映射，第二轮回填 `parent_id`（根部门 `parentid=1` 视为根，`parent_id=NULL`）。
3. **成员**：按部门逐个 `GET /cgi-bin/user/list?department_id=&fetch_child=0` 拉取（`fetch_child=0` 只取本部门，父部门不重复包含子部门成员，避免同一成员被重复计数）；按 `wecom_userid` upsert `sys_user`：
   - `username`：新建时用 `wecom_userid`，已存在则不改；
   - `name` ← `name`，`phone` ← `mobile`（`SensitiveTypeHandler` 自动加密），`email` ← `email`，`dept_id` ← 首个部门映射；
   - `password_hash`：新建用户写入**随机 32 字节 BCrypt 占位**（不可登录），由管理员重置或走扫码登录；
   - `status`：企业微信 `status=1`（已激活）→ `ACTIVE`，`2/4/5` → `DISABLED`；
   - `leader_id`：企业微信接口不返回直属上级，**不同步**，保持 CEAMS 本地值。
4. **手机号兜底绑定**：`wecom_userid` 未命中且按手机号能唯一命中一个 `sys_user`（`wecom_userid IS NULL`）→ 回填 `wecom_userid` 视为绑定；命中 0 条则新建；命中多条则跳过并计入 `failed_count`（不猜）。
5. **离职处理**：本轮未出现的、且 `wecom_userid IS NOT NULL` 的 `sys_user` → 置 `DISABLED`，不物理删除；**手工创建的本地账号（`wecom_userid IS NULL`）永不被同步禁用**。
6. **不反向写**：只读企业微信接口，`sys_dept.leader_id`、`sort_order` 之外的本土字段不改。
7. **批次日志**：每批写 1 条 `integration_wecom_sync`（`RUNNING → SUCCESS/PARTIAL/FAILED`），`error_msg` 截断 1000 字符；部门/成员单条失败不中断整批，累加 `failed_count`。
8. **幂等**：连续执行两次，`sys_dept`/`sys_user` 行数与 `wecom_*` 映射不变（只有 `update_time` 变化）。

### 8.5 扫码登录规则

1. `GET /auth/wecom/qrcode`：生成 `state`（32 位随机）写 Redis `wecom:login:state:{state}`（TTL 5 分钟，值为 `deviceId`），返回 `{loginUrl, state}`，`loginUrl` 形如
   `{login-base-url}/wwlogin/sso/login?login_type=CorpApp&appid={corpId}&agentid={agentId}&redirect_uri={urlencode(redirect-uri)}&state={state}`。
2. `POST /auth/wecom/login`：body `{code, state}`；校验 state 存在并**删除**（一次性），不存在 → 400「二维码已过期」。
3. `WeComApiClient.getUserIdByCode(code)` → `GET /cgi-bin/auth/getuserinfo?access_token=&code=`；`errcode!=0` 或 `userid` 为空 → 400「企业微信授权失败」。
4. 匹配顺序：`sys_user.wecom_userid = userid` → 命中即登录；否则 `getUser(userid)` 取 `mobile`，按手机号唯一命中且 `wecom_userid IS NULL` → 回填绑定后登录；仍未命中 → 400「账号未绑定，请联系管理员」。
5. 用户 `status != ACTIVE` → 403「账号已禁用」。
6. 登录成功复用 `issueTokens`（JWT 双令牌 + `sys_refresh_token` + 设备绑定），`deviceId` 取 state 中记录值，`deviceName` 缺省 `企业微信扫码`，并写登录审计。
7. `enabled=false` 或 `corp-id/secret` 为空 → 503「企业微信登录未启用」。
8. 前端：Web 登录页新增「企业微信登录」入口（`/login/wecom` 回调页读取 `code/state` 调登录接口），复用现有 token 存储与路由跳转。

### 8.6 应用消息发件箱

1. **接入点**：`BpmNotificationPublisher.send` 在站内通知写成功后调用 `WeComMessageOutbox.enqueue(notificationId, title, content, linkUrl, targets)`；`enqueue` 自身 `try/catch` 全吞，任何异常只 warn，**不影响审批事务**。
2. **入队规则**：每目标用户一行；`wecom_userid` 为空或与目标用户无关则跳过（不写空记录）；`payload_json` 存 `{title, content, linkUrl}`；`status=PENDING`、`next_retry_at=now`；`notification_id+target_user_id` 唯一键冲突视为已入队（忽略）。
3. **消费**：`WeComMessageDispatcher` `@Scheduled(fixedDelayString=ceams.wecom.dispatch-interval-ms)`，Redis 锁 `wecom:dispatch:lock`（`SET NX EX 60`）防多实例重复；每轮取 `status='PENDING' AND next_retry_at<=now()` 按 id 升序最多 50 条。
4. **发送**：`sendAppMessage(agentId, wecomUserid, text)` → `POST /cgi-bin/message/send?access_token=`，body `{touser, msgtype:"text", agentid, text:{content}, safe:0}`；正文格式 `【{title}】\n{content}\n{linkUrl}`，长度按企业微信限制截断 512 字符。
5. **结果**：`errcode=0` → `SENT`；`errcode` 为 `40014/42001`（token 失效）→ 清 token 缓存后立即重试 1 次；其他错误 → `retry_count+1`，按 **1m / 5m / 15m / 1h / 6h** 退避写 `next_retry_at`；`retry_count >= message-retry-max` → `FAILED` 并 warn 告警日志（不抛异常）。
6. **只做通知**：消息正文只提示「请回 CEAMS 处理」，不承载审批动作；待办链接沿用 `ceams://todo?...`。
7. **手工补救**：`POST /integration/wecom/messages/{id}/retry`（需 `integration:wecom:sync`）把 `FAILED/SENT` 重置为 `PENDING`、`retry_count=0`。

**P4 落地说明**（实现口径以上述规则为基础，差异点见本节末尾两条）：

- **入队**：`WeComMessageOutbox.enqueue(notificationId, targetUserIds, title, content, linkUrl)` 在 `BpmNotificationPublisher` 站内通知写成功后调用（独立 `try/catch`，异常只 `warn`）；只对 `sys_user.wecom_userid` 非空且未删除的目标用户逐行写入 `integration_wecom_message`（`msg_type=TEXT`、`status=PENDING`、`retry_count=0`、`next_retry_at=now`，`payload_json` 存 `{title,content,linkUrl}` 快照）；唯一键 `uk_wecom_msg_notification_user(notification_id, target_user_id)` 冲突视为「已入队」并静默跳过（幂等）；企业微信集成未配置或通知 ID 非法时直接返回。
- **调度**：`WeComMessageDispatcher` 以 `@Scheduled(fixedDelayString="ceams.wecom.dispatch-interval-ms")` 轮询，Redis `wecom:dispatch:lock`（`SET NX EX 60`，随机持有者值 + `finally` 比对后删除）保证多实例互斥；每轮取 `status='PENDING' AND (next_retry_at IS NULL OR next_retry_at <= now)` 的消息，按 id 升序最多 50 条，单条异常不影响其他条目。
- **投递与退避**：正文为 `title + 换行 + content`，`linkUrl` 为 http/https 时追加 `详情：{linkUrl}`，整体截断 512 字符；失败后 `retry_count+1`，按新 `retry_count` 取 **1m / 5m / 15m / 1h / 6h** 退避（超出取最后一档），`retry_count >= ceams.wecom.message-retry-max` 置 `FAILED` 且 `next_retry_at` 置空，`error_msg` 截断 500 字符。
- **token 失效**：`errcode` 为 **40014 / 42001** 时调用 `WeComTokenCache.invalidate()` 并在同一轮重试一次，仍失败按普通失败进入退避。
- **手工重投**：`POST /api/v1/integration/wecom/messages/{id}/retry` 仅接受 `FAILED`（其他状态 400「仅失败消息可重投」），复位为 `PENDING`、`retry_count=0`、`next_retry_at=now`、清空 `error_msg`，并写审计 `AuditAction.WECOM_MESSAGE_RETRY`（目标类型 `INTEGRATION_WECOM_MESSAGE`，before=原状态，after=`PENDING`）。
- 与上文差异：实际入队签名为 `enqueue(notificationId, targetUserIds, title, content, linkUrl)`（参数顺序以目标用户集合在前）；第 7 条重投仅允许 `FAILED`，`SENT` 不可重投。

### 8.7 接口清单

| 方法 | 路径 | 权限 | 说明 |
|---|---|---|---|
| POST | `/api/v1/integration/wecom/sync` | `integration:wecom:sync` | 手动触发同步，返回批次号 |
| GET | `/api/v1/integration/wecom/sync/logs` | `integration:wecom:view` | 批次分页（page/size/status） |
| GET | `/api/v1/integration/wecom/messages` | `integration:wecom:view` | 发件箱分页（status/重试次数/关键字） |
| POST | `/api/v1/integration/wecom/messages/{id}/retry` | `integration:wecom:sync` | 手工重投 |
| GET | `/api/v1/auth/wecom/qrcode` | 匿名 | 返回 `loginUrl` + `state` |
| POST | `/api/v1/auth/wecom/login` | 匿名 | `{code, state}` → `LoginResponse` |

`SecurityConfig` 现有 `PERMIT_ALL` 已含 `/api/v1/auth/**`，扫码登录两条接口天然匿名，无需额外放行（P3 核实）。

### 8.8 权限与审计

- 权限 184/185，角色权限 1122/1123（SYS_ADMIN）。
- 审计（`BizAuditSnapshot`）：同步批次记 `AuditAction.WECOM_SYNC`（新增枚举），目标 `INTEGRATION_WECOM_SYNC`；手工重投记 `WECOM_MESSAGE_RETRY`。
- 登录审计沿用 `AuthServiceImpl` 现有登录成功日志（`LOGIN`），不新增动作。

### 8.9 测试与验收

单测（目标 ≥480，即新增 ≥16）：

| 测试类 | 用例 |
|---|---|
| `WeComTokenCacheTest` | 缓存命中不重复请求（2） |
| `WeComSyncRunnerTest` | 部门两轮 parent 回填、成员新建、手机号唯一绑定、多命中跳过、缺失成员置 DISABLED、本地账号不禁用（6） |
| `WeComSyncServiceImplTest` | 锁冲突 409、批次 SUCCESS/PARTIAL 落库（2） |
| `WeComMessageOutboxTest` | 入队跳过无 wecom_userid、唯一键冲突忽略、异常不影响调用方（3） |
| `WeComMessageDispatcherTest` | 成功置 SENT、失败退避 1m、超限置 FAILED、token 失效重试（4） |
| `AuthWeComLoginTest` | state 过期 400、未绑定 400、手机号绑定成功、禁用 403（4） |

验收：①同步连跑两次幂等，部门层级正确；②同步失败不丢本地数据、批次可查；③扫码登录可签发 JWT，未绑定有明确提示；④消息失败进重试，最终 FAILED 告警，且审批主流程始终成功；⑤群机器人与应用消息可独立开关。

冒烟剧本（无真实企业微信凭证时用 mock 开关验证）：
1. `WECOM_APP_ENABLED=false` → 三个接口分别返回 503/空列表，既有审批通知正常；
2. 手动 `POST /sync` → 批次 `FAILED`（凭证缺失）且 `error_msg` 明确；
3. 伪造 state → 登录 400；
4. 直接向 `integration_wecom_message` 插 1 条 PENDING（手动 SQL）→ 调度消费后 `retry_count=1`、`next_retry_at` 递增；
5. `POST /messages/{id}/retry` → 重置 PENDING；
6. `GET /sync/logs` 与 `/messages` 分页正常。

### 8.10 实施步骤与风险

| 步骤 | 内容 | 完成标准 |
|---|---|---|
| P1 | V40 建表/加列 + V41 权限种子 | v41 成功、表与索引齐全、184/185 与 1122/1123 可查 |
| P2 | `WeComAppProperties` + `WeComApiClient` + `WeComTokenCache` + `WeComSyncRunner/Service` + 同步接口与日志 | 单测通过；`enabled=false` 短路；批次落库 |
| P3 | 扫码登录（`WeComLoginService` + `AuthService.loginByWeCom` + 两个匿名接口） | 单测通过；state 一次性；JWT 可签发 |
| P4 | 发件箱 + 调度消费 + 接入 `BpmNotificationPublisher` | 审批通知仍成功；PENDING 可被消费/重试 |
| P5 | Web 集成页（同步日志、手动同步、发件箱与重投）+ 登录页企业微信入口 + 联调收尾 | typecheck/build 通过；六步冒烟全绿 |

风险：

| 风险 | 应对 |
|---|---|
| 无真实企业微信企业凭证 | 全部外部调用可 mock；`enabled=false` 为默认，CI 与本地零依赖 |
| 扫码登录需可信域名与 redirect_uri 白名单 | 本地仅验证 state 与错误分支；真机验证列入上线前检查 |
| access_token 并发刷新 | Redis `SET NX` 抢锁 + 双重检查，失败方复用缓存值 |
| 手机号多命中 | 不自动绑定，计入 `failed_count` 并提示人工处理 |
| 误禁用本地账号 | 只禁用 `wecom_userid IS NOT NULL` 的账号 |
| 调度多实例重复发送 | Redis 锁 + 发件箱唯一键双保险 |
| 消息体超长 | 正文截断 512 字符，链接单独一行 |

**P5 落地说明（Web 前端）**

- 集成页：`apps/web/src/views/integration/WeComView.vue`，路由 `/integration/wecom`（`meta.title='企业微信集成'`、`meta.permission='integration:wecom:view'`、`meta.icon='ChatDotRound'`，并加入 `AppLayout` 侧边菜单）；页面含「最近一次同步」概览、手动同步按钮、同步批次表（状态筛选 + 分页）与应用消息表（FAILED 行可二次确认重投）。
- 扫码登录：登录页账号密码表单下方新增「企业微信扫码登录」入口（`getOrCreateDeviceId()` 取设备指纹 → `GET /auth/wecom/qrcode` → 整页跳转 `loginUrl`）；回调页 `apps/web/src/views/auth/WeComCallbackView.vue` 对应路由 `/login/wecom`，已加入 `router/index.ts` 的 `PUBLIC_PATHS` 匿名白名单。回调页读 `code/state` 后调用 `authStore.loginByWecom()`（内部复用 `setSession`，与账号密码登录共享同一套令牌持久化），成功后 `router.replace('/')`；400/403/503 分别提示「二维码已过期或账号未绑定」「账号已禁用」「企业微信未启用」。
- 权限码：`integration:wecom:view`（184，菜单/列表查询）与 `integration:wecom:sync`（185，立即同步/消息重投）在 `apps/web/src/composables/usePermission.ts` 中映射为 `SYS_ADMIN`；同步按钮与重投按钮均以 `hasPermission('integration:wecom:sync')` 控制显隐。
- 降级：后端 `ceams.wecom.enabled=false` 时，扫码取址、手动同步均返回 503，前端分别提示「企业微信扫码登录未启用」「企业微信集成未启用，请配置 WECOM_APP_* 环境变量」，不影响账号密码登录与既有菜单。
- 前端架构：`packages/types/src/integration.ts` 定义 `WecomSyncLogVO / WecomSyncResultVO / WecomMessageVO / WecomQrCodeVO / WecomLoginBody / WecomPageQuery` 与状态联合类型；`packages/api-client/src/domains/integration.ts` 提供 `getWecomQrcode / loginByWecom / triggerWecomSync / pageWecomSyncLogs / pageWecomMessages / retryWecomMessage`，匿名接口走 `skipAuth`，已挂载为 `ceamsApi.integration`。


---

## 九、M6 小程序与存量适配

### 9.1 迁移与权限

**结论**：M6 只新增 1 个迁移 `V42__m6_report_permission_seed.sql`（报表权限种子）；**不新增数据表、不新增字段、不新增索引**。

依据（已核实）：

1. `biz_notification.notification_type` 为 `VARCHAR(50)` 且已有索引（`src/main/resources/db/migration/V1__schema.sql`），枚举新增 `HR/ADMIN/DOC` 无需 DDL。
2. `bpm_task.reminded_at` 与索引 `idx_bpm_task_status_due_reminded` 已提供「条件更新抢占提醒权」的幂等范式（`BpmTimeoutServiceImpl`），M6 沿用该范式，不引入去重表。
3. 4 个报表接口、6 个 AI 工具均为只读聚合，不落库；小程序端无本地持久化表。
4. 薪资金额加密存储：`hr_salary_calc` / `hr_salary_slip` 的金额经 `SensitiveTypeHandler` 读写（`HrSalaryCalc`、`SalaryCalcVO`），报表不做 SQL 求和，见 9.8。

**权限点（sys_permission）**：

| ID | code | 名称 | type | parent_id | path | sort | 授予角色 |
|---|---|---|---|---|---|---|---|
| 186 | `report` | 报表 | MENU | NULL | `/report` | 80 | SYS_ADMIN |
| 187 | `report:hr-cost:view` | 人力成本报表 | BUTTON | 186 | NULL | 81 | SYS_ADMIN |
| 188 | `report:hr-performance:view` | 绩效分布报表 | BUTTON | 186 | NULL | 82 | SYS_ADMIN |
| 189 | `report:admin-assets:view` | 资产报表 | BUTTON | 186 | NULL | 83 | SYS_ADMIN |

**角色权限（sys_role_permission，共 4 条）**：1124→(1,186)、1125→(1,187)、1126→(1,188)、1127→(1,189)，角色 1=SYS_ADMIN。

**幂等写法要求（与 V41 一致）**：

```sql
-- 权限点按 code 判重
INSERT INTO sys_permission (id, code, name, type, parent_id, path, icon, sort_order, deleted, version)
SELECT 186, 'report', '报表', 'MENU', NULL, '/report', 'DataLine', 80, 0, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'report');

INSERT INTO sys_permission (id, code, name, type, parent_id, path, icon, sort_order, deleted, version)
SELECT 187, 'report:hr-cost:view', '人力成本报表', 'BUTTON', 186, NULL, NULL, 81, 0, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'report:hr-cost:view');

-- 角色-权限按 (role_id, permission_id) 判重
INSERT INTO sys_role_permission (id, role_id, permission_id)
SELECT 1124, 1, 186
WHERE NOT EXISTS (SELECT 1 FROM sys_role_permission WHERE role_id = 1 AND permission_id = 186);
```

1. 脚本头部注释须写明「ID 续接 V41：`sys_permission` 从 186 起、`sys_role_permission` 从 1124 起」，并声明增量脚本入库后不得修改。
2. 186 为菜单码，仅用于权限树与后续菜单化；`/report` 路由当前无 `meta.permission`（`apps/web/src/router/routes.ts`），本次不为它新增，避免收回 FINANCE/STAFF 对既有 3 个页签的访问权。
3. 187～189 在 `apps/web/src/composables/usePermission.ts` 的 `PERMISSION_ROLES` 中映射为 `['SYS_ADMIN']`；同时补登 `'bpm:report:view': ['SYS_ADMIN','FINANCE']`（149，V16 已授予 role 1/2），供 bpm-efficiency 卡片使用。
4. 若需向 FINANCE 开放人力成本/绩效报表，另起 V43 追加 (2,187)、(2,188)，不在 V42 内授予。
5. AI 工具与通知不新增权限码：AI 工具按 JWT 的 `ROLE_*` 判定（`ToolSecurity`），通知按登录用户本人数据过滤。

### 9.2 小程序工程结构

**结论**：新增 2 个页面（BPM 待办中心、知识库只读预览），改造 7 个既有页面与 4 个基础文件（`pages.json`、`form-schema.ts`、`schema-form.vue`、`auth.ts`）；原方案中的 `schemas/leave.json` 不新建，复用 M5 已落地的 `schemas/hr-leave.json`。

```
apps/miniprogram/src/
├─ pages.json                          改：注册 pages/bpm/todo、pages/doc/list
├─ pages/bpm/todo.vue                  新增：BPM 待办中心四类页签
├─ pages/doc/list.vue                  新增：知识库 PUBLIC 库列表 + 只读预览
├─ pages/index/index.vue               改：入口按登录用户角色过滤
├─ pages/notification/list.vue         改：业务域筛选 + ceams:// 深链解析
├─ pages/hr/checkin.vue                改：补卡面板由手写表单改为 SchemaForm
├─ pages/hr/performance-self.vue       改：可编辑判定改用后端 canSelfEvaluate
├─ pages/admin/asset-apply.vue         改：资产选择器 + SchemaForm 提交
├─ pages/admin/vehicle-apply.vue       改：车辆选择器 + SchemaForm 提交
├─ pages/admin/meeting-apply.vue       改：会议室/时段选择器 + SchemaForm 提交
├─ schemas/asset-borrow.json           新增：资产领用字段定义
├─ schemas/vehicle-apply.json          新增：用车字段定义
├─ schemas/meeting-apply.json          新增：会议室预定字段定义
├─ schemas/checkin-supplement.json     新增：补卡字段定义
├─ schemas/form-schema.ts              改：注册 4 个 bizType，FormField.format 增加 'time'
├─ components/schema-form/schema-form.vue  改：widgetOf 增加 time 分支（picker mode="time"）
└─ platform/auth.ts                    改：新增 rolesOf()，读取 getUser() 的 roles
```

1. `pages.json` 注册片段：`{ "path": "pages/bpm/todo", "style": { "navigationBarTitleText": "待办中心", "enablePullDownRefresh": true } }`、`{ "path": "pages/doc/list", "style": { "navigationBarTitleText": "知识库" } }`。
2. Schema 引擎能力边界（已核实）：`FormField` 支持 `type: string | number`、`format: date | textarea | attachment`、`enum/enumNames` 单选、`minimum/maximum/multipleOf/maxLength`；不支持时间、日期时间、多选与远程选择器，因此时间字段需扩展 1 个 `time` 控件，资产/车辆/会议室选择器保留在页面级。
3. M5 已落地、M6 不再改动的页面：`pages/hr/salary-slip.vue`（列表 + 详情 + 标记已读）、`pages/hr/leave-create.vue`（schema 与提交逻辑保持现状）。

### 9.3 四类待办

**结论**：新建 `pages/bpm/todo.vue`，四类页签全部复用 `apiClient.bpm` 既有方法与后端同一套接口，不新增后端接口。

| 页签 | 方法 | 后端接口 | 行主键 | 点击目标 | 空态 |
|---|---|---|---|---|---|
| 我的待办 | `apiClient.bpm.pendingTasks({page,size,bizType,overdue})` | `GET /api/v1/bpm/tasks/pending` | `TaskVO.id`（含 `instanceId`、`taskVersion`） | `pages/bpm/detail?taskId=&instanceId=` | 暂无待办 |
| 我发起的 | `apiClient.bpm.initiated({page,size,status,bizType})` | `GET /api/v1/bpm/tasks/initiated` | `InstanceVO.id` | `pages/bpm/detail?instanceId=` | 暂无发起记录 |
| 我已办 | `apiClient.bpm.doneTasks({page,size,bizType})` | `GET /api/v1/bpm/tasks/done` | `TaskVO.id`（含 `instanceId`） | `pages/bpm/detail?instanceId=` | 暂无已办记录 |
| 抄送我的 | `apiClient.bpm.ccRecords({page,size,bizType,readStatus})` | `GET /api/v1/bpm/tasks/cc` | `CcRecordVO.id`（含 `instanceId`） | `pages/bpm/detail?instanceId=` | 暂无抄送 |

1. 分页与刷新：`size=20`，页面 `enablePullDownRefresh` + `onReachBottom` 追加；切换页签时重置 `page=1` 并清空列表，四类数据分别缓存，避免重复请求。
2. 状态映射：任务状态 `PENDING/PENDING_ASSIGNEE/APPROVED/REJECTED/CLOSED` 与实例状态 `DRAFT/RUNNING/APPROVED/REJECTED/WITHDRAWN/TERMINATED` 统一由 `utils/labels.ts` 输出中文与标签色；`overdue=true` 的任务额外显示「已超时」标签（后端按 `due_at < now()` 计算）。
3. 深链参数：`onLoad` 解析 `tab`（pending/done/initiated/cc）、`taskId`、`instanceId`；`tab` 缺省为 `pending`。通知中的 `ceams://todo?tab=…&taskId=…&instanceId=…` 由 `pages/notification/list.vue` 解析后 `uni.navigateTo` 到本页，与 Web/桌面端协议保持一致（`BpmNotificationPublisher` 的 `DEEP_LINK_TODO`）。
4. 卡片信息：标题取 `bpmBizTypeLabels[item.bizType]`（`apps/miniprogram/src/utils/labels.ts`，当前仅覆盖 EXPENSE/CHANGE_REQUEST/HR_*，需补 `ADMIN_ASSET_BORROW`「资产领用」、`ADMIN_VEHICLE_APPLY`「用车」、`ADMIN_MEETING_APPLY`「会议室预定」三条），副信息展示节点名、发起人、发起/到期时间；待办页签提供「通过/驳回」快捷入口，跳转详情页完成操作，不在列表内直接审批。
5. 入口调整：首页「我的待办」指向 `pages/bpm/todo?tab=pending`；`pages/expense/pending.vue` 保留为报销专用视图，不删除。

### 9.4 五张动态表单

**结论**：`HR_LEAVE` 复用既有 `schemas/hr-leave.json`（字段不变）；新增 4 张 schema，页面级资源选择器与 schema 字段分离，提交仍调用既有业务接口，由业务接口内部发起 BPM。

引擎扩展（仅 1 处）：`FormField.format` 增加 `'time'`，`widgetOf()` 返回 `time`，模板使用 `<picker mode="time">` 复用 `onDateChange`；必填校验沿用现有 `validate()`（`schema-form.vue` 已暴露）。

| schema（bizType） | 字段（label / 类型 / 必填） | 页面级控件 | 提交调用 | 成功跳转 |
|---|---|---|---|---|
| `hr-leave.json`（`HR_LEAVE`） | 既有：`leaveType` 请假类型/单选（年假/病假/事假）/是；`startDate` 开始日期/date/是；`endDate` 结束日期/date/是；`days` 请假天数/number/是；`reason` 请假事由/textarea/否 | 无 | `apiClient.hr.createLeaveRequest` | toast「已提交审批」后 `navigateBack()` |
| `asset-borrow.json`（`ADMIN_ASSET_BORROW`） | `borrowDate` 领用日期/date/是；`expectedReturnDate` 预计归还日期/date/否；`remark` 领用说明/textarea/否 | 资产选择器（`apiClient.admin.assetPage({status:'IN_STOCK'})`） | `apiClient.admin.assetBorrow(assetId, body)` | `pages/bpm/todo?tab=initiated` |
| `vehicle-apply.json`（`ADMIN_VEHICLE_APPLY`） | `useDate` 用车日期/date/是；`startClock` 开始时间/time/是；`endClock` 结束时间/time/是；`destination` 目的地/text/是；`purpose` 用车事由/textarea/是 | 车辆选择器（`apiClient.admin.vehiclePage`） | `apiClient.admin.vehicleApply(body)` | `pages/bpm/todo?tab=initiated` |
| `meeting-apply.json`（`ADMIN_MEETING_APPLY`） | `useDate` 使用日期/date/是；`startClock` 开始时间/time/是；`endClock` 结束时间/time/是；`subject` 会议主题/text/是 | 会议室选择器（`apiClient.admin.meetingRoomList`）+ 时段占用校验（`meetingAvailability(roomId,date)`） | `apiClient.admin.meetingApply(body)` | `pages/bpm/todo?tab=initiated` |
| `checkin-supplement.json`（`HR_CHECKIN_SUPPLEMENT`） | `checkinDate` 补卡日期/date/是；`checkinTime` 补卡时间/time/是；`checkinType` 补卡类型/单选 IN 上班、OUT 下班/是；`reason` 补卡原因/textarea/是 | 无 | `apiClient.hr.checkinSupplement(body)` | 关闭面板并刷新打卡列表 |

1. 日期 + 时间合并：schema 只产出 `yyyy-MM-dd` 与 `HH:mm`，页面按 `useDate + 'T' + startClock + ':00'` 组装为 `DateTime`（`VehicleApplyRequest.startTime`、`MeetingApplyRequest.startTime` 要求该格式）。
2. 会议室参会人：`attendeeIds` 为可选字段，M6 不提供多选控件，提交时留空；如需参会人，后续另立控件需求。
3. 资产领用必须先在页面选择 `status=IN_STOCK` 的资产，未选择时阻断提交并提示「请选择要领用的资产」；schema 不承载资产下拉。
4. 失败提示：统一走 `apiErrorMessage(err, '提交失败')`；后端 `BIZ_STATE_CONFLICT`（如资产被占用、时段冲突）直接展示原始 message。
5. BPM 模板编码（已核实，`V26__bpm_hr_templates.sql`、`V37__bpm_admin_templates.sql`）：`HR_LEAVE`、`HR_CHECKIN_SUPPLEMENT`、`ADMIN_ASSET_BORROW`、`ADMIN_VEHICLE_APPLY`、`ADMIN_MEETING_APPLY` 均已发布，业务接口内部按编码取模板发起实例，小程序不直接调用 `bpm.startInstance`。

### 9.5 打卡

**结论**：打卡页已在 M5 实现（`pages/hr/checkin.vue`），M6 只做补卡面板 schema 化，采集与判定逻辑不变。

1. 采集流程：`uni.getLocation({type:'gcj02'})` 取经纬度 → `uni.getConnectedWifi()` 取 `SSID/BSSID` → `apiClient.hr.checkinClock({checkinType, latitude, longitude, wifiSsid, wifiBssid, source:'MINI'})`。
2. 与原始表述的差异（已核实）：实现使用 `uni.getConnectedWifi`（仅当前连接 WiFi，无需 `startWifi`），**不使用** `uni.getWifiList`；后者需先授权 WiFi 列表且 iOS 限制更多，M6 不改造。
3. 降级策略：定位失败 `resolve({})`、WiFi 失败返回空串（`MP-WEIXIN` 条件编译外直接返回空），**仍允许提交**，仅不上报对应字段；`source` 固定 `MINI`（后端缺省亦为 `MINI`）。权限拒绝时静默降级，不弹二次授权、不阻断打卡；M6 保持该行为。
4. 迟到早退判定：完全由后端完成（`HrCheckinServiceImpl.clock` 以排班上班时间 + 宽限分钟、下班时间 − 宽限分钟为界判定 `NORMAL/LATE/EARLY`），前端不做任何时间判定，只展示 `status`。
5. 校验口径（已核实）：类注释明确「本期不校验 WiFi 与地理位置，仅留痕」，因此经纬度/SSID/BSSID 只入库不参与判定；若后续要启用位置校验，需另立需求并同步改 `HrCheckinServiceImpl`。
6. 防重复：前端 `clocking` 状态位阻断连点；后端按 `(employee_id, checkin_date, checkin_type)` 计数，重复提交返回 `BIZ_STATE_CONFLICT`「今日已存在…打卡记录」；打卡成功后按钮置灰（`checkedIn/checkedOut`）。
7. 补卡：面板改为 `SchemaForm`（`CHECKIN_SUPPLEMENT`），提交 `apiClient.hr.checkinSupplement`，成功后刷新当月打卡并提示「补卡申请已提交」。

### 9.6 工资条与绩效自评

**结论**：两页 M5 已实现，M6 仅补 1 个后端只读字段（`canSelfEvaluate`），不改变接口与数据范围。

| 项 | 现状（已核实） | M6 变更 |
|---|---|---|
| 工资条列表 | `pages/hr/salary-slip.vue` 调 `apiClient.hr.mySalarySlips()`（`GET /api/v1/hr/payroll/slips/my`） | 无 |
| 工资条详情 | 同页展开，字段取 `SalarySlipVO`：`calcMonth`、`baseSalary`、`postSalary`、`performanceAmount`、`allowance`、`attendanceDeduction`、`socialInsurance`、`housingFund`、`tax`、`grossPay`、`netPay`、`attendanceDays`、`absentDays`、`lateCount`、`readStatus`、`publishedAt` | 无 |
| 标记已读 | `apiClient.hr.salarySlipMarkRead(id)` | 无 |
| 绩效自评 | `pages/hr/performance-self.vue` 调 `apiClient.hr.myPerformanceReviews({page,size})`（`GET /api/v1/hr/performance/reviews/my`，仅分页参数）+ `apiClient.hr.performanceSelf(id, body)`（`POST /api/v1/hr/performance/reviews/{id}/self`） | 可编辑判定改为后端字段 |

1. 数据范围：`mySalarySlips` / `myPerformanceReviews` 均由后端按 `employeeService.currentEmployeeId()` 过滤，只返回本人记录；前端不传 `employeeId`，无法横向越权；HR 导出走 Web 端 `hr:salary-slip:export`（169，V30 仅授予 SYS_ADMIN，前端 `PERMISSION_ROLES` 额外放行 FINANCE，与后端导出接口一致）。
2. 可编辑判定：`PerformanceReviewVO` 当前无 `canSelfEvaluate`，前端用 `status==='DRAFT'` 推断，与后端 `HrPerformanceServiceImpl.self()` 的校验重复。M6 在 `PerformanceReviewVO` **新增只读字段 `Boolean canSelfEvaluate`**，由服务层按「`status=DRAFT` 且 `employeeId=当前员工` 且 `selfSubmittedAt` 为空」计算；小程序改用该字段控制输入框 `disabled` 与「提交自评」按钮显隐。
3. 绩效自评字段：`templateItems` 渲染评分项、`selfItems` 回填、`selfScore` 实时合计、`selfComment` 说明；提交后状态转 `SELF_SUBMITTED`，页面只读展示 `selfSubmittedAt`。
4. 员工未绑定：两页均以 `isEmployeeUnbound(err)` 判定并展示「请联系 HR 绑定员工档案」，不抛原始错误。

### 9.7 AI 只读工具

**结论**：新增 6 个 `ReadOnlyTool` 实现（`com.ceams.module.ai.tool`），只调用业务服务的新增只读聚合方法，**不在 AI 模块写 SQL、不注入 Mapper**（`ReadOnlyTool` 的既有约束）。

| 工具名 | 类 | 入参 | 出参（VO） | 鉴权与数据范围 | 服务方法来源 |
|---|---|---|---|---|---|
| `queryHrHeadcount` | `HrHeadcountTool` | 无（可选 `deptId`） | `List<HeadcountDeptVO>`：部门、总人数、试用、在职、离职 | 仅 SYS_ADMIN（全量） | 新增 `HrEmployeeService.headcountByDept(Long deptId)` |
| `queryAttendanceSummary` | `AttendanceSummaryTool` | `start`、`end`（yyyy-MM-dd，必填）、`deptId` | `AttendanceSummaryVO`：人数、正常/迟到/早退/缺卡次数、补卡数 | SYS_ADMIN 全量（可按部门）；STAFF 仅本人 | 新增 `HrCheckinService.summary(start,end,deptId,employeeId)` |
| `queryPayrollSummary` | `PayrollSummaryTool` | `month`（yyyy-MM，必填）、`deptId` | `PayrollCostSummaryVO`：人数、应发、实发、社保、公积金、个税、人均实发 | SYS_ADMIN 全量；FINANCE 全量；STAFF 拒绝 | 新增 `HrPayrollService.costSummary(String month, Long deptId)` |
| `queryAssetInventory` | `AssetInventoryTool` | 无（可选 `category`） | `AssetInventorySummaryVO`：总数、在库/在用/维修/报废数、总价值、在借数、逾期数 | 仅 SYS_ADMIN | 新增 `AdminAssetService.inventorySummary(String category)` |
| `queryVehicleUsage` | `VehicleUsageTool` | `start`、`end`、可选 `vehicleId` | `VehicleUsageSummaryVO`：申请数、已通过数、总里程、总费用、按车辆明细 | 仅 SYS_ADMIN | 新增 `AdminVehicleService.usageSummary(start,end,vehicleId)` |
| `queryRepairStats` | `RepairStatsTool` | `start`、`end`、可选 `category` | `RepairStatsVO`：工单数、待受理/处理中/已完成数、完成率、平均处理时长 | SYS_ADMIN 全量；STAFF 仅本人提交 | 新增 `AdminRepairService.stats(start,end,category,employeeUserId)` |

1. 注册方式：`ToolRegistry` 通过构造注入 `List<ReadOnlyTool>` 自动收集，只需把 6 个工具名加入 `ALLOWED_TOOL_NAMES` 白名单（9→15），启动时白名单与实现不一致仍按现有逻辑直接失败。
2. 参数解析：沿用 `ToolArgs.text/longValue/enumValue/limit`；日期参数用正则校验 `yyyy-MM-dd`，月份校验 `yyyy-MM`，非法值返回 `ToolResult.denied("参数格式不正确：…")`。
3. 出参裁剪：行数上限沿用 `ToolResult.capped(rows, totalRows, maxRows)` 与 `ToolArgs.limit(args, aiProperties.getMaxToolRows())`（默认 20、最大 200，`PendingApprovalsTool`/`CashflowTool` 同款写法）；金额统一为字符串（`BigDecimal.toPlainString()`），禁止输出员工姓名+薪资组合。
4. 越权策略：`ToolSecurity` 新增 `isAdmin()`、`isFinance()` 便捷方法；无权限统一返回 `ToolResult.denied("无权查询该数据")`，不区分「不存在」与「无权限」。
5. 薪资明细拒绝：在 `AiChatServiceImpl` 新增 `SALARY_DETAIL_PATTERN`（包含「工资条」「实发」「应发」「个税」「某人的工资」等）与 `SALARY_DETAIL_REFUSAL` 常量，命中即返回固定话术并引导至「我的工资条」，不进入工具循环；`queryPayrollSummary` 本身永不返回个人明细。
6. 系统提示：`AiChatServiceImpl.buildSystemPrompt()` 的「可用数据范围」段落追加 HR/行政 6 类只读口径说明，并明确「不提供任何个人薪资明细、不推测未返回的数据」。
7. 越权兜底：`DATA_QUESTION_KEYWORDS` 追加「人数、考勤、工资、资产、车辆、报修」等词，保证无工具调用的纯文本回答也不会臆造数字。

### 9.8 报表指标

**结论**：4 个只读接口挂在既有 `ReportController`（`/api/v1/reports`）下，均返回单一聚合 VO；前端卡片按权限显隐，不改变 `DashboardVO` 与既有 dashboard 接口。

| 接口 | 权限码 | 查询参数 | 返回字段 | 聚合口径 |
|---|---|---|---|---|
| `GET /api/v1/reports/hr-cost` | `report:hr-cost:view`（187） | `month`（yyyy-MM，必填）、`deptId` | `month`、`headcount`、`payrollHeadcount`、`grossPay`、`netPay`、`socialInsurance`、`housingFund`、`tax`、`avgNetPay`、`byDept[]` | `HrPayrollService.costSummary(month, deptId)` 在 Java 内解密 `hr_salary_calc` 后求和（**禁止 SQL SUM 加密列**）；`headcount` 取 `HrEmployeeService.headcountByDept` |
| `GET /api/v1/reports/hr-performance` | `report:hr-performance:view`（188） | `period`（yyyy-MM 或 yyyy-Qn，必填）、`deptId` | `period`、`reviewCount`、`finalizedCount`、`avgFinalScore`、`distribution`（A ≥90、B 80–89、C 70–79、D <70）、`byDept[]` | `ReportMapper.selectHrPerformance`：`hr_performance_review` 按 `period` 聚合 `final_score`，部门维度 join `hr_employee.dept_id` |
| `GET /api/v1/reports/admin-assets` | `report:admin-assets:view`（189） | `category`（可选） | `totalCount`、`totalValue`、`byStatus`、`byCategory`、`borrowingCount`、`overdueCount`、`borrowRate` | `admin_asset`（`purchase_price` 明文）按 `status/category` 分组；借用率 = 在借数 / 总数；逾期取 `admin_asset_borrow.status='BORROWING' AND expected_return_date < CURDATE()` |
| `GET /api/v1/reports/bpm-efficiency` | `bpm:report:view`（149，复用） | `start`、`end`、`bizType`（可选） | `instanceCount`、`approvedCount`、`avgDurationHours`、`overdueTaskCount`、`overdueRate`、`byBizType[]`、`backlogByNode[]` | `bpm_instance` 平均时长 = `AVG(TIMESTAMPDIFF(SECOND, started_at, ended_at))/3600`，仅统计 `status='APPROVED'`；超时率 = `status='PENDING' AND due_at < NOW()` / 任务总数 |

1. 实现约束：延续 `ReportServiceImpl` 只读模式（只调 `ReportMapper` 查询与业务服务只读方法，不开事务写、不做缓存写）；`hr-cost` 是唯一跨模块聚合，其余 3 个在 `ReportMapper.xml` 内完成。
2. 鉴权实现：既有 4 个报表接口均为 `@PreAuthorize("isAuthenticated()")`，项目后端目前不按权限 `code` 做 `hasAuthority` 校验（全仓无 `hasAuthority` 用法），因此新接口按权限码对应的角色显式声明：`hr-cost`、`hr-performance` 用 `@PreAuthorize("hasAnyRole('SYS_ADMIN')")`，`admin-assets` 用 `@PreAuthorize("hasAnyRole('SYS_ADMIN')")`，`bpm-efficiency` 用 `@PreAuthorize("hasAnyRole('SYS_ADMIN','FINANCE')")`；权限码仅用于前端显隐与权限树，二者需保持一致。
3. 空数据处理：`hr-performance` 无数据时 `avgFinalScore=0`、`distribution` 返回 4 个 0 桶；`bpm-efficiency` 无实例时 `avgDurationHours=0`，不返回 null。
4. 前端 service：`packages/api-client/src/domains/report.ts` 新增 `hrCost(query)`、`hrPerformance(query)`、`adminAssets(query)`、`bpmEfficiency(query)`；`packages/types/src/report.ts` 新增对应 4 个 VO 与 Query 类型。
5. 卡片位置：`apps/web/src/views/report/ReportView.vue` 在两行既有 `card-row` 之后新增第 3 行 `el-row :gutter="16"`，4 个 `el-col :span="6"` 卡片：在职人数（含试用）、本月人力成本（`netPay`）、在借资产（含逾期）、审批超时率；每张卡片 `v-if="hasPermission(code)"`。
6. 页签：`activeTab` 增加 `hr-cost`、`hr-performance`、`admin-assets`、`bpm-efficiency` 四个 `el-tab-pane`，沿用现有 `loadedXxx` 懒加载模式；页签本身同样按权限 `v-if`。
7. 降级：无权限时卡片与页签不渲染、不发请求；接口返回 403 时 `apiErrorMessage` 提示「无权限查看该报表」并隐藏对应页签，不影响既有 3 个页签。

### 9.9 通知扩展

**结论**：`NotificationType` 增加 `HR/ADMIN/DOC` 三个枚举值；通知中心新增业务域筛选；5 类业务事件接入既有 `NotificationService.createAndPublishToUsers`。

| 枚举 | 用途 | 接入点 |
|---|---|---|
| `HR` | 工资条发布、合同到期 | `HrSalarySlipServiceImpl.publish`、`HrAdminReminderScheduler` |
| `ADMIN` | 资产逾期、报修派单 | `AdminAssetService.listOverdueBorrows` 调用方、`AdminRepairServiceImpl.assign` |
| `DOC` | 文档共享（本期仅建枚举与筛选，不新增触发点） | 预留 |
| `APPROVAL` | BPM 待办/抄送/超时（既有） | `BpmNotificationPublisher` |

| 事件 | 接入类.方法 | 标题 | 深链 |
|---|---|---|---|
| 薪资发布 | `HrSalarySlipServiceImpl.publish(SalarySlipPublishRequest)` 内逐员工发送（`hr_employee.user_id` 即通知接收人） | 工资条已发布（yyyy-MM） | `ceams://hr/salary-slip` |
| 合同到期 | 新增 `HrAdminReminderScheduler`，调 `HrContractService.expiring(30)`（`ContractVO.employeeId` → 经 `HrEmployeeService` 解析 `userId`） | 劳动合同即将到期（30 天内） | `ceams://hr/contract/{id}` |
| 资产逾期 | 新增 `AdminAssetService.listOverdueBorrows(LocalDate)`，由同一调度器调用 | 资产借用已逾期 | `ceams://admin/asset-borrow/{id}` |
| 报修派单 | `AdminRepairServiceImpl.assign(Long id, RepairAssignRequest)` 首次派单成功（`assignee_id`、`assigned_at` 由空写为非空）后，收件人为 `request.getAssigneeId()` | 新报修工单已派给您 | `ceams://admin/repair/{id}` |
| BPM 待办 | 既有 `BpmNotificationPublisher.onTaskCreated` | 审批待办提醒 | `ceams://todo?tab=pending&taskId=&instanceId=` |

1. 通知查询筛选：`UserNotificationService` 新增重载 `mine(long page, long size, NotificationType type)`，原两参方法委托新方法；`UserNotificationController` 的 `GET /api/v1/notifications/user-notifications/mine` 增加可选 `type` 参数（非法枚举由 Spring 转换失败返回 400）。
2. 查询实现：`UserNotificationMapper` 新增 `@Select` 联表方法（`biz_user_notification` join `biz_notification`，按 `user_id`、`n.notification_type`、`un.deleted=0` 过滤，按 `create_time` 倒序），避免先查全部再内存过滤。
3. 前端筛选：Web `NotificationView.vue` 增加「全部/审批/HR/行政/文档」单选组；小程序 `pages/notification/list.vue` 增加横向 chips，切换即重查。
4. 去重策略：
   - 薪资：发布本身按 `(employee_id, calc_month)` 幂等，仅首次发布成功时发送，重复发布（记录已存在被跳过）不发送。
   - 合同到期/资产逾期：`NotificationService` 新增 `boolean existsRecent(NotificationType type, String linkUrl, LocalDateTime since)`，按 `(type, link_url)` 在 24 小时内已发则跳过；深链内含业务 ID，天然按业务单据去重。
   - 报修派单：仅当 `assigned_at` 由空写为非空（首次派单）时发送；重复派单仅更新 `assignee_id`，按 `(type, ceams://admin/repair/{id})` 24 小时去重。
   - BPM：沿用 `bpm_task.reminded_at` 条件更新抢占，同一任务仅提醒一次。
5. 文案映射：`apps/miniprogram/src/utils/labels.ts` 的 `notificationTypeLabels: Record<NotificationType, string>` 为穷举类型，新增 3 个枚举值必须同步补「人力/行政/文档」条目，否则 `typecheck` 失败；Web 端同步补 `packages/types` 侧标签。
6. 事务与降级：新接入点统一放在独立事务（`PROPAGATION_REQUIRES_NEW`）并 try/catch 吞异常 + warn 日志，与 `BpmNotificationPublisher` 一致，通知失败不影响主业务。

### 9.10 测试与验收

**结论**：后端测试基线 488 个 `@Test`（`src/test/java` 实测），M6 新增 23 例达到 511，满足 ≥505；前端与小程序以构建 + 类型检查 + 真机冒烟为准。

| 测试类 | 覆盖点 | 用例数 |
|---|---|---|
| `ReportM6ServiceImplTest` | 4 个报表接口的空数据、正常聚合、权限拒绝、`hr-cost` 不出现 null 金额 | 4 |
| `HrPayrollCostSummaryTest` | 加密金额解密后汇总、按部门聚合、无核算月返回零值 | 3 |
| `HrEmployeeHeadcountTest` | 按状态计数、按部门分组 | 2 |
| `AdminStatsSummaryTest` | 资产库存/借用率、车辆使用与费用、报修完成率 | 3 |
| `HrCheckinSummaryTest` | 正常/迟到/早退/缺卡计数、补卡数 | 2 |
| `M6ReadOnlyToolSecurityTest` | 6 个工具注册、STAFF 越权拒绝、FINANCE 查薪资汇总放行、薪资明细话术拦截 | 4 |
| `UserNotificationTypeFilterTest` | `type` 过滤、无 `type` 兼容旧行为 | 2 |
| `M6ReminderNotificationTest` | 合同到期 24h 去重、资产逾期去重 | 2 |
| `PerformanceSelfFlagTest` | DRAFT 本人 `canSelfEvaluate=true`、非本人 false | 1 |

另需修改 `ToolRegistryTest`：`EXPECTED_NAMES` 增加 6 个工具名，两处 `assertEquals(9, …)` 改为 15（不新增用例）。

**检查命令**：

```bash
mvn test
pnpm --filter @ceams/web build
pnpm --filter @ceams/miniprogram typecheck
pnpm --filter @ceams/miniprogram build:mp-weixin
```

**验收标准**：

1. 小程序四类待办与 Web `/todo` 数据同源一致（同一账号同页签条数、状态、跳转一致）。
2. 四张新增动态表单可提交并生成对应 BPM 实例（`bizType` 与模板编码匹配，实例出现在「我发起的」）。
3. 工资条仅本人可见；未绑定员工返回 409 提示，不返回他人数据。
4. 6 个 AI 工具越权返回「无权查询该数据」，薪资明细类提问统一被拒并引导至工资条页。
5. 4 个报表接口数值与 SQL 直查一致（对同一月份/周期抽样比对，误差为 0）。

**冒烟剧本（6 步）**：

1. 小程序登录 → 首页仅显示角色允许的入口（SYS_ADMIN 可见行政，STAFF 不可见）。
2. 「我的待办」→ 四条页签切换 → 列表有数据 → 打开详情 → 通过一条 → 返回列表，该条消失且「我已办」出现。
3. 资产领用：选择在库资产 → 选择领用日期 → 提交 → 「我发起的」出现 `ADMIN_ASSET_BORROW` 实例。
4. 打卡：拒绝定位 → 仍可提交 → 列表出现 `source=MINI` 记录 → 再次点击上班卡提示「请勿重复打卡」。
5. Web 报表：SYS_ADMIN 打开 `/report`，4 张新卡片有值、4 个新页签可切换；无权限账号看不到卡片与页签。
6. 通知：工资条发布后员工收到 `HR` 通知 → 通知中心按「HR」筛选命中 → 点击跳转工资条页。

**风险与应对**：

| 风险 | 影响 | 应对 |
|---|---|---|
| iOS 无法获取 WiFi 名称（系统限制） | SSID/BSSID 为空 | 保留为空并可提交，考勤仅留痕不校验；提示文案区分「未获取」与「打卡失败」 |
| AI 工具越权或聚合口径泄露个人数据 | 敏感信息外泄 | 工具层 `ToolSecurity` 角色判定 + 出参不含姓名与薪资组合 + 明细提问直接拒绝 |
| 报表口径与库内数据不一致 | 验收失败、决策失真 | 报表与 AI 工具共用同一批 Service 方法与 VO，验收第 5 条做抽样比对 |
| 通知重复（定时任务重跑/多实例） | 打扰用户 | 合同/资产按 `(type, link_url, 24h)` 去重；BPM 用 `reminded_at` 抢占；调度默认单实例 |
| 真机测试依赖开发者工具与微信授权 | 无法覆盖真实 WiFi/定位降级 | 开发者工具模拟权限拒绝 + 真机各验 1 次；降级路径以「可提交」为准 |
| `report` 菜单码仅授 SYS_ADMIN | FINANCE 看不到新报表 | 按需另起 V43 追加 (2,187)、(2,188)，本次不扩大授权 |

---

## 十、迁移编号与实施顺序

| 版本 | 文件 | 内容 |
|---|---|---|
| V23 | `V23__hr_employee_contract.sql` | hr_position、hr_employee、hr_contract |
| V24 | `V24__hr_attendance.sql` | hr_leave_quota、hr_schedule、hr_checkin |
| V25 | `V25__hr_permission_seed.sql` | HR 菜单与权限点（157～163 / 1089～1095） |
| V26 | `V26__bpm_hr_templates.sql` | HR 流程模板（转正/调岗/离职/请假/补卡，9003～9007） |
| V27 | `V27__hr_salary.sql` | hr_salary、hr_salary_change_apply |
| V28 | `V28__hr_performance.sql` | hr_performance_template、hr_performance_review |
| V29 | `V29__hr_payroll.sql` | hr_salary_calc、hr_salary_slip |
| V30 | `V30__hr_salary_permission_seed.sql` | HR 薪酬绩效权限点（164～169 / 1096～1102） |
| V31 | `V31__bpm_hr_salary_templates.sql` | 薪资变动/更正审批模板（9008/9108、9009/9109） |
| V32 | `V32__hr_performance_items.sql` | 绩效明细打分列 + 绩效审批模板（9010/9110） |
| V33 | `V33__admin_asset_material.sql` | 资产、借还、易耗品、申领 |
| V34 | `V34__admin_vehicle_meeting.sql` | 车辆、用车、费用、会议室、预定 |
| V35 | `V35__admin_visitor_repair.sql` | 访客、报修 |
| V36 | `V36__admin_permission_seed.sql` | 行政权限点（170～177 / 1103～1110） |
| V37 | `V37__bpm_admin_templates.sql` | 行政流程模板（9011～9015 / 9111～9115） |
| V38 | `V38__doc_knowledge_base.sql` | 知识库 5 张表 |
| V39 | `V39__doc_permission_seed.sql` | 知识库权限点 |
| V40 | `V40__wecom_integration.sql` | 同步日志、消息发件箱、sys_user/sys_dept 字段 |
| V41 | `V41__wecom_permission_seed.sql` | 企微集成权限点 |

依赖顺序：M1(V23-V26) → M2(V27-V32) → M3(V33-V37) → M4(V38-V39) → M5(V40-V41)。  
说明：行政模板原计划放 V41，因 Flyway 默认不允许乱序执行，提前到 V37；M4/M5 顺延，历史编号不变。  
每个里程碑完成即执行 `flyway migrate`、后端单测、前端 typecheck/build 与接口冒烟。

---

## 十一、测试与验收

### 11.1 单元测试

- HR：薪资计算（正常/缺勤/绩效/手工调整/四舍五入）、离职资产校验、假期额度扣减与归还。
- 行政：库存扣减防负、会议室冲突、车辆状态流转。
- 知识库：权限合并（目录 + 文件）、编辑锁、版本回滚。
- 企微：同步幂等、消息重试退避、扫码登录绑定失败分支。

### 11.2 集成与并发

- 会签/或签并发审批、撤回与审批并发（沿用 BPM 乐观锁测试）。
- 工资核算重复初始化幂等。
- 资产领用并发扣减、会议室并发预定。
- OnlyOffice 回调重复保存幂等。

### 11.3 安全

- 薪资、身份证、手机号越权访问测试。
- 工资条导出权限测试。
- 知识库越权下载/预览测试。
- 企微回调 JWT 校验、扫码 state 防重放。

### 11.4 回归

- 报销、项目变更审批全流程回归。
- BPM 待办中心四类页签回归。
- 通知中心与企业微信推送回归。
- AI 助手只读问答回归。

### 11.5 命令

后端：

```powershell
mvn -o -q '-Dmaven.repo.local=D:\apache-maven-3.9.8\mvn_repo' '-Dtest=<测试类>' test
mvn -o -q '-Dmaven.repo.local=D:\apache-maven-3.9.8\mvn_repo' test
```

前端：

```powershell
pnpm --filter @ceams/web typecheck
pnpm --filter @ceams/web build
pnpm --filter @ceams/miniprogram typecheck
pnpm --filter @ceams/miniprogram build:mp-weixin
```

---

## 十二、风险与待确认

| 风险 | 影响 | 应对 |
|---|---|---|
| 薪资核算口径未最终确认 | 工资结果偏差 | 先落地可配置规则，财务确认社保/公积金/个税参数后再启用正式核算 |
| OnlyOffice 网络与授权 | 无法在线编辑 | 独立容器 + 内网回调；JWT 必开；本地文件服务短时 token |
| 企业微信凭证与可见范围 | 同步/登录失败 | 配置校验启动自检；失败告警；群机器人可作为兜底通知 |
| 小程序 WiFi 信息获取限制 | 打卡校验不完整 | WiFi 与定位双因子，缺失时标记异常并允许补卡 |
| 薪资字段加密与密钥管理 | 数据泄露或无法解密 | 复用现有加密 TypeHandler，密钥仅环境变量注入，提供轮换预案 |
| 历史数据迁移 | 员工与用户绑定缺失 | 提供上线前对账脚本，未绑定员工禁止发起审批 |

待确认事项：

1. 社保、公积金、个税的具体基数与比例由财务提供。
2. 绩效系数区间与模板指标由 HR 确认。
3. 企业微信自建应用凭证、可信域名、回调地址。
4. OnlyOffice 部署域名与证书方案。
5. 知识库各库默认权限矩阵（个人/部门/项目/公共）。

---

## 十三、交付物清单

| 类别 | 交付物 |
|---|---|
| 数据库 | V23～V41 增量迁移脚本 |
| 后端 | `hr`、`admin`、`doc`、`integration` 四个模块及单测 |
| 前端 Web | HR、行政、知识库、企微集成页面与路由 |
| 小程序 | 四类待办、HR/行政动态表单、打卡、工资条、绩效自评 |
| 共享包 | `packages/types`、`packages/api-client` 新增域 |
| 文档 | 本开发文档、接口说明、部署配置说明、验收记录 |
| 部署 | `docker-compose.yml` 增加 OnlyOffice，环境变量清单 |
