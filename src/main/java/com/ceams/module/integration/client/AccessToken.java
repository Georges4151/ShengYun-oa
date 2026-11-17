package com.ceams.module.integration.client;

/**
 * 企业微信 access_token。
 *
 * @param token 令牌字符串
 * @param expiresIn 有效期（秒）
 */
public record AccessToken(String token, int expiresIn) {}
