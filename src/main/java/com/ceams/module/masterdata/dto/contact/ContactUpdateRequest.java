package com.ceams.module.masterdata.dto.contact;

import com.ceams.module.masterdata.enums.ContactType;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 更新往来单位请求。 */
@Data
public class ContactUpdateRequest {

    @Size(max = 200, message = "单位名称长度不能超过 200")
    private String name;

    private ContactType type;

    @Size(max = 50, message = "联系人长度不能超过 50")
    private String contactPerson;

    @Size(max = 20, message = "手机号长度不能超过 20")
    private String phone;

    @Size(max = 100, message = "邮箱长度不能超过 100")
    private String email;

    @Size(max = 255, message = "地址长度不能超过 255")
    private String address;

    @Size(max = 50, message = "税号长度不能超过 50")
    private String taxIdentificationNumber;

    @Size(max = 100, message = "银行账号长度不能超过 100")
    private String bankAccount;
}
