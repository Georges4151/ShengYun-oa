package com.ceams.module.hr.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Data;

/** 绩效自评提交请求（提交即启动 BPM）。 */
@Data
public class PerformanceSelfRequest {

    /** 自评打分项（必须覆盖模板全部考评项）。 */
    @NotEmpty(message = "自评打分项不能为空")
    @Valid
    private List<PerformanceScoreItem> items;

    /** 自评说明。 */
    @Size(max = 1000, message = "自评说明长度不能超过 1000")
    private String comment;
}
