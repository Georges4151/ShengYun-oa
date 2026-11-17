package com.ceams.module.hr.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ceams.common.base.BaseEntity;
import com.ceams.module.hr.enums.LeaveType;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** HR 假期额度（hr_leave_quota）。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("hr_leave_quota")
public class HrLeaveQuota extends BaseEntity {

    /** 员工 ID（逻辑引用 hr_employee.id）。 */
    private Long employeeId;

    /** 额度年度。 */
    private Integer year;

    /** 假期类型。 */
    private LeaveType leaveType;

    /** 总额度（天）。 */
    private BigDecimal totalDays;

    /** 已使用（天）。 */
    private BigDecimal usedDays;

    /** 剩余额度（天）。 */
    private BigDecimal remainingDays;

    /** 额度失效日期。 */
    private LocalDate expireDate;
}
