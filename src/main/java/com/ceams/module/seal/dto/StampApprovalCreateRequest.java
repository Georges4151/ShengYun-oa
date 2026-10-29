package com.ceams.module.seal.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 新建用印审批。 */
@Data
public class StampApprovalCreateRequest {

    /** 用印申请 ID（逻辑引用 biz_electronic_seal_application.id）。 */
    @NotNull(message = "用印申请ID不能为空")
    private Long stampId;

    @Size(max = 100, message = "印章权限长度不能超过 100")
    private String sealPermission;

    @Size(max = 500, message = "用印位置长度不能超过 500")
    private String stampPositions;

    @Size(max = 500, message = "原始合同路径长度不能超过 500")
    private String originalContractPath;
}
