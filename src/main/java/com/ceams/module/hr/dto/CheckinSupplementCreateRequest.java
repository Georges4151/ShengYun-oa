package com.ceams.module.hr.dto;

import com.ceams.module.hr.enums.CheckinType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

/** 提交补卡申请请求。 */
@Data
public class CheckinSupplementCreateRequest {

    @NotNull(message = "补卡日期不能为空")
    private LocalDate checkinDate;

    @NotNull(message = "补卡时间不能为空")
    private LocalDateTime checkinTime;

    @NotNull(message = "打卡类型不能为空")
    private CheckinType checkinType;

    @Size(max = 500, message = "补卡事由长度不能超过 500")
    private String reason;
}
