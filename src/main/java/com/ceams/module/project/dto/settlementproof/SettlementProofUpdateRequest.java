package com.ceams.module.project.dto.settlementproof;

import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;

/** 更新结算凭证请求。 */
@Data
public class SettlementProofUpdateRequest {

    @Size(max = 255, message = "凭证名长度不能超过 255")
    private String proofName;

    @Size(max = 255, message = "凭证编号长度不能超过 255")
    private String proofNumber;

    private LocalDate proofDate;

    @PositiveOrZero(message = "结算金额不能为负")
    private BigDecimal settlementAmount;

    private String description;

    @Size(max = 500, message = "附件路径长度不能超过 500")
    private String filePath;

    @Size(max = 255, message = "附件名长度不能超过 255")
    private String fileName;
}
