package com.ceams.module.iam.util;

/**
 * 敏感字段脱敏工具。
 *
 * <p>i-am 域 VO 输出前脱敏：手机号中间四位掩码、邮箱局部掩码。密码绝不输出（VO 不含密码字段）。</p>
 */
public final class SensitiveMaskUtil {

    private static final String MASK = "****";

    private SensitiveMaskUtil() {
    }

    /** 手机号脱敏：保留前 3 后 4，中间打码。 */
    public static String maskPhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return phone;
        }
        String value = phone.trim();
        if (value.length() < 7) {
            return value;
        }
        return value.substring(0, 3) + MASK + value.substring(value.length() - 4);
    }

    /** 邮箱脱敏：保留首字符与域名，账号其余部分打码。 */
    public static String maskEmail(String email) {
        if (email == null || email.isBlank()) {
            return email;
        }
        String value = email.trim();
        int at = value.indexOf('@');
        if (at <= 0) {
            return value;
        }
        String account = value.substring(0, at);
        String domain = value.substring(at);
        if (account.length() <= 1) {
            return account.charAt(0) + MASK + domain;
        }
        return account.charAt(0) + MASK + domain;
    }
}