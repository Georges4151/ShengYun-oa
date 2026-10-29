package com.ceams.module.project.dto.changerequest;

import jakarta.validation.constraints.Size;
import lombok.Data;

/** 审批动作请求（通过/驳回）。 */
@Data
public class ApprovalActionRequest {

    @Size(max = 500, message = "审批意见长度不能超过 500")
    private String comment;
}
