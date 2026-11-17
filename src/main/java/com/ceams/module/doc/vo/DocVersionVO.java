package com.ceams.module.doc.vo;

import com.ceams.module.doc.enums.DocVersionSource;
import java.time.LocalDateTime;
import lombok.Data;

/** 文档版本展示对象。 */
@Data
public class DocVersionVO {

    /** 版本号（从 1 起）。 */
    private Integer versionNo;

    /** 文件字节数。 */
    private Long size;

    /** SHA-256 校验和。 */
    private String checksum;

    /** 上传人 ID。 */
    private Long uploadedBy;

    /** 变更说明。 */
    private String changeNote;

    /** 版本来源：UPLOAD/ONLINE_EDIT/ROLLBACK。 */
    private DocVersionSource source;

    /** 创建时间。 */
    private LocalDateTime createTime;
}
