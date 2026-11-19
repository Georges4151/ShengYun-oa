package com.ceams.module.bank.vo;

import com.ceams.module.bank.enums.TransactionType;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/** 总账视图对象。 */
@Data
public class GeneralLedgerVO {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    private String accountName;

    /** 账号【需加密（阶段4）】。 */
    private String accountNumber;

    private String bankName;

    private BigDecimal balance;

    private String accountType;

    private String summary;

    private LocalDateTime transactionTime;

    private TransactionType transactionType;

    private BigDecimal amount;

    private String status;

    private LocalDateTime createTime;
}
