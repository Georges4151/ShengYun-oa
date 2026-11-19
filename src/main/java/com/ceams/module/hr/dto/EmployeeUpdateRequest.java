package com.ceams.module.hr.dto;

import com.ceams.module.hr.enums.EmployeeStatus;
import com.ceams.module.hr.enums.EmploymentType;
import com.ceams.module.hr.enums.Gender;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import lombok.Data;

/** 修改员工档案请求：字段为 null 表示不修改（敏感字段同理）。 */
@Data
public class EmployeeUpdateRequest {

    /** 绑定系统用户 ID（显式传 0 表示解绑）。 */
    private Long userId;

    @Size(max = 64, message = "员工工号长度不能超过 64")
    private String employeeNo;

    @Size(max = 64, message = "姓名长度不能超过 64")
    private String name;

    private Gender gender;

    private LocalDate birthDate;

    /** 身份证号（明文）。 */
    @Size(max = 32, message = "身份证号长度不能超过 32")
    private String idCard;

    /** 手机号（明文）。 */
    @Size(max = 20, message = "手机号长度不能超过 20")
    private String phone;

    @Size(max = 128, message = "邮箱长度不能超过 128")
    private String email;

    private Long deptId;

    private Long positionId;

    private EmploymentType employmentType;

    private LocalDate hireDate;

    private LocalDate probationEndDate;

    private LocalDate regularDate;

    private LocalDate leaveDate;

    private EmployeeStatus status;

    @Size(max = 64, message = "紧急联系人长度不能超过 64")
    private String emergencyContact;

    /** 紧急联系电话（明文）。 */
    @Size(max = 20, message = "紧急联系电话长度不能超过 20")
    private String emergencyPhone;

    private Long idCardFileId;

    @Size(max = 500, message = "备注长度不能超过 500")
    private String remark;
}
