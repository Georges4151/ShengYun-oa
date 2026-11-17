package com.ceams.module.hr.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Data;

/** 绩效上级打分请求。 */
@Data
public class PerformanceLeaderRequest {

    /** 上级打分项（必须覆盖模板全部考评项）。 */
    @NotEmpty(message = "上级打分项不能为空")
    @Valid
    private List<PerformanceScoreItem> items;

    /** 上级评语。 */
    @Size(max = 1000, message = "上级评语长度不能超过 1000")
    private String comment;
}
