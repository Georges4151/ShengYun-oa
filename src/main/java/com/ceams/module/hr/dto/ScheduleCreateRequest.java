package com.ceams.module.hr.dto;

import com.ceams.module.hr.enums.ScheduleType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalTime;
import lombok.Data;

/** 新建排班请求。 */
@Data
public class ScheduleCreateRequest {

    @NotNull(message = "员工ID不能为空")
    private Long employeeId;

    @NotNull(message = "排班日期不能为空")
    private LocalDate workDate;

    @Size(max = 32, message = "班次编码长度不能超过 32")
    private String shiftCode;

    private LocalTime startTime;

    private LocalTime endTime;

    /** 排班类型，默认 FIXED。 */
    private ScheduleType scheduleType;

    @Size(max = 255, message = "备注长度不能超过 255")
    private String remark;
}
