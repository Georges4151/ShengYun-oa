package com.ceams.module.hr.vo;

import com.ceams.module.hr.enums.ScheduleType;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.time.LocalDate;
import java.time.LocalTime;
import lombok.Data;

/** 排班视图对象。 */
@Data
public class ScheduleVO {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long employeeId;

    private LocalDate workDate;

    private String shiftCode;

    private LocalTime startTime;

    private LocalTime endTime;

    private ScheduleType scheduleType;

    private String remark;
}
