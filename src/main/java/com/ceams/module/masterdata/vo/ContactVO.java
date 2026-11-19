package com.ceams.module.masterdata.vo;

import com.ceams.module.masterdata.enums.ContactType;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.time.LocalDateTime;
import lombok.Data;

/** 往来单位视图对象（敏感字段已脱敏）。 */
@Data
public class ContactVO {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    private String name;

    private ContactType type;

    private String contactPerson;

    /** 手机号（脱敏）。 */
    private String phone;

    private String email;

    private String address;

    /** 税号（脱敏）。 */
    private String taxIdentificationNumber;

    /** 银行账号（脱敏）。 */
    private String bankAccount;

    private LocalDateTime createTime;
}
