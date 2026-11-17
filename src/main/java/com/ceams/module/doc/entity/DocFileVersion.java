package com.ceams.module.doc.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ceams.common.base.BaseEntity;
import com.ceams.module.doc.enums.DocVersionSource;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 文档文件版本（doc_file_version，唯一键 file_id + version_no）。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("doc_file_version")
public class DocFileVersion extends BaseEntity {

    /** 文件 ID（逻辑引用 doc_file.id）。 */
    private Long fileId;

    /** 版本号（从 1 起递增）。 */
    private Integer versionNo;

    /** 存储键（相对 upload-dir 的路径）。 */
    private String storageKey;

    /** 文件字节数。 */
    private Long size;

    /** SHA-256 校验和。 */
    private String checksum;

    /** 上传人 ID（sys_user.id）。 */
    private Long uploadedBy;

    /** 变更说明。 */
    private String changeNote;

    /** 版本来源：UPLOAD/ONLINE_EDIT/ROLLBACK。 */
    private DocVersionSource source;
}
