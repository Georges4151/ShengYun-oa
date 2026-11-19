package com.ceams.module.hr.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.Data;

/** 绩效打分项（自评 / 上级评分明细 JSON 的元素模型）：{name,weight,score}。 */
@Data
public class PerformanceScoreItem {

    /** 考评项名称（必须与模板一致）。 */
    @NotBlank(message = "评分项名称不能为空")
    private String name;

    /** 权重（必须与模板一致，服务层二次校验防止篡改）。 */
    @NotNull(message = "评分项权重不能为空")
    @DecimalMin(value = "0", message = "评分项权重不能为负数")
    private BigDecimal weight;

    /** 得分（0~100）。 */
    @NotNull(message = "评分不能为空")
    @DecimalMin(value = "0", message = "评分不能为负数")
    @DecimalMax(value = "100", message = "评分不能超过 100")
    private BigDecimal score;
}
