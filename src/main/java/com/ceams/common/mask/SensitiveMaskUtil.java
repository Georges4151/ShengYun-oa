package com.ceams.common.mask;

/**
 * 敏感字段脱敏工具（全工程统一入口）。
 *
 * <p>供各领域 Converter 在 VO 输出前脱敏，规则与既有 masterdata / project 版本保持一致， 收敛 iam / masterdata / project
 * 三份重复实现为这一份公共实现。
 */
public final class SensitiveMaskUtil {

    private static final String MASK = "****";

    private SensitiveMaskUtil() {}

    /** 手机号脱敏：null/blank 原样返回；trim 后长度 &lt; 7 返回 {@code "****"}； 否则保留前 3 后 4，中间打码。 */
    public static String maskPhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return phone;
        }
        String value = phone.trim();
        if (value.length() < 7) {
            return MASK;
        }
        return value.substring(0, 3) + MASK + value.substring(value.length() - 4);
    }

    /** 银行账号脱敏：null/blank 原样返回；trim 后长度 &lt;= 4 返回 {@code "****"}； 否则仅保留后 4 位，其余打码。 */
    public static String maskBankAccount(String bankAccount) {
        if (bankAccount == null || bankAccount.isBlank()) {
            return bankAccount;
        }
        String value = bankAccount.trim();
        if (value.length() <= 4) {
            return MASK;
        }
        return MASK + value.substring(value.length() - 4);
    }

    /** 税号脱敏：null/blank 原样返回；trim 后长度 &lt;= 8 返回 {@code "****"}； 否则保留前 4 后 4，中间打码。 */
    public static String maskTaxNumber(String taxNumber) {
        if (taxNumber == null || taxNumber.isBlank()) {
            return taxNumber;
        }
        String value = taxNumber.trim();
        if (value.length() <= 8) {
            return MASK;
        }
        return value.substring(0, 4) + MASK + value.substring(value.length() - 4);
    }

    /** 邮箱脱敏：null/blank 原样返回；无 @ 或 @ 在首位时原样返回； 否则保留账号首字符与完整域名，账号其余部分打码。 */
    public static String maskEmail(String email) {
        if (email == null || email.isBlank()) {
            return email;
        }
        String value = email.trim();
        int at = value.indexOf('@');
        if (at <= 0) {
            return value;
        }
        return value.charAt(0) + MASK + value.substring(at);
    }
}
