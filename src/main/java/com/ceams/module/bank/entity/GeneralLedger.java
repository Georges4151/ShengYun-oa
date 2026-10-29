package com.ceams.module.bank.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ceams.common.base.BaseEntity;
import com.ceams.common.crypto.SensitiveTypeHandler;
import com.ceams.module.bank.enums.TransactionType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 总账（biz_general_ledger）。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "biz_general_ledger", autoResultMap = true)
public class GeneralLedger extends BaseEntity {

    /** 账户名。 */
    private String accountName;

    /** 账号【需加密（阶段4）】。 */
    @TableField(typeHandler = SensitiveTypeHandler.class)
    private String accountNumber;

    /** 开户行。 */
    private String bankName;

    /** 余额。 */
    private BigDecimal balance;

    /** 账户类型（代码值）。 */
    private String accountType;

    /** 摘要。 */
    private String summary;

    /** 交易时间。 */
    private LocalDateTime transactionTime;

    /** 交易类型：INCOME/EXPENSE。 */
    private TransactionType transactionType;

    /** 金额。 */
    private BigDecimal amount;

    /** 状态（代码值）。 */
    private String status;
}
