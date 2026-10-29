package com.ceams.module.project.vo;

import com.ceams.module.project.enums.ContractStatus;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

/** 分包合同视图对象。 */
@Data
public class SubcontractorContractVO {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long subcontractorId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long projectId;

    private String contractName;

    private String contractNumber;

    private BigDecimal contractAmount;

    private LocalDate startDate;

    private LocalDate endDate;

    private ContractStatus status;

    private String description;

    private String filePath;

    private String fileName;

    private BigDecimal paidAmount;

    private LocalDateTime createTime;
}
