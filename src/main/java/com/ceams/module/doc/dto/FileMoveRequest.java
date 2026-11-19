package com.ceams.module.doc.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** 文件移动请求。 */
@Data
public class FileMoveRequest {

    /** 目标目录 ID。 */
    @NotNull(message = "目标目录不能为空")
    private Long dirId;
}
