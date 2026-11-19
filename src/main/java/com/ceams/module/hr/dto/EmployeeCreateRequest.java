package com.ceams.module.hr.dto;

import com.ceams.module.hr.enums.EmployeeStatus;
import com.ceams.module.hr.enums.EmploymentType;
import com.ceams.module.hr.enums.Gender;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import lombok.Data;

/** 新建员工档案请求（敏感字段明文传入，落库由 TypeHandler 加密）。 */
@Data
public class EmployeeCreateRequest {

    /** 绑定系统用户 ID（可空，为空表示暂不绑定账号）。 */
    private Long userId;

    @NotBlank(message = "员工工号不能为空")
    @Size(max = 64, message = "员工工号长度不能超过 64")
    private String employeeNo;

    @NotBlank(message = "姓名不能为空")
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
