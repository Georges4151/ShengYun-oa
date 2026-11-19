-- =====================================================================
-- 文件：scripts/seed/seed_finance.sql
-- 用途：为本地开发库 ceams 补充「基础与财务域」演示数据（账号组织、主数据、
--       项目合同、预算收支、资金银行、印章审批、BPM 流程、站内通知）。
-- 幂等：所有业务表均显式指定主键并使用 INSERT IGNORE；主键号段统一为
--       9100000000000000001 起（不同表可复用同一号段，彼此不冲突）。
--       重复执行不会报错、不会产生重复行。
--       唯一的 UPDATE 只作用于本脚本新插入的 biz_receipt 行（回填 bpm_instance_id）。
-- 约束：不修改任何 Flyway 迁移文件；不 UPDATE/DELETE 既有业务数据。
-- 敏感列：sys_user.phone、biz_contact.phone/tax_identification_number/bank_account、
--       biz_subcontractor.phone/tax_identification_number/bank_account、
--       biz_payment_account.account_number、biz_general_ledger.account_number
--       均为 AES-256-GCM 密文（v1: 前缀），密钥与 application.yml 中
--       ceams.sensitive.secret 默认值一致，明文仅用于本地演示。
-- 执行（推荐，Windows/PowerShell 下中文最稳）：
--   docker cp scripts/seed/seed_finance.sql ceams-mysql:/tmp/seed_finance.sql
--   docker exec -i ceams-mysql sh -c "mysql -uroot -proot --default-character-set=utf8mb4 ceams < /tmp/seed_finance.sql"
-- Linux/macOS 也可直接：
--   docker exec -i ceams-mysql mysql -uroot -proot --default-character-set=utf8mb4 ceams < scripts/seed/seed_finance.sql
-- =====================================================================

SET NAMES utf8mb4;

-- ---------------------------------------------------------------------
-- 1. 组织与账号
-- ---------------------------------------------------------------------

INSERT IGNORE INTO sys_dept
    (id, name, parent_id, leader_id, sort_order, create_time, update_time, create_by, update_by, deleted, version)
VALUES
    (9100000000000000011, '财务部',     1, NULL, 10, NOW(), NOW(), 1, 1, 0, 0),
    (9100000000000000012, '人事行政部', 1, NULL, 20, NOW(), NOW(), 1, 1, 0, 0),
    (9100000000000000013, '工程部',     1, NULL, 30, NOW(), NOW(), 1, 1, 0, 0);

-- HR 角色在既有 sys_role 中不存在，这里按增量方式补一个（不修改既有角色）。
INSERT IGNORE INTO sys_role
    (id, code, name, description, status, create_time, update_time, create_by, update_by, deleted, version, data_scope)
VALUES
    (9100000000000000021, 'HR', '人力资源', '人事档案、员工关系与 HR 审批', 'ACTIVE', NOW(), NOW(), 1, 1, 0, 0, 'SELF');

-- password_hash 复制 admin 的 BCrypt 值，登录密码统一为 admin123。
INSERT IGNORE INTO sys_user
    (id, username, password_hash, name, email, phone, status, create_time, update_time,
     create_by, update_by, deleted, version, dept_id, leader_id)
VALUES
    (9100000000000000001, 'finance01', '$2b$10$d9JLCOaKdCGYsTp9lnf1vuDhxdDK25uDLAyN8CtfyYnORLMfCkahC',
     '陈静', 'finance01@ceams.local',
     'v1:DmKy+fXEhJV8Zxa9T6LtmL6ewOVLvhUKliWWZLQx8aa9YM9pC5nT',
     'ACTIVE', NOW(), NOW(), 1, 1, 0, 0, 9100000000000000011, 1),
    (9100000000000000002, 'pm01', '$2b$10$d9JLCOaKdCGYsTp9lnf1vuDhxdDK25uDLAyN8CtfyYnORLMfCkahC',
     '李建国', 'pm01@ceams.local',
     'v1:3pIM044JuKSh7ni1vCuRWPJCHVihzWxj5j+cm9SHXb7CYIwKSRXF',
     'ACTIVE', NOW(), NOW(), 1, 1, 0, 0, 9100000000000000013, 1),
    (9100000000000000003, 'hr01', '$2b$10$d9JLCOaKdCGYsTp9lnf1vuDhxdDK25uDLAyN8CtfyYnORLMfCkahC',
     '赵敏', 'hr01@ceams.local',
     'v1:shD1YdOp5QXdWOvdqIVbpZF75NwRORiHRhnJRV4z3Jmo0A2RSndb',
     'ACTIVE', NOW(), NOW(), 1, 1, 0, 0, 9100000000000000012, 1),
    (9100000000000000004, 'staff01', '$2b$10$d9JLCOaKdCGYsTp9lnf1vuDhxdDK25uDLAyN8CtfyYnORLMfCkahC',
     '王磊', 'staff01@ceams.local',
     'v1:gf62h1Qeutgm5vDvUWeJna9UUB993z775IY2RWU5cGx925X/79Wo',
     'ACTIVE', NOW(), NOW(), 1, 1, 0, 0, 9100000000000000013, 9100000000000000002);

-- 角色绑定：FINANCE=2、项目经理(CEO)=2122694194097471489、HR=9100000000000000021、STAFF=3
INSERT IGNORE INTO sys_user_role (id, user_id, role_id, create_time, create_by) VALUES
    (9100000000000000031, 9100000000000000001, 2,                    NOW(), 1),
    (9100000000000000032, 9100000000000000002, 2122694194097471489,  NOW(), 1),
    (9100000000000000033, 9100000000000000003, 9100000000000000021,  NOW(), 1),
    (9100000000000000034, 9100000000000000004, 3,                    NOW(), 1);

-- ---------------------------------------------------------------------
-- 2. 主数据：分类 / 往来单位 / 材料 / 设备
--    说明：biz_category.type 取 CategoryType 枚举名（EXPENSE/MATERIAL/PROJECT/CONTACT/PAYMENT/OTHER）；
--          收/支方向由 biz_account.type（INCOME/EXPENSE）表达，分类本身不含方向字段。
-- ---------------------------------------------------------------------

INSERT IGNORE INTO biz_category
    (id, name, type, code, parent_id, sort_order, enabled, create_time, update_time,
     create_by, update_by, deleted, version)
VALUES
    (9100000000000000101, '人工费',     'EXPENSE',  'LABOR',          NULL, 10, 1, NOW(), NOW(), 1, 1, 0, 0),
    (9100000000000000102, '材料费',     'MATERIAL', 'MATERIAL',       NULL, 20, 1, NOW(), NOW(), 1, 1, 0, 0),
    (9100000000000000103, '机械使用费', 'EXPENSE',  'MACHINE',        NULL, 30, 1, NOW(), NOW(), 1, 1, 0, 0),
    (9100000000000000104, '管理费用',   'EXPENSE',  'OVERHEAD',       NULL, 40, 1, NOW(), NOW(), 1, 1, 0, 0),
    (9100000000000000105, '税费',       'EXPENSE',  'TAX',            NULL, 50, 1, NOW(), NOW(), 1, 1, 0, 0),
    (9100000000000000106, '工程款收入', 'PROJECT',  'PROJECT_INCOME', NULL, 60, 1, NOW(), NOW(), 1, 1, 0, 0),
    (9100000000000000107, '其他支出',   'OTHER',    'OTHER',          NULL, 70, 1, NOW(), NOW(), 1, 1, 0, 0);

INSERT IGNORE INTO biz_contact
    (id, name, type, contact_person, phone, email, address,
     tax_identification_number, bank_account, create_time, update_time,
     create_by, update_by, deleted, version)
VALUES
    (9100000000000000201, '华东建材有限公司', 'SUPPLIER', '张伟', 'v1:yCUFbYr+0YYD+lktmWp6hWJ3TCxKcVodYCfVTWVUF5YNg3lgrjhz',
     'zhangwei@hd-jc.com', '上海市浦东新区建材路88号',
     'v1:9K/E804tPWHtJJIlT70rToP6B0FvT6rgVS23R5LSXsUClp+iNTKIITYf+Ojq7g==',
     'v1:RR2ldoUnbL4dWN7zgzkqBwZlRuzJ98VXPnoEhac0HO6C8apRHlhalPQvSX3k/PM=', NOW(), NOW(), 1, 1, 0, 0),
    (9100000000000000202, '江南机电设备有限公司', 'SUPPLIER', '李强', 'v1:eWgYCnqLf6fQCBveraetOky3IvhyMcLNbDu4tR/16z7PRC5tJcW1',
     'liqiang@jn-jd.com', '江苏省苏州市工业园区机电大道18号',
     'v1:fP9xtmv1LNxNR6RrGNLU4IpDf21fIZH+IsTuhlhE6CAo0CfgmromVBj8Ph/P0w==',
     'v1:O9Jb8QQQQ8sZEbMR7rrwPPtDWH4k9pqiC1lD5VqTpwAfdZcPtmACLdFiiK3H/Pg=', NOW(), NOW(), 1, 1, 0, 0),
    (9100000000000000203, '恒信物流服务有限公司', 'SUPPLIER', '周敏', 'v1:2EwWePEJQmTmJwnkzw2yomk7+4K15dqDnO8p+IzAAzF63Zdkyotj',
     'zhoumin@hx-logi.com', '上海市青浦区物流园6号',
     'v1:uia2ePoWniNzMjTj09iGvUHjySPjZ4ooSmoHbXq8WkLPgKjOzpWqo0kMPufW4w==',
     'v1:ahWYq+Iiej0flq1cwn66/NEo1LEMDGf3HcAwQXFmtZLmghY/n6MGBOVpQ7pfwQ4=', NOW(), NOW(), 1, 1, 0, 0),
    (9100000000000000204, '华润置地（上海）有限公司', 'CUSTOMER', '王芳', 'v1:bOpU3RQFtE64XM66gaUZiWx5nVb3zwGoUj62kYQUwSIFClEA2Caj',
     'wangfang@crland.com', '上海市静安区南京西路1266号',
     'v1:tznl2EPEQqga47z1urzJnYah9aLaQYFElhOPgci86feWKVwmPx0jxMxEfah4Dw==',
     'v1:d6FWz9xkRDTYNrWBCv5jaBpqG1zgmN20qjEly1Ug2JkRfEaVLwWc6LQz99E1Udk=', NOW(), NOW(), 1, 1, 0, 0),
    (9100000000000000205, '上海城投置业有限公司', 'CUSTOMER', '陈明', 'v1:oxpape8fTmj68Qu7o77r80kdbkIt0ykiYeuFcs4O6isM7290GIlG',
     'chenming@shct.com', '上海市黄浦区中山南路100号',
     'v1:PqlFOss86qCLKjn40xa9SuHreSFGT1gULKS00P3Say3i3PLRw8dOUNEkpWNGLg==',
     'v1:RKOMXKrTFY5OdYuQNTaIMZsrvgKJYhjeEgGOIYCGN7IDyTsnwZpHAk+Eoi2l4T4=', NOW(), NOW(), 1, 1, 0, 0),
    (9100000000000000206, '上海市建筑材料行业协会', 'OTHER', '刘洋', 'v1:VkHTHF6wkHFBKR1VtVW2Bmc7W6q3077uJuZ9FlJsMVNyGCoQ3+x/',
     'liuyang@shjc.org', '上海市徐汇区漕溪北路45号',
     'v1:NW3BuAaYuoUFAqF8etIW4onXZXbWPQIbPeKTbK6OVFVLIJbYB/yfUpQ8Fzu2CQ==',
     'v1:K4s+pdI6uun6djSDscZSotFeapTwPlSlrCYV2zMKEd7iplkEI08GRQAihd3wwYc=', NOW(), NOW(), 1, 1, 0, 0);

INSERT IGNORE INTO biz_material
    (id, material_name, material_code, specification, unit, current_price, supplier_id, category_id,
     create_time, update_time, create_by, update_by, deleted, version)
VALUES
    (9100000000000000301, 'P.O42.5 普通硅酸盐水泥', 'MAT-CEM-001', '42.5级 散装',   '吨',     420.00,  9100000000000000201, 9100000000000000102, NOW(), NOW(), 1, 1, 0, 0),
    (9100000000000000302, 'HRB400 螺纹钢',          'MAT-STL-002', 'Φ20mm',        '吨',     3850.00, 9100000000000000202, 9100000000000000102, NOW(), NOW(), 1, 1, 0, 0),
    (9100000000000000303, '中砂',                   'MAT-SND-003', '细度模数2.6',   '立方米', 128.00,  9100000000000000201, 9100000000000000102, NOW(), NOW(), 1, 1, 0, 0),
    (9100000000000000304, 'C30 商品混凝土',         'MAT-CON-004', '坍落度180±20mm','立方米', 465.00,  9100000000000000201, 9100000000000000102, NOW(), NOW(), 1, 1, 0, 0),
    (9100000000000000305, '阻燃电力电缆',           'MAT-CAB-005', 'YJV-4×25',      '米',      86.50,  9100000000000000202, 9100000000000000102, NOW(), NOW(), 1, 1, 0, 0),
    (9100000000000000306, 'ABS 安全帽',             'MAT-SAF-006', '国标 V型',      '顶',      32.00,  9100000000000000203, 9100000000000000107, NOW(), NOW(), 1, 1, 0, 0);

INSERT IGNORE INTO biz_equipment
    (id, name, type, model, status, purchase_date, purchase_price,
     create_time, update_time, create_by, update_by, deleted, version)
VALUES
    (9100000000000000401, '塔式起重机',   'TOWER_CRANE',    'QTZ63',     'IN_USE',      '2023-04-10 00:00:00', 1280000.00, NOW(), NOW(), 1, 1, 0, 0),
    (9100000000000000402, '履带式挖掘机', 'EXCAVATOR',      'SY215C',    'LEASED',      '2022-09-18 00:00:00', 860000.00,  NOW(), NOW(), 1, 1, 0, 0),
    (9100000000000000403, '混凝土输送泵', 'CONCRETE_PUMP',  'HBT60',     'IN_USE',      '2023-11-02 00:00:00', 420000.00,  NOW(), NOW(), 1, 1, 0, 0),
    (9100000000000000404, '载重货车',     'TRUCK',          '东风天锦',  'IDLE',        '2021-06-25 00:00:00', 260000.00,  NOW(), NOW(), 1, 1, 0, 0);

-- ---------------------------------------------------------------------
-- 3. 项目与合同
-- ---------------------------------------------------------------------

INSERT IGNORE INTO biz_project
    (id, project_name, client_name, project_address, project_type, start_date, end_date,
     contract_amount, budget_amount, actual_amount, status, description,
     manager_id, manager_name, create_time, update_time, create_by, update_by, deleted, version)
VALUES
    (9100000000000001001, '张江科技园二期总承包工程', '华润置地（上海）有限公司', '上海市浦东新区张江路1888号', 'CONSTRUCTION',
     '2026-03-01 00:00:00', '2027-09-30 00:00:00', 68000000.00, 62000000.00, 28500000.00, 'IN_PROGRESS',
     '含土建、机电安装及室外总体工程。', 9100000000000000002, '李建国', NOW(), NOW(), 1, 1, 0, 0),
    (9100000000000001002, '临港新片区人才公寓项目', '上海城投置业有限公司', '上海市浦东新区临港大道600号', 'CONSTRUCTION',
     '2026-12-01 00:00:00', '2028-06-30 00:00:00', 45000000.00, 42000000.00, 0.00, 'PLANNING',
     '规划阶段，待施工许可。', 1, '系统管理员', NOW(), NOW(), 1, 1, 0, 0),
    (9100000000000001003, '虹桥商务区办公楼幕墙工程', '上海城投置业有限公司', '上海市闵行区虹桥商务区申长路99号', 'CURTAIN_WALL',
     '2026-06-15 00:00:00', '2027-03-31 00:00:00', 18500000.00, 16800000.00, 9200000.00, 'IN_PROGRESS',
     '单元式玻璃幕墙，含深化设计与安装。', 9100000000000000002, '李建国', NOW(), NOW(), 1, 1, 0, 0),
    (9100000000000001004, '青浦工业园区厂房改造项目', '上海城投置业有限公司', '上海市青浦区工业园区崧泽大道200号', 'DECORATION',
     '2026-04-01 00:00:00', '2026-12-31 00:00:00', 9800000.00, 9200000.00, 5600000.00, 'PAUSED',
     '因业主产线调整暂停，等待复工通知。', 9100000000000000002, '李建国', NOW(), NOW(), 1, 1, 0, 0),
    (9100000000000001005, '松江新城学校迁建工程', '华润置地（上海）有限公司', '上海市松江区文诚路355号', 'CONSTRUCTION',
     '2025-03-01 00:00:00', '2026-08-31 00:00:00', 15600000.00, 15000000.00, 14600000.00, 'COMPLETED',
     '已竣工验收，进入结算质保期。', 1, '系统管理员', NOW(), NOW(), 1, 1, 0, 0);

INSERT IGNORE INTO biz_equipment_lease
    (id, equipment_id, project_id, lessor_id, lease_start_date, lease_end_date, daily_rent, total_rent,
     payment_status, create_time, update_time, create_by, update_by, deleted, version)
VALUES
    (9100000000000000501, 9100000000000000402, 9100000000000001001, 9100000000000000202,
     '2026-09-01 00:00:00', '2026-12-31 00:00:00', 1800.00, 198000.00, 'PARTIAL', NOW(), NOW(), 1, 1, 0, 0),
    (9100000000000000502, 9100000000000000404, 9100000000000001003, 9100000000000000203,
     '2026-10-01 00:00:00', '2026-11-30 00:00:00', 800.00,  48800.00,  'PAID',    NOW(), NOW(), 1, 1, 0, 0);

INSERT IGNORE INTO biz_subcontractor
    (id, name, contact_person, phone, address, qualification_level, credit_rating, business_scope,
     tax_identification_number, bank_account, description,
     create_time, update_time, create_by, update_by, deleted, version)
VALUES
    (9100000000000001101, '上海远建建筑劳务有限公司', '孙海', 'v1:Xox+syjF2X8OCWu7aUotAmZXOvbCxaMw2uoLG5gso2IGIdV3jxTU',
     '上海市浦东新区川沙路500号', '施工劳务不分等级', 'A',
     '建筑劳务分包、模板脚手架工程。',
     'v1:V94rLrmk2l5MpKN/IPNLprcy98LweU5ssga7V/bs+blGGI8gUafek1CR/Wg0ow==',
     'v1:gUl/uKASA20djpi7C8SZLDWNtkG9/4Q5Ju8BymMbutOQcQQ60Hzw8XKtVGgnMu0=', '长期劳务合作单位。',
     NOW(), NOW(), 1, 1, 0, 0),
    (9100000000000001102, '江苏恒通钢结构工程有限公司', '吴强', 'v1:JcSQDsRpHUAp4n9RF2Cg0kAxWtpkM2QVuGe5K2jtdmKtscEZwRoT',
     '江苏省南通市通州区钢构大道66号', '钢结构工程专业承包二级', 'A',
     '钢结构制作安装、幕墙工程。',
     'v1:79priXThDqW7PpLmEm8osj0t7CzVMJxzNVc29YpLQH+s+t1qjUkPS+URW++nyw==',
     'v1:sw84D4Tv0bcpnccIjf9ul19p/RSylPPn0PoojASfTUeuphNVkhIawHaWkSDf5KM=', '幕墙专业分包单位。',
     NOW(), NOW(), 1, 1, 0, 0),
    (9100000000000001103, '浙江中安消防工程有限公司', '郑涛', 'v1:IVgcVU4XnjF7CbjGuxQmL/IKVlE/EWHh/yF2xfvKGLUJ5KnIhFA9',
     '浙江省杭州市拱墅区消防路12号', '消防设施工程专业承包一级', 'B',
     '消防设施施工、检测与维保。',
     'v1:WbbSqwzsdfVAvsxYnVl3Nz4B6RKmf4qhp/c2NpXk7uaqeHifBFjfrRcKoip2Eg==',
     'v1:M1kmvRl7BILrxis+A8f+LQ4hd5Q+BD+QISapBO2pr6Wod4qYHIKJ/2e7c9Nrams=', '消防分包单位。',
     NOW(), NOW(), 1, 1, 0, 0);

INSERT IGNORE INTO biz_subcontractor_contract
    (id, subcontractor_id, project_id, contract_name, contract_number, contract_amount,
     start_date, end_date, status, description, file_path, file_name, paid_amount,
     create_time, update_time, create_by, update_by, deleted, version)
VALUES
    (9100000000000001201, 9100000000000001101, 9100000000000001001, '张江科技园二期劳务分包合同', 'FB-2026-ZJ-001', 12800000.00,
     '2026-03-15 00:00:00', '2027-06-30 00:00:00', 'ACTIVE', '主体结构及砌筑抹灰劳务分包。', '/files/contracts/FB-2026-ZJ-001.pdf', 'FB-2026-ZJ-001.pdf', 4800000.00,
     NOW(), NOW(), 1, 1, 0, 0),
    (9100000000000001202, 9100000000000001102, 9100000000000001003, '虹桥商务区幕墙专业分包合同', 'FB-2026-HQ-002', 8600000.00,
     '2026-07-01 00:00:00', '2027-02-28 00:00:00', 'ACTIVE', '玻璃幕墙深化设计、加工与安装。', '/files/contracts/FB-2026-HQ-002.pdf', 'FB-2026-HQ-002.pdf', 3220000.00,
     NOW(), NOW(), 1, 1, 0, 0),
    (9100000000000001203, 9100000000000001103, 9100000000000001004, '青浦厂房消防改造分包合同', 'FB-2026-QP-003', 1650000.00,
     '2026-04-10 00:00:00', '2026-10-31 00:00:00', 'COMPLETED', '消防喷淋、报警系统改造。', '/files/contracts/FB-2026-QP-003.pdf', 'FB-2026-QP-003.pdf', 1650000.00,
     NOW(), NOW(), 1, 1, 0, 0);

INSERT IGNORE INTO biz_progress_payment
    (id, project_id, payment_period, application_amount, approved_amount, payment_amount,
     application_date, approval_date, payment_date, status, application_by, approved_by,
     create_time, update_time, create_by, update_by, deleted, version)
VALUES
    (9100000000000001301, 9100000000000001001, '第1期', 1850000.00, 1850000.00, 1850000.00,
     '2026-09-25 00:00:00', '2026-10-05 00:00:00', '2026-10-08 00:00:00', 'APPROVED',
     9100000000000000002, 9100000000000000001, NOW(), NOW(), 1, 1, 0, 0),
    (9100000000000001302, 9100000000000001001, '第2期', 1620000.00, NULL, NULL,
     '2026-11-14 00:00:00', NULL, NULL, 'PENDING',
     9100000000000000002, NULL, NOW(), NOW(), 1, 1, 0, 0),
    (9100000000000001303, 9100000000000001003, '第1期', 980000.00, NULL, NULL,
     '2026-11-16 00:00:00', NULL, NULL, 'DRAFT',
     9100000000000000002, NULL, NOW(), NOW(), 1, 1, 0, 0),
    (9100000000000001304, 9100000000000001005, '第1期', 1200000.00, NULL, NULL,
     '2026-09-10 00:00:00', '2026-09-15 00:00:00', NULL, 'REJECTED',
     9100000000000000002, 9100000000000000001, NOW(), NOW(), 1, 1, 0, 0);

INSERT IGNORE INTO biz_settlement_proof
    (id, contract_id, subcontractor_id, proof_name, proof_number, proof_date, settlement_amount,
     file_path, file_name, description, create_time, update_time, create_by, update_by, deleted, version)
VALUES
    (9100000000000001401, 9100000000000001201, 9100000000000001101, '张江二期劳务分包结算凭证', 'JS-2026-001',
     '2026-10-20 00:00:00', 2860000.00, '/files/settlement/JS-2026-001.pdf', 'JS-2026-001.pdf',
     '第1期劳务工程量结算。', NOW(), NOW(), 1, 1, 0, 0),
    (9100000000000001402, 9100000000000001202, 9100000000000001102, '虹桥幕墙专业分包结算凭证', 'JS-2026-002',
     '2026-11-05 00:00:00', 1950000.00, '/files/settlement/JS-2026-002.pdf', 'JS-2026-002.pdf',
     '幕墙首批次安装结算。', NOW(), NOW(), 1, 1, 0, 0);

-- ---------------------------------------------------------------------
-- 4. 预算与票据
--    说明：biz_budget 无 category_id 列，类别与年度/月度维度通过 budget_name +
--          起止日期表达；biz_receipt 类型取 ReceiptType 枚举名。
-- ---------------------------------------------------------------------

INSERT IGNORE INTO biz_budget
    (id, budget_name, amount, project_id, contract_id, start_date, end_date, status,
     used_amount, remaining_amount, create_time, update_time, create_by, update_by, deleted, version)
VALUES
    (9100000000000001501, '张江二期-2026年度人工费预算',       8000000.00,  9100000000000001001, 9100000000000001201, '2026-01-01 00:00:00', '2026-12-31 00:00:00', 'ACTIVE', 3200000.00, 4800000.00, NOW(), NOW(), 1, 1, 0, 0),
    (9100000000000001502, '张江二期-2026年度材料费预算',       12000000.00, 9100000000000001001, NULL,                '2026-01-01 00:00:00', '2026-12-31 00:00:00', 'ACTIVE', 5500000.00, 6500000.00, NOW(), NOW(), 1, 1, 0, 0),
    (9100000000000001503, '张江二期-2026年12月管理费月度预算', 350000.00,   9100000000000001001, NULL,                '2026-12-01 00:00:00', '2026-12-31 00:00:00', 'ACTIVE', 120000.00,  230000.00,  NOW(), NOW(), 1, 1, 0, 0),
    (9100000000000001504, '临港人才公寓-前期设计费预算',       900000.00,   9100000000000001002, NULL,                '2026-11-01 00:00:00', '2027-03-31 00:00:00', 'DRAFT',  0.00,       900000.00,  NOW(), NOW(), 1, 1, 0, 0),
    (9100000000000001505, '虹桥幕墙-材料采购预算',             6500000.00,  9100000000000001003, 9100000000000001202, '2026-06-01 00:00:00', '2027-02-28 00:00:00', 'ACTIVE', 2800000.00, 3700000.00, NOW(), NOW(), 1, 1, 0, 0),
    (9100000000000001506, '虹桥幕墙-2026年第四季度机械使用费预算', 780000.00, 9100000000000001003, NULL,               '2026-10-01 00:00:00', '2026-12-31 00:00:00', 'ACTIVE', 310000.00,  470000.00,  NOW(), NOW(), 1, 1, 0, 0),
    (9100000000000001507, '青浦厂房改造-项目总预算',           4200000.00,  9100000000000001004, 9100000000000001203, '2026-04-01 00:00:00', '2026-12-31 00:00:00', 'ACTIVE', 3900000.00, 300000.00,  NOW(), NOW(), 1, 1, 0, 0),
    (9100000000000001508, '松江学校迁建-项目结算预算',         15000000.00, 9100000000000001005, NULL,                '2025-03-01 00:00:00', '2026-08-31 00:00:00', 'CLOSED', 14600000.00, 400000.00, NOW(), NOW(), 1, 1, 0, 0);

INSERT IGNORE INTO biz_receipt
    (id, receipt_number, user_id, reporter_id, project_id, reporter_name, receipt_date, amount, type,
     description, file_path, verification_status, verified_by, verified_at, payment_status, paid_amount,
     paid_at, paid_by, payment_method, current_approval_level, approval_status,
     create_time, update_time, create_by, update_by, deleted, version)
VALUES
    (9100000000000002001, 'RCPT-2026-0001', 9100000000000000004, 9100000000000000004, 9100000000000001001, '王磊',
     '2026-11-05 00:00:00', 3260.50, 'TRANSPORTATION', '张江二期现场差旅交通费', '/files/receipts/RCPT-2026-0001.pdf',
     'PENDING', NULL, NULL, 'UNPAID', 0.00, NULL, NULL, NULL, 1, 'PENDING', NOW(), NOW(), 1, 1, 0, 0),
    (9100000000000002002, 'RCPT-2026-0002', 9100000000000000004, 9100000000000000004, 9100000000000001001, '王磊',
     '2026-11-06 00:00:00', 4880.00, 'ACCOMMODATION', '项目驻场人员住宿费', '/files/receipts/RCPT-2026-0002.pdf',
     'VERIFIED', 9100000000000000001, '2026-11-08 10:20:00', 'UNPAID', 0.00, NULL, NULL, NULL, 2, 'APPROVED', NOW(), NOW(), 1, 1, 0, 0),
    (9100000000000002003, 'RCPT-2026-0003', 9100000000000000002, 9100000000000000002, 9100000000000001003, '李建国',
     '2026-11-09 00:00:00', 2150.00, 'MEAL', '幕墙项目业主沟通业务餐费', '/files/receipts/RCPT-2026-0003.pdf',
     'PENDING', NULL, NULL, 'UNPAID', 0.00, NULL, NULL, NULL, 1, 'PENDING', NOW(), NOW(), 1, 1, 0, 0),
    (9100000000000002004, 'RCPT-2026-0004', 9100000000000000003, 9100000000000000003, NULL, '赵敏',
     '2026-11-07 00:00:00', 1680.00, 'OFFICE', '人事行政部办公用品采购', '/files/receipts/RCPT-2026-0004.pdf',
     'VERIFIED', 9100000000000000001, '2026-11-09 14:05:00', 'PARTIAL', 1000.00, '2026-11-09 15:00:00', 9100000000000000001, 'CASH', 1, 'APPROVED', NOW(), NOW(), 1, 1, 0, 0),
    (9100000000000002005, 'RCPT-2026-0005', 9100000000000000004, 9100000000000000004, 9100000000000001004, '王磊',
     '2026-11-03 00:00:00', 560.00, 'TRANSPORTATION', '青浦厂房现场运输费', '/files/receipts/RCPT-2026-0005.pdf',
     'REJECTED', 9100000000000000001, '2026-11-04 09:30:00', 'UNPAID', 0.00, NULL, NULL, NULL, 1, 'REJECTED', NOW(), NOW(), 1, 1, 0, 0),
    (9100000000000002006, 'RCPT-2026-0006', 9100000000000000004, 9100000000000000004, 9100000000000001001, '王磊',
     '2026-11-05 00:00:00', 7200.00, 'OTHER', '张江二期安全防护物资采购', '/files/receipts/RCPT-2026-0006.pdf',
     'VERIFIED', 9100000000000000001, '2026-11-06 11:10:00', 'PAID', 7200.00, '2026-11-06 15:30:00', 9100000000000000001, 'BANK_TRANSFER', 2, 'APPROVED', NOW(), NOW(), 1, 1, 0, 0);

INSERT IGNORE INTO biz_receipt_verification
    (id, receipt_id, payment_account_id, amount, payment_method, verification_time, verified_by, remark,
     create_time, update_time, create_by, update_by, deleted, version)
VALUES
    (9100000000000002101, 9100000000000002002, 9100000000000003101, 4880.00, 'BANK_TRANSFER', '2026-11-08 10:20:00',
     9100000000000000001, '住宿发票与行程单一致，核验通过。', NOW(), NOW(), 1, 1, 0, 0),
    (9100000000000002102, 9100000000000002004, 9100000000000003103, 1000.00, 'CASH', '2026-11-09 14:05:00',
     9100000000000000001, '办公用品清单齐全，现金支付部分先予核验。', NOW(), NOW(), 1, 1, 0, 0),
    (9100000000000002103, 9100000000000002006, 9100000000000003102, 7200.00, 'BANK_TRANSFER', '2026-11-06 11:10:00',
     9100000000000000001, '安全物资发票与入库单一致，核验通过。', NOW(), NOW(), 1, 1, 0, 0);

-- ---------------------------------------------------------------------
-- 5. 资金账户、流水、银行对账、总账、成本归集
-- ---------------------------------------------------------------------

INSERT IGNORE INTO biz_payment_account
    (id, name, type, balance, account_number, description,
     create_time, update_time, create_by, update_by, deleted, version)
VALUES
    (9100000000000003101, '公司基本户', 'BASIC',
     3862500.00,
     'v1:GzdUq/XvfN/tYCIb9jzbkMzxfGrTBVQiA7tmkfVNcz5KkE/9UrFus7t5vgrhs2o=',
     '中国银行上海市张江支行，日常经营收支主账户。', NOW(), NOW(), 1, 1, 0, 0),
    (9100000000000003102, '公司一般户', 'GENERAL',
     1829100.00,
     'v1:X0pVTWRx97nOkpFgEjG6h4yYDwKms9o2vcV9vLZuP1PZyu67+ZUYU6x2QekDH8Q=',
     '工商银行上海市分行，项目专项资金账户。', NOW(), NOW(), 1, 1, 0, 0),
    (9100000000000003103, '备用现金账户', 'CASH',
     28600.00, NULL, '财务部现金备用金。', NOW(), NOW(), 1, 1, 0, 0);

INSERT IGNORE INTO biz_payment_transaction
    (id, account_id, receipt_id, project_id, amount, type, transaction_date, description,
     create_time, update_time, create_by, update_by, deleted, version)
VALUES
    (9100000000000003201, 9100000000000003101, NULL, 9100000000000001001, 1850000.00, 'INCOME',  '2026-10-08 09:20:00', '收到张江二期第1期工程款',           NOW(), NOW(), 1, 1, 0, 0),
    (9100000000000003202, 9100000000000003101, NULL, 9100000000000001001, 486000.00,  'EXPENSE', '2026-10-12 14:10:00', '支付华东建材商品混凝土及水泥款',   NOW(), NOW(), 1, 1, 0, 0),
    (9100000000000003203, 9100000000000003102, NULL, 9100000000000001003, 322000.00,  'EXPENSE', '2026-10-18 10:30:00', '支付江苏恒通幕墙分包进度款',       NOW(), NOW(), 1, 1, 0, 0),
    (9100000000000003204, 9100000000000003101, NULL, 9100000000000001003, 980000.00,  'INCOME',  '2026-10-22 16:00:00', '收到虹桥幕墙工程预付款',           NOW(), NOW(), 1, 1, 0, 0),
    (9100000000000003205, 9100000000000003102, 9100000000000002006, 9100000000000001001, 7200.00, 'EXPENSE', '2026-11-06 15:30:00', '安全防护物资报销付款',             NOW(), NOW(), 1, 1, 0, 0),
    (9100000000000003206, 9100000000000003103, 9100000000000002004, NULL, 1000.00,  'EXPENSE', '2026-11-09 15:00:00', '办公用品现金报销',                 NOW(), NOW(), 1, 1, 0, 0),
    (9100000000000003207, 9100000000000003101, NULL, 9100000000000001004, 125000.00, 'EXPENSE', '2026-11-08 11:45:00', '青浦厂房改造机械租赁费',           NOW(), NOW(), 1, 1, 0, 0),
    (9100000000000003208, 9100000000000003102, NULL, 9100000000000001005, 1200000.00,'INCOME',  '2026-11-10 09:05:00', '松江学校迁建工程结算尾款',         NOW(), NOW(), 1, 1, 0, 0),
    (9100000000000003209, 9100000000000003101, NULL, 9100000000000001001, 86500.00,  'EXPENSE', '2026-11-12 17:20:00', '11月现场管理费',                   NOW(), NOW(), 1, 1, 0, 0),
    (9100000000000003210, 9100000000000003102, NULL, 9100000000000001003, 48800.00,  'EXPENSE', '2026-11-14 13:15:00', '幕墙材料检测费',                   NOW(), NOW(), 1, 1, 0, 0);

INSERT IGNORE INTO biz_bank_statement
    (id, account_id, transaction_date, amount, type, description, counterparty, bank_reference, balance, status,
     create_time, update_time, create_by, update_by, deleted, version)
VALUES
    (9100000000000003301, 9100000000000003101, '2026-10-08 09:20:00', 1850000.00, 'INCOME',  '工程款到账',       '华润置地（上海）有限公司', 'BK20261008001', 3650000.00, 'MATCHED',      NOW(), NOW(), 1, 1, 0, 0),
    (9100000000000003302, 9100000000000003101, '2026-10-12 14:10:00', 486000.00,  'EXPENSE', '材料款支付',       '华东建材有限公司',         'BK20261012002', 3164000.00, 'MATCHED',      NOW(), NOW(), 1, 1, 0, 0),
    (9100000000000003303, 9100000000000003101, '2026-10-22 16:00:00', 980000.00,  'INCOME',  '工程预付款到账',   '上海城投置业有限公司',     'BK20261022003', 4144000.00, 'MATCHED',      NOW(), NOW(), 1, 1, 0, 0),
    (9100000000000003304, 9100000000000003101, '2026-11-09 11:45:00', 125000.00,  'EXPENSE', '机械租赁费支付',   '江南机电设备有限公司',     'BK20261109004', 4019000.00, 'RECONCILED',   NOW(), NOW(), 1, 1, 0, 0),
    (9100000000000003305, 9100000000000003101, '2026-11-12 17:20:00', 86500.00,   'EXPENSE', '现场管理费支付',   '恒信物流服务有限公司',     'BK20261112005', 3932500.00, 'MATCHED',      NOW(), NOW(), 1, 1, 0, 0),
    (9100000000000003306, 9100000000000003102, '2026-10-18 10:30:00', 322000.00,  'EXPENSE', '分包进度款支付',   '江苏恒通钢结构工程有限公司','BK20261018006', 678000.00,  'MATCHED',      NOW(), NOW(), 1, 1, 0, 0),
    (9100000000000003307, 9100000000000003102, '2026-11-10 09:05:00', 1200000.00, 'INCOME',  '结算尾款到账',     '上海城投置业有限公司',     'BK20261110007', 1878000.00, 'MATCHED',      NOW(), NOW(), 1, 1, 0, 0),
    (9100000000000003308, 9100000000000003102, '2026-11-14 13:15:00', 48900.00,   'EXPENSE', '检测费支付',       '上海建科检测技术有限公司', 'BK20261114008', 1829100.00, 'RECONCILED',   NOW(), NOW(), 1, 1, 0, 0);

INSERT IGNORE INTO biz_reconciliation_result
    (id, account_id, statement_id, transaction_id, status, discrepancy_type,
     bank_amount, system_amount, bank_date, system_date, bank_description, system_description,
     statement_reference, transaction_reference, resolution_status, resolution_notes, reconciliation_date,
     create_time, update_time, create_by, update_by, deleted, version)
VALUES
    (9100000000000003401, 9100000000000003101, 9100000000000003301, 9100000000000003201, 'MATCHED', NULL,
     1850000.00, 1850000.00, '2026-10-08 09:20:00', '2026-10-08 09:20:00', '工程款到账', '收到张江二期第1期工程款',
     'BK20261008001', 'TXN-3201', 'RESOLVED', '金额、日期、摘要一致，自动匹配。', '2026-11-01 10:00:00', NOW(), NOW(), 1, 1, 0, 0),
    (9100000000000003402, 9100000000000003101, 9100000000000003302, 9100000000000003202, 'MATCHED', NULL,
     486000.00, 486000.00, '2026-10-12 14:10:00', '2026-10-12 14:10:00', '材料款支付', '支付华东建材商品混凝土及水泥款',
     'BK20261012002', 'TXN-3202', 'RESOLVED', '金额、日期、摘要一致，自动匹配。', '2026-11-01 10:00:00', NOW(), NOW(), 1, 1, 0, 0),
    (9100000000000003403, 9100000000000003101, 9100000000000003304, 9100000000000003207, 'UNMATCHED', 'DATE_MISMATCH',
     125000.00, 125000.00, '2026-11-09 11:45:00', '2026-11-08 11:45:00', '机械租赁费支付', '青浦厂房改造机械租赁费',
     'BK20261109004', 'TXN-3207', 'UNRESOLVED', '银行记账日期与系统差1天，待财务确认。', '2026-11-15 09:30:00', NOW(), NOW(), 1, 1, 0, 0),
    (9100000000000003404, 9100000000000003102, 9100000000000003308, 9100000000000003210, 'PARTIAL_MATCHED', 'AMOUNT_MISMATCH',
     48900.00, 48800.00, '2026-11-14 13:15:00', '2026-11-14 13:15:00', '检测费支付', '幕墙材料检测费',
     'BK20261114008', 'TXN-3210', 'UNRESOLVED', '银行金额比系统多100元，疑似手续费，待补录。', '2026-11-15 09:40:00', NOW(), NOW(), 1, 1, 0, 0);

INSERT IGNORE INTO biz_account
    (id, project_id, transaction_date, amount, type, receipt_id, description, status,
     payment_account_id, contract_id, payment_type, allocation_status, category_id, contact_id,
     material_id, subcontractor_id, create_time, update_time, create_by, update_by, deleted, version)
VALUES
    (9100000000000003501, 9100000000000001001, '2026-10-08 09:20:00', 1850000.00, 'INCOME', NULL, '张江二期第1期工程款收入',
     'CONFIRMED', 9100000000000003101, NULL, 'BANK_TRANSFER', 'UNALLOCATED', 9100000000000000106, 9100000000000000204, NULL, NULL, NOW(), NOW(), 1, 1, 0, 0),
    (9100000000000003502, 9100000000000001001, '2026-10-12 14:10:00', 486000.00, 'EXPENSE', NULL, '华东建材材料采购支出',
     'CONFIRMED', 9100000000000003101, NULL, 'BANK_TRANSFER', 'ALLOCATED', 9100000000000000102, 9100000000000000201, 9100000000000000304, NULL, NOW(), NOW(), 1, 1, 0, 0),
    (9100000000000003503, 9100000000000001003, '2026-10-18 10:30:00', 322000.00, 'EXPENSE', NULL, '江苏恒通幕墙分包进度款',
     'CONFIRMED', 9100000000000003102, 9100000000000001202, 'BANK_TRANSFER', 'ALLOCATED', 9100000000000000101, NULL, NULL, 9100000000000001102, NOW(), NOW(), 1, 1, 0, 0),
    (9100000000000003504, 9100000000000001004, '2026-11-08 11:45:00', 125000.00, 'EXPENSE', NULL, '青浦厂房改造机械租赁支出',
     'CONFIRMED', 9100000000000003101, NULL, 'BANK_TRANSFER', 'PARTIAL', 9100000000000000103, 9100000000000000202, NULL, NULL, NOW(), NOW(), 1, 1, 0, 0),
    (9100000000000003505, 9100000000000001001, '2026-11-12 17:20:00', 86500.00, 'EXPENSE', NULL, '张江二期11月现场管理费',
     'CONFIRMED', 9100000000000003101, NULL, 'BANK_TRANSFER', 'ALLOCATED', 9100000000000000104, 9100000000000000203, NULL, NULL, NOW(), NOW(), 1, 1, 0, 0),
    (9100000000000003506, 9100000000000001005, '2026-08-20 10:00:00', 14600000.00, 'EXPENSE', NULL, '松江学校迁建工程结算成本',
     'CONFIRMED', 9100000000000003102, NULL, 'BANK_TRANSFER', 'ALLOCATED', 9100000000000000102, 9100000000000000201, NULL, NULL, NOW(), NOW(), 1, 1, 0, 0);

INSERT IGNORE INTO biz_cost_allocation
    (id, account_record_id, target_type, target_id, allocation_amount, allocation_ratio,
     create_time, update_time, create_by, update_by, deleted, version)
VALUES
    (9100000000000003601, 9100000000000003501, 'PROJECT', 9100000000000001001, 1850000.00,  1.0000, NOW(), NOW(), 1, 1, 0, 0),
    (9100000000000003602, 9100000000000003502, 'PROJECT', 9100000000000001001, 486000.00,   1.0000, NOW(), NOW(), 1, 1, 0, 0),
    (9100000000000003603, 9100000000000003503, 'PROJECT', 9100000000000001003, 322000.00,   1.0000, NOW(), NOW(), 1, 1, 0, 0),
    (9100000000000003604, 9100000000000003504, 'PROJECT', 9100000000000001004, 100000.00,   0.8000, NOW(), NOW(), 1, 1, 0, 0),
    (9100000000000003605, 9100000000000003505, 'PROJECT', 9100000000000001001, 86500.00,    1.0000, NOW(), NOW(), 1, 1, 0, 0),
    (9100000000000003606, 9100000000000003506, 'PROJECT', 9100000000000001005, 14600000.00, 1.0000, NOW(), NOW(), 1, 1, 0, 0);

INSERT IGNORE INTO biz_general_ledger
    (id, account_name, account_number, bank_name, balance, account_type, summary,
     transaction_time, transaction_type, amount, status,
     create_time, update_time, create_by, update_by, deleted, version)
VALUES
    (9100000000000003701, '公司基本户', 'v1:GzdUq/XvfN/tYCIb9jzbkMzxfGrTBVQiA7tmkfVNcz5KkE/9UrFus7t5vgrhs2o=',
     '中国银行上海市张江支行', 3650000.00, 'BASIC', '收到张江二期第1期工程款', '2026-10-08 09:20:00', 'INCOME', 1850000.00, 'CONFIRMED', NOW(), NOW(), 1, 1, 0, 0),
    (9100000000000003702, '公司基本户', 'v1:GzdUq/XvfN/tYCIb9jzbkMzxfGrTBVQiA7tmkfVNcz5KkE/9UrFus7t5vgrhs2o=',
     '中国银行上海市张江支行', 3164000.00, 'BASIC', '支付华东建材材料款', '2026-10-12 14:10:00', 'EXPENSE', 486000.00, 'CONFIRMED', NOW(), NOW(), 1, 1, 0, 0),
    (9100000000000003703, '公司基本户', 'v1:GzdUq/XvfN/tYCIb9jzbkMzxfGrTBVQiA7tmkfVNcz5KkE/9UrFus7t5vgrhs2o=',
     '中国银行上海市张江支行', 3932500.00, 'BASIC', '支付11月现场管理费', '2026-11-12 17:20:00', 'EXPENSE', 86500.00, 'CONFIRMED', NOW(), NOW(), 1, 1, 0, 0),
    (9100000000000003704, '公司一般户', 'v1:X0pVTWRx97nOkpFgEjG6h4yYDwKms9o2vcV9vLZuP1PZyu67+ZUYU6x2QekDH8Q=',
     '工商银行上海市分行', 678000.00, 'GENERAL', '支付江苏恒通幕墙分包进度款', '2026-10-18 10:30:00', 'EXPENSE', 322000.00, 'CONFIRMED', NOW(), NOW(), 1, 1, 0, 0),
    (9100000000000003705, '公司一般户', 'v1:X0pVTWRx97nOkpFgEjG6h4yYDwKms9o2vcV9vLZuP1PZyu67+ZUYU6x2QekDH8Q=',
     '工商银行上海市分行', 1878000.00, 'GENERAL', '收到松江学校迁建工程结算尾款', '2026-11-10 09:05:00', 'INCOME', 1200000.00, 'CONFIRMED', NOW(), NOW(), 1, 1, 0, 0),
    (9100000000000003706, '备用现金账户', NULL, NULL, 28600.00, 'CASH', '办公用品现金报销', '2026-11-09 15:00:00', 'EXPENSE', 1000.00, 'CONFIRMED', NOW(), NOW(), 1, 1, 0, 0);

-- ---------------------------------------------------------------------
-- 6. 印章与用印审批
-- ---------------------------------------------------------------------

INSERT IGNORE INTO biz_electronic_seal_application
    (id, seal_id, seal_name, applicant_id, applicant_name, purpose, status, project_id, project_name,
     contract_name, contract_path, stamp_position, approver_id, approver_name, approval_comment,
     apply_time, approval_time, create_time, update_time, create_by, update_by, deleted, version)
VALUES
    (9100000000000004001, 2, '合同专用章', 9100000000000000002, '李建国', '张江科技园二期施工总承包合同用印',
     'PENDING', 9100000000000001001, '张江科技园二期总承包工程', '张江科技园二期施工总承包合同',
     '/files/contracts/ZB-2026-ZJ-001.pdf', '第8页骑缝章', NULL, NULL, NULL,
     '2026-11-15 09:10:00', NULL, NOW(), NOW(), 1, 1, 0, 0),
    (9100000000000004002, 1, '公司公章', 9100000000000000002, '李建国', '虹桥幕墙工程竣工验收资料用印',
     'APPROVED', 9100000000000001003, '虹桥商务区办公楼幕墙工程', '虹桥幕墙工程验收报告',
     '/files/contracts/YS-2026-HQ-001.pdf', '末页公章', 1, '系统管理员', '资料齐全，同意用印。',
     '2026-11-10 10:20:00', '2026-11-11 09:00:00', NOW(), NOW(), 1, 1, 0, 0),
    (9100000000000004003, 3, '财务专用章', 9100000000000000001, '陈静', '张江二期第1期进度款付款申请单用印',
     'REJECTED', 9100000000000001001, '张江科技园二期总承包工程', '进度款付款申请单',
     '/files/contracts/FK-2026-ZJ-001.pdf', '申请单右下角', 1, '系统管理员', '付款资料不齐，退回补正。',
     '2026-11-12 14:00:00', '2026-11-12 16:30:00', NOW(), NOW(), 1, 1, 0, 0),
    (9100000000000004004, 4, '法定代表人章', 9100000000000000003, '赵敏', '新员工劳动合同用印',
     'APPROVED', NULL, NULL, '2026年度劳动合同', '/files/contracts/LD-2026-001.pdf', '合同签署页',
     1, '系统管理员', '同意。', '2026-11-13 11:00:00', '2026-11-13 15:00:00', NOW(), NOW(), 1, 1, 0, 0);

INSERT IGNORE INTO biz_stamp_approval
    (id, stamp_id, seal_id, seal_name, seal_code, seal_permission, project_id, project_name,
     contract_name, contract_path, original_contract_path, stamp_positions,
     applicant_id, applicant_name, approver_id, approver_name, status, reject_reason,
     apply_time, approval_time, create_time, update_time, create_by, update_by, deleted, version)
VALUES
    (9100000000000004101, 9100000000000004001, 2, '合同专用章', 'SEAL_CONTRACT', 'CONTRACT', 9100000000000001001,
     '张江科技园二期总承包工程', '张江科技园二期施工总承包合同', '/files/contracts/ZB-2026-ZJ-001.pdf',
     '/files/contracts/ZB-2026-ZJ-001-origin.pdf', '第8页骑缝章', 9100000000000000002, '李建国',
     NULL, NULL, 'PENDING', NULL, '2026-11-15 09:10:00', NULL, NOW(), NOW(), 1, 1, 0, 0),
    (9100000000000004102, 9100000000000004002, 1, '公司公章', 'SEAL_COMPANY', 'COMPANY', 9100000000000001003,
     '虹桥商务区办公楼幕墙工程', '虹桥幕墙工程验收报告', '/files/contracts/YS-2026-HQ-001.pdf',
     '/files/contracts/YS-2026-HQ-001-origin.pdf', '末页公章', 9100000000000000002, '李建国',
     1, '系统管理员', 'APPROVED', NULL, '2026-11-10 10:20:00', '2026-11-11 09:00:00', NOW(), NOW(), 1, 1, 0, 0),
    (9100000000000004103, 9100000000000004003, 3, '财务专用章', 'SEAL_FINANCE', 'FINANCE', 9100000000000001001,
     '张江科技园二期总承包工程', '进度款付款申请单', '/files/contracts/FK-2026-ZJ-001.pdf',
     '/files/contracts/FK-2026-ZJ-001-origin.pdf', '申请单右下角', 9100000000000000001, '陈静',
     1, '系统管理员', 'REJECTED', '付款资料不齐，退回补正。', '2026-11-12 14:00:00', '2026-11-12 16:30:00',
     NOW(), NOW(), 1, 1, 0, 0);

INSERT IGNORE INTO biz_electronic_seal_usage
    (id, seal_id, seal_name, user_id, user_name, purpose, document_type, document_path, usage_time, ip_address,
     create_time, update_time, create_by, update_by, deleted, version)
VALUES
    (9100000000000004201, 1, '公司公章', 1, '系统管理员', '虹桥幕墙工程验收资料盖章', 'CONTRACT',
     '/files/contracts/YS-2026-HQ-001.pdf', '2026-11-11 09:05:00', '127.0.0.1', NOW(), NOW(), 1, 1, 0, 0),
    (9100000000000004202, 2, '合同专用章', 9100000000000000002, '李建国', '张江二期分包合同盖章', 'CONTRACT',
     '/files/contracts/FB-2026-ZJ-001.pdf', '2026-11-12 10:30:00', '127.0.0.1', NOW(), NOW(), 1, 1, 0, 0),
    (9100000000000004203, 3, '财务专用章', 9100000000000000001, '陈静', '付款申请单盖章', 'PAYMENT',
     '/files/contracts/FK-2026-ZJ-001.pdf', '2026-11-12 14:20:00', '127.0.0.1', NOW(), NOW(), 1, 1, 0, 0);

INSERT IGNORE INTO biz_electronic_seal_stamp
    (id, seal_id, seal_name, project_id, project_name, contract_name, contract_path, stamp_position,
     stamp_image, stamp_time, user_id, user_name, ip_address,
     create_time, update_time, create_by, update_by, deleted, version)
VALUES
    (9100000000000004301, 1, '公司公章', 9100000000000001003, '虹桥商务区办公楼幕墙工程', '虹桥幕墙工程验收报告',
     '/files/contracts/YS-2026-HQ-001.pdf', '末页公章', '/files/stamps/YS-2026-HQ-001-stamped.png',
     '2026-11-11 09:05:00', 1, '系统管理员', '127.0.0.1', NOW(), NOW(), 1, 1, 0, 0),
    (9100000000000004302, 2, '合同专用章', 9100000000000001001, '张江科技园二期总承包工程', '张江二期分包合同',
     '/files/contracts/FB-2026-ZJ-001.pdf', '第8页骑缝章', '/files/stamps/FB-2026-ZJ-001-stamped.png',
     '2026-11-12 10:30:00', 9100000000000000002, '李建国', '127.0.0.1', NOW(), NOW(), 1, 1, 0, 0);

INSERT IGNORE INTO biz_approval_record
    (id, receipt_id, approval_level, approver_role, approver_id, approver_name, status,
     approval_comment, approval_time, create_time, update_time, create_by, update_by, deleted, version)
VALUES
    (9100000000000004401, 9100000000000002001, 1, 'PROJECT_MANAGER', 9100000000000000002, '李建国', 'APPROVED',
     '情况属实，同意上报财务。', '2026-11-05 11:00:00', NOW(), NOW(), 1, 1, 0, 0),
    (9100000000000004402, 9100000000000002001, 2, 'FINANCE', 9100000000000000001, '陈静', 'PENDING',
     NULL, NULL, NOW(), NOW(), 1, 1, 0, 0),
    (9100000000000004403, 9100000000000002002, 2, 'FINANCE', 9100000000000000001, '陈静', 'APPROVED',
     '票据合规，同意报销。', '2026-11-08 10:20:00', NOW(), NOW(), 1, 1, 0, 0),
    (9100000000000004404, 9100000000000002003, 1, 'PROJECT_MANAGER', 9100000000000000002, '李建国', 'PENDING',
     NULL, NULL, NOW(), NOW(), 1, 1, 0, 0),
    (9100000000000004405, 9100000000000002005, 1, 'FINANCE', 9100000000000000001, '陈静', 'REJECTED',
     '运输票据抬头不符，退回重开。', '2026-11-04 09:30:00', NOW(), NOW(), 1, 1, 0, 0),
    (9100000000000004406, 9100000000000002006, 2, 'FINANCE', 9100000000000000001, '陈静', 'APPROVED',
     '安全物资采购票据齐全，同意付款。', '2026-11-06 11:10:00', NOW(), NOW(), 1, 1, 0, 0);

-- ---------------------------------------------------------------------
-- 7. BPM 流程实例（复用既有模板版本）
--    报销类：EXPENSE 模板版本 9101，业务单据 = biz_receipt 9100000000000002001
--    进度款类：CHANGE_REQUEST 模板版本 9102，业务单据 = biz_progress_payment 9100000000000001302
--    用印类：CHANGE_REQUEST 模板版本 9102，业务单据 = biz_electronic_seal_application 9100000000000004001
-- ---------------------------------------------------------------------

INSERT IGNORE INTO bpm_instance
    (id, template_version_id, biz_type, biz_id, initiator_id, status, started_at, ended_at,
     create_time, update_time, create_by, update_by, deleted, version)
VALUES
    (9100000000000005001, 9101, 'EXPENSE',        9100000000000002001, 9100000000000000004, 'RUNNING', '2026-11-05 09:30:00', NULL, NOW(), NOW(), 1, 1, 0, 0),
    (9100000000000005002, 9102, 'CHANGE_REQUEST', 9100000000000001302, 9100000000000000002, 'RUNNING', '2026-11-14 10:00:00', NULL, NOW(), NOW(), 1, 1, 0, 0),
    (9100000000000005003, 9102, 'CHANGE_REQUEST', 9100000000000004001, 9100000000000000002, 'RUNNING', '2026-11-15 09:10:00', NULL, NOW(), NOW(), 1, 1, 0, 0);

INSERT IGNORE INTO bpm_node
    (id, instance_id, node_key, node_type, seq_no, status, arrived_at, completed_at,
     create_time, update_time, create_by, update_by, deleted, version)
VALUES
    (9100000000000005101, 9100000000000005001, 'start',          'START',    1, 'COMPLETED', '2026-11-05 09:30:00', '2026-11-05 09:30:00', NOW(), NOW(), 1, 1, 0, 0),
    (9100000000000005102, 9100000000000005001, 'dept_approve',   'OR_SIGN',  2, 'COMPLETED', '2026-11-05 09:30:00', '2026-11-05 11:00:00', NOW(), NOW(), 1, 1, 0, 0),
    (9100000000000005103, 9100000000000005001, 'finance_verify', 'OR_SIGN',  3, 'ACTIVE',    '2026-11-05 11:00:00', NULL, NOW(), NOW(), 1, 1, 0, 0),
    (9100000000000005104, 9100000000000005001, 'end',            'END',      4, 'PENDING',   NULL, NULL, NOW(), NOW(), 1, 1, 0, 0),
    (9100000000000005111, 9100000000000005002, 'start',          'START',    1, 'COMPLETED', '2026-11-14 10:00:00', '2026-11-14 10:00:00', NOW(), NOW(), 1, 1, 0, 0),
    (9100000000000005112, 9100000000000005002, 'dept_approve',   'OR_SIGN',  2, 'COMPLETED', '2026-11-14 10:00:00', '2026-11-14 15:20:00', NOW(), NOW(), 1, 1, 0, 0),
    (9100000000000005113, 9100000000000005002, 'finance_verify', 'OR_SIGN',  3, 'ACTIVE',    '2026-11-14 15:20:00', NULL, NOW(), NOW(), 1, 1, 0, 0),
    (9100000000000005114, 9100000000000005002, 'end',            'END',      4, 'PENDING',   NULL, NULL, NOW(), NOW(), 1, 1, 0, 0),
    (9100000000000005121, 9100000000000005003, 'start',          'START',    1, 'COMPLETED', '2026-11-15 09:10:00', '2026-11-15 09:10:00', NOW(), NOW(), 1, 1, 0, 0),
    (9100000000000005122, 9100000000000005003, 'dept_approve',   'OR_SIGN',  2, 'COMPLETED', '2026-11-15 09:10:00', '2026-11-15 10:00:00', NOW(), NOW(), 1, 1, 0, 0),
    (9100000000000005123, 9100000000000005003, 'admin_review',   'OR_SIGN',  3, 'ACTIVE',    '2026-11-15 10:00:00', NULL, NOW(), NOW(), 1, 1, 0, 0),
    (9100000000000005124, 9100000000000005003, 'end',            'END',      4, 'PENDING',   NULL, NULL, NOW(), NOW(), 1, 1, 0, 0);

INSERT IGNORE INTO bpm_task
    (id, instance_id, node_id, assignee_id, owner_assignee_id, source_type, task_type, status,
     due_at, reminded_at, create_time, update_time, create_by, update_by, deleted, version)
VALUES
    (9100000000000005201, 9100000000000005001, 9100000000000005102, 9100000000000000002, NULL, 'DEPT_LEADER', 'APPROVE', 'APPROVED',
     '2026-11-06 09:30:00', NULL, NOW(), NOW(), 1, 1, 0, 0),
    (9100000000000005202, 9100000000000005001, 9100000000000005103, 9100000000000000001, NULL, 'ROLE', 'APPROVE', 'PENDING',
     '2026-11-20 09:30:00', NULL, NOW(), NOW(), 1, 1, 0, 0),
    (9100000000000005211, 9100000000000005002, 9100000000000005112, 1, NULL, 'USER', 'APPROVE', 'APPROVED',
     '2026-11-15 10:00:00', NULL, NOW(), NOW(), 1, 1, 0, 0),
    (9100000000000005212, 9100000000000005002, 9100000000000005113, 9100000000000000001, NULL, 'ROLE', 'APPROVE', 'PENDING',
     '2026-11-21 10:00:00', NULL, NOW(), NOW(), 1, 1, 0, 0),
    (9100000000000005221, 9100000000000005003, 9100000000000005122, 1, NULL, 'USER', 'APPROVE', 'APPROVED',
     '2026-11-16 09:10:00', NULL, NOW(), NOW(), 1, 1, 0, 0),
    (9100000000000005222, 9100000000000005003, 9100000000000005123, 9100000000000000003, NULL, 'ROLE', 'APPROVE', 'PENDING',
     '2026-11-22 09:10:00', NULL, NOW(), NOW(), 1, 1, 0, 0);

INSERT IGNORE INTO bpm_variable
    (id, instance_id, var_key, var_value, create_time, update_time, create_by, update_by, deleted, version)
VALUES
    (9100000000000005501, 9100000000000005001, 'biz_id',         '9100000000000002001', NOW(), NOW(), 1, 1, 0, 0),
    (9100000000000005502, 9100000000000005001, 'biz_type',       'EXPENSE',             NOW(), NOW(), 1, 1, 0, 0),
    (9100000000000005503, 9100000000000005001, 'template_code',  'EXPENSE',             NOW(), NOW(), 1, 1, 0, 0),
    (9100000000000005504, 9100000000000005001, 'initiator_id',   '9100000000000000004', NOW(), NOW(), 1, 1, 0, 0),
    (9100000000000005505, 9100000000000005001, 'amount',         '3260.50',             NOW(), NOW(), 1, 1, 0, 0),
    (9100000000000005511, 9100000000000005002, 'biz_id',         '9100000000000001302', NOW(), NOW(), 1, 1, 0, 0),
    (9100000000000005512, 9100000000000005002, 'biz_type',       'CHANGE_REQUEST',      NOW(), NOW(), 1, 1, 0, 0),
    (9100000000000005513, 9100000000000005002, 'template_code',  'CHANGE_REQUEST',      NOW(), NOW(), 1, 1, 0, 0),
    (9100000000000005514, 9100000000000005002, 'initiator_id',   '9100000000000000002', NOW(), NOW(), 1, 1, 0, 0),
    (9100000000000005515, 9100000000000005002, 'amount',         '1620000.00',          NOW(), NOW(), 1, 1, 0, 0),
    (9100000000000005521, 9100000000000005003, 'biz_id',         '9100000000000004001', NOW(), NOW(), 1, 1, 0, 0),
    (9100000000000005522, 9100000000000005003, 'biz_type',       'CHANGE_REQUEST',      NOW(), NOW(), 1, 1, 0, 0),
    (9100000000000005523, 9100000000000005003, 'template_code',  'CHANGE_REQUEST',      NOW(), NOW(), 1, 1, 0, 0),
    (9100000000000005524, 9100000000000005003, 'initiator_id',   '9100000000000000002', NOW(), NOW(), 1, 1, 0, 0),
    (9100000000000005525, 9100000000000005003, 'seal_id',        '2',                   NOW(), NOW(), 1, 1, 0, 0);

INSERT IGNORE INTO bpm_cc_record
    (id, instance_id, node_id, receiver_id, read_status, create_time, update_time, create_by, update_by, deleted, version)
VALUES
    (9100000000000005301, 9100000000000005001, 9100000000000005103, 1, 'UNREAD', NOW(), NOW(), 1, 1, 0, 0),
    (9100000000000005302, 9100000000000005003, 9100000000000005123, 1, 'UNREAD', NOW(), NOW(), 1, 1, 0, 0);

INSERT IGNORE INTO bpm_assignee_snapshot
    (id, node_id, assignee_id, source_type, resolved_at, create_time, update_time, create_by, update_by, deleted, version)
VALUES
    (9100000000000005401, 9100000000000005102, 9100000000000000002, 'DEPT_LEADER', '2026-11-05 09:30:00', NOW(), NOW(), 1, 1, 0, 0),
    (9100000000000005402, 9100000000000005103, 9100000000000000001, 'ROLE',        '2026-11-05 11:00:00', NOW(), NOW(), 1, 1, 0, 0),
    (9100000000000005403, 9100000000000005112, 1,                    'USER',        '2026-11-14 10:00:00', NOW(), NOW(), 1, 1, 0, 0),
    (9100000000000005404, 9100000000000005113, 9100000000000000001, 'ROLE',        '2026-11-14 15:20:00', NOW(), NOW(), 1, 1, 0, 0),
    (9100000000000005405, 9100000000000005122, 1,                    'USER',        '2026-11-15 09:10:00', NOW(), NOW(), 1, 1, 0, 0),
    (9100000000000005406, 9100000000000005123, 9100000000000000003, 'ROLE',        '2026-11-15 10:00:00', NOW(), NOW(), 1, 1, 0, 0);

-- 业务表回填流程实例 ID（只更新本脚本新增的票据行，且仅在为空时回填）。
UPDATE biz_receipt
SET bpm_instance_id = 9100000000000005001,
    update_time = NOW(),
    update_by = 1
WHERE id = 9100000000000002001
  AND bpm_instance_id IS NULL;

-- ---------------------------------------------------------------------
-- 8. 站内通知
-- ---------------------------------------------------------------------

INSERT IGNORE INTO biz_notification
    (id, title, content_type, content, link_url, priority, notification_type,
     sender_id, sender_name, valid_start_time, valid_end_time, status, publish_time,
     create_time, update_time, create_by, update_by, deleted, version)
VALUES
    (9100000000000006001, '报销单待财务核验：RCPT-2026-0001', 'LINK',
     '王磊提交的张江二期现场差旅交通费报销单（3260.50元）已由项目经理审批，等待财务核验。',
     '/expenses?approvalStatus=PENDING', 'HIGH', 'APPROVAL', 1, '系统管理员',
     '2026-11-05 00:00:00', '2026-12-31 23:59:59', 'PUBLISHED', NOW(), NOW(), NOW(), 1, 1, 0, 0),
    (9100000000000006002, '进度款第2期待审批：张江科技园二期', 'LINK',
     '张江科技园二期第2期进度款申请 1620000.00 元，等待财务复核。',
     '/progress-payments', 'HIGH', 'APPROVAL', 1, '系统管理员',
     '2026-11-14 00:00:00', '2026-12-31 23:59:59', 'PUBLISHED', NOW(), NOW(), NOW(), 1, 1, 0, 0),
    (9100000000000006003, '用印申请待审批：张江二期总承包合同', 'LINK',
     '合同专用章用印申请（张江科技园二期施工总承包合同）等待审批。',
     '/seal/applications', 'URGENT', 'APPROVAL', 9100000000000000002, '李建国',
     '2026-11-15 00:00:00', '2026-12-31 23:59:59', 'PUBLISHED', NOW(), NOW(), NOW(), 1, 1, 0, 0),
    (9100000000000006004, '2026年11月工资条已发布', 'TEXT',
     '2026年11月工资条已发布，请登录系统查看个人工资明细。',
     '/hr/payroll', 'NORMAL', 'HR', 9100000000000000003, '赵敏',
     '2026-11-15 00:00:00', '2026-12-31 23:59:59', 'PUBLISHED', NOW(), NOW(), NOW(), 1, 1, 0, 0),
    (9100000000000006005, '资产借用逾期提醒', 'TEXT',
     '您借用的会议平板已超过归还日期，请尽快归还行政部。',
     '/admin/assets', 'HIGH', 'ADMIN', 1, '系统管理员',
     '2026-11-13 00:00:00', '2026-12-31 23:59:59', 'PUBLISHED', NOW(), NOW(), NOW(), 1, 1, 0, 0),
    (9100000000000006006, '文档共享：张江二期施工组织设计 V2', 'LINK',
     '李建国向您共享了《张江科技园二期施工组织设计 V2》。',
     '/doc/files', 'LOW', 'DOC', 9100000000000000002, '李建国',
     '2026-11-14 00:00:00', '2026-12-31 23:59:59', 'PUBLISHED', NOW(), NOW(), NOW(), 1, 1, 0, 0);

INSERT IGNORE INTO biz_user_notification
    (id, notification_id, user_id, is_read, read_time, channel, push_status, push_time,
     create_time, update_time, create_by, update_by, deleted, version)
VALUES
    (9100000000000006101, 9100000000000006001, 1,                    0, NULL, 'SYSTEM', 'SENT', NOW(), NOW(), NOW(), 1, 1, 0, 0),
    (9100000000000006102, 9100000000000006001, 9100000000000000001, 0, NULL, 'SYSTEM', 'SENT', NOW(), NOW(), NOW(), 1, 1, 0, 0),
    (9100000000000006103, 9100000000000006002, 1,                    0, NULL, 'SYSTEM', 'SENT', NOW(), NOW(), NOW(), 1, 1, 0, 0),
    (9100000000000006104, 9100000000000006002, 9100000000000000002, 0, NULL, 'SYSTEM', 'SENT', NOW(), NOW(), NOW(), 1, 1, 0, 0),
    (9100000000000006105, 9100000000000006003, 1,                    0, NULL, 'SYSTEM', 'SENT', NOW(), NOW(), NOW(), 1, 1, 0, 0),
    (9100000000000006106, 9100000000000006003, 9100000000000000003, 0, NULL, 'SYSTEM', 'SENT', NOW(), NOW(), NOW(), 1, 1, 0, 0),
    (9100000000000006107, 9100000000000006004, 9100000000000000004, 0, NULL, 'SYSTEM', 'SENT', NOW(), NOW(), NOW(), 1, 1, 0, 0),
    (9100000000000006108, 9100000000000006004, 9100000000000000003, 0, NULL, 'SYSTEM', 'SENT', NOW(), NOW(), NOW(), 1, 1, 0, 0),
    (9100000000000006109, 9100000000000006005, 9100000000000000004, 0, NULL, 'SYSTEM', 'SENT', NOW(), NOW(), NOW(), 1, 1, 0, 0),
    (9100000000000006110, 9100000000000006006, 9100000000000000002, 0, NULL, 'SYSTEM', 'SENT', NOW(), NOW(), NOW(), 1, 1, 0, 0);

-- ---------------------------------------------------------------------
-- 9. 执行后自检（只读，便于确认各表增量行数）
-- ---------------------------------------------------------------------
SELECT 'sys_user' AS table_name, COUNT(*) AS seeded_rows FROM sys_user WHERE id BETWEEN 9100000000000000001 AND 9100000000000000009
UNION ALL SELECT 'sys_dept', COUNT(*) FROM sys_dept WHERE id BETWEEN 9100000000000000011 AND 9100000000000000019
UNION ALL SELECT 'sys_user_role', COUNT(*) FROM sys_user_role WHERE id BETWEEN 9100000000000000031 AND 9100000000000000039
UNION ALL SELECT 'biz_category', COUNT(*) FROM biz_category WHERE id BETWEEN 9100000000000000101 AND 9100000000000000199
UNION ALL SELECT 'biz_contact', COUNT(*) FROM biz_contact WHERE id BETWEEN 9100000000000000201 AND 9100000000000000299
UNION ALL SELECT 'biz_material', COUNT(*) FROM biz_material WHERE id BETWEEN 9100000000000000301 AND 9100000000000000399
UNION ALL SELECT 'biz_equipment', COUNT(*) FROM biz_equipment WHERE id BETWEEN 9100000000000000401 AND 9100000000000000499
UNION ALL SELECT 'biz_equipment_lease', COUNT(*) FROM biz_equipment_lease WHERE id BETWEEN 9100000000000000501 AND 9100000000000000599
UNION ALL SELECT 'biz_project', COUNT(*) FROM biz_project WHERE id BETWEEN 9100000000000001001 AND 9100000000000001099
UNION ALL SELECT 'biz_subcontractor', COUNT(*) FROM biz_subcontractor WHERE id BETWEEN 9100000000000001101 AND 9100000000000001199
UNION ALL SELECT 'biz_subcontractor_contract', COUNT(*) FROM biz_subcontractor_contract WHERE id BETWEEN 9100000000000001201 AND 9100000000000001299
UNION ALL SELECT 'biz_progress_payment', COUNT(*) FROM biz_progress_payment WHERE id BETWEEN 9100000000000001301 AND 9100000000000001399
UNION ALL SELECT 'biz_settlement_proof', COUNT(*) FROM biz_settlement_proof WHERE id BETWEEN 9100000000000001401 AND 9100000000000001499
UNION ALL SELECT 'biz_budget', COUNT(*) FROM biz_budget WHERE id BETWEEN 9100000000000001501 AND 9100000000000001599
UNION ALL SELECT 'biz_receipt', COUNT(*) FROM biz_receipt WHERE id BETWEEN 9100000000000002001 AND 9100000000000002099
UNION ALL SELECT 'biz_receipt_verification', COUNT(*) FROM biz_receipt_verification WHERE id BETWEEN 9100000000000002101 AND 9100000000000002199
UNION ALL SELECT 'biz_payment_account', COUNT(*) FROM biz_payment_account WHERE id BETWEEN 9100000000000003101 AND 9100000000000003199
UNION ALL SELECT 'biz_payment_transaction', COUNT(*) FROM biz_payment_transaction WHERE id BETWEEN 9100000000000003201 AND 9100000000000003299
UNION ALL SELECT 'biz_bank_statement', COUNT(*) FROM biz_bank_statement WHERE id BETWEEN 9100000000000003301 AND 9100000000000003399
UNION ALL SELECT 'biz_reconciliation_result', COUNT(*) FROM biz_reconciliation_result WHERE id BETWEEN 9100000000000003401 AND 9100000000000003499
UNION ALL SELECT 'biz_account', COUNT(*) FROM biz_account WHERE id BETWEEN 9100000000000003501 AND 9100000000000003599
UNION ALL SELECT 'biz_cost_allocation', COUNT(*) FROM biz_cost_allocation WHERE id BETWEEN 9100000000000003601 AND 9100000000000003699
UNION ALL SELECT 'biz_general_ledger', COUNT(*) FROM biz_general_ledger WHERE id BETWEEN 9100000000000003701 AND 9100000000000003799
UNION ALL SELECT 'biz_electronic_seal_application', COUNT(*) FROM biz_electronic_seal_application WHERE id BETWEEN 9100000000000004001 AND 9100000000000004099
UNION ALL SELECT 'biz_stamp_approval', COUNT(*) FROM biz_stamp_approval WHERE id BETWEEN 9100000000000004101 AND 9100000000000004199
UNION ALL SELECT 'biz_electronic_seal_usage', COUNT(*) FROM biz_electronic_seal_usage WHERE id BETWEEN 9100000000000004201 AND 9100000000000004299
UNION ALL SELECT 'biz_electronic_seal_stamp', COUNT(*) FROM biz_electronic_seal_stamp WHERE id BETWEEN 9100000000000004301 AND 9100000000000004399
UNION ALL SELECT 'biz_approval_record', COUNT(*) FROM biz_approval_record WHERE id BETWEEN 9100000000000004401 AND 9100000000000004499
UNION ALL SELECT 'bpm_instance', COUNT(*) FROM bpm_instance WHERE id BETWEEN 9100000000000005001 AND 9100000000000005099
UNION ALL SELECT 'bpm_node', COUNT(*) FROM bpm_node WHERE id BETWEEN 9100000000000005101 AND 9100000000000005199
UNION ALL SELECT 'bpm_task', COUNT(*) FROM bpm_task WHERE id BETWEEN 9100000000000005201 AND 9100000000000005299
UNION ALL SELECT 'bpm_cc_record', COUNT(*) FROM bpm_cc_record WHERE id BETWEEN 9100000000000005301 AND 9100000000000005399
UNION ALL SELECT 'bpm_assignee_snapshot', COUNT(*) FROM bpm_assignee_snapshot WHERE id BETWEEN 9100000000000005401 AND 9100000000000005499
UNION ALL SELECT 'bpm_variable', COUNT(*) FROM bpm_variable WHERE id BETWEEN 9100000000000005501 AND 9100000000000005599
UNION ALL SELECT 'biz_notification', COUNT(*) FROM biz_notification WHERE id BETWEEN 9100000000000006001 AND 9100000000000006099
UNION ALL SELECT 'biz_user_notification', COUNT(*) FROM biz_user_notification WHERE id BETWEEN 9100000000000006101 AND 9100000000000006199;
