package com.ceams.module.bpm.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import lombok.Data;

/** 新建用户代理请求（委托人自助设置）。 */
@Data
public class DelegateCreateRequest {

    /** 代理人 ID。 */
    @NotNull(message = "代理人不能为空")
    private Long delegateId;

    /** 适用范围：为空表示全部业务。 */
    @Size(max = 64, message = "业务类型长度不能超过 64")
    private String bizType;

    /** 代理开始时间。 */
    @NotNull(message = "开始时间不能为空")
    private LocalDateTime startTime;

    /** 代理结束时间。 */
    @NotNull(message = "结束时间不能为空")
    private LocalDateTime endTime;

    /** 代理原因。 */
    @Size(max = 255, message = "代理原因长度不能超过 255")
    private String reason;
}
