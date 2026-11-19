package com.ceams.module.doc.dto;

import com.ceams.module.doc.enums.DocSubjectType;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Data;

/** 文件级授权请求。 */
@Data
public class PermissionGrantRequest {

    /** 主体类型：USER/ROLE/DEPT。 */
    @NotNull(message = "授权主体类型不能为空")
    private DocSubjectType subjectType;

    /** 主体 ID（用户 / 角色 / 部门）。 */
    @NotNull(message = "授权主体 ID 不能为空")
    private Long subjectId;

    /** 动作集合：PREVIEW/DOWNLOAD/EDIT/DELETE。 */
    @NotEmpty(message = "授权动作不能为空")
    private List<String> actions;
}
