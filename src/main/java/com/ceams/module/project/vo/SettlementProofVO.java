package com.ceams.module.project.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

/** 结算凭证视图对象。 */
@Data
public class SettlementProofVO {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long contractId;

    private String contractName;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long subcontractorId;

    private String subcontractorName;

    private String proofName;

    private String proofNumber;

    private LocalDate proofDate;

    private BigDecimal settlementAmount;

    private String filePath;

    private String fileName;

    private String description;

    private LocalDateTime createTime;
}
