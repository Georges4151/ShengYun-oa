package com.ceams.module.integration.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 企业微信扫码登录请求（授权码 + 一次性 state）。 */
@Data
public class WeComLoginRequest {

    /** 企业微信回调带回的授权码。 */
    @NotBlank(message = "企业微信授权码不能为空")
    @Size(max = 512, message = "企业微信授权码长度不能超过 512")
    private String code;

    /** 扫码时下发的一次性 state。 */
    @NotBlank(message = "state 不能为空")
    @Size(max = 64, message = "state 长度不能超过 64")
    private String state;
}
