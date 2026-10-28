package com.ceams.module.seal.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 更新用印审批（仅 PENDING 可编辑）。
 */
@Data
public class StampApprovalUpdateRequest {

    @Size(max = 100, message = "印章权限长度不能超过 100")
    private String sealPermission;

    @Size(max = 500, message = "用印位置长度不能超过 500")
    private String stampPositions;

    @Size(max = 500, message = "原始合同路径长度不能超过 500")
    private String originalContractPath;
}
