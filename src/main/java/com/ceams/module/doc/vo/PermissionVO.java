package com.ceams.module.doc.vo;

import com.ceams.module.doc.enums.DocSubjectType;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.util.List;
import lombok.Data;

/** 文档授权条目展示对象。 */
@Data
public class PermissionVO {

    /** 授权记录 ID。 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /** 主体类型。 */
    private DocSubjectType subjectType;

    /** 主体 ID。 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long subjectId;

    /** 动作集合：PREVIEW/DOWNLOAD/EDIT/DELETE。 */
    private List<String> actions;

    /** 授权人 ID。 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long grantedBy;
}
