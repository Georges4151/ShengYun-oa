package com.ceams.module.iam.dto.auth;

import com.ceams.module.iam.enums.Platform;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 账号密码登录请求。
 */
@Data
public class LoginRequest {

    @NotBlank(message = "用户名不能为空")
    @Size(max = 50, message = "用户名长度不能超过 50")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(max = 128, message = "密码长度不能超过 128")
    private String password;

    /** 设备指纹（可选）。 */
    private String deviceId;

    /** 设备名（可选）。 */
    private String deviceName;

    /** 平台（可选）。 */
    private Platform platform;
}