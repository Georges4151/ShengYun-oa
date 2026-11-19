package com.ceams.module.integration.controller;

import com.ceams.common.response.ApiResponse;
import com.ceams.module.iam.service.AuthService;
import com.ceams.module.iam.vo.LoginResponse;
import com.ceams.module.integration.dto.WeComLoginRequest;
import com.ceams.module.integration.service.WeComLoginService;
import com.ceams.module.integration.vo.WeComQrCodeVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 企业微信扫码登录接口（匿名，无需 @PreAuthorize）。
 *
 * <p>路径落在 {@code /api/v1/auth/**} 下，由 SecurityConfig 既有放行规则覆盖；Controller 仅做参数绑定，
 * state 校验、成员匹配、令牌签发与设备绑定全部在服务层完成。
 */
@RestController
@RequestMapping("/api/v1/auth/wecom")
@RequiredArgsConstructor
public class WeComLoginController {

    private final WeComLoginService weComLoginService;
    private final AuthService authService;

    /** 生成扫码登录地址与一次性 state。 */
    @GetMapping("/qrcode")
    public ApiResponse<WeComQrCodeVO> qrCode(
            @RequestParam(value = "deviceId", required = false) String deviceId) {
        return ApiResponse.success(weComLoginService.qrCode(deviceId));
    }

    /** 授权码换令牌：deviceId 由一次性 state 决定。 */
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(
            @Valid @RequestBody WeComLoginRequest request, HttpServletRequest httpRequest) {
        return ApiResponse.success(
                authService.loginByWeCom(
                        request.getCode(), request.getState(), null, null, httpRequest));
    }
}
