package com.ceams.module.ai.schedule;

import com.ceams.module.ai.service.AiDailySummaryService;
import java.time.LocalDate;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 经营日报定时任务：每天生成昨日摘要，并补录历史缺失。 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "ceams.ai.summary",
        name = "schedule-enabled",
        havingValue = "true",
        matchIfMissing = true)
public class AiSummaryScheduler {

    /** 服务端统一时区。 */
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");

    private final AiDailySummaryService aiDailySummaryService;

    /** 生成昨日摘要（允许润色），再补录历史缺失（模板不润色）。异常只记日志，不中断后续调度。 */
    @Scheduled(cron = "${ceams.ai.summary.cron:0 0 8 * * ?}", zone = "Asia/Shanghai")
    public void generateYesterdayAndCatchUp() {
        LocalDate yesterday = LocalDate.now(BUSINESS_ZONE).minusDays(1);
        try {
            aiDailySummaryService.generate(yesterday, AiDailySummaryService.TriggerType.SCHEDULED);
        } catch (Exception e) {
            log.warn("生成昨日经营日报失败：date={}，原因={}", yesterday, e.getMessage());
        }
        try {
            aiDailySummaryService.catchUpMissing();
        } catch (Exception e) {
            log.warn("补录历史经营日报失败：原因={}", e.getMessage());
        }
    }
}
