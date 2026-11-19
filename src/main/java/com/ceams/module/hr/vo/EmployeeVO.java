package com.ceams.module.hr.vo;

import com.ceams.module.hr.enums.EmployeeStatus;
import com.ceams.module.hr.enums.EmploymentType;
import com.ceams.module.hr.enums.Gender;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

/** 员工档案视图对象（身份证 / 手机号 / 邮箱 / 紧急电话均脱敏，不返回密文）。 */
@Data
public class EmployeeVO {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;

    private String employeeNo;

    private String name;

    private Gender gender;

    private LocalDate birthDate;

    /** 身份证号（脱敏）。 */
    private String idCard;

    /** 手机号（脱敏）。 */
    private String phone;

    /** 邮箱（脱敏）。 */
    private String email;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long deptId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long positionId;

    private EmploymentType employmentType;

    private LocalDate hireDate;

    private LocalDate probationEndDate;

    private LocalDate regularDate;

    private LocalDate leaveDate;

    private EmployeeStatus status;

    private String emergencyContact;

    /** 紧急联系电话（脱敏）。 */
    private String emergencyPhone;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long idCardFileId;

    private String remark;

    private LocalDateTime createTime;
}
