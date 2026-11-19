# CEAMS OpenAPI v1 契约冻结说明

本目录存放 CEAMS 重构后的 OpenAPI 3.0.x 契约文档，是后端实现、前端 api-client 类型生成与三端联调的唯一契约来源。

## 冻结范围（本波）

| 文件 | 域 | 说明 |
|---|---|---|
| `iam.openapi.yaml` | iam（身份与访问） | 登录 / 双轮换 JWT / RBAC / 设备绑定 / 操作日志 |
| `masterdata.openapi.yaml` | masterdata（主数据） | 分类树 / 往来单位 / 材料库 / 设备台账与租赁 |
| `project.openapi.yaml` | project（项目合同） | 项目台账 / 预算 / 变更 / 进度款 / 分包 / 结算凭证 |

其余 5 域（expense / payment / bank / seal / notification）与跨域 report 属后续阶段，暂不在本目录产出契约，届时追加 `*.openapi.yaml` 并保持统一规范。

## 已冻结的全局决策

1. **路径**：`/api/v1/{group}/...`，`group` 为域标识（`auth` / `masterdata` / `project` / `expense` / `payment` / `bank` / `seal` / `notification`）。iam 域的鉴权与账号路径统一挂 `auth` 前缀；用户/角色/权限等管理路径挂 `users` / `roles` / `permissions` 等资源名。
2. **统一响应体**：

   ```json
   { "code": 0, "message": "success", "data": {}, "traceId": "uuid", "timestamp": "2026-10-28T10:00:00Z" }
   ```

   - `code = 0` 表示业务成功；非 0 为业务错误码（RFC7807 语义 + 业务码前缀）。
3. **分页结构** `PageResult`：

   ```json
   { "records": [], "total": 0, "page": 1, "size": 20 }
   ```

   请求参数统一 `page`（默认 1）、`size`（默认 20，上限 100）。
4. **错误码**：遵循 RFC7807，HTTP 状态码严格匹配语义；业务码前缀：
   - `AUTH_`：认证/授权（如 `AUTH_INVALID_CREDENTIALS`、`AUTH_TOKEN_EXPIRED`）
   - `VALID_`：参数/校验（如 `VALID_REQUIRED_FIELD`）
   - `BIZ_`：业务规则（如 `BIZ_DUPLICATE_CODE`）
   - `SIGN_`：签名（如 `SIGN_TIMESTAMP_EXPIRED`、`SIGN_NONCE_REPLAY`）
5. **鉴权头**：`Authorization: Bearer <access>`；access token 15min，refresh token 7d 轮换且校验吊销状态。
6. **高风险接口签名**：涉及 `X-Sign` 的接口（seal.stamp / expense.approve / payment.execute 等，本波仅预留标注）额外携带：
   - `X-Sign: HMAC-SHA256(timestamp + nonce + bodyHash)`（bodyHash 为请求体 SHA-256，hex）
   - `X-Timestamp`：Unix 毫秒时间戳
   - `X-Nonce`：客户端随机字符串（服务端 ±5min 窗口 + Redis nonce 防重放）
7. **类型序列化约定**：
   - ID 一律 `int64`（雪花 ID），JSON 序列化为**字符串**。
   - 金额一律 `decimal(18,2)` / `BigDecimal`，JSON 序列化为**字符串**。
   - 时间一律 `LocalDateTime`，序列化为 **ISO8601**（`yyyy-MM-dd'T'HH:mm:ss`，无时区偏移，北京时间语义由前端按站点时区解析）。
   - 空列表返回 `[]`，不返回 `null`。

## 变更管理

- 契约冻结后，任何破坏性变更须单独提 PR，并在 MR 中附兼容性评审结论。
- 新增字段遵循向后兼容原则（新增可选字段、新增接口不得删除既有字段）。