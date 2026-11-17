package com.ceams.module.bpm.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.Map;
import lombok.Data;

/** 发起流程实例请求。 */
@Data
public class InstanceStartRequest {

    /** 模板编码。 */
    @NotBlank(message = "模板编码不能为空")
    private String templateCode;

    /** 业务类型（为空时取模板配置）。 */
    private String bizType;

    /** 业务单据 ID。 */
    private Long bizId;

    /** 流程变量（业务条件变量，统一字符串存储）。 */
    private Map<String, String> variables;
}
