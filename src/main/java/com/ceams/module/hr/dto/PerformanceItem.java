package com.ceams.module.hr.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import lombok.Data;

/**
 * 绩效考评项（模板 items_json 的元素模型）。
 *
 * <p>同时作为模板保存入参与模板详情出参使用，保证落库 JSON 与前端契约只有一份定义。
 */
@Data
public class PerformanceItem {

    /** 考评项名称。 */
    @NotBlank(message = "考评项名称不能为空")
    @Size(max = 64, message = "考评项名称长度不能超过 64")
    private String name;

    /** 权重（全部考评项合计必须为 100）。 */
    @NotNull(message = "考评项权重不能为空")
    @DecimalMin(value = "0", message = "考评项权重不能为负数")
    private BigDecimal weight;

    /** 单项最低分（可空）。 */
    @DecimalMin(value = "0", message = "单项最低分不能为负数")
    private BigDecimal minScore;

    /** 单项最高分（可空）。 */
    @DecimalMax(value = "100", message = "单项最高分不能超过 100")
    private BigDecimal maxScore;

    /** 考评项说明。 */
    @Size(max = 255, message = "考评项说明长度不能超过 255")
    private String description;
}
