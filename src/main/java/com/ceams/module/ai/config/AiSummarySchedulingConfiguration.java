package com.ceams.module.ai.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** 经营日报定时调度开关：仅当 {@code ceams.ai.summary.schedule-enabled=true} 时启用。 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(
        prefix = "ceams.ai.summary",
        name = "schedule-enabled",
        havingValue = "true",
        matchIfMissing = true)
public class AiSummarySchedulingConfiguration {}
