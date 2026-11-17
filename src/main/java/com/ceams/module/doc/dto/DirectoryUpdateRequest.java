package com.ceams.module.doc.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/** 目录编辑请求（字段为空表示不修改）。 */
@Data
public class DirectoryUpdateRequest {

    /** 目录名。 */
    @Size(max = 200, message = "目录名称不能超过 200 个字符")
    private String name;

    /** 同级排序号。 */
    private Integer sortOrder;
}
