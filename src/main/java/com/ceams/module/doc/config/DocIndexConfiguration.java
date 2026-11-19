package com.ceams.module.doc.config;

import java.util.concurrent.ThreadPoolExecutor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * 文档索引专用线程池。
 *
 * <p>规格：core 1 / max 2 / queue 500，线程名前缀 {@code doc-index-}，拒绝策略
 * {@link ThreadPoolExecutor.AbortPolicy}，由服务层捕获 {@code TaskRejectedException} 并把任务置 FAILED，
 * 避免上传主链路被索引阻塞。
 *
 * <p>与 AI 对话线程池的关键差异：索引任务只依赖 fileId，<b>不需要</b>登录态，因此不做
 * {@code SecurityContext} 传递（否则子线程持有过期的认证信息，反而容易误判）。
 */
@Configuration
public class DocIndexConfiguration {

    /** 线程池 bean 名称（服务层按名称注入）。 */
    public static final String EXECUTOR_BEAN_NAME = "docIndexTaskExecutor";

    @Bean(name = EXECUTOR_BEAN_NAME)
    public ThreadPoolTaskExecutor docIndexTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(2);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("doc-index-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }
}
