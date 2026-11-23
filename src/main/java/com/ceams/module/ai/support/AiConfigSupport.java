package com.ceams.module.ai.support;

import java.net.URI;
import org.springframework.util.StringUtils;

/**
 * AI 配置通用工具：Base64 地址归一化、http/https 校验、日志脱敏与 API Key 掩码。
 *
 * <p>安全约定：
 *
 * <ul>
 *   <li>服务地址只允许 http/https；
 *   <li>日志中的地址必须去掉 query/fragment，避免把临时签名参数写进日志；
 *   <li>API Key 永远不输出明文，仅返回 {@code 前缀 + **** + 后四位} 的掩码串。
 * </ul>
 */
public final class AiConfigSupport {

    /** API Key 掩码中间固定片段。 */
    private static final String MASK = "****";

    /** 掩码保留的前缀长度。 */
    private static final int PREFIX_LENGTH = 3;

    /** 掩码保留的后缀长度。 */
    private static final int SUFFIX_LENGTH = 4;

    private AiConfigSupport() {}

    /**
     * 归一化服务地址：去首尾空白并删除末尾多余斜杠。
     *
     * @param baseUrl 原始地址
     * @return 归一化地址；空值返回 null
     */
    public static String normalizeBaseUrl(String baseUrl) {
        if (!StringUtils.hasText(baseUrl)) {
            return null;
        }
        String trimmed = baseUrl.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }

    /**
     * 校验是否为 http/https 地址。
     *
     * @param baseUrl 原始地址
     * @return true 表示协议合法且包含主机
     */
    public static boolean isHttpUrl(String baseUrl) {
        String normalized = normalizeBaseUrl(baseUrl);
        if (normalized == null) {
            return false;
        }
        if (!normalized.startsWith("http://") && !normalized.startsWith("https://")) {
            return false;
        }
        try {
            URI uri = URI.create(normalized);
            return StringUtils.hasText(uri.getHost());
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * 生成可安全写入日志的地址：只保留协议、主机、端口与路径。
     *
     * @param baseUrl 原始地址
     * @return 脱敏地址；非法地址返回 {@code invalid-url}
     */
    public static String sanitizeUrl(String baseUrl) {
        String normalized = normalizeBaseUrl(baseUrl);
        if (normalized == null) {
            return "";
        }
        try {
            URI uri = URI.create(normalized);
            String scheme = uri.getScheme();
            String host = uri.getHost();
            if (scheme == null || host == null) {
                return "invalid-url";
            }
            StringBuilder safe = new StringBuilder(scheme).append("://").append(host);
            if (uri.getPort() > 0) {
                safe.append(':').append(uri.getPort());
            }
            if (StringUtils.hasText(uri.getPath())) {
                safe.append(uri.getPath());
            }
            return safe.toString();
        } catch (IllegalArgumentException e) {
            return "invalid-url";
        }
    }

    /**
     * API Key 掩码：保留前 3 位与后 4 位，中间固定 {@code ****}。
     *
     * <p>示例：{@code sk-abcdefgh -> sk-****efgh}。
     *
     * @param apiKey 明文 Key
     * @return 掩码串；空值返回 null
     */
    public static String maskApiKey(String apiKey) {
        if (!StringUtils.hasText(apiKey)) {
            return null;
        }
        String trimmed = apiKey.trim();
        int length = trimmed.length();
        if (length <= PREFIX_LENGTH + SUFFIX_LENGTH) {
            return MASK + (length > SUFFIX_LENGTH ? trimmed.substring(length - SUFFIX_LENGTH) : "");
        }
        String prefix = trimmed.substring(0, PREFIX_LENGTH);
        String suffix = trimmed.substring(length - SUFFIX_LENGTH);
        return prefix + MASK + suffix;
    }
}
