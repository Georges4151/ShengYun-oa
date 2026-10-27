package com.ceams.module.project.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 分包商视图对象（敏感字段已脱敏）。
 */
@Data
public class SubcontractorVO {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    private String name;

    private String contactPerson;

    /** 手机号（脱敏）。 */
    private String phone;

    private String address;

    private String qualificationLevel;

    private String creditRating;

    private String businessScope;

    /** 税号（脱敏）。 */
    private String taxIdentificationNumber;

    /** 银行账号（脱敏）。 */
    private String bankAccount;

    private String description;

    private LocalDateTime createTime;
}