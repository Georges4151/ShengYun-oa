package com.ceams.module.hr.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ceams.common.base.BaseEntity;
import com.ceams.common.crypto.SensitiveTypeHandler;
import com.ceams.module.hr.enums.SalaryStatus;
import java.time.LocalDate;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** HR 薪资台账（hr_salary）。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "hr_salary", autoResultMap = true)
public class HrSalary extends BaseEntity {

    /** 员工 ID（逻辑引用 hr_employee.id）。 */
    private Long employeeId;

    /** 生效日期。 */
    private LocalDate effectiveDate;

    /** 基本工资（库内加密，接口输出明文数值）。 */
    @TableField(typeHandler = SensitiveTypeHandler.class)
    private String baseSalaryEnc;

    /** 岗位工资（库内加密，接口输出明文数值）。 */
    @TableField(typeHandler = SensitiveTypeHandler.class)
    private String postSalaryEnc;

    /** 绩效基数（库内加密，接口输出明文数值）。 */
    @TableField(typeHandler = SensitiveTypeHandler.class)
    private String performanceBaseEnc;

    /** 固定补贴（库内加密，接口输出明文数值）。 */
    @TableField(typeHandler = SensitiveTypeHandler.class)
    private String allowanceEnc;

    /** 状态：ACTIVE/HISTORY；同一员工仅允许一条 ACTIVE（唯一键兜底）。 */
    private SalaryStatus status;

    /** 来源变动申请 ID（逻辑引用 hr_salary_change_apply.id）。 */
    private Long changeApplyId;

    /** 备注。 */
    private String remark;
}
