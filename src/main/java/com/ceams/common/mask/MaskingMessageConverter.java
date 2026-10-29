package com.ceams.common.mask;

import ch.qos.logback.classic.pattern.MessageConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Logback 消息脱敏转换器。
 *
 * <p>在日志输出前对消息文本做正则脱敏，避免手机号、银行账号、税号等敏感明文泄露到日志。 通过 {@code src/main/resources/logback-spring.xml} 中的
 * {@code <conversionRule>} 替换默认的 {@code %m} / {@code %msg} 消息转换器。
 */
public class MaskingMessageConverter extends MessageConverter {

    /** 手机号：11 位、1 开头（避免匹配到更长数字串中的子串）。 */
    private static final Pattern PHONE = Pattern.compile("(?<!\\d)(1\\d{2})\\d{4}(\\d{4})(?!\\d)");

    /** 银行账号 / 税号：连续 15~25 位数字（覆盖常见银行账号与税号长度，兼容雪花 ID）。 */
    private static final Pattern ACCOUNT_OR_TAX = Pattern.compile("(?<!\\d)(\\d{15,25})(?!\\d)");

    private static final String MASK = "****";

    @Override
    public String convert(ILoggingEvent event) {
        String message = super.convert(event);
        if (message == null || message.isEmpty()) {
            return message;
        }
        return maskAccountOrTax(maskPhone(message));
    }

    /** 手机号脱敏：保留前 3 后 4，中间打码。 */
    private String maskPhone(String input) {
        Matcher matcher = PHONE.matcher(input);
        if (!matcher.find()) {
            return input;
        }
        StringBuffer sb = new StringBuffer();
        do {
            matcher.appendReplacement(
                    sb, Matcher.quoteReplacement(matcher.group(1) + MASK + matcher.group(2)));
        } while (matcher.find());
        matcher.appendTail(sb);
        return sb.toString();
    }

    /** 账号 / 税号脱敏：仅保留后 4 位，其余打码。 */
    private String maskAccountOrTax(String input) {
        Matcher matcher = ACCOUNT_OR_TAX.matcher(input);
        if (!matcher.find()) {
            return input;
        }
        StringBuffer sb = new StringBuffer();
        do {
            String digits = matcher.group(1);
            matcher.appendReplacement(
                    sb, Matcher.quoteReplacement(MASK + digits.substring(digits.length() - 4)));
        } while (matcher.find());
        matcher.appendTail(sb);
        return sb.toString();
    }
}
