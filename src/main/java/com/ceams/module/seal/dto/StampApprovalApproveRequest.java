package com.ceams.module.seal.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/** 用印审批通过请求。 */
@Data
public class StampApprovalApproveRequest {

    /** 审批意见（可选）。 */
    @Size(max = 500, message = "审批意见长度不能超过 500")
    private String approvalComment;
}
