package com.ceams.module.ai.config;

import java.util.concurrent.ThreadPoolExecutor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.task.DelegatingSecurityContextAsyncTaskExecutor;

/**
 * AI 对话专用线程池。
 *
 * <p>关键点：Spring Security 的 {@code SecurityContext} 是 ThreadLocal，异步线程会丢失认证信息，导致 工具层 {@code
 * ToolSecurity} 判定为未登录、数据全部被拒。因此用 {@link DelegatingSecurityContextAsyncTaskExecutor}
 * 包装，提交任务时捕获当前线程的安全上下文并在执行线程内恢复。
 *
 * <p>线程池规格：core 4 / max 8 / queue 32，拒绝策略为 {@link ThreadPoolExecutor.AbortPolicy}， 由服务层捕获 {@code
 * TaskRejectedException} 转为 SSE {@code error} 事件，避免阻塞 Tomcat 线程。
 */
@Configuration
public class AiChatExecutorConfiguration {

    /** 线程池 bean 名称（服务层按名称注入）。 */
    public static final String EXECUTOR_BEAN_NAME = "aiChatTaskExecutor";

    @Bean(name = EXECUTOR_BEAN_NAME)
    public TaskExecutor aiChatTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(32);
        executor.setThreadNamePrefix("ai-chat-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return new DelegatingSecurityContextAsyncTaskExecutor(executor);
    }
}
