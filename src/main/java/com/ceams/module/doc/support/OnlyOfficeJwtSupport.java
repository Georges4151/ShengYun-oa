package com.ceams.module.doc.support;

import com.ceams.common.exception.BusinessException;
import com.ceams.common.exception.ErrorCode;
import com.ceams.module.doc.config.OnlyOfficeProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

/**
 * OnlyOffice JWT 签发 / 校验（HS256，密钥取 {@code ceams.doc.onlyoffice.jwt-secret}）。
 *
 * <p>约定：
 *
 * <ul>
 *   <li>{@link #sign(Map)}：把任意 payload 作为 claims 签发，用于回调模拟与通用场景；
 *   <li>{@link #signConfig(Map)}：把整份编辑器 config 作为 claims 签发（OnlyOffice 8.1 的 {@code body.token} 要求）；
 *   <li>{@link #verify(String)}：校验签名与过期，非法时抛异常。
 * </ul>
 *
 * <p>密钥始终按<b>原始字符串字节</b>参与 HMAC，保证与 Document Server（Node jsonwebtoken）互通。
 * jjwt 对 HS256 强制要求密钥 ≥ 256 位，因此当配置密钥不足 32 字节时，退化到等价的 JDK HMAC 实现，
 * 避免 16 字节开发密钥（{@code ceams-dev-secret}）无法使用。密钥不写入任何日志；密钥为空时抛 503 语义异常。
 */
@Slf4j
@Component
public class OnlyOfficeJwtSupport {

    /** HMAC-SHA256 要求的最小密钥长度（字节），与 jjwt 的 HS256 校验一致。 */
    private static final int MIN_JJWT_KEY_BYTES = 32;

    /** 短密钥兼容路径的 JSON 序列化器（仅处理配置类简单结构）。 */
    private static final ObjectMapper JSON = new ObjectMapper();

    /** 需要按秒级时间戳还原为 Date 的注册声明。 */
    private static final List<String> DATE_CLAIMS = List.of("exp", "nbf", "iat");

    private final OnlyOfficeProperties properties;

    public OnlyOfficeJwtSupport(OnlyOfficeProperties properties) {
        this.properties = properties;
    }

    /** 签发 token（payload 整体作为 claims）。 */
    public String sign(Map<String, Object> payload) {
        byte[] secret = requireSecret("OnlyOffice JWT 未配置");
        Map<String, Object> claims = payload == null ? Map.of() : payload;
        if (secret.length >= MIN_JJWT_KEY_BYTES) {
            return Jwts.builder()
                    .claims(claims)
                    .signWith(secretKey(secret), Jwts.SIG.HS256)
                    .compact();
        }
        return signRaw(claims, secret);
    }

    /** 把整份编辑器 config 作为 claims 签发（OnlyOffice 8.1 要求 body.token）。 */
    public String signConfig(Map<String, Object> config) {
        return sign(config);
    }

    /** 校验 token：签名错误、格式非法或已过期统一抛 401 语义异常。 */
    public Claims verify(String token) {
        byte[] secret = requireSecret("OnlyOffice JWT 未配置");
        if (StringUtils.isBlank(token)) {
            throw invalidToken("OnlyOffice token 缺失");
        }
        if (secret.length >= MIN_JJWT_KEY_BYTES) {
            try {
                return Jwts.parser()
                        .verifyWith(secretKey(secret))
                        .build()
                        .parseSignedClaims(token)
                        .getPayload();
            } catch (JwtException | IllegalArgumentException e) {
                // 仅记录类型，不输出 token / 密钥，避免敏感信息落日志
                log.warn("OnlyOffice JWT 校验失败：{}", e.getClass().getSimpleName());
                throw invalidToken("OnlyOffice token 无效或已过期");
            }
        }
        return verifyRaw(token, secret);
    }

    // ------------------------------------------------------------------
    // 短密钥兼容路径（原始字节 HMAC，与 Document Server 完全一致）
    // ------------------------------------------------------------------

    private String signRaw(Map<String, Object> claims, byte[] secret) {
        try {
            String header = "{\"alg\":\"HS256\"}";
            String payload = JSON.writeValueAsString(claims);
            String content =
                    base64Url(header.getBytes(StandardCharsets.UTF_8))
                            + "."
                            + base64Url(payload.getBytes(StandardCharsets.UTF_8));
            return content + "." + base64Url(hmacSha256(content, secret));
        } catch (JsonProcessingException e) {
            throw new BusinessException(
                    ErrorCode.SYSTEM_INTERNAL_ERROR, "OnlyOffice token 签发失败", e);
        }
    }

    private Claims verifyRaw(String token, byte[] secret) {
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            throw invalidToken("OnlyOffice token 格式非法");
        }
        String expected = base64Url(hmacSha256(parts[0] + "." + parts[1], secret));
        if (!MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                parts[2].getBytes(StandardCharsets.UTF_8))) {
            log.warn("OnlyOffice JWT 校验失败：SignatureException");
            throw invalidToken("OnlyOffice token 签名无效");
        }
        Map<String, Object> claims = parseClaims(parts[1]);
        Object exp = claims.get("exp");
        if (exp instanceof Number number
                && number.longValue() * 1000L < System.currentTimeMillis()) {
            log.warn("OnlyOffice JWT 校验失败：ExpiredJwtException");
            throw invalidToken("OnlyOffice token 已过期");
        }
        normalizeDateClaims(claims);
        return Jwts.claims(claims);
    }

    private Map<String, Object> parseClaims(String payloadPart) {
        try {
            byte[] payload = Base64.getUrlDecoder().decode(payloadPart);
            return JSON.readValue(payload, new TypeReference<Map<String, Object>>() {});
        } catch (IOException | IllegalArgumentException e) {
            log.warn("OnlyOffice JWT 校验失败：MalformedJwtException");
            throw invalidToken("OnlyOffice token 格式非法");
        }
    }

    /** 秒级时间戳还原为 Date，保证 Claims#getExpiration 等读取正常。 */
    private void normalizeDateClaims(Map<String, Object> claims) {
        for (String name : DATE_CLAIMS) {
            Object value = claims.get(name);
            if (value instanceof Number number) {
                claims.put(name, new Date(number.longValue() * 1000L));
            }
        }
    }

    private byte[] hmacSha256(String content, byte[] secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(secretKey(secret));
            return mac.doFinal(content.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException e) {
            throw new BusinessException(ErrorCode.SYSTEM_INTERNAL_ERROR, "HMAC 计算失败", e);
        }
    }

    private String base64Url(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    // ------------------------------------------------------------------
    // 密钥与异常
    // ------------------------------------------------------------------

    private byte[] requireSecret(String message) {
        String secret = properties.getJwtSecret();
        if (StringUtils.isBlank(secret)) {
            throw BusinessException.of(ErrorCode.SYSTEM_SERVICE_UNAVAILABLE, message);
        }
        return secret.getBytes(StandardCharsets.UTF_8);
    }

    private SecretKey secretKey(byte[] secret) {
        return new SecretKeySpec(secret, "HmacSHA256");
    }

    private BusinessException invalidToken(String message) {
        return BusinessException.of(ErrorCode.AUTH_TOKEN_INVALID, message);
    }
}
