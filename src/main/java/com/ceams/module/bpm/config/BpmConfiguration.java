package com.ceams.module.bpm.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** BPM 内核装配：仅绑定配置，不注册调度器（本期不接通知与定时任务）。 */
@Configuration
@EnableConfigurationProperties(BpmProperties.class)
public class BpmConfiguration {}
