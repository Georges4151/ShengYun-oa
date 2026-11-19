package com.ceams.module.project.dto.subcontractorcontract;

import com.ceams.module.project.enums.ContractStatus;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;

/** 更新分包合同请求。 */
@Data
public class SubcontractorContractUpdateRequest {

    private Long projectId;

    @Size(max = 255, message = "合同名长度不能超过 255")
    private String contractName;

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
