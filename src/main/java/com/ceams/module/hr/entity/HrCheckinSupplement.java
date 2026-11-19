package com.ceams.module.hr.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ceams.common.base.BaseEntity;
import com.ceams.module.hr.enums.CheckinType;
import com.ceams.module.hr.enums.HrApprovalStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** HR 补卡申请单（hr_checkin_supplement）。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("hr_checkin_supplement")
public class HrCheckinSupplement extends BaseEntity {

    /** 员工 ID（逻辑引用 hr_employee.id）。 */
    private Long employeeId;

    /** 补卡日期。 */
    private LocalDate checkinDate;

    /** 补卡时间。 */
    private LocalDateTime checkinTime;

    /** 打卡类型。 */
    private CheckinType checkinType;

    /** 补卡事由。 */
    private String reason;

    /** 审批状态（BPM 实例状态快照）。 */
    private HrApprovalStatus approvalStatus;

    /** BPM 流程实例 ID（逻辑引用 bpm_instance.id）。 */
    private Long bpmInstanceId;
}
