-- =============================================================================
-- 项目变更申请补充数据（一期演示用）
-- 用途：让「项目变更」页面覆盖 DRAFT / PENDING / REJECTED 三种状态（既有 1 条 APPROVED）
-- 幂等：固定主键 + INSERT IGNORE，可重复执行
-- 执行：
--   docker cp scripts/seed/seed_change_request_extra.sql ceams-mysql:/tmp/seed_change_request_extra.sql
--   docker exec -i ceams-mysql sh -c "mysql -uroot -proot --default-character-set=utf8mb4 ceams < /tmp/seed_change_request_extra.sql"
-- =============================================================================

INSERT IGNORE INTO biz_change_request
  (id, project_id, request_type, description, estimated_cost, status,
   requested_by, approved_by, approved_at, approval_comment,
   create_time, update_time, create_by, update_by, deleted, version, bpm_instance_id)
VALUES
  (9100000000000006101, 9100000000000001001, '设计变更', '业主要求调整二层结构梁截面，需重新出图并核算增量成本。', 26800.00, 'PENDING',
   9100000000000000002, NULL, NULL, NULL,
   NOW(), NOW(), 9100000000000000002, 9100000000000000002, 0, 0, NULL),
  (9100000000000006102, 9100000000000001003, '工程量变更', '现场签证增加土方外运 320 立方米，待补充单价确认。', 15600.00, 'DRAFT',
   9100000000000000002, NULL, NULL, NULL,
   NOW(), NOW(), 9100000000000000002, 9100000000000000002, 0, 0, NULL),
  (9100000000000006103, 9100000000000001005, '材料变更', '原定品牌断货，申请更换同档次替代品牌。', 9800.00, 'REJECTED',
   9100000000000000002, 1, DATE_SUB(NOW(), INTERVAL 3 DAY), '替代品牌未提供检测报告，暂不通过。',
   NOW(), NOW(), 9100000000000000002, 1, 0, 0, NULL);
