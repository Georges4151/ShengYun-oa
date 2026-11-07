package com.ceams.module.ai.service.impl;

import com.ceams.module.ai.client.OllamaClient;
import com.ceams.module.ai.config.AiProperties;
import com.ceams.module.ai.service.AiStatusService;
import com.ceams.module.ai.vo.AiStatusVO;
import java.util.concurrent.atomic.AtomicReference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * AI 服务状态实现。
 *
 * <p>探活结果做 60 秒进程内缓存：
 *
 * <ul>
 *   <li>用 {@link AtomicReference} 持有不可变快照（可达性 + 耗时 + 采样时间），无需额外依赖；
 *   <li>缓存新鲜期内直接复用，避免每次状态查询都打 Ollama；
 *   <li>探活异常只降级为 {@code reachable=false}，不向上抛 500。
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiStatusServiceImpl implements AiStatusService {

    /** 探活缓存有效期（毫秒）。 */
    private static final long CACHE_TTL_MILLIS = 60_000L;

    private final AiProperties aiProperties;
    private final OllamaClient ollamaClient;

    private final AtomicReference<ProbeSnapshot> cachedProbe = new AtomicReference<>();

    @Override
    public AiStatusVO getStatus() {
        AiStatusVO vo = new AiStatusVO();
        vo.setEnabled(aiProperties.isEnabled());
        vo.setModel(aiProperties.getModel());
        if (!aiProperties.isEnabled()) {
            // 关闭时不得发起任何网络请求。
            vo.setReachable(false);
            vo.setLatencyMs(0L);
            return vo;
        }
        ProbeSnapshot snapshot = resolveSnapshot();
        vo.setReachable(snapshot.reachable());
        vo.setLatencyMs(snapshot.latencyMs());
        return vo;
    }

    private ProbeSnapshot resolveSnapshot() {
        long now = System.currentTimeMillis();
        ProbeSnapshot current = cachedProbe.get();
        if (current != null && now - current.sampledAt() < CACHE_TTL_MILLIS) {
            return current;
        }

        long startNanos = System.nanoTime();
        boolean reachable;
        try {
            reachable = ollamaClient.isReachable();
        } catch (RuntimeException e) {
            // OllamaClient 已内部兜底；此处再防一层，保证状态接口永不 500。
            log.warn("Ollama 探活出现异常，降级为不可用：{}", e.getMessage());
            reachable = false;
        }
        long latencyMs = Math.max(0L, (System.nanoTime() - startNanos) / 1_000_000L);

        ProbeSnapshot fresh = new ProbeSnapshot(reachable, latencyMs, now);
        cachedProbe.set(fresh);
        return fresh;
    }

    /** 不可变探活快照。 */
    private record ProbeSnapshot(boolean reachable, long latencyMs, long sampledAt) {}
}
