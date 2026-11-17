package com.ceams.module.hr.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import lombok.Data;

/** HR 绩效复核定稿请求。 */
@Data
public class PerformanceReviewRequest {

    /** 定稿分数（0~100）。 */
    @NotNull(message = "定稿分数不能为空")
    @DecimalMin(value = "0", message = "定稿分数不能为负数")
    @DecimalMax(value = "100", message = "定稿分数不能超过 100")
    private BigDecimal finalScore;

    /** 复核意见（追加到上级评语后）。 */
    @Size(max = 500, message = "复核意见长度不能超过 500")
    private String comment;
}
