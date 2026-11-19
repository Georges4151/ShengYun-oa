package com.ceams.common.sign;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * HMAC-SHA256 请求签名校验骨架。
 *
 * <p>高危接口（seal.stamp / expense.approve / payment.execute 等）在 v1 阶段预留，后续阶段启用。 签名约定：{@code X-Sign =
 * HMAC-SHA256(X-Timestamp + X-Nonce + bodyHash)}， bodyHash 为请求体 SHA-256 的十六进制小写。
 *
 * <p>服务端校验步骤：
 *
 * <ol>
 *   <li>校验 {@code X-Timestamp} 与服务器时间差在允许窗口内。
 *   <li>校验 {@code X-Nonce} 未重复出现（防重放，由 {@link NonceStore} 实现）。
 *   <li>以共享 secret 重算签名并做常量时间比较。
 * </ol>
 *
 * <p>本类保持纯函数职责：签名计算、时间窗判断、常量时间比较；nonce 防重放编排逻辑在 {@link HmacSignatureFilter} 中完成。
 */
public final class HmacSignUtil {

    /** 签名时间戳允许偏差（毫秒），默认 ±5 分钟。 */
    public static final long DEFAULT_WINDOW_MILLIS = 5 * 60 * 1000L;

    private static final String HMAC_SHA256 = "HmacSHA256";

    private HmacSignUtil() {}

    /** 请求体 SHA-256 摘要（十六进制小写）。 */
    public static String sha256Hex(String body) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(body.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    /** 计算 HMAC-SHA256 签名。 */
    public static String sign(String timestamp, String nonce, String bodyHash, String secret) {
        try {
            String payload = timestamp + nonce + bodyHash;
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256));
            byte[] raw = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(raw);
        } catch (Exception e) {
            throw new IllegalStateException("HMAC-SHA256 sign failed", e);
        }
    }

    /** 时间戳是否落在允许窗口内。 */
    public static boolean withinWindow(
            long timestampMillis, long serverTimeMillis, long windowMillis) {
        return Math.abs(serverTimeMillis - timestampMillis) <= windowMillis;
    }

    /** 常量时间比较，避免时序侧信道。 */
    public static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        return MessageDigest.isEqual(
                a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }
}
