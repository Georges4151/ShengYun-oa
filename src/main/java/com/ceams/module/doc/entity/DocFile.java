package com.ceams.module.doc.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ceams.common.base.BaseEntity;
import com.ceams.module.doc.enums.DocFileStatus;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 文档文件元数据（doc_file，物理文件落 storage_key）。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("doc_file")
public class DocFile extends BaseEntity {

    /** 所属目录 ID（逻辑引用 doc_directory.id）。 */
    private Long dirId;

    /** 文件名（含扩展名）。 */
    private String name;

    /** 小写扩展名。 */
    private String ext;

    /** 文件字节数。 */
    private Long size;

    /** 存储键（相对 upload-dir 的路径）。 */
    private String storageKey;

    /** SHA-256 校验和（秒传与变更判定）。 */
    private String checksum;

    /** 当前版本号（从 1 起）。 */
    private Integer currentVersionNo;

    /** 状态：NORMAL/LOCKED。 */
    private DocFileStatus status;

    /** 编辑锁持有人 ID（与 Redis 冗余）。 */
    private Long lockUserId;

    /** 加锁时间。 */
    private LocalDateTime lockTime;

    /** 上传人 ID（sys_user.id）。 */
    private Long uploadedBy;

    /** 下载次数。 */
    private Integer downloadCount;
}
