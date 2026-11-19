package com.ceams.module.hr.dto;

import com.ceams.module.hr.enums.ScheduleType;
import jakarta.validation.constraints.Size;
import java.time.LocalTime;
import lombok.Data;

/** 修改排班请求：字段为 null 表示不修改。 */
@Data
public class ScheduleUpdateRequest {

    @Size(max = 32, message = "班次编码长度不能超过 32")
    private String shiftCode;

    private LocalTime startTime;

    private LocalTime endTime;

    private ScheduleType scheduleType;

    @Size(max = 255, message = "备注长度不能超过 255")
    private String remark;
}
