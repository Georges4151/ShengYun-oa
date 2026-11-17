package com.ceams.module.notification.config;

import java.util.concurrent.ThreadPoolExecutor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * 企业微信推送装配：注册配置与专用线程池。
 *
 * <p>推送使用独立线程池，避免 webhook 响应慢时阻塞审批主流程；线程池满时由调用方捕获拒绝异常并降级为日志。
 */
@Configuration
@EnableConfigurationProperties(WeComProperties.class)
public class WeComConfiguration {

    /** 推送线程池 bean 名称（客户端按名称注入）。 */
    public static final String EXECUTOR_BEAN_NAME = "weComPushExecutor";

    @Bean(name = EXECUTOR_BEAN_NAME)
    public TaskExecutor weComPushExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(2);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("wecom-push-");
        executor.setDaemon(true);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(10);
        executor.initialize();
        return executor;
    }
}
