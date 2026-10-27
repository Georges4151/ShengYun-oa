package com.ceams.module.project.dto.settlementproof;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 新建结算凭证请求。
 */
@Data
public class SettlementProofCreateRequest {

    @NotNull(message = "分包合同 ID 不能为空")
    private Long contractId;

    private Long subcontractorId;

    @NotBlank(message = "凭证名不能为空")
    @Size(max = 255, message = "凭证名长度不能超过 255")
    private String proofName;

    @NotBlank(message = "凭证编号不能为空")
    @Size(max = 255, message = "凭证编号长度不能超过 255")
    private String proofNumber;

    private LocalDate proofDate;

    @NotNull(message = "结算金额不能为空")
    @PositiveOrZero(message = "结算金额不能为负")
    private BigDecimal settlementAmount;

    private String description;
}