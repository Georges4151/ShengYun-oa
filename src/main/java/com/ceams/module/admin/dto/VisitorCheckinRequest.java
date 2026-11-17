package com.ceams.module.admin.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** 访客二维码核销请求。 */
@Data
public class VisitorCheckinRequest {

    /** 一次性核销令牌。 */
    @NotBlank(message = "核销令牌不能为空")
    private String token;
}
