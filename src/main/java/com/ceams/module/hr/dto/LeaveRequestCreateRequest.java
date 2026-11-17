package com.ceams.module.hr.dto;

import com.ceams.module.hr.enums.LeaveType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;

/** 提交请假申请请求。 */
@Data
public class LeaveRequestCreateRequest {

    @NotNull(message = "假期类型不能为空")
    private LeaveType leaveType;

    @NotNull(message = "开始日期不能为空")
    private LocalDate startDate;

    @NotNull(message = "结束日期不能为空")
    private LocalDate endDate;

    /** 请假天数；为空时按起止日期（含首尾）自动计算。 */
    private BigDecimal days;

    @Size(max = 500, message = "请假事由长度不能超过 500")
    private String reason;
}
