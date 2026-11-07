package com.ceams.module.ai.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** AI 模块配置装配：注册 {@link AiProperties}，与主类 {@code @SpringBootApplication} 扫描范围兼容。 */
@Configuration
@EnableConfigurationProperties(AiProperties.class)
public class AiConfiguration {}
