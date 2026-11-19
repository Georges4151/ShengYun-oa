package com.ceams.module.bpm.support;

import com.ceams.module.bpm.service.BpmTimeoutService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * BPM 超时提醒定时调度器。
 *
 * <p>按 {@code ceams.bpm.timeout.cron}（默认每 30 分钟）触发一次扫描提醒；
 * 调度异常只记录日志，不向容器抛出，避免调度线程被后续任务阻塞。
 *
 * <p>注意：{@code @EnableScheduling} 已由 {@code AiSummarySchedulingConfiguration} 开启，
 * 本类不重复声明，避免同一应用内出现多个调度开关。
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "ceams.bpm.timeout",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public class BpmTimeoutScheduler {

    private final BpmTimeoutService timeoutService;

    /** 定时扫描超时任务并提醒（同一任务仅提醒一次）。 */
    @Scheduled(cron = "${ceams.bpm.timeout.cron:0 0/30 * * * ?}")
    public void scanAndRemind() {
        try {
            int reminded = timeoutService.scanAndRemind();
            if (reminded > 0) {
                log.info("BPM 超时提醒任务执行完成：本次提醒 {} 条", reminded);
            }
        } catch (Exception e) {
            log.error("BPM 超时提醒任务执行失败（已忽略，等待下一轮调度）", e);
        }
    }
}
