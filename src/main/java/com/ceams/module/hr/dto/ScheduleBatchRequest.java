package com.ceams.module.hr.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import lombok.Data;

/** 批量排班请求：按员工 × 日期区间逐日 upsert。 */
@Data
public class ScheduleBatchRequest {

    @NotEmpty(message = "员工列表不能为空")
    private List<Long> employeeIds;

    @NotNull(message = "开始日期不能为空")
    private LocalDate startDate;

    @NotNull(message = "结束日期不能为空")
    private LocalDate endDate;

    @Size(max = 32, message = "班次编码长度不能超过 32")
    private String shiftCode;

    private LocalTime startTime;

    private LocalTime endTime;
}
