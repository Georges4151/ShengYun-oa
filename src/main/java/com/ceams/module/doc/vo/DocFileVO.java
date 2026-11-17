package com.ceams.module.doc.vo;

import com.ceams.module.doc.enums.DocFileStatus;
import java.time.LocalDateTime;
import lombok.Data;

/** 文档文件展示对象（仅元数据）。 */
@Data
public class DocFileVO {

    /** 文件 ID。 */
    private Long id;

    /** 所属目录 ID。 */
    private Long dirId;

    /** 文件名（含扩展名）。 */
    private String name;

    /** 小写扩展名。 */
    private String ext;

    /** 文件字节数。 */
    private Long size;

    /** 当前版本号。 */
    private Integer currentVersionNo;

    /** 状态：NORMAL/LOCKED。 */
    private DocFileStatus status;

    /** 上传人 ID。 */
    private Long uploadedBy;

    /** 创建时间。 */
    private LocalDateTime createTime;

    /** 更新时间。 */
    private LocalDateTime updateTime;

    /** 下载次数。 */
    private Integer downloadCount;
}
