package com.ceams.module.hr.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ceams.common.base.BaseEntity;
import com.ceams.module.hr.enums.HrApprovalStatus;
import com.ceams.module.hr.enums.LeaveType;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** HR 请假申请单（hr_leave_request）。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("hr_leave_request")
public class HrLeaveRequest extends BaseEntity {

    /** 员工 ID（逻辑引用 hr_employee.id）。 */
    private Long employeeId;

    /** 假期类型。 */
    private LeaveType leaveType;

    /** 请假开始日期。 */
    private LocalDate startDate;

    /** 请假结束日期。 */
    private LocalDate endDate;

    /** 请假天数。 */
    private BigDecimal days;

    /** 请假事由。 */
    private String reason;

    /** 审批状态（BPM 实例状态快照）。 */
    private HrApprovalStatus approvalStatus;

    /** BPM 流程实例 ID（逻辑引用 bpm_instance.id）。 */
    private Long bpmInstanceId;
}
