package com.ceams.module.bank.dto;

import com.ceams.module.bank.enums.TransactionType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 更新总账。
 */
@Data
public class GeneralLedgerUpdateRequest {

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
}
