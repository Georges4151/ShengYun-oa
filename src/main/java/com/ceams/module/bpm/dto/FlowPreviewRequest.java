package com.ceams.module.bpm.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** 发起前流程预览入参。 */
@Data
public class FlowPreviewRequest {

    @NotBlank private String templateCode;

    /** 业务类型；为空时取模板绑定的业务类型。 */
    private String bizType;

    /** 业务单据 ID；HR 等按业务员工解析上级时必须传入。 */
    private Long bizId;

    /** 业务主体用户 ID（如员工绑定的系统用户）；单据尚未创建时用于按目标员工解析审批人。 */
    private Long subjectUserId;
}
