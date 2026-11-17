package com.ceams.module.bpm.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 改派任务请求（管理员或具备 bpm:task:delegate 权限）。 */
@Data
public class TaskDelegateRequest {

    /** 任务版本号（乐观锁）。 */
    @NotNull(message = "taskVersion 不能为空")
    private Integer taskVersion;

    /** 实例版本号（可选加固；传入时一并校验）。 */
    private Integer instanceVersion;

    /** 新处理人 ID。 */
    @NotNull(message = "目标处理人不能为空")
    private Long targetUserId;

    /** 改派原因。 */
    @Size(max = 500, message = "改派原因长度不能超过 500")
    private String reason;
}
