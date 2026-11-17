package com.ceams.module.bpm.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import lombok.Data;

/** 发布新版本请求。 */
@Data
public class TemplatePublishRequest {

    /** 流程定义 JSON。 */
    @NotBlank(message = "流程定义 JSON 不能为空")
    private String schemaJson;

    /** 生效时间（为空时立即生效）。 */
    private LocalDateTime effectiveTime;
}
