package com.ceams.module.iam.service;

import com.ceams.common.response.PageResult;
import com.ceams.module.iam.dto.auth.LoginRequest;
import com.ceams.module.iam.dto.auth.RefreshRequest;
import com.ceams.module.iam.vo.LoginResponse;
import com.ceams.module.iam.vo.SignSecretVO;
import com.ceams.module.iam.vo.UserSessionVO;
import jakarta.servlet.http.HttpServletRequest;

/** 认证服务：登录、刷新轮换、登出、会话管理。 */
public interface AuthService {

    LoginResponse login(LoginRequest request, HttpServletRequest httpRequest);

    /**
     * 企业微信扫码登录：复用账号密码登录的令牌签发、设备绑定与审计链路。
     *
     * @param code 企业微信授权码
     * @param state 扫码时下发的一次性 state（内部记录 deviceId）
     * @param deviceId 显式设备指纹（可为空，为空时取 state 中记录值）
     * @param deviceName 设备名（为空时缺省「企业微信扫码」）
     * @param httpRequest 当前请求（用于 IP 与审计）
     * @return 登录响应（access + refresh 令牌与用户信息）
     */
    LoginResponse loginByWeCom(
            String code,
            String state,
            String deviceId,
            String deviceName,
            HttpServletRequest httpRequest);

    LoginResponse refresh(RefreshRequest request);

    void logout(HttpServletRequest httpRequest);

    PageResult<UserSessionVO> sessions(long page, long size);

    void revokeSession(String sessionId);

    /** 下发指定设备的高危接口 HMAC 签名密钥（AppSecret，当前登录用户）。 */
    SignSecretVO signSecret(String deviceId);
}
