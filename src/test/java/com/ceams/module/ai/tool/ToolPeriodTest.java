package com.ceams.module.ai.tool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ceams.common.exception.BusinessException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** {@link ToolPeriod} 的默认本月与超范围兜底行为（固定「今天」= 2026-11-07）。 */
class ToolPeriodTest {

    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-07T02:00:00Z"), ZONE);

    @Test
    void 未传期间时默认当前月() {
        ToolPeriod period = ToolPeriod.resolve(Map.of(), CLOCK);

        assertEquals("2026-11", period.label());
        assertFalse(period.fallback());
        assertEquals(LocalDateTime.of(2026, 11, 1, 0, 0), period.start());
        assertEquals(LocalDateTime.of(2026, 11, 30, 23, 59, 59, 999_999_999), period.end());
    }

    @Test
    void 空串期间视为未传并默认当前月() {
        ToolPeriod period = ToolPeriod.resolve(Map.of("period", "  "), CLOCK);

        assertEquals("2026-11", period.label());
        assertFalse(period.fallback());
    }

    @Test
    void 正常月份与次月首日均不兜底() {
        assertEquals("2026-11", ToolPeriod.resolve(Map.of("period", "2026-11"), CLOCK).label());
        ToolPeriod nextMonth = ToolPeriod.resolve(Map.of("period", "2026-12"), CLOCK);
        assertEquals("2026-12", nextMonth.label());
        assertFalse(nextMonth.fallback());
    }

    @Test
    void 早于2000年的期间兜底为本月并标记() {
        ToolPeriod tooEarly = ToolPeriod.resolve(Map.of("period", "1999-12"), CLOCK);

        assertEquals("2026-11", tooEarly.label());
        assertTrue(tooEarly.fallback());

        ToolPeriod earlyRange =
                ToolPeriod.resolve(Map.of("period", "1999-01-01~1999-12-31"), CLOCK);
        assertEquals("2026-11", earlyRange.label());
        assertTrue(earlyRange.fallback());
    }

    @Test
    void 晚于当前月加一的期间兜底为本月并标记() {
        ToolPeriod tooLate = ToolPeriod.resolve(Map.of("period", "2027-01"), CLOCK);

        assertEquals("2026-11", tooLate.label());
        assertTrue(tooLate.fallback());
    }

    @Test
    void 合理区间不兜底() {
        ToolPeriod range = ToolPeriod.resolve(Map.of("period", "2026-01-01~2026-11-07"), CLOCK);

        assertEquals("2026-01-01~2026-11-07", range.label());
        assertFalse(range.fallback());
        assertEquals(LocalDateTime.of(2026, 1, 1, 0, 0), range.start());
    }

    @Test
    void 非法格式仍抛业务异常() {
        assertThrows(
                BusinessException.class,
                () -> ToolPeriod.resolve(Map.of("period", "2026-13"), CLOCK));
        assertThrows(
                BusinessException.class,
                () -> ToolPeriod.resolve(Map.of("period", "2026-11-07~2026-01-01"), CLOCK));
    }
}
