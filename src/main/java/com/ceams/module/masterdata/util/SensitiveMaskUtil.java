package com.ceams.module.masterdata.util;

/**
 * 敏感字段脱敏工具（masterdata 域独立实现）。
 *
 * <p>针对 biz_contact 的敏感字段在 VO 输出前脱敏：
 * 手机号中间四位打码、银行账号仅保留后四位、税号保留前四后四。</p>
 */
public final class SensitiveMaskUtil {

    private static final String MASK = "****";

    private SensitiveMaskUtil() {
    }

    /**
     * 手机号脱敏：保留前 3 后 4，中间打码。
     */
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

    /**
     * 银行账号脱敏：仅保留后四位，其余打码。
     */
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

    /**
     * 税号脱敏：保留前 4 后 4，中间打码。
     */
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
}