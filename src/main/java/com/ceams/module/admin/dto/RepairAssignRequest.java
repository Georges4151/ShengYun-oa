package com.ceams.module.admin.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** 报修派单请求。 */
@Data
public class RepairAssignRequest {

    /** 维修人 ID。 */
    @NotNull(message = "维修人不能为空")
    private Long assigneeId;

    /** 派单备注。 */
    private String remark;
}
