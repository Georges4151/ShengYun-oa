package com.ceams.module.project.dto.changerequest;

import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 更新变更请求（仅草稿/驳回态可编辑）。
 */
@Data
public class ChangeRequestUpdateRequest {

    @Size(max = 50, message = "变更类型长度不能超过 50")
    private String requestType;

    private String description;

    @PositiveOrZero(message = "预估成本不能为负")
    private BigDecimal estimatedCost;
}