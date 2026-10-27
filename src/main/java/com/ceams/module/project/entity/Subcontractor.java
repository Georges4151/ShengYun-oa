package com.ceams.module.project.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ceams.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 分包商（biz_subcontractor）。
 *
 * <p>手机号 / 税号 / 银行账号为敏感字段，库内需加密、VO 输出脱敏。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_subcontractor")
public class Subcontractor extends BaseEntity {

    /** 分包商名。 */
    private String name;

    /** 联系人。 */
    private String contactPerson;

    /** 手机号（敏感，需加密）。 */
    private String phone;

    /** 地址。 */
    private String address;

    /** 资质等级。 */
    private String qualificationLevel;

    /** 信用评级。 */
    private String creditRating;

    /** 经营范围。 */
    private String businessScope;

    /** 税号（敏感，需加密）。 */
    private String taxIdentificationNumber;

    /** 银行账号（敏感，需加密）。 */
    private String bankAccount;

    /** 备注。 */
    private String description;
}