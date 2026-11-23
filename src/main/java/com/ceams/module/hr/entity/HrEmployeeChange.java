package com.ceams.module.hr.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ceams.common.base.BaseEntity;
import com.ceams.module.hr.enums.ChangeType;
import com.ceams.module.hr.enums.HrApprovalStatus;
import java.time.LocalDate;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** HR 入转调离申请单（hr_employee_change）。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("hr_employee_change")
public class HrEmployeeChange extends BaseEntity {

    /** 员工 ID（逻辑引用 hr_employee.id）。 */
    private Long employeeId;

    /** 申请类型。 */
    private ChangeType changeType;

    /** 调岗目标部门 ID。 */
    private Long targetDeptId;

    /** 调岗目标岗位 ID。 */
    private Long targetPositionId;

    /** 晋升目标职级 ID（PROMOTION 使用）。 */
    private Long targetLevelId;

    /** 离职时下属转交的上级员工 ID（RESIGN 使用）。 */
    private Long resignLeaderId;

    /** 生效日期。 */
    private LocalDate effectiveDate;

    /** 申请原因。 */
    private String reason;

    /** 审批状态（BPM 实例状态快照）。 */
    private HrApprovalStatus approvalStatus;

    /** BPM 流程实例 ID（逻辑引用 bpm_instance.id）。 */
    private Long bpmInstanceId;

    /** 备注。 */
    private String remark;
}
