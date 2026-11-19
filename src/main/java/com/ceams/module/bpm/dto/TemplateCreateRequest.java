package com.ceams.module.bpm.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 创建流程模板请求。 */
@Data
public class TemplateCreateRequest {

    /** 模板编码（唯一）。 */
    @NotBlank(message = "模板编码不能为空")
    @Size(max = 64, message = "模板编码长度不能超过 64")
    private String code;

    /** 模板名称。 */
    @NotBlank(message = "模板名称不能为空")
    @Size(max = 128, message = "模板名称长度不能超过 128")
    private String name;

    /** 业务类型。 */
    @NotBlank(message = "业务类型不能为空")
    @Size(max = 64, message = "业务类型长度不能超过 64")
    private String bizType;
}
