-- ======================================================================
-- V38：文档知识库 - 目录 / 文件元数据 / 版本 / 目录与文件级授权
--
-- 说明：
--   1. 主键为雪花 ID（BIGINT），不 AUTO_INCREMENT。
--   2. 本脚本仅建表，权限点由 V39__doc_permission_seed.sql 负责；
--      两脚本按 V38 → V39 顺序执行，V39 依赖本脚本无直接外键（逻辑引用）。
--   3. 目录同级不重名、删除非空目录等约束由业务层校验，故不加同级唯一键
--      （软删除后重建同名会与唯一键冲突）。
--   4. 文件仅存元数据，物理文件落 storage_key（相对 upload-dir），
--      秒传与变更判定依赖 checksum；多文件共用 storage_key 时按引用计数物理删除。
--   5. 本脚本为增量迁移，入库后不得修改；如需变更另起更高版本号脚本。
-- ======================================================================

-- ----------------------------------------------------------------------
-- 1. 文档目录（doc_directory）
-- ----------------------------------------------------------------------
CREATE TABLE doc_directory (
    id            BIGINT       NOT NULL                 COMMENT '主键（雪花ID）',
    name          VARCHAR(200) NOT NULL                 COMMENT '目录名（同级不重名，业务层校验）',
    parent_id     BIGINT       NULL     DEFAULT NULL    COMMENT '父目录ID（逻辑引用 doc_directory.id，NULL 为库根）',
    library_type  VARCHAR(20)  NOT NULL                 COMMENT '库类型：PERSONAL/DEPT/PROJECT/PUBLIC',
    owner_user_id BIGINT       NULL     DEFAULT NULL    COMMENT '所有者用户ID（PERSONAL 必填）',
    owner_dept_id BIGINT       NULL     DEFAULT NULL    COMMENT '所有者部门ID（DEPT 必填）',
    project_id    BIGINT       NULL     DEFAULT NULL    COMMENT '项目ID（PROJECT 必填，逻辑引用 biz_project.id）',
    path          VARCHAR(500) NOT NULL                 COMMENT '目录路径（/id/id/ 形式，支撑子树查询与移动校验）',
    sort_order    INT          NOT NULL DEFAULT 0       COMMENT '排序',
    status        VARCHAR(20)  NOT NULL DEFAULT 'NORMAL' COMMENT '状态：NORMAL/ARCHIVED',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by     BIGINT       NULL     DEFAULT NULL    COMMENT '创建人ID',
    update_by     BIGINT       NULL     DEFAULT NULL    COMMENT '更新人ID',
    deleted       TINYINT      NOT NULL DEFAULT 0       COMMENT '逻辑删除：0未删除 1已删除',
    version       INT          NOT NULL DEFAULT 0       COMMENT '乐观锁版本号',
    PRIMARY KEY (id),
    KEY idx_doc_dir_parent (parent_id),
    KEY idx_doc_dir_lib (library_type, owner_user_id, owner_dept_id, project_id),
    KEY idx_doc_dir_path (path)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文档知识库目录';

-- ----------------------------------------------------------------------
-- 2. 文档文件元数据（doc_file）
-- ----------------------------------------------------------------------
CREATE TABLE doc_file (
    id                 BIGINT       NOT NULL                  COMMENT '主键（雪花ID）',
    dir_id             BIGINT       NOT NULL                  COMMENT '所属目录ID（逻辑引用 doc_directory.id）',
    name               VARCHAR(255) NOT NULL                  COMMENT '文件名（含扩展名）',
    ext                VARCHAR(20)  NULL     DEFAULT NULL     COMMENT '小写扩展名',
    size               BIGINT       NOT NULL                  COMMENT '文件字节数',
    storage_key        VARCHAR(500) NOT NULL                  COMMENT '存储键（相对 upload-dir 的路径）',
    checksum           CHAR(64)     NOT NULL                  COMMENT 'SHA-256 校验和（秒传与变更判定）',
    current_version_no INT          NOT NULL DEFAULT 1        COMMENT '当前版本号（从 1 起）',
    status             VARCHAR(20)  NOT NULL DEFAULT 'NORMAL' COMMENT '状态：NORMAL/LOCKED（LOCKED 仅用于列表展示）',
    lock_user_id       BIGINT       NULL     DEFAULT NULL     COMMENT '编辑锁持有人ID（与 Redis 冗余）',
    lock_time          DATETIME     NULL     DEFAULT NULL     COMMENT '加锁时间',
    uploaded_by        BIGINT       NOT NULL                  COMMENT '上传人ID（sys_user.id）',
    download_count     INT          NOT NULL DEFAULT 0        COMMENT '下载次数',
    create_time        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by          BIGINT       NULL     DEFAULT NULL     COMMENT '创建人ID',
    update_by          BIGINT       NULL     DEFAULT NULL     COMMENT '更新人ID',
    deleted            TINYINT      NOT NULL DEFAULT 0        COMMENT '逻辑删除：0未删除 1已删除',
    version            INT          NOT NULL DEFAULT 0        COMMENT '乐观锁版本号',
    PRIMARY KEY (id),
    KEY idx_doc_file_dir (dir_id, deleted),
    KEY idx_doc_file_checksum (checksum),
    KEY idx_doc_file_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文档文件元数据';

-- ----------------------------------------------------------------------
-- 3. 文档文件版本（doc_file_version）
-- ----------------------------------------------------------------------
CREATE TABLE doc_file_version (
    id          BIGINT       NOT NULL                COMMENT '主键（雪花ID）',
    file_id     BIGINT       NOT NULL                COMMENT '文件ID（逻辑引用 doc_file.id）',
    version_no  INT          NOT NULL                COMMENT '版本号（从 1 起递增）',
    storage_key VARCHAR(500) NOT NULL                COMMENT '存储键（相对 upload-dir 的路径）',
    size        BIGINT       NOT NULL                COMMENT '文件字节数',
    checksum    CHAR(64)     NOT NULL                COMMENT 'SHA-256 校验和',
    uploaded_by BIGINT       NOT NULL                COMMENT '上传人ID（sys_user.id）',
    change_note VARCHAR(500) NULL     DEFAULT NULL   COMMENT '变更说明',
    source      VARCHAR(20)  NOT NULL                COMMENT '版本来源：UPLOAD/ONLINE_EDIT/ROLLBACK',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by   BIGINT       NULL     DEFAULT NULL   COMMENT '创建人ID',
    update_by   BIGINT       NULL     DEFAULT NULL   COMMENT '更新人ID',
    deleted     TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除：0未删除 1已删除',
    version     INT          NOT NULL DEFAULT 0      COMMENT '乐观锁版本号',
    PRIMARY KEY (id),
    UNIQUE KEY uk_doc_ver (file_id, version_no),
    KEY idx_doc_ver_file (file_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文档文件版本';

-- ----------------------------------------------------------------------
-- 4. 目录级授权（doc_dir_permission）
-- ----------------------------------------------------------------------
CREATE TABLE doc_dir_permission (
    id           BIGINT      NOT NULL                COMMENT '主键（雪花ID）',
    dir_id       BIGINT      NOT NULL                COMMENT '目录ID（逻辑引用 doc_directory.id）',
    subject_type VARCHAR(20) NOT NULL                COMMENT '主体类型：USER/ROLE/DEPT',
    subject_id   BIGINT      NOT NULL                COMMENT '主体ID（sys_user.id / sys_role.id / sys_dept.id）',
    actions      JSON        NOT NULL                COMMENT '操作集合（JSON 数组：PREVIEW/DOWNLOAD/EDIT/DELETE）',
    granted_by   BIGINT      NULL     DEFAULT NULL   COMMENT '授权人ID（sys_user.id）',
    create_time  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by    BIGINT      NULL     DEFAULT NULL   COMMENT '创建人ID',
    update_by    BIGINT      NULL     DEFAULT NULL   COMMENT '更新人ID',
    deleted      TINYINT     NOT NULL DEFAULT 0      COMMENT '逻辑删除：0未删除 1已删除',
    version      INT         NOT NULL DEFAULT 0      COMMENT '乐观锁版本号',
    PRIMARY KEY (id),
    UNIQUE KEY uk_doc_dir_perm (dir_id, subject_type, subject_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文档目录级授权';

-- ----------------------------------------------------------------------
-- 5. 文件级授权（doc_file_permission，覆盖目录权限）
-- ----------------------------------------------------------------------
CREATE TABLE doc_file_permission (
    id           BIGINT      NOT NULL                COMMENT '主键（雪花ID）',
    file_id      BIGINT      NOT NULL                COMMENT '文件ID（逻辑引用 doc_file.id）',
    subject_type VARCHAR(20) NOT NULL                COMMENT '主体类型：USER/ROLE/DEPT',
    subject_id   BIGINT      NOT NULL                COMMENT '主体ID（sys_user.id / sys_role.id / sys_dept.id）',
    actions      JSON        NOT NULL                COMMENT '操作集合（JSON 数组：PREVIEW/DOWNLOAD/EDIT/DELETE）',
    granted_by   BIGINT      NULL     DEFAULT NULL   COMMENT '授权人ID（sys_user.id）',
    create_time  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by    BIGINT      NULL     DEFAULT NULL   COMMENT '创建人ID',
    update_by    BIGINT      NULL     DEFAULT NULL   COMMENT '更新人ID',
    deleted      TINYINT     NOT NULL DEFAULT 0      COMMENT '逻辑删除：0未删除 1已删除',
    version      INT         NOT NULL DEFAULT 0      COMMENT '乐观锁版本号',
    PRIMARY KEY (id),
    UNIQUE KEY uk_doc_file_perm (file_id, subject_type, subject_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文档文件级授权';
