package com.ceams.module.ai.tool;

import com.ceams.common.exception.BusinessException;
import com.ceams.common.exception.ErrorCode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 工具期间参数。
 *
 * <p>支持 {@code yyyy-MM}、{@code yyyy-MM-dd} 与 {@code yyyy-MM-dd~yyyy-MM-dd} 三种写法； 未传时默认当前自然月（
 * Asia/Shanghai），闭区间。
 *
 * <p>兜底：模型可能把「本月」猜成错误年份，因此当期间明显不合理（早于 2000-01 或晚于当前月 + 1）时，不抛错， 而是按当前月处理并置 {@code fallback=true}
 * ，由工具在结果行里带 {@code periodFallback:true} 标记。
 */
public record ToolPeriod(LocalDateTime start, LocalDateTime end, String label, boolean fallback) {

    /** 统一使用中国时区，避免服务器时区不同导致「本月」漂移。 */
    public static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    private static final Pattern MONTH_PATTERN = Pattern.compile("^\\d{4}-(0[1-9]|1[0-2])$");

    /** STRICT 模式下必须使用 uuuu（年）而非 yyyy（纪元年），否则会解析失败。 */
    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("uuuu-MM-dd").withResolverStyle(ResolverStyle.STRICT);

    /** 允许的最早期间（防止模型查到错误年代的数据）。 */
    private static final YearMonth EARLIEST_MONTH = YearMonth.of(2000, 1);

    /** 兼容旧三参构造（无兜底标记）。 */
    public ToolPeriod(LocalDateTime start, LocalDateTime end, String label) {
        this(start, end, label, false);
    }

    /** 从工具参数解析期间；空值默认当前自然月（Asia/Shanghai）。 */
    public static ToolPeriod resolve(Map<String, Object> args) {
        return resolve(args, Clock.system(ZONE));
    }

    /** 从工具参数解析期间（可注入 Clock，便于测试固定「今天」）。 */
    public static ToolPeriod resolve(Map<String, Object> args, Clock clock) {
        YearMonth currentMonth = YearMonth.now(clock);
        String text = ToolArgs.text(args, "period");
        if (text == null) {
            return ofMonth(currentMonth, false);
        }
        if (MONTH_PATTERN.matcher(text).matches()) {
            int year = Integer.parseInt(text.substring(0, 4));
            int month = Integer.parseInt(text.substring(5));
            YearMonth target = YearMonth.of(year, month);
            if (!withinReasonableRange(target, currentMonth)) {
                return ofMonth(currentMonth, true);
            }
            return ofMonth(target, false);
        }
        if (text.contains("~")) {
            String[] parts = text.split("~", -1);
            if (parts.length != 2) {
                throw invalidPeriod(text);
            }
            LocalDate start = parseDate(parts[0].trim(), text);
            LocalDate end = parseDate(parts[1].trim(), text);
            if (start.isAfter(end)) {
                throw invalidPeriod(text);
            }
            if (!withinReasonableRange(YearMonth.from(start), currentMonth)
                    || !withinReasonableRange(YearMonth.from(end), currentMonth)) {
                return ofMonth(currentMonth, true);
            }
            return new ToolPeriod(start.atStartOfDay(), end.atTime(LocalTime.MAX), text, false);
        }
        LocalDate day = parseDate(text, text);
        if (!withinReasonableRange(YearMonth.from(day), currentMonth)) {
            return ofMonth(currentMonth, true);
        }
        return new ToolPeriod(day.atStartOfDay(), day.atTime(LocalTime.MAX), text, false);
    }

    /** 判断时间点是否落在期间内（含首尾）。 */
    public boolean contains(LocalDateTime time) {
        return time != null && !time.isBefore(start) && !time.isAfter(end);
    }

    /** 期间覆盖的首个月份。 */
    public YearMonth firstMonth() {
        return YearMonth.from(start);
    }

    /** 期间覆盖的最后月份。 */
    public YearMonth lastMonth() {
        return YearMonth.from(end);
    }

    private static boolean withinReasonableRange(YearMonth month, YearMonth currentMonth) {
        return !month.isBefore(EARLIEST_MONTH) && !month.isAfter(currentMonth.plusMonths(1));
    }

    private static ToolPeriod ofMonth(YearMonth month, boolean fallback) {
        return new ToolPeriod(
                month.atDay(1).atStartOfDay(),
                month.atEndOfMonth().atTime(LocalTime.MAX),
                month.toString(),
                fallback);
    }

    private static LocalDate parseDate(String text, String rawPeriod) {
        try {
            return LocalDate.parse(text, DATE_FORMATTER);
        } catch (DateTimeParseException ex) {
            throw invalidPeriod(rawPeriod);
        }
    }

    private static BusinessException invalidPeriod(String text) {
        return BusinessException.of(
                ErrorCode.VALID_ILLEGAL_ARGUMENT,
                "参数 period 非法，应为 yyyy-MM、yyyy-MM-dd 或 yyyy-MM-dd~yyyy-MM-dd：" + text);
    }
}
