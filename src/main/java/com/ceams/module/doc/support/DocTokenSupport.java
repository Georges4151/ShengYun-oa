package com.ceams.module.doc.support;

import com.ceams.common.exception.BusinessException;
import com.ceams.common.exception.ErrorCode;
import com.ceams.module.doc.config.DocProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 文档短时令牌签发 / 解析（独立于登录用的 {@code JwtUtil}，不修改其实现）。
 *
 * <p>签名密钥复用 {@code ceams.jwt.secret}，claims 约定：
 *
 * <ul>
 *   <li>{@code typ=doc}：区分登录 token；
 *   <li>{@code scope=download | onlyoffice-file}：用途隔离；
 *   <li>{@code fid}：文件 ID，{@code ver}：版本号；
 *   <li>{@code sub}：签发用户 ID（onlyoffice-file 为系统签发时可为 0）；{@code jti}：一次性标识。
 * </ul>
 *
 * <p>本类只负责签名与过期校验，<b>不负责</b> nonce 判重；一次性由下载服务用 Redis 记录 {@code doc:token:used:{jti}}。
 */
@Component
public class DocTokenSupport {

    /** 令牌类型 claim。 */
    public static final String CLAIM_TYPE = "typ";

    /** 用途 claim。 */
    public static final String CLAIM_SCOPE = "scope";

    /** 文件 ID claim。 */
    public static final String CLAIM_FILE_ID = "fid";

    /** 版本号 claim。 */
    public static final String CLAIM_VERSION_NO = "ver";

    /** 令牌类型值：文档令牌。 */
    public static final String TYPE_DOC = "doc";

    /** 用途：一次性下载。 */
    public static final String SCOPE_DOWNLOAD = "download";

    /** 用途：OnlyOffice 拉取原件（P4 使用）。 */
    public static final String SCOPE_ONLYOFFICE_FILE = "onlyoffice-file";

    private final SecretKey key;

    private final DocProperties docProperties;

    public DocTokenSupport(
            @Value("${ceams.jwt.secret}") String secret, DocProperties docProperties) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.docProperties = docProperties;
    }

    /** 签发一次性下载令牌（300s，含 userId / fileId / versionNo / jti）。 */
    public String signDownload(Long fileId, Long userId, int versionNo) {
        return sign(fileId, userId, versionNo, SCOPE_DOWNLOAD);
    }

    /** 签发 OnlyOffice 拉取原件令牌（P4 使用，无用户上下文时 userId 可传 0）。 */
    public String signOnlyOfficeFile(Long fileId, int versionNo) {
        return sign(fileId, 0L, versionNo, SCOPE_ONLYOFFICE_FILE);
    }

    /**
     * 解析并校验令牌：签名错误、格式非法或已过期统一抛 401 语义的 {@link BusinessException}。
     */
    public Claims parse(String token) {
        if (StringUtils.isBlank(token)) {
            throw BusinessException.of(ErrorCode.AUTH_TOKEN_INVALID, "缺少下载令牌");
        }
        try {
            return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            throw BusinessException.of(ErrorCode.AUTH_TOKEN_INVALID, "下载令牌无效或已过期，请重新获取");
        }
    }

    /** 是否文档令牌（{@code typ=doc}）。 */
    public boolean isDocToken(Claims claims) {
        return claims != null && TYPE_DOC.equals(claims.get(CLAIM_TYPE, String.class));
    }

    /** 用途是否匹配。 */
    public boolean hasScope(Claims claims, String scope) {
        return claims != null && scope != null && scope.equals(claims.get(CLAIM_SCOPE, String.class));
    }

    /** 取文件 ID（兼容数字被反序列化为 Integer 的情况）。 */
    public Long fileIdOf(Claims claims) {
        return claims == null ? null : toLong(claims.get(CLAIM_FILE_ID));
    }

    /** 取版本号。 */
    public Integer versionNoOf(Claims claims) {
        Long version = claims == null ? null : toLong(claims.get(CLAIM_VERSION_NO));
        return version == null ? null : version.intValue();
    }

    /** 取签发用户 ID（仅用于审计展示，非鉴权依据）。 */
    public Long userIdOf(Claims claims) {
        if (claims == null || StringUtils.isBlank(claims.getSubject())) {
            return null;
        }
        return toLong(claims.getSubject());
    }

    /** 令牌有效期（秒），供 Redis 一次性标记设置相同 TTL。 */
    public int ttlSeconds() {
        return docProperties.getDownloadTokenTtlSeconds();
    }

    private String sign(Long fileId, Long userId, int versionNo, String scope) {
        if (fileId == null) {
            throw BusinessException.of(ErrorCode.VALID_REQUIRED_FIELD, "文件 ID 不能为空");
        }
        Date now = new Date();
        long ttlMillis = docProperties.getDownloadTokenTtlSeconds() * 1000L;
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(String.valueOf(userId == null ? 0L : userId))
                .claim(CLAIM_TYPE, TYPE_DOC)
                .claim(CLAIM_SCOPE, scope)
                .claim(CLAIM_FILE_ID, fileId)
                .claim(CLAIM_VERSION_NO, versionNo)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + ttlMillis))
                .signWith(key)
                .compact();
    }

    /** 数字 / 字符串 claim 统一转 Long，无法解析返回 null。 */
    private Long toLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text && StringUtils.isNotBlank(text)) {
            try {
                return Long.parseLong(text.trim());
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
}
