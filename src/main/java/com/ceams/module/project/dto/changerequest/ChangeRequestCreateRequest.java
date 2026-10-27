package com.ceams.module.project.dto.changerequest;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 新建变更请求。
 */
@Data
public class ChangeRequestCreateRequest {

    @NotNull(message = "项目 ID 不能为空")
    private Long projectId;

    @NotBlank(message = "变更类型不能为空")
    @Size(max = 50, message = "变更类型长度不能超过 50")
    private String requestType;

    private String description;

    @PositiveOrZero(message = "预估成本不能为负")
    private BigDecimal estimatedCost;
}