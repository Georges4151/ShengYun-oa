package com.ceams.module.hr.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import lombok.Data;

/** 入转调离申请请求（转正 / 调岗 / 离职共用）。 */
@Data
public class EmployeeChangeRequest {

    @NotNull(message = "生效日期不能为空")
    private LocalDate effectiveDate;

    @Size(max = 500, message = "申请原因长度不能超过 500")
    private String reason;

    /** 调岗目标部门 ID（仅调岗使用）。 */
    private Long targetDeptId;

    /** 调岗目标岗位 ID（仅调岗使用）。 */
    private Long targetPositionId;

    @Size(max = 500, message = "备注长度不能超过 500")
    private String remark;
}
