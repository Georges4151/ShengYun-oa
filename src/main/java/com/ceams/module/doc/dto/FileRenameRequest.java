package com.ceams.module.doc.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 文件重命名请求。 */
@Data
public class FileRenameRequest {

    /** 新文件名（含扩展名）。 */
    @NotBlank(message = "文件名不能为空")
    @Size(max = 255, message = "文件名不能超过 255 个字符")
    private String name;
}
