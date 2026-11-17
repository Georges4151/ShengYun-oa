package com.ceams.module.hr.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ceams.common.base.BaseEntity;
import com.ceams.module.hr.enums.ScheduleType;
import java.time.LocalDate;
import java.time.LocalTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** HR 排班（hr_schedule）。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("hr_schedule")
public class HrSchedule extends BaseEntity {

    /** 员工 ID（逻辑引用 hr_employee.id）。 */
    private Long employeeId;

    /** 排班日期。 */
    private LocalDate workDate;

    /** 班次编码。 */
    private String shiftCode;

    /** 上班时间。 */
    private LocalTime startTime;

    /** 下班时间。 */
    private LocalTime endTime;

    /** 排班类型。 */
    private ScheduleType scheduleType;

    /** 备注。 */
    private String remark;
}
