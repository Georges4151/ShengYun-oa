package com.ceams.module.hr.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ceams.common.base.BaseEntity;
import com.ceams.common.crypto.SensitiveTypeHandler;
import com.ceams.module.hr.enums.EmployeeStatus;
import com.ceams.module.hr.enums.EmploymentType;
import com.ceams.module.hr.enums.Gender;
import java.time.LocalDate;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** HR 员工档案（hr_employee）。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "hr_employee", autoResultMap = true)
public class HrEmployee extends BaseEntity {

    /** 绑定系统用户 ID（逻辑引用 sys_user.id，唯一，可空）。 */
    private Long userId;

    /** 员工工号（唯一）。 */
    private String employeeNo;

    /** 姓名。 */
    private String name;

    /** 性别。 */
    private Gender gender;

    /** 出生日期。 */
    private LocalDate birthDate;

    /** 身份证号（库内加密，接口脱敏）。 */
    @TableField(typeHandler = SensitiveTypeHandler.class)
    private String idCardEnc;

    /** 手机号（库内加密，接口脱敏）。 */
    @TableField(typeHandler = SensitiveTypeHandler.class)
    private String phoneEnc;

    /** 邮箱（接口脱敏）。 */
    private String email;

    /** 所属部门 ID（逻辑引用 sys_dept.id）。 */
    private Long deptId;

    /** 岗位 ID（逻辑引用 hr_position.id）。 */
    private Long positionId;

    /** 用工类型。 */
    private EmploymentType employmentType;

    /** 入职日期。 */
    private LocalDate hireDate;

    /** 试用期结束日期。 */
    private LocalDate probationEndDate;

    /** 转正日期。 */
    private LocalDate regularDate;

    /** 离职日期。 */
    private LocalDate leaveDate;

    /** 员工状态。 */
    private EmployeeStatus status;

    /** 紧急联系人。 */
    private String emergencyContact;

    /** 紧急联系电话（库内加密，接口脱敏）。 */
    @TableField(typeHandler = SensitiveTypeHandler.class)
    private String emergencyPhoneEnc;

    /** 身份证附件 ID。 */
    private Long idCardFileId;

    /** 备注。 */
    private String remark;
}
