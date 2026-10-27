package com.ceams.module.iam.service;

import com.ceams.common.response.PageResult;
import com.ceams.module.iam.dto.auth.LoginRequest;
import com.ceams.module.iam.dto.auth.RefreshRequest;
import com.ceams.module.iam.vo.LoginResponse;
import com.ceams.module.iam.vo.UserSessionVO;
import jakarta.servlet.http.HttpServletRequest;

/**
 * 认证服务：登录、刷新轮换、登出、会话管理。
 */
public interface AuthService {

    LoginResponse login(LoginRequest request, HttpServletRequest httpRequest);

    LoginResponse refresh(RefreshRequest request);

    void logout(HttpServletRequest httpRequest);

    PageResult<UserSessionVO> sessions(long page, long size);

    void revokeSession(String sessionId);
}