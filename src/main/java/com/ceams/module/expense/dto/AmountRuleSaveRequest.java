package com.ceams.module.expense.dto;

import com.ceams.module.expense.enums.AmountRuleStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import lombok.Data;

/**
 * 新增 / 修改报销额度分级规则请求。
 *
 * <p>PUT 为全量覆盖语义：未传 priority 时按 0 保存，未传 status 时按 ENABLED 保存，
 * deptId / projectId 传空即表示「不限部门 / 不限项目」。
 */
@Data
public class AmountRuleSaveRequest {

    @NotBlank(message = "规则名称不能为空")
    @Size(max = 100, message = "规则名称长度不能超过 100")
    private String name;

    @NotNull(message = "金额下限不能为空")
    @DecimalMin(value = "0", message = "金额下限不能为负")
    private BigDecimal minAmount;

    /** 金额上限（不含），可为空表示无上限；不为空时必须大于金额下限。 */
    private BigDecimal maxAmount;

    /** 适用部门 ID（可空，匹配时含子部门）。 */
    private Long deptId;

    /** 适用项目 ID（可空）。 */
    private Long projectId;

    @NotBlank(message = "流程模板编码不能为空")
    @Size(max = 64, message = "流程模板编码长度不能超过 64")
    private String templateCode;

    /** 优先级，默认 0。 */
    private Integer priority;

    /** 状态，默认 ENABLED。 */
    private AmountRuleStatus status;

    @Size(max = 255, message = "备注长度不能超过 255")
    private String remark;
}
