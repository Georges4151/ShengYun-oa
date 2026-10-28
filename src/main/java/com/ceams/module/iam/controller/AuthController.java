package com.ceams.module.iam.controller;

import com.ceams.common.response.ApiResponse;
import com.ceams.common.response.PageResult;
import com.ceams.module.iam.dto.auth.LoginRequest;
import com.ceams.module.iam.dto.auth.RefreshRequest;
import com.ceams.module.iam.service.AuthService;
import com.ceams.module.iam.vo.LoginResponse;
import com.ceams.module.iam.vo.SignSecretVO;
import com.ceams.module.iam.vo.UserSessionVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证接口：登录 / 刷新轮换 / 登出 / 会话管理。
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        return ApiResponse.success(authService.login(request, httpRequest));
    }

    @PostMapping("/refresh")
    public ApiResponse<LoginResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ApiResponse.success(authService.refresh(request));
    }

    @PostMapping("/logout")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Void> logout(HttpServletRequest httpRequest) {
        authService.logout(httpRequest);
        return ApiResponse.success();
    }

    @GetMapping("/sign-secret")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<SignSecretVO> signSecret(@RequestParam("deviceId") String deviceId) {
        return ApiResponse.success(authService.signSecret(deviceId));
    }

    @GetMapping("/sessions")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<PageResult<UserSessionVO>> sessions(@RequestParam(defaultValue = "1") long page,
                                                           @RequestParam(defaultValue = "20") long size) {
        return ApiResponse.success(authService.sessions(page, size));
    }

    @DeleteMapping("/sessions/{sessionId}/revoke")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Void> revokeSession(@PathVariable String sessionId) {
        authService.revokeSession(sessionId);
        return ApiResponse.success();
    }
}