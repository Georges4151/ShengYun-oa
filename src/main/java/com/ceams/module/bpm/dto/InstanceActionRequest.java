package com.ceams.module.bpm.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 实例动作请求（撤回/终止）。 */
@Data
public class InstanceActionRequest {

    /** 实例版本号（乐观锁）。 */
    @NotNull(message = "instanceVersion 不能为空")
    private Integer instanceVersion;

    /** 原因说明。 */
    @Size(max = 500, message = "原因长度不能超过 500")
    private String reason;
}
