package com.ceams.module.project.dto.subcontractor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 新建分包商请求。
 */
@Data
public class SubcontractorCreateRequest {

    @NotBlank(message = "分包商名不能为空")
    @Size(max = 200, message = "分包商名长度不能超过 200")
    private String name;

    @Size(max = 50, message = "联系人长度不能超过 50")
    private String contactPerson;

    @Size(max = 20, message = "手机号长度不能超过 20")
    private String phone;

    @Size(max = 255, message = "地址长度不能超过 255")
    private String address;

    @Size(max = 50, message = "资质等级长度不能超过 50")
    private String qualificationLevel;

    @Size(max = 20, message = "信用评级长度不能超过 20")
    private String creditRating;

    private String businessScope;

    @Size(max = 50, message = "税号长度不能超过 50")
    private String taxIdentificationNumber;

    @Size(max = 100, message = "银行账号长度不能超过 100")
    private String bankAccount;

    private String description;
}