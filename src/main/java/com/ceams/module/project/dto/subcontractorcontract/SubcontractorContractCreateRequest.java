package com.ceams.module.project.dto.subcontractorcontract;

import com.ceams.module.project.enums.ContractStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;

/** 新建分包合同请求。 */
@Data
public class SubcontractorContractCreateRequest {

    @NotNull(message = "分包商 ID 不能为空")
    private Long subcontractorId;

    private Long projectId;

    @NotBlank(message = "合同名不能为空")
    @Size(max = 255, message = "合同名长度不能超过 255")
    private String contractName;

    @NotBlank(message = "合同编号不能为空")
    @Size(max = 255, message = "合同编号长度不能超过 255")
    private String contractNumber;

    @NotNull(message = "合同金额不能为空")
    @PositiveOrZero(message = "合同金额不能为负")
    private BigDecimal contractAmount;

    private LocalDate startDate;

    private LocalDate endDate;

    private ContractStatus status;

    private String description;

    @Size(max = 500, message = "附件路径长度不能超过 500")
    private String filePath;

    @Size(max = 255, message = "附件名长度不能超过 255")
    private String fileName;
}
