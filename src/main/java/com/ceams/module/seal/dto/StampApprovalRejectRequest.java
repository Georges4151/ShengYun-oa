package com.ceams.module.seal.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 用印审批驳回请求。
 */
@Data
public class StampApprovalRejectRequest {

    /** 驳回原因。 */
    @NotBlank(message = "驳回原因不能为空")
    @Size(max = 500, message = "驳回原因长度不能超过 500")
    private String rejectReason;
}
