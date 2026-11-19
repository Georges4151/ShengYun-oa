package com.ceams.module.bpm.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/** 更新流程模板请求（仅元数据，不影响已发布版本）。 */
@Data
public class TemplateUpdateRequest {

    /** 模板名称。 */
    @Size(max = 128, message = "模板名称长度不能超过 128")
    private String name;

    /** 业务类型。 */
    @Size(max = 64, message = "业务类型长度不能超过 64")
    private String bizType;
}
