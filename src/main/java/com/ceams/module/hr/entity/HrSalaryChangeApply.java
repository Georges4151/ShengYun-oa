package com.ceams.module.hr.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ceams.common.base.BaseEntity;
import com.ceams.module.hr.enums.SalaryChangeStatus;
import com.ceams.module.hr.enums.SalaryChangeType;
import java.time.LocalDate;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** HR 薪资变动申请单（hr_salary_change_apply）。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("hr_salary_change_apply")
public class HrSalaryChangeApply extends BaseEntity {

    /** 目标员工 ID（逻辑引用 hr_employee.id）。 */
    private Long employeeId;

    /** 发起人 ID（逻辑引用 sys_user.id）。 */
    private Long applicantId;

    /** 变动类型。 */
    private SalaryChangeType changeType;

    /** 变更前快照 JSON（金额字段为服务端密文，禁止明文落库）。 */
    private String beforeSnapshot;

    /** 变更后快照 JSON（金额字段为服务端密文，禁止明文落库）。 */
    private String afterSnapshot;

    /** 期望生效日期。 */
    private LocalDate effectiveDate;

    /** 申请原因。 */
    private String reason;

    /** BPM 流程实例 ID（逻辑引用 bpm_instance.id）。 */
    private Long bpmInstanceId;

    /** 申请状态（BPM 终态快照）。 */
    private SalaryChangeStatus status;
}
