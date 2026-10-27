package com.ceams.module.masterdata.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ceams.common.base.BaseEntity;
import com.ceams.module.masterdata.enums.ContactType;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 往来单位（biz_contact）。
 *
 * <p>税号 / 银行账号 / 手机号为敏感字段，库内需加密、VO 输出脱敏。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_contact")
public class Contact extends BaseEntity {

    /** 单位名称。 */
    private String name;

    /** 类型。 */
    private ContactType type;

    /** 联系人。 */
    private String contactPerson;

    /** 手机号（敏感，需加密）。 */
    private String phone;

    /** 邮箱。 */
    private String email;

    /** 地址。 */
    private String address;

    /** 税号（敏感，需加密）。 */
    private String taxIdentificationNumber;

    /** 银行账号（敏感，需加密）。 */
    private String bankAccount;
}