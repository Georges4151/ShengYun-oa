-- ======================================================================
-- V43：IAM 账号头像 - sys_user 增加头像访问地址列
--
-- 说明：
--   1. avatar 存储头像的相对访问地址（/api/v1/profile/avatars/{文件名}），
--      不存物理磁盘路径，避免换机/换目录后地址失效。
--   2. 头像文件由 ProfileService 落盘到 ceams.file.upload-dir 配置的 avatars 子目录，
--      文件名为雪花 ID + 白名单扩展名（jpg/jpeg/png/webp），库内仅保存引用。
--   3. 该列可空：NULL 表示未设置头像，前端回退到默认头像。
--   4. 本脚本为增量迁移，入库后不得修改；如需变更另起更高版本号脚本。
-- ======================================================================

ALTER TABLE sys_user
    ADD COLUMN avatar VARCHAR(255) NULL DEFAULT NULL COMMENT '头像访问地址（/api/v1/profile/avatars/{文件名}）' AFTER name;
