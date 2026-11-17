package com.ceams.module.integration.service;

import com.ceams.module.iam.entity.SysUser;

/**
 * 企业微信扫码登录的解析结果。
 *
 * <p>{@code deviceId} 来自一次性 state 中记录的设备指纹，供 {@code AuthService} 复用统一的设备绑定
 * 逻辑；为空表示本次扫码未携带设备标识，由调用方决定兜底设备号。
 *
 * @param user 已匹配并校验状态的本地用户
 * @param deviceId 扫码发起时记录的设备指纹（可为 null）
 */
public record WeComLoginResult(SysUser user, String deviceId) {}
