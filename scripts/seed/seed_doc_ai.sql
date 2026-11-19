-- =====================================================================
-- 文件：scripts/seed/seed_doc_ai.sql
-- 用途：为本地开发库 ceams 补充「文档知识库 + AI + 通知 + 企业微信集成 + 设备 + 代理」
--       演示数据，用于接口联调与前端页面展示。
-- 幂等：所有表均显式指定主键并使用 INSERT IGNORE；主键号段统一为
--       918000000000000xxxx（与 910000000000000xxxx、920000000000000xxxx 号段不冲突）。
--       注意：初始拟用的 9180000000000000001 超过 MySQL 有符号 BIGINT 上限
--       （9223372036854775807），会被截断导致主键冲突，故下移一位改用 918 号段。
--       重复执行不会报错、不会产生重复行，也不 UPDATE/DELETE 任何既有数据。
-- 号段：
--       0001-0004 文档目录          0011-0012 目录授权
--       0101-0106 文档文件          0121-0129 文档版本（6 条当前版本 + 3 条历史版本）
--       0021-0022 文件授权
--       0201-0203 AI 会话           0301-0312 AI 消息
--       0401-0402 AI 日报摘要       0501-0502 AI 反馈
--       0601-0608 通知主表          0621-0628 用户通知
--       0701-0704 通知偏好
--       0801-0804 企微消息          0901 企微同步批次
--       1001-1002 用户设备          1101 BPM 代理
-- 约束：不修改任何 Flyway 迁移文件（V1~V43 不新增/不修改）；不修改后端代码。
-- 敏感列：sys_user_device.app_secret 挂了 SensitiveTypeHandler，本脚本写入
--       v1: 前缀 AES-256-GCM 密文（IV12 || 密文 || Tag16，Base64），
--       密钥为开发默认值 Y2VhbXMtc2Vuc2l0aXZlLWRldi1rZXktMjAyNi0wMTI=；
--       其余本阶段表无敏感列。
-- 执行（Windows/PowerShell 下中文最稳）：
--   docker cp scripts/seed/seed_doc_ai.sql ceams-mysql:/tmp/seed_doc_ai.sql
--   docker exec -i ceams-mysql sh -c "mysql -uroot -proot --default-character-set=utf8mb4 ceams < /tmp/seed_doc_ai.sql"
-- =====================================================================

SET NAMES utf8mb4;

-- ---------------------------------------------------------------------
-- 1. 文档知识库目录（4 个：3 个一级 + 1 个二级）
--    库类型取 LibraryType 真实枚举：PUBLIC / DEPT；
--    可见性：PUBLIC 全员可见；DEPT 为归属部门本部门及祖先链可见。
--    「制度文件」公共库，create_by=admin；
--    「项目资料」工程部部门库，create_by=admin；
--    「人事资料」人事行政部部门库，create_by=hr01；
--    「财务报表」为「项目资料」的二级子目录（工程部），create_by=hr01。
-- ---------------------------------------------------------------------
INSERT IGNORE INTO doc_directory
    (id, name, parent_id, library_type, owner_user_id, owner_dept_id, project_id, path,
     sort_order, status, create_time, update_time, create_by, update_by, deleted, version)
VALUES
    (9180000000000000001, '制度文件', NULL, 'PUBLIC', NULL, NULL, NULL,
     '/9180000000000000001/', 10, 'NORMAL', NOW(), NOW(), 1, 1, 0, 0),
    (9180000000000000002, '项目资料', NULL, 'DEPT', NULL, 9100000000000000013, NULL,
     '/9180000000000000002/', 20, 'NORMAL', NOW(), NOW(), 1, 1, 0, 0),
    (9180000000000000003, '人事资料', NULL, 'DEPT', NULL, 9100000000000000012, NULL,
     '/9180000000000000003/', 30, 'NORMAL', NOW(), NOW(), 9100000000000000003, 9100000000000000003, 0, 0),
    (9180000000000000004, '财务报表', 9180000000000000002, 'DEPT', NULL, 9100000000000000013, NULL,
     '/9180000000000000002/9180000000000000004/', 10, 'NORMAL', NOW(), NOW(), 9100000000000000003, 9100000000000000003, 0, 0);

-- ---------------------------------------------------------------------
-- 2. 目录级授权（2 条）
--    actions 取 DocAction 真实枚举（PREVIEW/DOWNLOAD/EDIT/DELETE）的 JSON 数组；
--    subject_type 取 DocSubjectType（USER/ROLE/DEPT）。
--    01011：项目资料（工程部）授权 HR 角色，hr01 可通过权限看到工程部目录；
--    01012：人事资料授权 staff01 本人，staff01 可通过权限看到人事资料。
-- ---------------------------------------------------------------------
INSERT IGNORE INTO doc_dir_permission
    (id, dir_id, subject_type, subject_id, actions, granted_by,
     create_time, update_time, create_by, update_by, deleted, version)
VALUES
    (9180000000000000011, 9180000000000000002, 'ROLE', 9100000000000000021,
     '["PREVIEW","DOWNLOAD"]', 9100000000000000003, NOW(), NOW(), 1, 1, 0, 0),
    (9180000000000000012, 9180000000000000003, 'USER', 9100000000000000004,
     '["PREVIEW","DOWNLOAD","EDIT"]', 9100000000000000003, NOW(), NOW(), 1, 1, 0, 0);

-- ---------------------------------------------------------------------
-- 3. 文档文件（6 个：Word/Excel/PDF 各 2）
--    存储键沿用既有样例格式 doc/202611/<雪花ID>.<ext>；
--    size / storage_key / checksum 与 doc_file_version 的当前版本行保持一致，
--    文件实体本身允许不真实存在（仅用于列表与版本展示）。
-- ---------------------------------------------------------------------
INSERT IGNORE INTO doc_file
    (id, dir_id, name, ext, size, storage_key, checksum, current_version_no, status,
     lock_user_id, lock_time, uploaded_by, download_count,
     create_time, update_time, create_by, update_by, deleted, version)
VALUES
    (9180000000000000101, 9180000000000000001, '员工手册V3.docx', 'docx', 184320,
     'doc/202611/9180000000000000101.docx',
     'a1b2c3d4e5f60718293a4b5c6d7e8f90a1b2c3d4e5f60718293a4b5c6d7e8f90', 1, 'NORMAL',
     NULL, NULL, 9100000000000000003, 12, NOW(), NOW(), 1, 1, 0, 0),
    (9180000000000000102, 9180000000000000001, '财务报销制度.pdf', 'pdf', 524288,
     'doc/202611/9180000000000000102.pdf',
     '0f1e2d3c4b5a69788796a5b4c3d2e1f00f1e2d3c4b5a69788796a5b4c3d2e1f0', 1, 'NORMAL',
     NULL, NULL, 9100000000000000001, 7, NOW(), NOW(), 1, 1, 0, 0),
    (9180000000000000103, 9180000000000000002, '张江二期施工组织设计.docx', 'docx', 1048576,
     'doc/202611/9180000000000000103.docx',
     '11223344556677889900aabbccddeeff11223344556677889900aabbccddeeff', 2, 'NORMAL',
     NULL, NULL, 9100000000000000002, 21, NOW(), NOW(), 1, 1, 0, 0),
    (9180000000000000104, 9180000000000000002, '项目进度计划.xlsx', 'xlsx', 65536,
     'doc/202611/9180000000000000104.xlsx',
     'deadbeefcafebabe0123456789abcdefdeadbeefcafebabe0123456789abcdef', 1, 'NORMAL',
     NULL, NULL, 9100000000000000004, 5, NOW(), NOW(), 1, 1, 0, 0),
    (9180000000000000105, 9180000000000000003, '人事制度汇编.pdf', 'pdf', 786432,
     'doc/202611/9180000000000000105.pdf',
     '9876543210fedcbaabcdef01234567899876543210fedcbaabcdef0123456789', 2, 'NORMAL',
     NULL, NULL, 9100000000000000003, 9, NOW(), NOW(), 1, 1, 0, 0),
    (9180000000000000106, 9180000000000000004, '2026年11月费用明细.xlsx', 'xlsx', 40960,
     'doc/202611/9180000000000000106.xlsx',
     '5a5a5a5a3c3c3c3c1f1f1f1f0e0e0e0e5a5a5a5a3c3c3c3c1f1f1f1f0e0e0e0e', 2, 'NORMAL',
     NULL, NULL, 9100000000000000001, 3, NOW(), NOW(), 1, 1, 0, 0);

-- ---------------------------------------------------------------------
-- 4. 文档版本（9 条）
--    每个文件至少 1 条当前版本；其中 3 个文件（0103/0105/0106）各补 1 条
--    version=1 的历史版本，使版本列表可展示多条记录。
--    source 取 DocVersionSource 真实枚举：UPLOAD / ONLINE_EDIT / ROLLBACK。
-- ---------------------------------------------------------------------
INSERT IGNORE INTO doc_file_version
    (id, file_id, version_no, storage_key, size, checksum, uploaded_by, change_note, source,
     create_time, update_time, create_by, update_by, deleted, version)
VALUES
    (9180000000000000121, 9180000000000000101, 1, 'doc/202611/9180000000000000101.docx', 184320,
     'a1b2c3d4e5f60718293a4b5c6d7e8f90a1b2c3d4e5f60718293a4b5c6d7e8f90',
     9100000000000000003, '首次上传', 'UPLOAD', NOW(), NOW(), 1, 1, 0, 0),
    (9180000000000000122, 9180000000000000102, 1, 'doc/202611/9180000000000000102.pdf', 524288,
     '0f1e2d3c4b5a69788796a5b4c3d2e1f00f1e2d3c4b5a69788796a5b4c3d2e1f0',
     9100000000000000001, '首次上传', 'UPLOAD', NOW(), NOW(), 1, 1, 0, 0),
    (9180000000000000123, 9180000000000000103, 1, 'doc/202611/9180000000000000123.docx', 917504,
     'f00dbabe12345678abcdef00fedcba98f00dbabe12345678abcdef00fedcba98',
     9100000000000000002, '首次上传（初版）', 'UPLOAD', NOW(), NOW(), 1, 1, 0, 0),
    (9180000000000000124, 9180000000000000103, 2, 'doc/202611/9180000000000000103.docx', 1048576,
     '11223344556677889900aabbccddeeff11223344556677889900aabbccddeeff',
     9100000000000000002, '按评审意见修订施工部署', 'ONLINE_EDIT', NOW(), NOW(), 1, 1, 0, 0),
    (9180000000000000125, 9180000000000000104, 1, 'doc/202611/9180000000000000104.xlsx', 65536,
     'deadbeefcafebabe0123456789abcdefdeadbeefcafebabe0123456789abcdef',
     9100000000000000004, '首次上传', 'UPLOAD', NOW(), NOW(), 1, 1, 0, 0),
    (9180000000000000126, 9180000000000000105, 1, 'doc/202611/9180000000000000126.pdf', 712704,
     'c0ffee00b16b00b5dead10cc0badf00dc0ffee00b16b00b5dead10cc0badf00d',
     9100000000000000003, '首次上传（初版）', 'UPLOAD', NOW(), NOW(), 1, 1, 0, 0),
    (9180000000000000127, 9180000000000000105, 2, 'doc/202611/9180000000000000105.pdf', 786432,
     '9876543210fedcbaabcdef01234567899876543210fedcbaabcdef0123456789',
     9100000000000000003, '补充2026年考勤与假期制度', 'UPLOAD', NOW(), NOW(), 1, 1, 0, 0),
    (9180000000000000128, 9180000000000000106, 1, 'doc/202611/9180000000000000128.xlsx', 38912,
     '13579bdf2468ace002468ace13579bdf13579bdf2468ace002468ace13579bdf',
     9100000000000000001, '首次上传（初版）', 'UPLOAD', NOW(), NOW(), 1, 1, 0, 0),
    (9180000000000000129, 9180000000000000106, 2, 'doc/202611/9180000000000000106.xlsx', 40960,
     '5a5a5a5a3c3c3c3c1f1f1f1f0e0e0e0e5a5a5a5a3c3c3c3c1f1f1f1f0e0e0e0e',
     9100000000000000001, '补充11月分包与检测费用明细', 'UPLOAD', NOW(), NOW(), 1, 1, 0, 0);

-- ---------------------------------------------------------------------
-- 5. 文件级授权（2 条）
--    0021：费用明细文件授权 FINANCE 角色（role_id=2）；
--    0022：人事制度汇编文件授权 pm01（sys_user.id=9100000000000000002）。
-- ---------------------------------------------------------------------
INSERT IGNORE INTO doc_file_permission
    (id, file_id, subject_type, subject_id, actions, granted_by,
     create_time, update_time, create_by, update_by, deleted, version)
VALUES
    (9180000000000000021, 9180000000000000106, 'ROLE', 2,
     '["PREVIEW","DOWNLOAD"]', 1, NOW(), NOW(), 1, 1, 0, 0),
    (9180000000000000022, 9180000000000000105, 'USER', 9100000000000000002,
     '["PREVIEW","DOWNLOAD"]', 9100000000000000003, NOW(), NOW(), 1, 1, 0, 0);

-- ---------------------------------------------------------------------
-- 6. AI 会话（3 个，分属 admin / pm01 / staff01）
--    update_time 取该会话最后一条消息时间，保证会话列表倒序合理。
-- ---------------------------------------------------------------------
INSERT IGNORE INTO ai_conversation
    (id, user_id, title, create_time, update_time, create_by, update_by, deleted, version)
VALUES
    (9180000000000000201, 1, '本月报销与项目收款汇总',
     DATE_ADD(NOW(), INTERVAL -120 MINUTE), DATE_ADD(NOW(), INTERVAL -38 MINUTE), 1, 1, 0, 0),
    (9180000000000000202, 9100000000000000002, '项目进度款回款情况',
     DATE_ADD(NOW(), INTERVAL -1500 MINUTE), DATE_ADD(NOW(), INTERVAL -1198 MINUTE), 1, 1, 0, 0),
    (9180000000000000203, 9100000000000000004, '差旅报销标准咨询',
     DATE_ADD(NOW(), INTERVAL -180 MINUTE), DATE_ADD(NOW(), INTERVAL -58 MINUTE), 1, 1, 0, 0);

-- ---------------------------------------------------------------------
-- 7. AI 消息（12 条，每个会话 4 条，USER / ASSISTANT 交替）
--    role 取 AiMessageRole 枚举名（USER/ASSISTANT/TOOL）；
--    tool_calls 列为 TEXT，内容为合法 JSON 文本（工具名 + 参数 + 返回行数）。
-- ---------------------------------------------------------------------
INSERT IGNORE INTO ai_message
    (id, conversation_id, role, content, tool_calls, model,
     prompt_tokens, completion_tokens, latency_ms,
     create_time, update_time, create_by, update_by, deleted, version)
VALUES
    (9180000000000000301, 9180000000000000201, 'USER',
     '本月报销合计是多少？还有哪些报销单待审批？', NULL, NULL, NULL, NULL, NULL,
     DATE_ADD(NOW(), INTERVAL -110 MINUTE), DATE_ADD(NOW(), INTERVAL -110 MINUTE), 1, 1, 0, 0),
    (9180000000000000302, 9180000000000000201, 'ASSISTANT',
     '本月共提交报销 6 笔，合计 24320.50 元；其中待审批 3 笔，合计 5820.00 元，建议优先处理张江二期的差旅与住宿报销。',
     '[{"tool":"queryExpenseSummary","arguments":{"month":"2026-11"},"rows":6}]',
     'qwen2.5:7b', 356, 128, 1860,
     DATE_ADD(NOW(), INTERVAL -108 MINUTE), DATE_ADD(NOW(), INTERVAL -108 MINUTE), 1, 1, 0, 0),
    (9180000000000000303, 9180000000000000201, 'USER',
     '张江二期的项目进度款还有多少未收？', NULL, NULL, NULL, NULL, NULL,
     DATE_ADD(NOW(), INTERVAL -40 MINUTE), DATE_ADD(NOW(), INTERVAL -40 MINUTE), 1, 1, 0, 0),
    (9180000000000000304, 9180000000000000201, 'ASSISTANT',
     '张江科技园二期合同额 6800 万元，已收第1期 185 万元，第2期 162 万元仍在审批中，扣除已收与在途后未收约 6453 万元。',
     '[{"tool":"queryProgressPayment","arguments":{"projectId":"9100000000000001001"},"rows":2}]',
     'qwen2.5:7b', 402, 156, 2140,
     DATE_ADD(NOW(), INTERVAL -38 MINUTE), DATE_ADD(NOW(), INTERVAL -38 MINUTE), 1, 1, 0, 0),
    (9180000000000000305, 9180000000000000202, 'USER',
     '张江二期第2期进度款审批到哪一步了？', NULL, NULL, NULL, NULL, NULL,
     DATE_ADD(NOW(), INTERVAL -1500 MINUTE), DATE_ADD(NOW(), INTERVAL -1500 MINUTE), 1, 1, 0, 0),
    (9180000000000000306, 9180000000000000202, 'ASSISTANT',
     '第2期进度款 1620000.00 元已完成部门审批，当前停留在财务核验节点，等待财务复核后进入付款。',
     '[{"tool":"queryBpmTask","arguments":{"bizId":"9100000000000001302"},"rows":1}]',
     'qwen2.5:7b', 288, 112, 1720,
     DATE_ADD(NOW(), INTERVAL -1498 MINUTE), DATE_ADD(NOW(), INTERVAL -1498 MINUTE), 1, 1, 0, 0),
    (9180000000000000307, 9180000000000000202, 'USER',
     '虹桥幕墙项目本月成本支出多少？', NULL, NULL, NULL, NULL, NULL,
     DATE_ADD(NOW(), INTERVAL -1200 MINUTE), DATE_ADD(NOW(), INTERVAL -1200 MINUTE), 1, 1, 0, 0),
    (9180000000000000308, 9180000000000000202, 'ASSISTANT',
     '虹桥幕墙项目本月确认支出 37.08 万元，主要是分包进度款 32.20 万元与材料检测费 4.88 万元。',
     '[{"tool":"queryProjectCost","arguments":{"projectId":"9100000000000001003","month":"2026-11"},"rows":4}]',
     'qwen2.5:7b', 331, 134, 1960,
     DATE_ADD(NOW(), INTERVAL -1198 MINUTE), DATE_ADD(NOW(), INTERVAL -1198 MINUTE), 1, 1, 0, 0),
    (9180000000000000309, 9180000000000000203, 'USER',
     '出差住宿费的报销标准是多少？', NULL, NULL, NULL, NULL, NULL,
     DATE_ADD(NOW(), INTERVAL -170 MINUTE), DATE_ADD(NOW(), INTERVAL -170 MINUTE), 1, 1, 0, 0),
    (9180000000000000310, 9180000000000000203, 'ASSISTANT',
     '上海市内出差住宿标准为每人每天 500 元，凭合规发票据实报销，超出标准部分由个人承担。',
     '[{"tool":"queryExpensePolicy","arguments":{"category":"ACCOMMODATION"},"rows":1}]',
     'qwen2.5:7b', 245, 96, 1480,
     DATE_ADD(NOW(), INTERVAL -168 MINUTE), DATE_ADD(NOW(), INTERVAL -168 MINUTE), 1, 1, 0, 0),
    (9180000000000000311, 9180000000000000203, 'USER',
     '我上个月提交的交通费报销单现在是什么状态？', NULL, NULL, NULL, NULL, NULL,
     DATE_ADD(NOW(), INTERVAL -60 MINUTE), DATE_ADD(NOW(), INTERVAL -60 MINUTE), 1, 1, 0, 0),
    (9180000000000000312, 9180000000000000203, 'ASSISTANT',
     '您提交的 RCPT-2026-0001 交通费报销单（3260.50 元）已由项目经理审批通过，目前正在等待财务核验。',
     '[{"tool":"queryReceiptStatus","arguments":{"receiptNo":"RCPT-2026-0001"},"rows":1}]',
     'qwen2.5:7b', 267, 104, 1560,
     DATE_ADD(NOW(), INTERVAL -58 MINUTE), DATE_ADD(NOW(), INTERVAL -58 MINUTE), 1, 1, 0, 0);

-- ---------------------------------------------------------------------
-- 8. AI 经营日报摘要（2 条，不同日期）
--    period_type 固定 DAILY；trigger_type 取 SCHEDULED / MANUAL；
--    metrics_json 为合法 JSON 文本（与 AiDailySummaryServiceImpl 指标键一致）。
-- ---------------------------------------------------------------------
INSERT IGNORE INTO ai_daily_summary
    (id, summary_date, period_type, content, metrics_json, model, trigger_type, notification_id,
     create_time, update_time, create_by, update_by, deleted, version)
VALUES
    (9180000000000000401, '2026-11-15', 'DAILY',
     '2026-11-15 经营日报：新增报销 4 笔、金额 12860.00 元；待审批 2 笔、金额 3760.00 元；资金收入 1200000.00 元、支出 86500.00 元，净流入 1113500.00 元；银行未匹配 1 笔、金额 125000.00 元；用印 1 次。',
     '{"receiptCount":4,"receiptAmount":12860.00,"pendingCount":2,"pendingAmount":3760.00,"income":1200000.00,"expense":86500.00,"net":1113500.00,"unmatchedCount":1,"unmatchedAmount":125000.00,"sealCount":1}',
     'TEMPLATE', 'SCHEDULED', NULL, NOW(), NOW(), 1, 1, 0, 0),
    (9180000000000000402, '2026-11-16', 'DAILY',
     '2026-11-16 经营日报：新增报销 2 笔、金额 5630.00 元；待审批 3 笔、金额 5820.00 元；当日无资金流入、支出 48800.00 元，净流出 48800.00 元；银行未匹配 1 笔、金额 100.00 元；用印 2 次。',
     '{"receiptCount":2,"receiptAmount":5630.00,"pendingCount":3,"pendingAmount":5820.00,"income":0.00,"expense":48800.00,"net":-48800.00,"unmatchedCount":1,"unmatchedAmount":100.00,"sealCount":2}',
     'qwen2.5:7b', 'MANUAL', NULL, NOW(), NOW(), 1, 1, 0, 0);

-- ---------------------------------------------------------------------
-- 9. AI 反馈（2 条：1 条点赞 5 分、1 条点踩 2 分，关联真实 ai_message）
-- ---------------------------------------------------------------------
INSERT IGNORE INTO ai_feedback
    (id, message_id, user_id, score, remark, create_time, update_time, create_by, update_by, deleted, version)
VALUES
    (9180000000000000501, 9180000000000000302, 1, 5, '报销汇总口径清晰，可直接用于周会汇报。',
     NOW(), NOW(), 1, 1, 0, 0),
    (9180000000000000502, 9180000000000000310, 9100000000000000004, 2, '住宿标准回答不够完整，希望补充不同城市的档位。',
     NOW(), NOW(), 1, 1, 0, 0);

-- ---------------------------------------------------------------------
-- 10. 站内通知（8 条：APPROVAL 3 / HR 2 / ADMIN 2 / DOC 1）
--     notification_type 取 NotificationType 枚举名；
--     content_type 取 ContentType（TEXT/LINK）；priority 取 NotificationPriority；
--     link_url 沿用既有 ceams:// 深链格式。
-- ---------------------------------------------------------------------
INSERT IGNORE INTO biz_notification
    (id, title, content_type, content, link_url, priority, notification_type,
     sender_id, sender_name, valid_start_time, valid_end_time, status, publish_time,
     create_time, update_time, create_by, update_by, deleted, version)
VALUES
    (9180000000000000601, '审批待办：报销单待财务核验', 'LINK',
     '王磊提交的张江二期现场差旅交通费报销单（3260.50 元）已由项目经理审批，等待财务核验。',
     'ceams://todo?tab=pending&taskId=9100000000000005202&instanceId=9100000000000005001',
     'HIGH', 'APPROVAL', 1, '系统管理员',
     '2026-11-17 00:00:00', '2026-12-31 23:59:59', 'PUBLISHED', NOW(), NOW(), NOW(), 1, 1, 0, 0),
    (9180000000000000602, '审批待办：进度款第2期', 'LINK',
     '张江科技园二期第2期进度款申请 1620000.00 元，等待财务复核。',
     'ceams://todo?tab=pending&taskId=9100000000000005212&instanceId=9100000000000005002',
     'NORMAL', 'APPROVAL', 9100000000000000002, '李建国',
     '2026-11-17 00:00:00', '2026-12-31 23:59:59', 'PUBLISHED', NOW(), NOW(), NOW(), 1, 1, 0, 0),
    (9180000000000000603, '审批待办：用印申请', 'LINK',
     '合同专用章用印申请（张江科技园二期施工总承包合同）等待行政审批。',
     'ceams://todo?tab=pending&taskId=9100000000000005222&instanceId=9100000000000005003',
     'URGENT', 'APPROVAL', 9100000000000000002, '李建国',
     '2026-11-17 00:00:00', '2026-12-31 23:59:59', 'PUBLISHED', NOW(), NOW(), NOW(), 1, 1, 0, 0),
    (9180000000000000604, '工资条已发布（2026-11）', 'LINK',
     '2026年11月工资条已发布，请及时查看个人工资明细并核对。',
     'ceams://hr/salary-slip?month=2026-11',
     'NORMAL', 'HR', 9100000000000000003, '赵敏',
     '2026-11-17 00:00:00', '2026-12-31 23:59:59', 'PUBLISHED', NOW(), NOW(), NOW(), 1, 1, 0, 0),
    (9180000000000000605, '劳动合同即将到期提醒', 'LINK',
     '赵敏的劳动合同将于 2026-11-30 到期，请尽快确认续签安排。',
     'ceams://hr/contract?id=9200000000000000303',
     'HIGH', 'HR', 9100000000000000003, '赵敏',
     '2026-11-17 00:00:00', '2026-12-31 23:59:59', 'PUBLISHED', NOW(), NOW(), NOW(), 1, 1, 0, 0),
    (9180000000000000606, '资产借用逾期提醒', 'LINK',
     '您借用的会议平板已超过归还日期，请尽快归还行政部。',
     'ceams://admin/asset-borrow?id=9200000000000003001',
     'HIGH', 'ADMIN', 1, '系统管理员',
     '2026-11-17 00:00:00', '2026-12-31 23:59:59', 'PUBLISHED', NOW(), NOW(), NOW(), 1, 1, 0, 0),
    (9180000000000000607, '报修工单已派单', 'LINK',
     '工程部会议室空调报修工单已派给维修人员，请关注处理进度。',
     'ceams://admin/repair?id=9200000000000002801',
     'NORMAL', 'ADMIN', 1, '系统管理员',
     '2026-11-17 00:00:00', '2026-12-31 23:59:59', 'PUBLISHED', NOW(), NOW(), NOW(), 1, 1, 0, 0),
    (9180000000000000608, '文档共享：张江二期施工组织设计', 'LINK',
     '李建国向您共享了《张江二期施工组织设计.docx》，可在线预览与下载。',
     'ceams://doc/file?id=9180000000000000103',
     'LOW', 'DOC', 9100000000000000002, '李建国',
     '2026-11-17 00:00:00', '2026-12-31 23:59:59', 'PUBLISHED', NOW(), NOW(), NOW(), 1, 1, 0, 0);

-- ---------------------------------------------------------------------
-- 11. 用户通知（8 条，收件人覆盖 admin / pm01 / hr01 / staff01）
--     channel 取 PushChannel（SYSTEM/EMAIL/SMS）；push_status 取 PushStatus；
--     0603、0607 标记为已读，其余未读。
-- ---------------------------------------------------------------------
INSERT IGNORE INTO biz_user_notification
    (id, notification_id, user_id, is_read, read_time, channel, push_status, push_time,
     create_time, update_time, create_by, update_by, deleted, version)
VALUES
    (9180000000000000621, 9180000000000000601, 1, 0, NULL, 'SYSTEM', 'SENT', NOW(), NOW(), NOW(), 1, 1, 0, 0),
    (9180000000000000622, 9180000000000000602, 9100000000000000002, 0, NULL, 'SYSTEM', 'SENT', NOW(), NOW(), NOW(), 1, 1, 0, 0),
    (9180000000000000623, 9180000000000000603, 9100000000000000003, 1, DATE_ADD(NOW(), INTERVAL -30 MINUTE), 'SYSTEM', 'SENT', NOW(), NOW(), NOW(), 1, 1, 0, 0),
    (9180000000000000624, 9180000000000000604, 9100000000000000004, 0, NULL, 'SYSTEM', 'SENT', NOW(), NOW(), NOW(), 1, 1, 0, 0),
    (9180000000000000625, 9180000000000000605, 9100000000000000003, 0, NULL, 'SYSTEM', 'SENT', NOW(), NOW(), NOW(), 1, 1, 0, 0),
    (9180000000000000626, 9180000000000000606, 9100000000000000004, 0, NULL, 'SYSTEM', 'SENT', NOW(), NOW(), NOW(), 1, 1, 0, 0),
    (9180000000000000627, 9180000000000000607, 1, 1, DATE_ADD(NOW(), INTERVAL -20 MINUTE), 'SYSTEM', 'SENT', NOW(), NOW(), NOW(), 1, 1, 0, 0),
    (9180000000000000628, 9180000000000000608, 9100000000000000004, 0, NULL, 'SYSTEM', 'SENT', NOW(), NOW(), NOW(), 1, 1, 0, 0);

-- ---------------------------------------------------------------------
-- 12. 用户通知偏好（4 个测试账号各 1 条，notification_type=SYSTEM）
--     说明：当前 DDL 无独立企微列，企微消息由 integration_wecom_message 承载；
--     此处开启站内信（receive_system=1）并演示邮件通道。
-- ---------------------------------------------------------------------
INSERT IGNORE INTO biz_user_notification_preference
    (id, user_id, notification_type, receive_system, receive_email, receive_sms,
     quiet_mode, quiet_start_time, quiet_end_time, priority_threshold,
     create_time, update_time, create_by, update_by, deleted, version)
VALUES
    (9180000000000000701, 1, 'SYSTEM', 1, 1, 0, 1, '22:00:00', '08:00:00', 'NORMAL', NOW(), NOW(), 1, 1, 0, 0),
    (9180000000000000702, 9100000000000000002, 'SYSTEM', 1, 1, 0, 0, NULL, NULL, 'HIGH', NOW(), NOW(), 1, 1, 0, 0),
    (9180000000000000703, 9100000000000000003, 'SYSTEM', 1, 0, 0, 0, NULL, NULL, 'NORMAL', NOW(), NOW(), 1, 1, 0, 0),
    (9180000000000000704, 9100000000000000004, 'SYSTEM', 1, 0, 0, 0, NULL, NULL, 'LOW', NOW(), NOW(), 1, 1, 0, 0);

-- ---------------------------------------------------------------------
-- 13. 企业微信应用消息发件箱（4 条：SENT 2 / PENDING 1 / FAILED 1）
--     status 取 WeComMessageStatus（PENDING/SENT/FAILED）；
--     payload_json 为合法 JSON（title/content/linkUrl 快照）。
-- ---------------------------------------------------------------------
INSERT IGNORE INTO integration_wecom_message
    (id, notification_id, target_user_id, wecom_userid, msg_type, payload_json, status,
     retry_count, next_retry_at, error_msg, sent_at,
     create_time, update_time, create_by, update_by, deleted, version)
VALUES
    (9180000000000000801, 9180000000000000601, 1, 'admin', 'TEXT',
     '{"title":"审批待办：报销单待财务核验","content":"王磊提交的差旅交通费报销单等待财务核验。","linkUrl":"ceams://todo?tab=pending&taskId=9100000000000005202&instanceId=9100000000000005001"}',
     'SENT', 0, NULL, NULL, DATE_ADD(NOW(), INTERVAL -15 MINUTE), NOW(), NOW(), 1, 1, 0, 0),
    (9180000000000000802, 9180000000000000602, 9100000000000000002, 'pm01', 'TEXT',
     '{"title":"审批待办：进度款第2期","content":"张江科技园二期第2期进度款申请等待财务复核。","linkUrl":"ceams://todo?tab=pending&taskId=9100000000000005212&instanceId=9100000000000005002"}',
     'SENT', 0, NULL, NULL, DATE_ADD(NOW(), INTERVAL -12 MINUTE), NOW(), NOW(), 1, 1, 0, 0),
    (9180000000000000803, 9180000000000000604, 9100000000000000004, 'staff01', 'TEXT',
     '{"title":"工资条已发布（2026-11）","content":"2026年11月工资条已发布，请及时查看。","linkUrl":"ceams://hr/salary-slip?month=2026-11"}',
     'PENDING', 1, DATE_ADD(NOW(), INTERVAL 10 MINUTE), NULL, NULL, NOW(), NOW(), 1, 1, 0, 0),
    (9180000000000000804, 9180000000000000603, 9100000000000000003, 'hr01', 'TEXT',
     '{"title":"审批待办：用印申请","content":"合同专用章用印申请等待行政审批。","linkUrl":"ceams://todo?tab=pending&taskId=9100000000000005222&instanceId=9100000000000005003"}',
     'FAILED', 3, NULL, '企业微信接口返回 errcode=40013：invalid corpid，请检查应用配置。', NULL,
     NOW(), NOW(), 1, 1, 0, 0);

-- ---------------------------------------------------------------------
-- 14. 企业微信组织同步批次（补 1 条 SUCCESS；既有 1 条 FAILED 不动）
--     sync_type 取 WeComSyncType（MANUAL/SCHEDULED）；status 取 WeComSyncStatus。
-- ---------------------------------------------------------------------
INSERT IGNORE INTO integration_wecom_sync
    (id, sync_batch_no, sync_type, total, success_count, failed_count, status, error_msg,
     started_at, finished_at, create_time, update_time, create_by, update_by, deleted, version)
VALUES
    (9180000000000000901, 'WECOM-20261117-SEED01', 'MANUAL', 7, 7, 0, 'SUCCESS', NULL,
     '2026-11-17 10:00:00', '2026-11-17 10:00:05', NOW(), NOW(), 1, 1, 0, 0);

-- ---------------------------------------------------------------------
-- 15. 用户设备（pm01 / staff01 各 1 台）
--     platform 取 Platform（WEB/DESKTOP/MINIPROGRAM）；status 取 DeviceStatus；
--     app_secret 为 v1: 开头 AES-256-GCM 密文（见头注释）。
-- ---------------------------------------------------------------------
INSERT IGNORE INTO sys_user_device
    (id, user_id, device_id, device_name, platform, last_login_ip, status,
     bind_time, last_login_time, create_time, update_time, create_by, update_by, deleted, version, app_secret)
VALUES
    (9180000000000001001, 9100000000000000002, 'seed-pm01-device-2026', '李建国的笔记本', 'DESKTOP',
     '172.19.0.1', 'ACTIVE', '2026-10-08 09:00:00', '2026-11-17 09:30:00',
     NOW(), NOW(), 1, 1, 0, 0,
     'v1:EQInID71GzBGHE3Js0KweUjclN8SBuSrQJgmQVMzzNWuYNA+d3V5lFW8/LzY6DGA4xquU7U='),
    (9180000000000001002, 9100000000000000004, 'seed-staff01-device-2026', '王磊的浏览器', 'WEB',
     '172.19.0.1', 'ACTIVE', '2026-10-12 08:30:00', '2026-11-17 08:45:00',
     NOW(), NOW(), 1, 1, 0, 0,
     'v1:XwkmGAFSuxt3MYwVDLzfABnN65HN+jdB225gnOD5J6+irM6P2LuuFairTpgWtI8pv67voIJio84=');

-- ---------------------------------------------------------------------
-- 16. BPM 代理设置（pm01 代理给 admin，生效中，覆盖今天）
--     status 取 DelegateStatus（ENABLED/DISABLED）。
-- ---------------------------------------------------------------------
INSERT IGNORE INTO bpm_delegate
    (id, user_id, delegate_id, biz_type, start_time, end_time, status, reason,
     create_time, update_time, create_by, update_by, deleted, version)
VALUES
    (9180000000000001101, 9100000000000000002, 1, NULL, '2026-11-01 00:00:00', '2026-12-31 23:59:59',
     'ENABLED', '项目经理出差期间由系统管理员代为处理审批', NOW(), NOW(), 1, 1, 0, 0);

-- ---------------------------------------------------------------------
-- 17. 执行后自检（只读，按号段统计新增行数）
-- ---------------------------------------------------------------------
SELECT 'doc_directory' AS table_name, COUNT(*) AS seeded_rows FROM doc_directory WHERE id BETWEEN 9180000000000000001 AND 9180000000000000099
UNION ALL SELECT 'doc_dir_permission', COUNT(*) FROM doc_dir_permission WHERE id BETWEEN 9180000000000000011 AND 9180000000000000019
UNION ALL SELECT 'doc_file', COUNT(*) FROM doc_file WHERE id BETWEEN 9180000000000000101 AND 9180000000000000199
UNION ALL SELECT 'doc_file_version', COUNT(*) FROM doc_file_version WHERE id BETWEEN 9180000000000000121 AND 9180000000000000199
UNION ALL SELECT 'doc_file_permission', COUNT(*) FROM doc_file_permission WHERE id BETWEEN 9180000000000000021 AND 9180000000000000029
UNION ALL SELECT 'ai_conversation', COUNT(*) FROM ai_conversation WHERE id BETWEEN 9180000000000000201 AND 9180000000000000299
UNION ALL SELECT 'ai_message', COUNT(*) FROM ai_message WHERE id BETWEEN 9180000000000000301 AND 9180000000000000399
UNION ALL SELECT 'ai_daily_summary', COUNT(*) FROM ai_daily_summary WHERE id BETWEEN 9180000000000000401 AND 9180000000000000499
UNION ALL SELECT 'ai_feedback', COUNT(*) FROM ai_feedback WHERE id BETWEEN 9180000000000000501 AND 9180000000000000599
UNION ALL SELECT 'biz_notification', COUNT(*) FROM biz_notification WHERE id BETWEEN 9180000000000000601 AND 9180000000000000608
UNION ALL SELECT 'biz_user_notification', COUNT(*) FROM biz_user_notification WHERE id BETWEEN 9180000000000000621 AND 9180000000000000628
UNION ALL SELECT 'biz_user_notification_preference', COUNT(*) FROM biz_user_notification_preference WHERE id BETWEEN 9180000000000000701 AND 9180000000000000709
UNION ALL SELECT 'integration_wecom_message', COUNT(*) FROM integration_wecom_message WHERE id BETWEEN 9180000000000000801 AND 9180000000000000809
UNION ALL SELECT 'integration_wecom_sync', COUNT(*) FROM integration_wecom_sync WHERE id BETWEEN 9180000000000000901 AND 9180000000000000909
UNION ALL SELECT 'sys_user_device', COUNT(*) FROM sys_user_device WHERE id BETWEEN 9180000000000001001 AND 9180000000000001009
UNION ALL SELECT 'bpm_delegate', COUNT(*) FROM bpm_delegate WHERE id BETWEEN 9180000000000001101 AND 9180000000000001109;
