package com.ceams.module.hr.vo;

import com.ceams.module.hr.enums.LeaveType;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;

/** 假期额度视图对象。 */
@Data
public class LeaveQuotaVO {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long employeeId;

    private Integer year;

    private LeaveType leaveType;

    private BigDecimal totalDays;

    private BigDecimal usedDays;

    private BigDecimal remainingDays;

    private LocalDate expireDate;
}
