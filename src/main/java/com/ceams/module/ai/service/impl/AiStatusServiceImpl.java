package com.ceams.module.ai.service.impl;

import com.ceams.module.ai.client.AiChatClient;
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
 *   <li>用 {@link AtomicReference} 持有不可变快照（可达性 + 耗时 + 采样时间 + 配置修订号），无需额外依赖；
 *   <li>配置保存后修订号变化，缓存自动失效，保证「改完立即生效」；
 *   <li>探活异常只降级为 {@code reachable=false}，不向上抛 500。
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiStatusServiceImpl implements AiStatusService {

    /** 探活缓存有效期（毫秒）。 */
    private static final long CACHE_TTL_MILLIS = 60_000L;

    private final AiChatClient aiChatClient;

    private final AtomicReference<ProbeSnapshot> cachedProbe = new AtomicReference<>();

    @Override
    public AiStatusVO getStatus() {
        AiStatusVO vo = new AiStatusVO();
        boolean enabled = aiChatClient.isEnabled();
        vo.setEnabled(enabled);
        vo.setModel(aiChatClient.textModel());
        if (!enabled) {
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
        long revision = aiChatClient.configRevision();
        ProbeSnapshot current = cachedProbe.get();
        if (current != null
                && current.revision() == revision
                && now - current.sampledAt() < CACHE_TTL_MILLIS) {
            return current;
        }

        long startNanos = System.nanoTime();
        boolean reachable;
        try {
            reachable = aiChatClient.isReachable();
        } catch (RuntimeException e) {
            // 客户端已内部兜底；此处再防一层，保证状态接口永不 500。
            log.warn("AI 探活出现异常，降级为不可用：{}", e.getMessage());
            reachable = false;
        }
        long latencyMs = Math.max(0L, (System.nanoTime() - startNanos) / 1_000_000L);

        ProbeSnapshot fresh = new ProbeSnapshot(reachable, latencyMs, now, revision);
        cachedProbe.set(fresh);
        return fresh;
    }

    /** 不可变探活快照。 */
    private record ProbeSnapshot(boolean reachable, long latencyMs, long sampledAt, long revision) {}
}
