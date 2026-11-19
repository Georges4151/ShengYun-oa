package com.ceams.module.hr.dto;

import com.ceams.module.hr.enums.PeriodType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Data;

/** 绩效模板新建 / 修改请求。 */
@Data
public class PerformanceTemplateSaveRequest {

    /** 模板编码（唯一）。 */
    @NotBlank(message = "模板编码不能为空")
    @Size(max = 64, message = "模板编码长度不能超过 64")
    private String code;

    /** 模板名称。 */
    @NotBlank(message = "模板名称不能为空")
    @Size(max = 128, message = "模板名称长度不能超过 128")
    private String name;

    /** 适用岗位 ID（为空表示通用）。 */
    private Long positionId;

    /** 考核周期类型。 */
    @NotNull(message = "考核周期类型不能为空")
    private PeriodType periodType;

    /** 考评项（权重合计必须为 100）。 */
    @NotEmpty(message = "考评项不能为空")
    @Valid
    private List<PerformanceItem> items;

    /** 系数规则（区间连续且不重叠，首尾覆盖 0~100）。 */
    @NotEmpty(message = "系数规则不能为空")
    @Valid
    private List<PerformanceScoreRule> scoreRules;

    /** 状态：ENABLED/DISABLED，为空时新建默认 ENABLED、修改保持原值。 */
    private String status;
}
