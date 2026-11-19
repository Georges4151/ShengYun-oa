package com.ceams.module.bank.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** 触发自动对账。 */
@Data
public class ReconciliationMatchRequest {

    @NotNull(message = "付款账户ID不能为空")
    private Long accountId;

    /** 日期容忍天数（金额相等 + 日期±N 天内的记录视为候选），默认 3。 */
    @Min(value = 0, message = "日期容忍天数不能为负")
    private Integer daysTolerance;
}
