package com.ceams.module.bpm.dto;

import com.ceams.module.bpm.enums.RejectTarget;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 驳回任务请求。 */
@Data
public class TaskRejectRequest {

    /** 任务版本号（乐观锁）。 */
    @NotNull(message = "taskVersion 不能为空")
    private Integer taskVersion;

    /** 实例版本号（可选加固；传入时一并校验）。 */
    private Integer instanceVersion;

    /** 驳回目标：发起人 / 上一级。 */
    @NotNull(message = "驳回目标不能为空")
    private RejectTarget target;

    /** 驳回原因。 */
    @Size(max = 500, message = "驳回原因长度不能超过 500")
    private String reason;
}
