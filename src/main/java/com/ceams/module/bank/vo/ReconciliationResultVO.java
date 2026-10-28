package com.ceams.module.bank.vo;

import com.ceams.module.bank.enums.DiscrepancyType;
import com.ceams.module.bank.enums.ReconciliationStatus;
import com.ceams.module.bank.enums.ResolutionStatus;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 对账结果视图对象。
 */
@Data
public class ReconciliationResultVO {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long accountId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long statementId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long transactionId;

    private ReconciliationStatus status;

    private DiscrepancyType discrepancyType;

    private BigDecimal bankAmount;

    private BigDecimal systemAmount;

    private LocalDateTime bankDate;

    private LocalDateTime systemDate;

    private String bankDescription;

    private String systemDescription;

    private String statementReference;

    private String transactionReference;

    private ResolutionStatus resolutionStatus;

    private String resolutionNotes;

    private LocalDateTime reconciliationDate;

    private LocalDateTime createTime;
}
