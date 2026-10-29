package com.ceams.module.project.dto.changerequest;

import com.ceams.module.project.enums.ApprovalStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import lombok.Data;

/** 新建变更请求。 */
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

    /** 审批状态（可选，默认 PENDING；DRAFT 表示保存草稿）。 */
    private ApprovalStatus status;
}
