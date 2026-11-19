package com.ceams.module.bpm.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 同意任务请求。 */
@Data
public class TaskApproveRequest {

    /** 任务版本号（乐观锁）。 */
    @NotNull(message = "taskVersion 不能为空")
    private Integer taskVersion;

    /** 实例版本号（可选加固；传入时一并校验）。 */
    private Integer instanceVersion;

    /** 审批意见。 */
    @Size(max = 500, message = "审批意见长度不能超过 500")
    private String comment;
}
