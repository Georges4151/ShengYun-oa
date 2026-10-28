package com.ceams.module.payment.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ceams.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 付款账户（biz_payment_account）。
 *
 * <p>M2 阶段仅提供最小只读查询服务，供 expense 域财务核验反向依赖。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_payment_account")
public class PaymentAccount extends BaseEntity {

    /** 账户名。 */
    private String name;

    /** 账户类型（代码值）。 */
    private String type;

    /** 余额。 */
    private BigDecimal balance;

    /** 账号【需加密（阶段4）】。 */
    private String accountNumber;

    /** 描述。 */
    private String description;
}
