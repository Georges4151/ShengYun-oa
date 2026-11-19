-- =====================================================================
-- 文件：scripts/seed/seed_hr_admin.sql
-- 用途：为本地开发库 ceams 补充「HR 人事 + 行政后勤」演示数据。
--   HR  ：岗位、员工档案、劳动合同、排班、打卡、补卡、假期额度、请假单、
--         薪资台账、调薪申请、绩效模板、绩效考评、月度工资核算、工资条、人事异动。
--   行政：易耗品与领用、会议室与预定、车辆/用车/费用、访客登记、报修工单、
--         固定资产与领用记录。
-- 幂等：所有表均显式指定主键并使用 INSERT IGNORE；重复执行不会报错、不会产生重复行。
--       主键号段统一为 920000000000000xxxx（与上一阶段 910000000000000xxxx 号段不冲突）：
--         0101-0107 岗位   0201-0205 员工   0301-0306 合同   0401-0430 排班
--         0501-0560 打卡   0601-0602 补卡   0701-0712 假期额度 0801-0804 请假单
--         0901-0904 薪资   0911-0912 调薪   1001-1002 绩效模板 1101-1104 绩效考评
--         1201-1203 工资核算 1301-1303 工资条 1401-1402 人事异动
--         2001-2007 易耗品 2101-2103 领用单 2201-2203 会议室 2301-2304 会议预定
--         2401-2403 车辆   2501-2503 用车单 2601-2604 车辆费用 2701-2704 访客
--         2801-2804 报修   2901-2906 资产   3001-3004 资产领用
-- 约束：不修改任何 Flyway 迁移文件；不 UPDATE / DELETE 既有业务数据（本脚本无 UPDATE）。
-- 敏感列：hr_employee.id_card_enc / phone_enc / emergency_phone_enc、
--         hr_salary.*_enc、hr_salary_calc.*_enc、hr_salary_change_apply 前后快照 JSON 中的金额、
--         hr_salary_slip.content_json 中的金额、admin_visitor.phone_enc
--         均为 AES-256-GCM 密文（v1: + Base64(IV12 || 密文 || Tag16)），
--         密钥与 application.yml 的 ceams.sensitive.secret 默认值一致：
--         Y2VhbXMtc2Vuc2l0aXZlLWRldi1rZXktMjAyNi0wMTI=
-- 执行（Windows/PowerShell 下中文最稳）：
--   docker cp scripts/seed/seed_hr_admin.sql ceams-mysql:/tmp/seed_hr_admin.sql
--   docker exec -i ceams-mysql sh -c "mysql -uroot -proot --default-character-set=utf8mb4 ceams < /tmp/seed_hr_admin.sql"
-- 预期新增行数：岗位 7、员工 5、合同 6、排班 30、打卡 60、补卡 2、假期额度 12、
--   请假单 4、薪资 4、调薪 2、绩效模板 2、绩效考评 4、工资核算 3、工资条 3、
--   人事异动 2、易耗品 7、领用单 3、会议室 3、会议预定 4、车辆 3、用车单 3、
--   车辆费用 4、访客 4、报修 4、资产 6、资产领用 4。
-- =====================================================================

SET NAMES utf8mb4;

-- ---------------------------------------------------------------------
-- 1. HR 岗位（5 个启用 + 2 个停用）
-- ---------------------------------------------------------------------
INSERT IGNORE INTO hr_position
    (id, code, name, level, job_family, sort_order, status, remark,
     create_time, update_time, create_by, update_by, deleted, version)
VALUES
    (9200000000000000101, 'FIN-ACCOUNTANT',    '会计',       'P5', '职能', 10, 'ENABLED',  '总账、报税与财务报表',   NOW(), NOW(), 1, 1, 0, 0),
    (9200000000000000102, 'FIN-MANAGER',       '财务经理',   'M2', '管理', 20, 'ENABLED',  '统筹财务部日常工作',     NOW(), NOW(), 1, 1, 0, 0),
    (9200000000000000103, 'HR-SPECIALIST',     '人事专员',   'P4', '职能', 30, 'ENABLED',  '招聘、入转调离办理',     NOW(), NOW(), 1, 1, 0, 0),
    (9200000000000000104, 'HR-MANAGER',        '人事经理',   'M2', '管理', 40, 'ENABLED',  '人力规划与制度管理',     NOW(), NOW(), 1, 1, 0, 0),
    (9200000000000000105, 'ENG-PROJECT-MGR',   '项目经理',   'P6', '技术', 50, 'ENABLED',  '项目交付与团队管理',     NOW(), NOW(), 1, 1, 0, 0),
    (9200000000000000106, 'ENG-INTERN',        '工程实习生', 'P1', '技术', 60, 'DISABLED', '校招实习岗位，暂停使用', NOW(), NOW(), 1, 1, 0, 0),
    (9200000000000000107, 'ADMIN-ASSISTANT',   '行政助理',   'P3', '职能', 70, 'DISABLED', '岗位合并，暂停使用',     NOW(), NOW(), 1, 1, 0, 0);

-- ---------------------------------------------------------------------
-- 2. HR 员工档案（5 名，状态覆盖 PROBATION/ACTIVE/LEAVE/TERMINATED）
--    账号绑定：staff01 / pm01 / hr01 / finance01 + 1 名不绑账号员工。
--    既有员工（Smoke Test）user_id 为 NULL，不冲突。
-- ---------------------------------------------------------------------
INSERT IGNORE INTO hr_employee
    (id, user_id, employee_no, name, gender, birth_date, id_card_enc, phone_enc, email,
     dept_id, position_id, employment_type, hire_date, probation_end_date, regular_date,
     leave_date, status, emergency_contact, emergency_phone_enc, id_card_file_id, remark,
     create_time, update_time, create_by, update_by, deleted, version)
VALUES
    (9200000000000000201, 9100000000000000004, 'EMP-0001', '王磊', 'MALE',   '1990-03-07',
     'v1:X7DbEtImVPnzc0XYSMNxW067hnhb67dhEgPX/6MmXoM0YlXZgiT45dZJvYQVIg==', 'v1:udPo2bCag5DSVSGsDUcXcxzsyRA0o/H65hL/ag2G/r0iucUUf7Cw', 'wanglei@ceams.local',
     9100000000000000013, 9200000000000000105, 'FULL_TIME', '2023-03-01', NULL, '2023-06-01',
     NULL, 'ACTIVE', '王秀英', 'v1:MhIXVZnaDKg+T1kDSSQXH9PnoP39uVxZuKKp/KPTB5nDYG48gqwc', NULL, '工程部项目经理（staff01 绑定）',
     NOW(), NOW(), 1, 1, 0, 0),
    (9200000000000000202, 9100000000000000002, 'EMP-0002', '李建国', 'MALE', '1988-07-15',
     'v1:jiAtYog853CFTeRTh639DVrIlWzEMxMnj0gPctO431j/LP8V88Y2jykbSZjPXQ==', 'v1:YYfpZwWi5T+cCp+rcFTd+ZRfBE59Dw0q/9d7e89D7aANhvyBAcW3', 'lijianguo@ceams.local',
     9100000000000000013, 9200000000000000105, 'FULL_TIME', '2021-07-15', NULL, '2021-10-15',
     NULL, 'ACTIVE', '张丽', 'v1:3g4Cq7Tb4sAyOC4lfO1mVP35KlM2+V86bMem2jLXR546+H1FDt1s', NULL, '工程部项目经理（pm01 绑定）',
     NOW(), NOW(), 1, 1, 0, 0),
    (9200000000000000203, 9100000000000000003, 'EMP-0003', '赵敏', 'FEMALE',   '1995-11-20',
     'v1:wS+nNZU4ywhS4osmeoGhx46G3DZD9HoczJG8O7GJrCbTT5kNHYfxggNZC+DMEQ==', 'v1:Qw4sRh/8mHcsUoNEOr0CH/KrS/xBb3NH/ouEpgeutKcLwC0/jzSl', 'zhaomin@ceams.local',
     9100000000000000012, 9200000000000000104, 'FULL_TIME', '2026-09-01', '2026-12-01', NULL,
     NULL, 'PROBATION', '赵国强', 'v1:tPNo+mLjUeUbpN7SqPyYP6FAb/R7gKeEJqzfrTcGURgB9x+0AbHC', NULL, '人事经理，试用期（hr01 绑定）',
     NOW(), NOW(), 1, 1, 0, 0),
    (9200000000000000204, 9100000000000000001, 'EMP-0004', '陈静', 'FEMALE',   '1992-09-09',
     'v1:mqQJvAZ9jXCTGpfwHCXF+19ytDC7V44pdFto93t4iSTywbOmf2bdi/zjjfhtFQ==', 'v1:DxFSxB0Mg9E0a3WnLuhOaZssHAMN1ruNXGe+SxWVwhUXT2jpq7AQ', 'chenjing@ceams.local',
     9100000000000000011, 9200000000000000101, 'FULL_TIME', '2022-05-10', NULL, '2022-08-10',
     NULL, 'LEAVE', '陈立', 'v1:AIOICvW5X1InZJ88Uru+cNMrp/N7MGLoV7KPE6q0o7LrCvyO6I7B', NULL, '会计，长期病假中（finance01 绑定）',
     NOW(), NOW(), 1, 1, 0, 0),
    (9200000000000000205, NULL, 'EMP-0005', '孙晓雯', 'FEMALE', '1998-02-02',
     'v1:GFJl2IRH8zHTk1G2UOlwOwbu9uE1Es3q1lC+fsbVahrbXxw7eJch8aU49s7bvw==', 'v1:mf7t01k2xbEdK43mOVOjIM9Z+tHj72m14IvZVy6AMLF+XauFrhK7', 'sunxiaowen@ceams.local',
     9100000000000000012, 9200000000000000107, 'FULL_TIME', '2024-02-01', NULL, '2024-05-01',
     '2026-10-31', 'TERMINATED', '孙建平', 'v1:7ijvldGHRgVhfAWBSIXIrPszPbKcRdZF7Mj3DPpWPqAhLQfvBvUF', NULL, '行政助理，已离职（不绑定账号）',
     NOW(), NOW(), 1, 1, 0, 0);

-- ---------------------------------------------------------------------
-- 3. HR 劳动合同（6 份，覆盖 ACTIVE/EXPIRED/TERMINATED；
--    其中 303、306 两份 end_date 落在 30 天内，用于到期提醒）
-- ---------------------------------------------------------------------
INSERT IGNORE INTO hr_contract
    (id, employee_id, contract_no, contract_type, start_date, end_date, sign_date, file_id,
     status, renewal_remind_date, remark,
     create_time, update_time, create_by, update_by, deleted, version)
VALUES
    (9200000000000000301, 9200000000000000201, 'HT-2025-001', 'FIXED',   '2025-03-01', '2028-02-28', '2025-02-20', NULL,
     'ACTIVE', '2027-12-29', '三年固定期限', NOW(), NOW(), 1, 1, 0, 0),
    (9200000000000000302, 9200000000000000202, 'HT-2021-002', 'UNFIXED', '2021-07-15', NULL, '2021-07-10', NULL,
     'ACTIVE', NULL, '无固定期限', NOW(), NOW(), 1, 1, 0, 0),
    (9200000000000000303, 9200000000000000203, 'HT-2026-003', 'FIXED',   '2026-09-01', '2026-11-30', '2026-08-25', NULL,
     'ACTIVE', '2026-11-15', '试用期短期合同，30 天内到期', NOW(), NOW(), 1, 1, 0, 0),
    (9200000000000000304, 9200000000000000204, 'HT-2022-004', 'FIXED',   '2022-05-10', '2025-05-09', '2022-05-06', NULL,
     'EXPIRED', NULL, '已到期未续签', NOW(), NOW(), 1, 1, 0, 0),
    (9200000000000000305, 9200000000000000205, 'HT-2024-005', 'FIXED',   '2024-02-01', '2026-10-31', '2024-01-28', NULL,
     'TERMINATED', NULL, '随离职解除', NOW(), NOW(), 1, 1, 0, 0),
    (9200000000000000306, 9200000000000000204, 'HT-2025-006', 'FIXED',   '2025-06-01', '2026-12-01', '2025-05-26', NULL,
     'ACTIVE', '2026-11-16', '返岗后补签，30 天内到期', NOW(), NOW(), 1, 1, 0, 0);

-- ---------------------------------------------------------------------
-- 4. HR 排班（员工 201/202/203，本周 11-16~11-20 + 下周 11-23~11-27，共 30 条）
-- ---------------------------------------------------------------------
INSERT IGNORE INTO hr_schedule
    (id, employee_id, work_date, shift_code, start_time, end_time, schedule_type, remark,
     create_time, update_time, create_by, update_by, deleted, version)
VALUES
    (9200000000000000401, 9200000000000000201, '2026-11-16', 'DAY', '09:00:00', '18:00:00', 'FIXED', NULL, NOW(), NOW(), 1, 1, 0, 0),
    (9200000000000000402, 9200000000000000201, '2026-11-17', 'DAY', '09:00:00', '18:00:00', 'FIXED', NULL, NOW(), NOW(), 1, 1, 0, 0),
    (9200000000000000403, 9200000000000000201, '2026-11-18', 'DAY', '09:00:00', '18:00:00', 'FIXED', NULL, NOW(), NOW(), 1, 1, 0, 0),
    (9200000000000000404, 9200000000000000201, '2026-11-19', 'DAY', '09:00:00', '18:00:00', 'FIXED', NULL, NOW(), NOW(), 1, 1, 0, 0),
    (9200000000000000405, 9200000000000000201, '2026-11-20', 'DAY', '09:00:00', '18:00:00', 'FIXED', NULL, NOW(), NOW(), 1, 1, 0, 0),
    (9200000000000000406, 9200000000000000201, '2026-11-23', 'DAY', '09:00:00', '18:00:00', 'FIXED', NULL, NOW(), NOW(), 1, 1, 0, 0),
    (9200000000000000407, 9200000000000000201, '2026-11-24', 'DAY', '09:00:00', '18:00:00', 'FIXED', NULL, NOW(), NOW(), 1, 1, 0, 0),
    (9200000000000000408, 9200000000000000201, '2026-11-25', 'DAY', '09:00:00', '18:00:00', 'FIXED', NULL, NOW(), NOW(), 1, 1, 0, 0),
    (9200000000000000409, 9200000000000000201, '2026-11-26', 'DAY', '09:00:00', '18:00:00', 'FIXED', NULL, NOW(), NOW(), 1, 1, 0, 0),
    (9200000000000000410, 9200000000000000201, '2026-11-27', 'DAY', '09:00:00', '18:00:00', 'FIXED', NULL, NOW(), NOW(), 1, 1, 0, 0),
    (9200000000000000411, 9200000000000000202, '2026-11-16', 'DAY', '09:00:00', '18:00:00', 'FIXED', NULL, NOW(), NOW(), 1, 1, 0, 0),
    (9200000000000000412, 9200000000000000202, '2026-11-17', 'DAY', '09:00:00', '18:00:00', 'FIXED', NULL, NOW(), NOW(), 1, 1, 0, 0),
    (9200000000000000413, 9200000000000000202, '2026-11-18', 'DAY', '09:00:00', '18:00:00', 'FIXED', NULL, NOW(), NOW(), 1, 1, 0, 0),
    (9200000000000000414, 9200000000000000202, '2026-11-19', 'DAY', '09:00:00', '18:00:00', 'FIXED', NULL, NOW(), NOW(), 1, 1, 0, 0),
    (9200000000000000415, 9200000000000000202, '2026-11-20', 'DAY', '09:00:00', '18:00:00', 'FIXED', NULL, NOW(), NOW(), 1, 1, 0, 0),
    (9200000000000000416, 9200000000000000202, '2026-11-23', 'DAY', '09:00:00', '18:00:00', 'FIXED', NULL, NOW(), NOW(), 1, 1, 0, 0),
    (9200000000000000417, 9200000000000000202, '2026-11-24', 'DAY', '09:00:00', '18:00:00', 'FIXED', NULL, NOW(), NOW(), 1, 1, 0, 0),
    (9200000000000000418, 9200000000000000202, '2026-11-25', 'DAY', '09:00:00', '18:00:00', 'FIXED', NULL, NOW(), NOW(), 1, 1, 0, 0),
    (9200000000000000419, 9200000000000000202, '2026-11-26', 'DAY', '09:00:00', '18:00:00', 'FIXED', NULL, NOW(), NOW(), 1, 1, 0, 0),
    (9200000000000000420, 9200000000000000202, '2026-11-27', 'DAY', '09:00:00', '18:00:00', 'FIXED', NULL, NOW(), NOW(), 1, 1, 0, 0),
    (9200000000000000421, 9200000000000000203, '2026-11-16', 'DAY', '09:00:00', '18:00:00', 'FIXED', NULL, NOW(), NOW(), 1, 1, 0, 0),
    (9200000000000000422, 9200000000000000203, '2026-11-17', 'DAY', '09:00:00', '18:00:00', 'FIXED', NULL, NOW(), NOW(), 1, 1, 0, 0),
    (9200000000000000423, 9200000000000000203, '2026-11-18', 'DAY', '09:00:00', '18:00:00', 'FIXED', NULL, NOW(), NOW(), 1, 1, 0, 0),
    (9200000000000000424, 9200000000000000203, '2026-11-19', 'DAY', '09:00:00', '18:00:00', 'FIXED', NULL, NOW(), NOW(), 1, 1, 0, 0),
    (9200000000000000425, 9200000000000000203, '2026-11-20', 'DAY', '09:00:00', '18:00:00', 'FIXED', NULL, NOW(), NOW(), 1, 1, 0, 0),
    (9200000000000000426, 9200000000000000203, '2026-11-23', 'DAY', '09:00:00', '18:00:00', 'FIXED', NULL, NOW(), NOW(), 1, 1, 0, 0),
    (9200000000000000427, 9200000000000000203, '2026-11-24', 'DAY', '09:00:00', '18:00:00', 'FIXED', NULL, NOW(), NOW(), 1, 1, 0, 0),
    (9200000000000000428, 9200000000000000203, '2026-11-25', 'DAY', '09:00:00', '18:00:00', 'FIXED', NULL, NOW(), NOW(), 1, 1, 0, 0),
    (9200000000000000429, 9200000000000000203, '2026-11-26', 'DAY', '09:00:00', '18:00:00', 'FIXED', NULL, NOW(), NOW(), 1, 1, 0, 0),
    (9200000000000000430, 9200000000000000203, '2026-11-27', 'DAY', '09:00:00', '18:00:00', 'FIXED', NULL, NOW(), NOW(), 1, 1, 0, 0);

-- ---------------------------------------------------------------------
-- 5. HR 打卡（员工 201/202/203，10 个工作日 × 上下班，共 60 条）
--    状态覆盖 NORMAL/LATE/EARLY/ABSENT，来源覆盖 MINI/DEVICE/MANUAL。
--    2026-11-25 员工 203 缺卡两条；2026-11-19 203 下班为补卡（MANUAL）。
-- ---------------------------------------------------------------------
INSERT IGNORE INTO hr_checkin
    (id, employee_id, checkin_date, checkin_time, checkin_type, latitude, longitude,
     wifi_ssid, wifi_bssid, address, source, status, remark,
     create_time, update_time, create_by, update_by, deleted, version)
VALUES
    (9200000000000000501, 9200000000000000201, '2026-11-16', '2026-11-16 08:55:12', 'IN',  39.9834000, 116.3166000, 'CEAMS-OFFICE', 'AC:84:C6:11:22:01', '北京市海淀区中关村软件园', 'MINI',   'NORMAL', NULL, NOW(), NOW(), 1, 1, 0, 0),
    (9200000000000000502, 9200000000000000201, '2026-11-16', '2026-11-16 18:05:31', 'OUT', 39.9834000, 116.3166000, 'CEAMS-OFFICE', 'AC:84:C6:11:22:01', '北京市海淀区中关村软件园', 'DEVICE', 'NORMAL', NULL, NOW(), NOW(), 1, 1, 0, 0),
    (9200000000000000503, 9200000000000000201, '2026-11-17', '2026-11-17 08:51:40', 'IN',  39.9834000, 116.3166000, 'CEAMS-OFFICE', 'AC:84:C6:11:22:01', '北京市海淀区中关村软件园', 'DEVICE', 'NORMAL', NULL, NOW(), NOW(), 1, 1, 0, 0),
    (9200000000000000504, 9200000000000000201, '2026-11-17', '2026-11-17 18:02:18', 'OUT', 39.9834000, 116.3166000, 'CEAMS-OFFICE', 'AC:84:C6:11:22:01', '北京市海淀区中关村软件园', 'MINI',   'NORMAL', NULL, NOW(), NOW(), 1, 1, 0, 0),
    (9200000000000000505, 9200000000000000201, '2026-11-18', '2026-11-18 08:57:03', 'IN',  39.9834000, 116.3166000, 'CEAMS-OFFICE', 'AC:84:C6:11:22:01', '北京市海淀区中关村软件园', 'MINI',   'NORMAL', NULL, NOW(), NOW(), 1, 1, 0, 0),
    (9200000000000000506, 9200000000000000201, '2026-11-18', '2026-11-18 18:07:45', 'OUT', 39.9834000, 116.3166000, 'CEAMS-OFFICE', 'AC:84:C6:11:22:01', '北京市海淀区中关村软件园', 'DEVICE', 'NORMAL', NULL, NOW(), NOW(), 1, 1, 0, 0),
    (9200000000000000507, 9200000000000000201, '2026-11-19', '2026-11-19 08:49:55', 'IN',  39.9834000, 116.3166000, 'CEAMS-OFFICE', 'AC:84:C6:11:22:01', '北京市海淀区中关村软件园', 'DEVICE', 'NORMAL', NULL, NOW(), NOW(), 1, 1, 0, 0),
    (9200000000000000508, 9200000000000000201, '2026-11-19', '2026-11-19 18:03:09', 'OUT', 39.9834000, 116.3166000, 'CEAMS-OFFICE', 'AC:84:C6:11:22:01', '北京市海淀区中关村软件园', 'MINI',   'NORMAL', NULL, NOW(), NOW(), 1, 1, 0, 0),
    (9200000000000000509, 9200000000000000201, '2026-11-20', '2026-11-20 08:56:22', 'IN',  39.9834000, 116.3166000, 'CEAMS-OFFICE', 'AC:84:C6:11:22:01', '北京市海淀区中关村软件园', 'MINI',   'NORMAL', NULL, NOW(), NOW(), 1, 1, 0, 0),
    (9200000000000000510, 9200000000000000201, '2026-11-20', '2026-11-20 18:06:40', 'OUT', 39.9834000, 116.3166000, 'CEAMS-OFFICE', 'AC:84:C6:11:22:01', '北京市海淀区中关村软件园', 'DEVICE', 'NORMAL', NULL, NOW(), NOW(), 1, 1, 0, 0),
    (9200000000000000511, 9200000000000000201, '2026-11-23', '2026-11-23 08:53:11', 'IN',  39.9834000, 116.3166000, 'CEAMS-OFFICE', 'AC:84:C6:11:22:01', '北京市海淀区中关村软件园', 'DEVICE', 'NORMAL', NULL, NOW(), NOW(), 1, 1, 0, 0),
    (9200000000000000512, 9200000000000000201, '2026-11-23', '2026-11-23 18:04:27', 'OUT', 39.9834000, 116.3166000, 'CEAMS-OFFICE', 'AC:84:C6:11:22:01', '北京市海淀区中关村软件园', 'MINI',   'NORMAL', NULL, NOW(), NOW(), 1, 1, 0, 0),
    (9200000000000000513, 9200000000000000201, '2026-11-24', '2026-11-24 08:58:36', 'IN',  39.9834000, 116.3166000, 'CEAMS-OFFICE', 'AC:84:C6:11:22:01', '北京市海淀区中关村软件园', 'MINI',   'NORMAL', NULL, NOW(), NOW(), 1, 1, 0, 0),
    (9200000000000000514, 9200000000000000201, '2026-11-24', '2026-11-24 18:01:52', 'OUT', 39.9834000, 116.3166000, 'CEAMS-OFFICE', 'AC:84:C6:11:22:01', '北京市海淀区中关村软件园', 'DEVICE', 'NORMAL', NULL, NOW(), NOW(), 1, 1, 0, 0),
    (9200000000000000515, 9200000000000000201, '2026-11-25', '2026-11-25 08:50:44', 'IN',  39.9834000, 116.3166000, 'CEAMS-OFFICE', 'AC:84:C6:11:22:01', '北京市海淀区中关村软件园', 'DEVICE', 'NORMAL', NULL, NOW(), NOW(), 1, 1, 0, 0),
    (9200000000000000516, 9200000000000000201, '2026-11-25', '2026-11-25 18:08:13', 'OUT', 39.9834000, 116.3166000, 'CEAMS-OFFICE', 'AC:84:C6:11:22:01', '北京市海淀区中关村软件园', 'MINI',   'NORMAL', NULL, NOW(), NOW(), 1, 1, 0, 0),
    (9200000000000000517, 9200000000000000201, '2026-11-26', '2026-11-26 08:54:29', 'IN',  39.9834000, 116.3166000, 'CEAMS-OFFICE', 'AC:84:C6:11:22:01', '北京市海淀区中关村软件园', 'MINI',   'NORMAL', NULL, NOW(), NOW(), 1, 1, 0, 0),
    (9200000000000000518, 9200000000000000201, '2026-11-26', '2026-11-26 18:05:06', 'OUT', 39.9834000, 116.3166000, 'CEAMS-OFFICE', 'AC:84:C6:11:22:01', '北京市海淀区中关村软件园', 'DEVICE', 'NORMAL', NULL, NOW(), NOW(), 1, 1, 0, 0),
    (9200000000000000519, 9200000000000000201, '2026-11-27', '2026-11-27 08:52:58', 'IN',  39.9834000, 116.3166000, 'CEAMS-OFFICE', 'AC:84:C6:11:22:01', '北京市海淀区中关村软件园', 'DEVICE', 'NORMAL', NULL, NOW(), NOW(), 1, 1, 0, 0),
    (9200000000000000520, 9200000000000000201, '2026-11-27', '2026-11-27 18:00:41', 'OUT', 39.9834000, 116.3166000, 'CEAMS-OFFICE', 'AC:84:C6:11:22:01', '北京市海淀区中关村软件园', 'MINI',   'NORMAL', NULL, NOW(), NOW(), 1, 1, 0, 0),

    (9200000000000000521, 9200000000000000202, '2026-11-16', '2026-11-16 08:47:21', 'IN',  39.9834000, 116.3166000, 'CEAMS-OFFICE', 'AC:84:C6:11:22:01', '北京市海淀区中关村软件园', 'MINI',   'NORMAL', NULL, NOW(), NOW(), 1, 1, 0, 0),
    (9200000000000000522, 9200000000000000202, '2026-11-16', '2026-11-16 18:11:05', 'OUT', 39.9834000, 116.3166000, 'CEAMS-OFFICE', 'AC:84:C6:11:22:01', '北京市海淀区中关村软件园', 'MINI',   'NORMAL', NULL, NOW(), NOW(), 1, 1, 0, 0),
    (9200000000000000523, 9200000000000000202, '2026-11-17', '2026-11-17 08:44:09', 'IN',  39.9834000, 116.3166000, 'CEAMS-OFFICE', 'AC:84:C6:11:22:01', '北京市海淀区中关村软件园', 'DEVICE', 'NORMAL', NULL, NOW(), NOW(), 1, 1, 0, 0),
    (9200000000000000524, 9200000000000000202, '2026-11-17', '2026-11-17 18:09:33', 'OUT', 39.9834000, 116.3166000, 'CEAMS-OFFICE', 'AC:84:C6:11:22:01', '北京市海淀区中关村软件园', 'DEVICE', 'NORMAL', NULL, NOW(), NOW(), 1, 1, 0, 0),
    (9200000000000000525, 9200000000000000202, '2026-11-18', '2026-11-18 09:17:46', 'IN',  39.9834000, 116.3166000, 'CEAMS-OFFICE', 'AC:84:C6:11:22:01', '北京市海淀区中关村软件园', 'MINI',   'LATE',   '迟到 17 分钟', NOW(), NOW(), 1, 1, 0, 0),
    (9200000000000000526, 9200000000000000202, '2026-11-18', NULL, 'OUT', 39.9834000, 116.3166000, 'CEAMS-OFFICE', 'AC:84:C6:11:22:01', '北京市海淀区中关村软件园', 'MINI',   'ABSENT', '下班未打卡，已提交补卡申请', NOW(), NOW(), 1, 1, 0, 0),
    (9200000000000000527, 9200000000000000202, '2026-11-19', '2026-11-19 08:46:12', 'IN',  39.9834000, 116.3166000, 'CEAMS-OFFICE', 'AC:84:C6:11:22:01', '北京市海淀区中关村软件园', 'MINI',   'NORMAL', NULL, NOW(), NOW(), 1, 1, 0, 0),
    (9200000000000000528, 9200000000000000202, '2026-11-19', '2026-11-19 18:12:50', 'OUT', 39.9834000, 116.3166000, 'CEAMS-OFFICE', 'AC:84:C6:11:22:01', '北京市海淀区中关村软件园', 'MINI',   'NORMAL', NULL, NOW(), NOW(), 1, 1, 0, 0),
    (9200000000000000529, 9200000000000000202, '2026-11-20', '2026-11-20 08:42:33', 'IN',  39.9834000, 116.3166000, 'CEAMS-OFFICE', 'AC:84:C6:11:22:01', '北京市海淀区中关村软件园', 'MINI',   'NORMAL', NULL, NOW(), NOW(), 1, 1, 0, 0),
    (9200000000000000530, 9200000000000000202, '2026-11-20', '2026-11-20 17:35:18', 'OUT', 39.9834000, 116.3166000, 'CEAMS-OFFICE', 'AC:84:C6:11:22:01', '北京市海淀区中关村软件园', 'MINI',   'EARLY',  '早退 25 分钟', NOW(), NOW(), 1, 1, 0, 0),
    (9200000000000000531, 9200000000000000202, '2026-11-23', '2026-11-23 08:45:59', 'IN',  39.9834000, 116.3166000, 'CEAMS-OFFICE', 'AC:84:C6:11:22:01', '北京市海淀区中关村软件园', 'MINI',   'NORMAL', NULL, NOW(), NOW(), 1, 1, 0, 0),
    (9200000000000000532, 9200000000000000202, '2026-11-23', '2026-11-23 18:10:27', 'OUT', 39.9834000, 116.3166000, 'CEAMS-OFFICE', 'AC:84:C6:11:22:01', '北京市海淀区中关村软件园', 'MINI',   'NORMAL', NULL, NOW(), NOW(), 1, 1, 0, 0),
    (9200000000000000533, 9200000000000000202, '2026-11-24', '2026-11-24 09:12:07', 'IN',  39.9834000, 116.3166000, 'CEAMS-OFFICE', 'AC:84:C6:11:22:01', '北京市海淀区中关村软件园', 'MINI',   'LATE',   '迟到 12 分钟', NOW(), NOW(), 1, 1, 0, 0),
    (9200000000000000534, 9200000000000000202, '2026-11-24', '2026-11-24 18:08:44', 'OUT', 39.9834000, 116.3166000, 'CEAMS-OFFICE', 'AC:84:C6:11:22:01', '北京市海淀区中关村软件园', 'MINI',   'NORMAL', NULL, NOW(), NOW(), 1, 1, 0, 0),
    (9200000000000000535, 9200000000000000202, '2026-11-25', '2026-11-25 08:48:15', 'IN',  39.9834000, 116.3166000, 'CEAMS-OFFICE', 'AC:84:C6:11:22:01', '北京市海淀区中关村软件园', 'MINI',   'NORMAL', NULL, NOW(), NOW(), 1, 1, 0, 0),
    (9200000000000000536, 9200000000000000202, '2026-11-25', '2026-11-25 18:13:02', 'OUT', 39.9834000, 116.3166000, 'CEAMS-OFFICE', 'AC:84:C6:11:22:01', '北京市海淀区中关村软件园', 'MINI',   'NORMAL', NULL, NOW(), NOW(), 1, 1, 0, 0),
    (9200000000000000537, 9200000000000000202, '2026-11-26', '2026-11-26 08:43:51', 'IN',  39.9834000, 116.3166000, 'CEAMS-OFFICE', 'AC:84:C6:11:22:01', '北京市海淀区中关村软件园', 'MINI',   'NORMAL', NULL, NOW(), NOW(), 1, 1, 0, 0),
    (9200000000000000538, 9200000000000000202, '2026-11-26', '2026-11-26 18:06:36', 'OUT', 39.9834000, 116.3166000, 'CEAMS-OFFICE', 'AC:84:C6:11:22:01', '北京市海淀区中关村软件园', 'MINI',   'NORMAL', NULL, NOW(), NOW(), 1, 1, 0, 0),
    (9200000000000000539, 9200000000000000202, '2026-11-27', '2026-11-27 08:49:24', 'IN',  39.9834000, 116.3166000, 'CEAMS-OFFICE', 'AC:84:C6:11:22:01', '北京市海淀区中关村软件园', 'MINI',   'NORMAL', NULL, NOW(), NOW(), 1, 1, 0, 0),
    (9200000000000000540, 9200000000000000202, '2026-11-27', '2026-11-27 18:07:19', 'OUT', 39.9834000, 116.3166000, 'CEAMS-OFFICE', 'AC:84:C6:11:22:01', '北京市海淀区中关村软件园', 'MINI',   'NORMAL', NULL, NOW(), NOW(), 1, 1, 0, 0),

    (9200000000000000541, 9200000000000000203, '2026-11-16', '2026-11-16 08:59:33', 'IN',  39.9834000, 116.3166000, 'CEAMS-OFFICE', 'AC:84:C6:11:22:01', '北京市海淀区中关村软件园', 'DEVICE', 'NORMAL', NULL, NOW(), NOW(), 1, 1, 0, 0),
    (9200000000000000542, 9200000000000000203, '2026-11-16', '2026-11-16 18:04:55', 'OUT', 39.9834000, 116.3166000, 'CEAMS-OFFICE', 'AC:84:C6:11:22:01', '北京市海淀区中关村软件园', 'DEVICE', 'NORMAL', NULL, NOW(), NOW(), 1, 1, 0, 0),
    (9200000000000000543, 9200000000000000203, '2026-11-17', '2026-11-17 08:58:02', 'IN',  39.9834000, 116.3166000, 'CEAMS-OFFICE', 'AC:84:C6:11:22:01', '北京市海淀区中关村软件园', 'DEVICE', 'NORMAL', NULL, NOW(), NOW(), 1, 1, 0, 0),
    (9200000000000000544, 9200000000000000203, '2026-11-17', '2026-11-17 18:02:47', 'OUT', 39.9834000, 116.3166000, 'CEAMS-OFFICE', 'AC:84:C6:11:22:01', '北京市海淀区中关村软件园', 'DEVICE', 'NORMAL', NULL, NOW(), NOW(), 1, 1, 0, 0),
    (9200000000000000545, 9200000000000000203, '2026-11-18', '2026-11-18 09:21:18', 'IN',  39.9834000, 116.3166000, 'CEAMS-OFFICE', 'AC:84:C6:11:22:01', '北京市海淀区中关村软件园', 'DEVICE', 'LATE',   '迟到 21 分钟', NOW(), NOW(), 1, 1, 0, 0),
    (9200000000000000546, 9200000000000000203, '2026-11-18', '2026-11-18 18:06:29', 'OUT', 39.9834000, 116.3166000, 'CEAMS-OFFICE', 'AC:84:C6:11:22:01', '北京市海淀区中关村软件园', 'DEVICE', 'NORMAL', NULL, NOW(), NOW(), 1, 1, 0, 0),
    (9200000000000000547, 9200000000000000203, '2026-11-19', '2026-11-19 08:57:41', 'IN',  39.9834000, 116.3166000, 'CEAMS-OFFICE', 'AC:84:C6:11:22:01', '北京市海淀区中关村软件园', 'MINI',   'NORMAL', NULL, NOW(), NOW(), 1, 1, 0, 0),
    (9200000000000000548, 9200000000000000203, '2026-11-19', '2026-11-19 18:05:58', 'OUT', 39.9834000, 116.3166000, 'CEAMS-OFFICE', 'AC:84:C6:11:22:01', '北京市海淀区中关村软件园', 'MANUAL', 'NORMAL', '补卡审批通过后补录', NOW(), NOW(), 1, 1, 0, 0),
    (9200000000000000549, 9200000000000000203, '2026-11-20', '2026-11-20 08:59:07', 'IN',  39.9834000, 116.3166000, 'CEAMS-OFFICE', 'AC:84:C6:11:22:01', '北京市海淀区中关村软件园', 'DEVICE', 'NORMAL', NULL, NOW(), NOW(), 1, 1, 0, 0),
    (9200000000000000550, 9200000000000000203, '2026-11-20', '2026-11-20 18:03:36', 'OUT', 39.9834000, 116.3166000, 'CEAMS-OFFICE', 'AC:84:C6:11:22:01', '北京市海淀区中关村软件园', 'DEVICE', 'NORMAL', NULL, NOW(), NOW(), 1, 1, 0, 0),
    (9200000000000000551, 9200000000000000203, '2026-11-23', '2026-11-23 08:56:24', 'IN',  39.9834000, 116.3166000, 'CEAMS-OFFICE', 'AC:84:C6:11:22:01', '北京市海淀区中关村软件园', 'MINI',   'NORMAL', NULL, NOW(), NOW(), 1, 1, 0, 0),
    (9200000000000000552, 9200000000000000203, '2026-11-23', '2026-11-23 18:00:59', 'OUT', 39.9834000, 116.3166000, 'CEAMS-OFFICE', 'AC:84:C6:11:22:01', '北京市海淀区中关村软件园', 'MINI',   'NORMAL', NULL, NOW(), NOW(), 1, 1, 0, 0),
    (9200000000000000553, 9200000000000000203, '2026-11-24', '2026-11-24 08:58:50', 'IN',  39.9834000, 116.3166000, 'CEAMS-OFFICE', 'AC:84:C6:11:22:01', '北京市海淀区中关村软件园', 'MINI',   'NORMAL', NULL, NOW(), NOW(), 1, 1, 0, 0),
    (9200000000000000554, 9200000000000000203, '2026-11-24', '2026-11-24 18:07:11', 'OUT', 39.9834000, 116.3166000, 'CEAMS-OFFICE', 'AC:84:C6:11:22:01', '北京市海淀区中关村软件园', 'MINI',   'NORMAL', NULL, NOW(), NOW(), 1, 1, 0, 0),
    (9200000000000000555, 9200000000000000203, '2026-11-25', NULL, 'IN',  NULL, NULL, NULL, NULL, NULL, 'MINI', 'ABSENT', '全天缺卡', NOW(), NOW(), 1, 1, 0, 0),
    (9200000000000000556, 9200000000000000203, '2026-11-25', NULL, 'OUT', NULL, NULL, NULL, NULL, NULL, 'MINI', 'ABSENT', '全天缺卡', NOW(), NOW(), 1, 1, 0, 0),
    (9200000000000000557, 9200000000000000203, '2026-11-26', '2026-11-26 08:59:16', 'IN',  39.9834000, 116.3166000, 'CEAMS-OFFICE', 'AC:84:C6:11:22:01', '北京市海淀区中关村软件园', 'MINI',   'NORMAL', NULL, NOW(), NOW(), 1, 1, 0, 0),
    (9200000000000000558, 9200000000000000203, '2026-11-26', '2026-11-26 18:04:22', 'OUT', 39.9834000, 116.3166000, 'CEAMS-OFFICE', 'AC:84:C6:11:22:01', '北京市海淀区中关村软件园', 'MINI',   'NORMAL', NULL, NOW(), NOW(), 1, 1, 0, 0),
    (9200000000000000559, 9200000000000000203, '2026-11-27', '2026-11-27 08:57:33', 'IN',  39.9834000, 116.3166000, 'CEAMS-OFFICE', 'AC:84:C6:11:22:01', '北京市海淀区中关村软件园', 'MINI',   'NORMAL', NULL, NOW(), NOW(), 1, 1, 0, 0),
    (9200000000000000560, 9200000000000000203, '2026-11-27', '2026-11-27 18:02:04', 'OUT', 39.9834000, 116.3166000, 'CEAMS-OFFICE', 'AC:84:C6:11:22:01', '北京市海淀区中关村软件园', 'MINI',   'NORMAL', NULL, NOW(), NOW(), 1, 1, 0, 0);

-- ---------------------------------------------------------------------
-- 6. HR 补卡申请（2 条：已通过 / 待审批）
-- ---------------------------------------------------------------------
INSERT IGNORE INTO hr_checkin_supplement
    (id, employee_id, checkin_date, checkin_time, checkin_type, reason, approval_status,
     bpm_instance_id, create_time, update_time, create_by, update_by, deleted, version)
VALUES
    (9200000000000000601, 9200000000000000203, '2026-11-19', '2026-11-19 18:05:58', 'OUT',
     '下班忘记打卡，门禁记录可佐证', 'APPROVED', NULL, NOW(), NOW(), 1, 1, 0, 0),
    (9200000000000000602, 9200000000000000202, '2026-11-18', '2026-11-18 18:10:00', 'OUT',
     '手机没电导致未打卡', 'PENDING', NULL, NOW(), NOW(), 1, 1, 0, 0);

-- ---------------------------------------------------------------------
-- 7. HR 假期额度（4 名员工 × 3 类 = 12 条，2026 年度，已用 + 剩余 = 总额）
-- ---------------------------------------------------------------------
INSERT IGNORE INTO hr_leave_quota
    (id, employee_id, year, leave_type, total_days, used_days, remaining_days, expire_date,
     create_time, update_time, create_by, update_by, deleted, version)
VALUES
    (9200000000000000701, 9200000000000000201, 2026, 'ANNUAL',   10.0, 2.0, 8.0,  '2026-12-31', NOW(), NOW(), 1, 1, 0, 0),
    (9200000000000000702, 9200000000000000201, 2026, 'SICK',     10.0, 0.0, 10.0, '2026-12-31', NOW(), NOW(), 1, 1, 0, 0),
    (9200000000000000703, 9200000000000000201, 2026, 'PERSONAL',  5.0, 0.0, 5.0,  '2026-12-31', NOW(), NOW(), 1, 1, 0, 0),
    (9200000000000000704, 9200000000000000202, 2026, 'ANNUAL',   10.0, 0.0, 10.0, '2026-12-31', NOW(), NOW(), 1, 1, 0, 0),
    (9200000000000000705, 9200000000000000202, 2026, 'SICK',     10.0, 0.0, 10.0, '2026-12-31', NOW(), NOW(), 1, 1, 0, 0),
    (9200000000000000706, 9200000000000000202, 2026, 'PERSONAL',  5.0, 1.0, 4.0,  '2026-12-31', NOW(), NOW(), 1, 1, 0, 0),
    (9200000000000000707, 9200000000000000203, 2026, 'ANNUAL',    5.0, 0.0, 5.0,  '2026-12-31', NOW(), NOW(), 1, 1, 0, 0),
    (9200000000000000708, 9200000000000000203, 2026, 'SICK',     10.0, 1.0, 9.0,  '2026-12-31', NOW(), NOW(), 1, 1, 0, 0),
    (9200000000000000709, 9200000000000000203, 2026, 'PERSONAL',  5.0, 0.0, 5.0,  '2026-12-31', NOW(), NOW(), 1, 1, 0, 0),
    (9200000000000000710, 9200000000000000204, 2026, 'ANNUAL',   15.0, 2.0, 13.0, '2026-12-31', NOW(), NOW(), 1, 1, 0, 0),
    (9200000000000000711, 9200000000000000204, 2026, 'SICK',     10.0, 0.0, 10.0, '2026-12-31', NOW(), NOW(), 1, 1, 0, 0),
    (9200000000000000712, 9200000000000000204, 2026, 'PERSONAL',  5.0, 0.0, 5.0,  '2026-12-31', NOW(), NOW(), 1, 1, 0, 0);

-- ---------------------------------------------------------------------
-- 8. HR 请假单（4 条：APPROVED/PENDING/REJECTED，类型含 ANNUAL/SICK/PERSONAL）
-- ---------------------------------------------------------------------
INSERT IGNORE INTO hr_leave_request
    (id, employee_id, leave_type, start_date, end_date, days, reason, approval_status,
     bpm_instance_id, create_time, update_time, create_by, update_by, deleted, version)
VALUES
    (9200000000000000801, 9200000000000000201, 'ANNUAL',   '2026-10-08', '2026-10-09', 2.0,
     '国庆后调休', 'APPROVED', NULL, NOW(), NOW(), 1, 1, 0, 0),
    (9200000000000000802, 9200000000000000202, 'PERSONAL', '2026-11-23', '2026-11-23', 1.0,
     '家中有事需处理', 'APPROVED', NULL, NOW(), NOW(), 1, 1, 0, 0),
    (9200000000000000803, 9200000000000000203, 'SICK',     '2026-11-25', '2026-11-25', 1.0,
     '感冒发烧需就诊', 'PENDING', NULL, NOW(), NOW(), 1, 1, 0, 0),
    (9200000000000000804, 9200000000000000204, 'ANNUAL',   '2026-12-01', '2026-12-02', 2.0,
     '外出旅行', 'REJECTED', NULL, NOW(), NOW(), 1, 1, 0, 0);

-- ---------------------------------------------------------------------
-- 9. HR 薪资台账（4 名员工各 1 条生效记录，金额与密文成对写入）
-- ---------------------------------------------------------------------
INSERT IGNORE INTO hr_salary
    (id, employee_id, effective_date, base_salary_enc, post_salary_enc, performance_base_enc,
     allowance_enc, status, change_apply_id, remark,
     create_time, update_time, create_by, update_by, deleted, version)
VALUES
    (9200000000000000901, 9200000000000000201, '2025-01-01',
     'v1:evSMb/VSENJmwRqGdEznN3pGiNKZbKeK3Ff0N0iS0ou8dko=', 'v1:oxARIzzF3LNqFZ4WJHAuN+/w6/W0/4Qn/y+DrTXUzUwhpWE=', 'v1:GemeEQjDQeDRUf5ZpFcrnVm/cknzVpLUfNGjou1u1vCCUqo=', 'v1:IWui+hgaQ5vp13rWw/BpNzcsS3rLqTnNvL0FQ8uHXXcUqw==',
     'ACTIVE', NULL, '项目经理标准薪资', NOW(), NOW(), 1, 1, 0, 0),
    (9200000000000000902, 9200000000000000202, '2024-01-01',
     'v1:A17FXebdE1xAu6cjs1xbtvjxs06QiqaJkcOMl/q1tYMV7nnB', 'v1:evSMb/VSENJmwRqGdEznN3pGiNKZbKeK3Ff0N0iS0ou8dko=', 'v1:qu2164SBAjUJdJ2t6uPL+UvsAiTTIUh84T0MGE6rDTRltXo=', 'v1:k4+oPyh6EM0bfKql77pl9+dZlOslbhart5sFeqU3TSd6jdQ=',
     'ACTIVE', NULL, '高级项目经理薪资', NOW(), NOW(), 1, 1, 0, 0),
    (9200000000000000903, 9200000000000000203, '2026-09-01',
     'v1:8nJ21hWK69s80a4vn5ye6+pU/+Q4Ibfh2qO5IZQrB7MDF2U=', 'v1:qu2164SBAjUJdJ2t6uPL+UvsAiTTIUh84T0MGE6rDTRltXo=', 'v1:biXL4xxT4N+zhch7Y2xiaXXG5uMHwZiU57hA9zs4OzFzU+Y=', 'v1:JB0i5+Rltc/J3aYLu6LzQGv8PTXoqKSf01VoilDArReA0Q==',
     'ACTIVE', NULL, '人事经理试用期薪资', NOW(), NOW(), 1, 1, 0, 0),
    (9200000000000000904, 9200000000000000204, '2023-01-01',
     'v1:nSzIx9re1agXzUu0m8L2Wx0phdsP507ceYyiR3Uq8SzXty4q', 'v1:VCFOm8oEfs8RU89uy8Ig5hwwXWUQw07WFeEoCtd43p1DZOs=', 'v1:biXL4xxT4N+zhch7Y2xiaXXG5uMHwZiU57hA9zs4OzFzU+Y=', 'v1:2ufEoZPtC3UBrIBw44ZObaxpd6SpgpDc+VWUpEQ7ihIsKg==',
     'ACTIVE', NULL, '会计薪资', NOW(), NOW(), 1, 1, 0, 0);

-- ---------------------------------------------------------------------
-- 10. HR 调薪申请（APPROVING / APPROVED，前后快照金额加密）
-- ---------------------------------------------------------------------
INSERT IGNORE INTO hr_salary_change_apply
    (id, employee_id, applicant_id, change_type, before_snapshot, after_snapshot,
     effective_date, reason, bpm_instance_id, status,
     create_time, update_time, create_by, update_by, deleted, version)
VALUES
    (9200000000000000911, 9200000000000000201, 1, 'ADJUST',
     '{"employeeId":"9200000000000000201","effectiveDate":"2025-01-01","baseSalary":"v1:XNAQ/NAQFwEBtJEptYXsig+pwuf507r0Z2bY6DhmWIU/aQg=","postSalary":"v1:8f/O9L+QCB6f2II0PwXr2Y1ncduPcKjLCQwy9Bw5rM2IRlk=","performanceBase":"v1:Jb2NkHGEhelggoEBAVMak+SMHHZRf3qXCcHb0pyQs16/MnQ=","allowance":"v1:IWui+hgaQ5vp13rWw/BpNzcsS3rLqTnNvL0FQ8uHXXcUqw=="}',
     '{"employeeId":"9200000000000000201","effectiveDate":"2026-12-01","baseSalary":"v1:evSMb/VSENJmwRqGdEznN3pGiNKZbKeK3Ff0N0iS0ou8dko=","postSalary":"v1:oxARIzzF3LNqFZ4WJHAuN+/w6/W0/4Qn/y+DrTXUzUwhpWE=","performanceBase":"v1:GemeEQjDQeDRUf5ZpFcrnVm/cknzVpLUfNGjou1u1vCCUqo=","allowance":"v1:IWui+hgaQ5vp13rWw/BpNzcsS3rLqTnNvL0FQ8uHXXcUqw=="}',
     '2026-12-01', '年度调薪（审批中）', NULL, 'APPROVING',
     NOW(), NOW(), 1, 1, 0, 0),
    (9200000000000000912, 9200000000000000202, 1, 'PROMOTION',
     '{"employeeId":"9200000000000000202","effectiveDate":"2024-01-01","baseSalary":"v1:z66+k6p789xKgNVzMJsrExDYA0NBrmZ4c6H523e+9qI0epn3","postSalary":"v1:V2uDgfrh0Q6kzeVSdqpC65JKxj7flFLIGrkUlgOcmtVzNds=","performanceBase":"v1:oxARIzzF3LNqFZ4WJHAuN+/w6/W0/4Qn/y+DrTXUzUwhpWE=","allowance":"v1:k4+oPyh6EM0bfKql77pl9+dZlOslbhart5sFeqU3TSd6jdQ="}',
     '{"employeeId":"9200000000000000202","effectiveDate":"2026-10-01","baseSalary":"v1:A17FXebdE1xAu6cjs1xbtvjxs06QiqaJkcOMl/q1tYMV7nnB","postSalary":"v1:evSMb/VSENJmwRqGdEznN3pGiNKZbKeK3Ff0N0iS0ou8dko=","performanceBase":"v1:qu2164SBAjUJdJ2t6uPL+UvsAiTTIUh84T0MGE6rDTRltXo=","allowance":"v1:k4+oPyh6EM0bfKql77pl9+dZlOslbhart5sFeqU3TSd6jdQ="}',
     '2026-10-01', '晋升调薪（已通过）', NULL, 'APPROVED',
     NOW(), NOW(), 1, 1, 0, 0);

-- ---------------------------------------------------------------------
-- 11. HR 绩效模板（2 个，权重合计 100）
-- ---------------------------------------------------------------------
INSERT IGNORE INTO hr_performance_template
    (id, code, name, position_id, period_type, items_json, score_rules_json, total_weight,
     status, create_time, update_time, create_by, update_by, deleted, version)
VALUES
    (9200000000000001001, 'TPL-ENG-MONTH', '工程月度绩效', 9200000000000000105, 'MONTH',
     '[{"name":"业绩目标","weight":"50","minScore":"0","maxScore":"100","description":"月度产值与回款目标达成"},{"name":"项目交付","weight":"30","minScore":"0","maxScore":"100","description":"项目节点按期交付"},{"name":"团队协作","weight":"20","minScore":"0","maxScore":"100","description":"跨部门协同与人才培养"}]',
     '[{"minScore":"0","maxScore":"60","coefficient":"0.6"},{"minScore":"60","maxScore":"80","coefficient":"1.0"},{"minScore":"80","maxScore":"100","coefficient":"1.2"}]',
     100, 'ENABLED', NOW(), NOW(), 1, 1, 0, 0),
    (9200000000000001002, 'TPL-FIN-MONTH', '财务月度绩效', 9200000000000000101, 'MONTH',
     '[{"name":"核算准确性","weight":"50","minScore":"0","maxScore":"100","description":"账务与报表差错率"},{"name":"报表及时性","weight":"30","minScore":"0","maxScore":"100","description":"月结与报表按时率"},{"name":"协作支持","weight":"20","minScore":"0","maxScore":"100","description":"业务部门支持满意度"}]',
     '[{"minScore":"0","maxScore":"60","coefficient":"0.8"},{"minScore":"60","maxScore":"80","coefficient":"1.0"},{"minScore":"80","maxScore":"100","coefficient":"1.2"}]',
     100, 'ENABLED', NOW(), NOW(), 1, 1, 0, 0);

-- ---------------------------------------------------------------------
-- 12. HR 绩效考评（4 条，2026-10 周期，覆盖 DRAFT/SELF_SUBMITTED/LEADER_DONE/APPROVED）
-- ---------------------------------------------------------------------
INSERT IGNORE INTO hr_performance_review
    (id, employee_id, template_id, period, leader_id, self_score, self_comment, self_submitted_at,
     leader_score, leader_comment, final_score, coefficient, status, bpm_instance_id,
     create_time, update_time, create_by, update_by, deleted, version,
     self_items_json, leader_items_json)
VALUES
    (9200000000000001101, 9200000000000000201, 9200000000000001001, '2026-10', 9100000000000000002,
     NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'DRAFT', NULL,
     NOW(), NOW(), 1, 1, 0, 0, NULL, NULL),
    (9200000000000001102, 9200000000000000202, 9200000000000001001, '2026-10', 1,
     88.50, '本月完成两个项目节点，回款达成 95%', '2026-10-28 17:00:00',
     NULL, NULL, NULL, NULL, 'SELF_SUBMITTED', NULL,
     NOW(), NOW(), 1, 1, 0, 0,
     '[{"name":"业绩目标","weight":50,"score":90},{"name":"项目交付","weight":30,"score":88},{"name":"团队协作","weight":20,"score":86}]',
     NULL),
    (9200000000000001103, 9200000000000000203, 9200000000000001002, '2026-10', 1,
     86.00, '试用期第二个月，核算差错 1 笔', '2026-10-27 18:20:00',
     85.00, '整体达标，报表及时性需继续提升', NULL, NULL, 'LEADER_DONE', NULL,
     NOW(), NOW(), 1, 1, 0, 0,
     '[{"name":"核算准确性","weight":50,"score":85},{"name":"报表及时性","weight":30,"score":86},{"name":"协作支持","weight":20,"score":88}]',
     '[{"name":"核算准确性","weight":50,"score":84},{"name":"报表及时性","weight":30,"score":85},{"name":"协作支持","weight":20,"score":87}]'),
    (9200000000000001104, 9200000000000000204, 9200000000000001002, '2026-10', 1,
     90.00, '月结提前完成，税务申报零差错', '2026-10-26 16:40:00',
     92.00, '表现优秀，可承担更多核算复核工作', 92.00, 1.2000, 'APPROVED', NULL,
     NOW(), NOW(), 1, 1, 0, 0,
     '[{"name":"核算准确性","weight":50,"score":90},{"name":"报表及时性","weight":30,"score":90},{"name":"协作支持","weight":20,"score":90}]',
     '[{"name":"核算准确性","weight":50,"score":92},{"name":"报表及时性","weight":30,"score":92},{"name":"协作支持","weight":20,"score":92}]');

-- ---------------------------------------------------------------------
-- 13. HR 月度工资核算（2026-10，3 名员工；与既有 2026-11/2026-12 记录不冲突）
--     口径：日工资=(基本+岗位)/21.75；社保=基数*10.5%；公积金=基数*7%；
--           应发=基本+岗位+绩效+补贴；实发=应发-考勤扣款-社保-公积金-个税。
--     员工 202 迟到 1 次扣 50；员工 203 缺勤 1 天(643.68)+迟到 1 次(50)=693.68。
-- ---------------------------------------------------------------------
INSERT IGNORE INTO hr_salary_calc
    (id, employee_id, calc_month, base_salary_enc, post_salary_enc, attendance_days, absent_days,
     late_count, attendance_deduction_enc, performance_coefficient, performance_amount_enc,
     allowance_enc, social_insurance_enc, housing_fund_enc, tax_enc, gross_pay_enc, net_pay_enc,
     status, confirmed_by, confirmed_at, create_time, update_time, create_by, update_by, deleted, version)
VALUES
    (9200000000000001201, 9200000000000000201, '2026-10',
     'v1:evSMb/VSENJmwRqGdEznN3pGiNKZbKeK3Ff0N0iS0ou8dko=', 'v1:oxARIzzF3LNqFZ4WJHAuN+/w6/W0/4Qn/y+DrTXUzUwhpWE=', 21.75, 0.00, 0,
     'v1:7THp1pU0ppMLgumcIY0bQ7X04WGjv/t3ww5efbhbyFk=', 1.0000, 'v1:GemeEQjDQeDRUf5ZpFcrnVm/cknzVpLUfNGjou1u1vCCUqo=',
     'v1:IWui+hgaQ5vp13rWw/BpNzcsS3rLqTnNvL0FQ8uHXXcUqw==', 'v1:gnE6SguLiU+AIJvDgtRIWI9c9SJlEzTbDCC+CM9SegTWeHg=', 'v1:rxBBevtHUfRWXR1zCpzMi2FceQl3dFG0MZqN7BJhvqLZ1A==', 'v1:i9Fg/s1mgeqUhfn/TwKRyFy6Qo3oX2OscShjK08rilIa3A==',
     'v1:zN2XjB9967r6sphqS8VGGJbKjgadfpP5auuLRqMPNsn4cEma', 'v1:3bWEjeQvNDjQIb+q9KPF5D89LpvzgUfKUTHV5aSJ4e8+FNlj',
     'CONFIRMED', 1, '2026-11-05 09:30:00', NOW(), NOW(), 1, 1, 0, 0),
    (9200000000000001202, 9200000000000000202, '2026-10',
     'v1:A17FXebdE1xAu6cjs1xbtvjxs06QiqaJkcOMl/q1tYMV7nnB', 'v1:evSMb/VSENJmwRqGdEznN3pGiNKZbKeK3Ff0N0iS0ou8dko=', 21.75, 0.00, 1,
     'v1:wmoip/Fn3ApK0sxITi47CjuBIQ9YQVG9Vlvm2jtVrD8C', 1.2000, 'v1:+vuYPh806M7OILy9I7zZ2Z5yiXpB8vc5lhJt5W87rE5f9OI=',
     'v1:k4+oPyh6EM0bfKql77pl9+dZlOslbhart5sFeqU3TSd6jdQ=', 'v1:Gqtr5tqdBLTfg3rLyekINUDKEtMqeb+jFrNl9bL9pUhSNQ8=', 'v1:JCt6o4lFDmvTxw7RwxC7cWFEa6KWD3A6KQFhKUPOckrnWC8=', 'v1:ZBJyrpUoyyhLGATo13UrjqL1RDFwcu0DBqyDY18cSK8Q1JY=',
     'v1:ABpzQWR7dCwWgqIIDWPMmRiVlnheOh4+I6V0Yuy44D8eTK6F', 'v1:tc7o68mPQAZPZOPT6siVQ83yHMGCm0kFfkjKDiIt0Qo6mZMd',
     'CONFIRMED', 1, '2026-11-05 09:30:00', NOW(), NOW(), 1, 1, 0, 0),
    (9200000000000001203, 9200000000000000203, '2026-10',
     'v1:8nJ21hWK69s80a4vn5ye6+pU/+Q4Ibfh2qO5IZQrB7MDF2U=', 'v1:qu2164SBAjUJdJ2t6uPL+UvsAiTTIUh84T0MGE6rDTRltXo=', 21.75, 1.00, 1,
     'v1:wxw4RiPoptxszjBTVJUda4xfPs7zyj8ZOg7EwmfVkSlMBw==', 0.9000, 'v1:YfQzZl2Lqx6/29Wb5XX0zAiSlG8QAkjYO6IfKBvX4QLurTs=',
     'v1:JB0i5+Rltc/J3aYLu6LzQGv8PTXoqKSf01VoilDArReA0Q==', 'v1:EJJbhGtVO5cG7a0tCa632KM364NOr0eu4C2+iqB96kIhQVg=', 'v1:qb4JKFblKwYQhrU8oPXO2SykdcjBNJzjCTTqni6Mdary/g==', 'v1:8JPN943JPp1SmOAYExV4m+zYLsblwKO+z2yQODuPXWKeBA==',
     'v1:Vy6y7hDkNLSRmCWyToXhxnuepivIcs7Js7ef2cR51ZiRqapP', 'v1:PFDeEqTjVP+RKNrVdAbsbSvVYn0897d2qqacAcsY30o9Ysn4',
     'CONFIRMED', 1, '2026-11-05 09:30:00', NOW(), NOW(), 1, 1, 0, 0);

-- ---------------------------------------------------------------------
-- 14. HR 工资条（3 张，已发布；content_json 金额均为密文，与核算台账口径一致）
-- ---------------------------------------------------------------------
INSERT IGNORE INTO hr_salary_slip
    (id, employee_id, calc_id, calc_month, content_json, published_at, read_status, read_at,
     create_time, update_time, create_by, update_by, deleted, version)
VALUES
    (9200000000000001301, 9200000000000000201, 9200000000000001201, '2026-10',
     '{"calcMonth":"2026-10","employeeNo":"EMP-0001","employeeName":"王磊","deptId":9200000000000000013,"baseSalary":"v1:evSMb/VSENJmwRqGdEznN3pGiNKZbKeK3Ff0N0iS0ou8dko=","postSalary":"v1:oxARIzzF3LNqFZ4WJHAuN+/w6/W0/4Qn/y+DrTXUzUwhpWE=","performanceAmount":"v1:GemeEQjDQeDRUf5ZpFcrnVm/cknzVpLUfNGjou1u1vCCUqo=","allowance":"v1:IWui+hgaQ5vp13rWw/BpNzcsS3rLqTnNvL0FQ8uHXXcUqw==","attendanceDeduction":"v1:7THp1pU0ppMLgumcIY0bQ7X04WGjv/t3ww5efbhbyFk=","socialInsurance":"v1:gnE6SguLiU+AIJvDgtRIWI9c9SJlEzTbDCC+CM9SegTWeHg=","housingFund":"v1:rxBBevtHUfRWXR1zCpzMi2FceQl3dFG0MZqN7BJhvqLZ1A==","tax":"v1:i9Fg/s1mgeqUhfn/TwKRyFy6Qo3oX2OscShjK08rilIa3A==","grossPay":"v1:zN2XjB9967r6sphqS8VGGJbKjgadfpP5auuLRqMPNsn4cEma","netPay":"v1:3bWEjeQvNDjQIb+q9KPF5D89LpvzgUfKUTHV5aSJ4e8+FNlj","attendanceDays":21.75,"absentDays":0.00,"lateCount":0,"performanceCoefficient":1.0000}',
     '2026-11-05 10:00:00', 'PENDING', NULL, NOW(), NOW(), 1, 1, 0, 0),
    (9200000000000001302, 9200000000000000202, 9200000000000001202, '2026-10',
     '{"calcMonth":"2026-10","employeeNo":"EMP-0002","employeeName":"李建国","deptId":9200000000000000013,"baseSalary":"v1:A17FXebdE1xAu6cjs1xbtvjxs06QiqaJkcOMl/q1tYMV7nnB","postSalary":"v1:evSMb/VSENJmwRqGdEznN3pGiNKZbKeK3Ff0N0iS0ou8dko=","performanceAmount":"v1:+vuYPh806M7OILy9I7zZ2Z5yiXpB8vc5lhJt5W87rE5f9OI=","allowance":"v1:k4+oPyh6EM0bfKql77pl9+dZlOslbhart5sFeqU3TSd6jdQ=","attendanceDeduction":"v1:wmoip/Fn3ApK0sxITi47CjuBIQ9YQVG9Vlvm2jtVrD8C","socialInsurance":"v1:Gqtr5tqdBLTfg3rLyekINUDKEtMqeb+jFrNl9bL9pUhSNQ8=","housingFund":"v1:JCt6o4lFDmvTxw7RwxC7cWFEa6KWD3A6KQFhKUPOckrnWC8=","tax":"v1:ZBJyrpUoyyhLGATo13UrjqL1RDFwcu0DBqyDY18cSK8Q1JY=","grossPay":"v1:ABpzQWR7dCwWgqIIDWPMmRiVlnheOh4+I6V0Yuy44D8eTK6F","netPay":"v1:tc7o68mPQAZPZOPT6siVQ83yHMGCm0kFfkjKDiIt0Qo6mZMd","attendanceDays":21.75,"absentDays":0.00,"lateCount":1,"performanceCoefficient":1.2000}',
     '2026-11-05 10:00:00', 'PENDING', NULL, NOW(), NOW(), 1, 1, 0, 0),
    (9200000000000001303, 9200000000000000203, 9200000000000001203, '2026-10',
     '{"calcMonth":"2026-10","employeeNo":"EMP-0003","employeeName":"赵敏","deptId":9200000000000000012,"baseSalary":"v1:8nJ21hWK69s80a4vn5ye6+pU/+Q4Ibfh2qO5IZQrB7MDF2U=","postSalary":"v1:qu2164SBAjUJdJ2t6uPL+UvsAiTTIUh84T0MGE6rDTRltXo=","performanceAmount":"v1:YfQzZl2Lqx6/29Wb5XX0zAiSlG8QAkjYO6IfKBvX4QLurTs=","allowance":"v1:JB0i5+Rltc/J3aYLu6LzQGv8PTXoqKSf01VoilDArReA0Q==","attendanceDeduction":"v1:wxw4RiPoptxszjBTVJUda4xfPs7zyj8ZOg7EwmfVkSlMBw==","socialInsurance":"v1:EJJbhGtVO5cG7a0tCa632KM364NOr0eu4C2+iqB96kIhQVg=","housingFund":"v1:qb4JKFblKwYQhrU8oPXO2SykdcjBNJzjCTTqni6Mdary/g==","tax":"v1:8JPN943JPp1SmOAYExV4m+zYLsblwKO+z2yQODuPXWKeBA==","grossPay":"v1:Vy6y7hDkNLSRmCWyToXhxnuepivIcs7Js7ef2cR51ZiRqapP","netPay":"v1:PFDeEqTjVP+RKNrVdAbsbSvVYn0897d2qqacAcsY30o9Ysn4","attendanceDays":21.75,"absentDays":1.00,"lateCount":1,"performanceCoefficient":0.9000}',
     '2026-11-05 10:00:00', 'PENDING', NULL, NOW(), NOW(), 1, 1, 0, 0);

-- ---------------------------------------------------------------------
-- 15. HR 人事异动（转正 PENDING / 调岗 APPROVED）
-- ---------------------------------------------------------------------
INSERT IGNORE INTO hr_employee_change
    (id, employee_id, change_type, target_dept_id, target_position_id, effective_date, reason,
     approval_status, bpm_instance_id, remark, create_time, update_time, create_by, update_by,
     deleted, version)
VALUES
    (9200000000000001401, 9200000000000000203, 'REGULAR', NULL, NULL, '2026-12-01',
     '试用期考核通过，办理转正', 'PENDING', NULL, '待直属上级审批', NOW(), NOW(), 1, 1, 0, 0),
    (9200000000000001402, 9200000000000000205, 'TRANSFER', 9200000000000000012,
     9200000000000000103, '2026-09-01', '由行政助理转岗人事专员', 'APPROVED', NULL,
     '已生效', NOW(), NOW(), 1, 1, 0, 0);

-- ---------------------------------------------------------------------
-- 16. 行政易耗品（6 条常规 + 1 条低于安全库存用于预警）
-- ---------------------------------------------------------------------
INSERT IGNORE INTO admin_material
    (id, code, name, specification, unit, stock_qty, safety_stock, unit_price, status,
     create_time, update_time, create_by, update_by, deleted, version)
VALUES
    (9200000000000002001, 'MAT-A4-PAPER',   'A4 复印纸',   '70g/500张',    '包', 120.00, 50.00, 22.50,  'ENABLED',  NOW(), NOW(), 1, 1, 0, 0),
    (9200000000000002002, 'MAT-PEN-BLACK',  '中性笔',      '0.5mm 黑色',   '支', 300.00, 100.00, 2.50,  'ENABLED',  NOW(), NOW(), 1, 1, 0, 0),
    (9200000000000002003, 'MAT-TONER-HP88A','硒鼓',        'HP 88A',       '个', 12.00,  5.00,  380.00, 'ENABLED',  NOW(), NOW(), 1, 1, 0, 0),
    (9200000000000002004, 'MAT-TISSUE',     '抽纸',        '3 层 130 抽',  '箱', 40.00,  20.00, 95.00,  'ENABLED',  NOW(), NOW(), 1, 1, 0, 0),
    (9200000000000002005, 'MAT-CUP',        '一次性纸杯',  '250ml',        '包', 60.00,  30.00, 18.00,  'ENABLED',  NOW(), NOW(), 1, 1, 0, 0),
    (9200000000000002006, 'MAT-BATTERY-5',  '5 号电池',    '碱性',         '节', 200.00, 80.00, 1.80,   'ENABLED',  NOW(), NOW(), 1, 1, 0, 0),
    (9200000000000002007, 'MAT-GLOVE',      '劳保手套',    '均码',         '副', 8.00,   20.00, 6.00,   'ENABLED',  NOW(), NOW(), 1, 1, 0, 0);

-- ---------------------------------------------------------------------
-- 17. 行政易耗品申领（APPROVING / APPROVED / REJECTED）
-- ---------------------------------------------------------------------
INSERT IGNORE INTO admin_material_apply
    (id, material_id, applicant_id, quantity, reason, bpm_instance_id, status, issued_at, issued_by,
     create_time, update_time, create_by, update_by, deleted, version)
VALUES
    (9200000000000002101, 9200000000000002001, 9100000000000000004, 5.00,
     '工程部打印施工资料', NULL, 'APPROVING', NULL, NULL, NOW(), NOW(), 1, 1, 0, 0),
    (9200000000000002102, 9200000000000002003, 9100000000000000002, 2.00,
     '项目部更换硒鼓', NULL, 'APPROVED', NULL, NULL, NOW(), NOW(), 1, 1, 0, 0),
    (9200000000000002103, 9200000000000002004, 9100000000000000003, 3.00,
     '人事部日常办公使用', NULL, 'REJECTED', NULL, NULL, NOW(), NOW(), 1, 1, 0, 0);

-- ---------------------------------------------------------------------
-- 18. 行政会议室（3 间，2 启用 + 1 停用）
-- ---------------------------------------------------------------------
INSERT IGNORE INTO admin_meeting_room
    (id, name, location, capacity, equipment, status, create_time, update_time,
     create_by, update_by, deleted, version)
VALUES
    (9200000000000002201, '大会议室 A', '3 号楼 301', 20, '投影仪,白板,视频会议', 'ENABLED',  NOW(), NOW(), 1, 1, 0, 0),
    (9200000000000002202, '中会议室 B', '3 号楼 305', 10, '投影仪,白板',          'ENABLED',  NOW(), NOW(), 1, 1, 0, 0),
    (9200000000000002203, '小会议室 C', '4 号楼 402', 6,  '电视,白板',            'DISABLED', NOW(), NOW(), 1, 1, 0, 0);

-- ---------------------------------------------------------------------
-- 19. 行政会议室预定（未来日期，时间段互不冲突）
-- ---------------------------------------------------------------------
INSERT IGNORE INTO admin_meeting_apply
    (id, room_id, applicant_id, subject, start_time, end_time, attendee_ids, bpm_instance_id,
     status, create_time, update_time, create_by, update_by, deleted, version)
VALUES
    (9200000000000002301, 9200000000000002201, 9100000000000000002, '项目周例会',
     '2026-11-20 10:00:00', '2026-11-20 11:30:00', '1,9100000000000000002,9100000000000000003',
     NULL, 'APPROVING', NOW(), NOW(), 1, 1, 0, 0),
    (9200000000000002302, 9200000000000002201, 9100000000000000001, '2027 年度预算评审',
     '2026-11-24 14:00:00', '2026-11-24 15:00:00', '1,9100000000000000001,9100000000000000002',
     NULL, 'APPROVED', NOW(), NOW(), 1, 1, 0, 0),
    (9200000000000002303, 9200000000000002202, 9100000000000000003, '人事月度沟通会',
     '2026-11-19 09:30:00', '2026-11-19 10:30:00', '9100000000000000003,9100000000000000004',
     NULL, 'APPROVED', NOW(), NOW(), 1, 1, 0, 0),
    (9200000000000002304, 9200000000000002202, 9100000000000000001, '供应商沟通会（已取消）',
     '2026-11-26 16:00:00', '2026-11-26 17:00:00', '1,9100000000000000002',
     NULL, 'CANCELLED', NOW(), NOW(), 1, 1, 0, 0);

-- ---------------------------------------------------------------------
-- 20. 行政车辆（IDLE 可用 / MAINTENANCE 维修 / USING 使用中；
--     车辆枚举无「停用」值，以 USING 表示被占用不可派车）
-- ---------------------------------------------------------------------
INSERT IGNORE INTO admin_vehicle
    (id, plate_no, brand_model, seats, purchase_date, insurance_expire, inspection_expire,
     status, remark, create_time, update_time, create_by, update_by, deleted, version)
VALUES
    (9200000000000002401, '京A·12345', '别克 GL8 商务车', 7,  '2023-03-15', '2027-03-14', '2026-12-01',
     'IDLE',        '商务接待用车', NOW(), NOW(), 1, 1, 0, 0),
    (9200000000000002402, '京B·67890', '大众帕萨特',     5,  '2022-06-20', '2026-11-30', '2027-06-19',
     'MAINTENANCE', '例行保养中，保险 30 天内到期', NOW(), NOW(), 1, 1, 0, 0),
    (9200000000000002403, '京C·24680', '丰田考斯特',     19, '2021-09-01', '2027-08-31', '2027-09-01',
     'USING',       '员工通勤班车，执行中', NOW(), NOW(), 1, 1, 0, 0);

-- ---------------------------------------------------------------------
-- 21. 行政用车申请（APPROVING / APPROVED / RETURNED）
-- ---------------------------------------------------------------------
INSERT IGNORE INTO admin_vehicle_apply
    (id, vehicle_id, applicant_id, purpose, destination, start_time, end_time, bpm_instance_id,
     status, return_time, mileage, create_time, update_time, create_by, update_by, deleted, version)
VALUES
    (9200000000000002501, 9200000000000002401, 9100000000000000002, '客户拜访',
     '中关村软件园', '2026-11-25 08:30:00', '2026-11-25 12:00:00', NULL,
     'APPROVING', NULL, NULL, NOW(), NOW(), 1, 1, 0, 0),
    (9200000000000002502, 9200000000000002403, 9100000000000000004, '机场接送客户',
     '大兴国际机场', '2026-11-28 09:00:00', '2026-11-28 18:00:00', NULL,
     'APPROVED', NULL, NULL, NOW(), NOW(), 1, 1, 0, 0),
    (9200000000000002503, 9200000000000002401, 9100000000000000003, '政务大厅办事',
     '海淀区政务服务中心', '2026-11-12 09:00:00', '2026-11-12 11:00:00', NULL,
     'RETURNED', '2026-11-12 11:10:00', 42.5, NOW(), NOW(), 1, 1, 0, 0);

-- ---------------------------------------------------------------------
-- 22. 行政车辆费用（油费 / 保养 / 保险 / 维修；枚举无 INSURANCE，保险按 MAINTENANCE 归集）
-- ---------------------------------------------------------------------
INSERT IGNORE INTO admin_vehicle_expense
    (id, vehicle_id, apply_id, expense_type, amount, occur_date, attachment_name, remark,
     create_time, update_time, create_by, update_by, deleted, version)
VALUES
    (9200000000000002601, 9200000000000002401, NULL, 'FUEL',        420.00,  '2026-11-05', NULL, '95 号汽油加满', NOW(), NOW(), 1, 1, 0, 0),
    (9200000000000002602, 9200000000000002402, NULL, 'MAINTENANCE', 1280.00, '2026-11-08', NULL, '常规保养（机油机滤）', NOW(), NOW(), 1, 1, 0, 0),
    (9200000000000002603, 9200000000000002402, NULL, 'MAINTENANCE', 3600.00, '2026-11-01', NULL, '年度商业险（枚举无 INSURANCE，按 MAINTENANCE 归集）', NOW(), NOW(), 1, 1, 0, 0),
    (9200000000000002604, 9200000000000002403, NULL, 'MAINTENANCE', 860.00,  '2026-11-14', NULL, '刹车片更换', NOW(), NOW(), 1, 1, 0, 0);

-- ---------------------------------------------------------------------
-- 23. 行政访客登记（PENDING / CHECKED_IN / CHECKED_OUT / CANCELLED）
-- ---------------------------------------------------------------------
INSERT IGNORE INTO admin_visitor
    (id, visitor_name, phone_enc, company, host_user_id, visit_date, purpose, qr_token, status,
     checkin_time, checkout_time, create_time, update_time, create_by, update_by, deleted, version)
VALUES
    (9200000000000002701, '张伟', 'v1:wDYCokpfK/I/NI0NQ2G4MvJEE4X/fKPz4E/KaxKU7fAj9/9wJgrG', '中建八局', 9100000000000000002, '2026-11-20',
     '项目合作洽谈', 'SEEDVISITOR0000000000000000000001', 'PENDING', NULL, NULL,
     NOW(), NOW(), 1, 1, 0, 0),
    (9200000000000002702, '李娜', 'v1:Mkv05GukSmtibYOI+A8Nwa0Jhaiz3zr/krU64+GxMIUkYkRktz/8', '华信咨询', 9100000000000000004, '2026-11-17',
     '造价咨询沟通', 'SEEDVISITOR0000000000000000000002', 'CHECKED_IN',
     '2026-11-17 09:20:00', NULL, NOW(), NOW(), 1, 1, 0, 0),
    (9200000000000002703, '王强', 'v1:B1BIiQ7u9MrVUxj3Adu6yr7Dq8/A32/0rmnUqAYZvTCQ3q+2gWCS', '德勤审计', 9100000000000000003, '2026-11-14',
     '年度审计访谈', NULL, 'CHECKED_OUT',
     '2026-11-14 09:10:00', '2026-11-14 10:35:00', NOW(), NOW(), 1, 1, 0, 0),
    (9200000000000002704, '刘洋', 'v1:D205OuCpIHwlxZGNRwsvP+XCvhGEDFAKq3sto91R+2IH8MsEWtq+', '京诚律师事务所', 9100000000000000001, '2026-11-21',
     '合同法律意见沟通', NULL, 'CANCELLED', NULL, NULL, NOW(), NOW(), 1, 1, 0, 0);

-- ---------------------------------------------------------------------
-- 24. 行政报修（ASSIGNED 待派单 / PROCESSING 处理中 / DONE 已完工 / ACCEPTED 已验收）
-- ---------------------------------------------------------------------
INSERT IGNORE INTO admin_repair
    (id, repair_no, reporter_id, dept_id, location, category, description, images_json,
     assignee_id, bpm_instance_id, status, assigned_at, processed_at, finished_at, accepted_at,
     remark, create_time, update_time, create_by, update_by, deleted, version)
VALUES
    (9200000000000002801, 'RP-20261117-0001', 9100000000000000004, 9100000000000000013, '3 号楼茶水间',
     'WATER', '茶水间下水道堵塞，积水严重', NULL, NULL, NULL, 'ASSIGNED',
     '2026-11-17 09:00:00', NULL, NULL, NULL, '已受理待派单', NOW(), NOW(), 1, 1, 0, 0),
    (9200000000000002802, 'RP-20261117-0002', 9100000000000000003, 9100000000000000012, '4 号楼 402',
     'IT', '办公电脑无法连接内网', NULL, 1, NULL, 'PROCESSING',
     '2026-11-16 14:20:00', '2026-11-16 15:00:00', NULL, NULL, '网络排查中', NOW(), NOW(), 1, 1, 0, 0),
    (9200000000000002803, 'RP-20261117-0003', 9100000000000000002, 9100000000000000013, '3 号楼 301',
     'ELECTRIC', '会议室插座无电', NULL, 1, NULL, 'DONE',
     '2026-11-13 10:00:00', '2026-11-13 11:00:00', '2026-11-13 15:30:00', NULL,
     '已更换插座面板，待验收', NOW(), NOW(), 1, 1, 0, 0),
    (9200000000000002804, 'RP-20261117-0004', 1, 1, '2 号楼前台',
     'FURNITURE', '前台接待台抽屉滑轨损坏', NULL, 1, NULL, 'ACCEPTED',
     '2026-11-10 09:30:00', '2026-11-10 10:00:00', '2026-11-10 16:00:00', '2026-11-11 09:00:00',
     '已更换滑轨并验收通过', NOW(), NOW(), 1, 1, 0, 0);

-- ---------------------------------------------------------------------
-- 25. 行政固定资产（6 条：IN_STOCK / BORROWED / REPAIRING / SCRAPPED）
-- ---------------------------------------------------------------------
INSERT IGNORE INTO admin_asset
    (id, asset_no, name, category, specification, purchase_date, purchase_price, dept_id,
     keeper_id, location, status, depreciation_months, remark,
     create_time, update_time, create_by, update_by, deleted, version)
VALUES
    (9200000000000002901, 'AST-202611-0101', '联想 ThinkPad E14', 'ELECTRONIC',
     'i5/16G/512G', '2025-03-15', 6500.00, 9100000000000000013, 9100000000000000004,
     '3 号楼 302', 'IN_STOCK', 36, '项目经理办公笔记本', NOW(), NOW(), 1, 1, 0, 0),
    (9200000000000002902, 'AST-202611-0102', '戴尔显示器 U2723QE', 'ELECTRONIC',
     '27 寸 4K', '2025-06-01', 3200.00, 9100000000000000012, 9100000000000000003,
     '4 号楼 402', 'BORROWED', 36, '人事部外接显示器（借用中）', NOW(), NOW(), 1, 1, 0, 0),
    (9200000000000002903, 'AST-202611-0103', '办公桌 1.6 米', 'FURNITURE',
     '1600×800×750', '2024-09-10', 1800.00, 9100000000000000012, 9100000000000000003,
     '4 号楼 402', 'IN_STOCK', 60, '人事部办公家具', NOW(), NOW(), 1, 1, 0, 0),
    (9200000000000002904, 'AST-202611-0104', '爱普生投影仪 CB-X06', 'ELECTRONIC',
     '3600 流明', '2023-05-20', 4500.00, 9100000000000000012, 1,
     '3 号楼 301', 'REPAIRING', 36, '会议室投影仪送修', NOW(), NOW(), 1, 1, 0, 0),
    (9200000000000002905, 'AST-202611-0105', '电动叉车', 'VEHICLE_TOOL',
     '1.5 吨', '2020-11-01', 26000.00, 9100000000000000013, 1,
     '仓库一层', 'SCRAPPED', 60, '已达报废年限', NOW(), NOW(), 1, 1, 0, 0),
    (9200000000000002906, 'AST-202611-0106', '惠普激光打印机 M405dn', 'ELECTRONIC',
     '黑白激光', '2024-12-01', 2800.00, 9100000000000000011, 9100000000000000001,
     '2 号楼财务室', 'BORROWED', 36, '财务部共享打印机（借出中）', NOW(), NOW(), 1, 1, 0, 0);

-- ---------------------------------------------------------------------
-- 26. 行政资产领用记录（BORROWING×2 其中 1 条逾期未还 / RETURNED×2）
-- ---------------------------------------------------------------------
INSERT IGNORE INTO admin_asset_borrow
    (id, asset_id, borrower_id, borrow_date, expected_return_date, actual_return_date,
     bpm_instance_id, status, remark, create_time, update_time, create_by, update_by,
     deleted, version)
VALUES
    (9200000000000003001, 9200000000000002902, 9100000000000000004, '2026-11-01', '2026-12-01',
     NULL, NULL, 'BORROWING', '外接显示器借用中', NOW(), NOW(), 1, 1, 0, 0),
    (9200000000000003002, 9200000000000002901, 9100000000000000002, '2026-10-10', '2026-11-10',
     '2026-11-08', NULL, 'RETURNED', '按期归还', NOW(), NOW(), 1, 1, 0, 0),
    (9200000000000003003, 9200000000000002906, 9100000000000000003, '2026-09-01', '2026-09-30',
     NULL, NULL, 'BORROWING', '已逾期未还，需催还', NOW(), NOW(), 1, 1, 0, 0),
    (9200000000000003004, 9200000000000002903, 9100000000000000001, '2026-08-01', '2026-08-31',
     '2026-08-30', NULL, 'RETURNED', '正常归还', NOW(), NOW(), 1, 1, 0, 0);
