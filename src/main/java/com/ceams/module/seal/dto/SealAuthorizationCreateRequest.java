package com.ceams.module.seal.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import lombok.Data;

/** 新建印章授权。 */
@Data
public class SealAuthorizationCreateRequest {

    @NotNull(message = "印章ID不能为空")
    private Long sealId;

    @NotNull(message = "被授权用户ID不能为空")
    private Long userId;

    @NotNull(message = "授权开始时间不能为空")
    private LocalDateTime startTime;

    @NotNull(message = "授权结束时间不能为空")
    private LocalDateTime endTime;

    @Size(max = 255, message = "备注长度不能超过 255")
    private String remark;
}
