package com.ceams.module.payment.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ceams.common.base.BaseEntity;
import com.ceams.common.crypto.SensitiveTypeHandler;
import java.math.BigDecimal;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 付款账户（biz_payment_account）。
 *
 * <p>M2 阶段仅提供最小只读查询服务，供 expense 域财务核验反向依赖。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "biz_payment_account", autoResultMap = true)
public class PaymentAccount extends BaseEntity {

    /** 账户名。 */
    private String name;

    /** 账户类型（代码值）。 */
    private String type;

    /** 余额。 */
    private BigDecimal balance;

    /** 账号【需加密（阶段4）】。 */
    @TableField(typeHandler = SensitiveTypeHandler.class)
    private String accountNumber;

    /** 描述。 */
    private String description;
}
