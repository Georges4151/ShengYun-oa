package com.ceams.module.seal.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 更新用印申请（仅 PENDING 可编辑）。
 */
@Data
public class ElectronicSealApplicationUpdateRequest {

    @Size(max = 500, message = "用印事由长度不能超过 500")
    private String purpose;

    private Long projectId;

    @Size(max = 200, message = "项目名长度不能超过 200")
    private String projectName;

    @Size(max = 255, message = "合同名长度不能超过 255")
    private String contractName;

    @Size(max = 500, message = "合同文件路径长度不能超过 500")
    private String contractPath;

    @Size(max = 100, message = "用印位置长度不能超过 100")
    private String stampPosition;
}
