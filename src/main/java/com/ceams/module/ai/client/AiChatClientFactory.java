package com.ceams.module.ai.client;

import com.ceams.module.ai.config.AiPurposeConfig;
import com.ceams.module.ai.enums.AiPurpose;
import com.ceams.module.ai.service.AiSettingsService;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 客户端工厂：按当前生效配置路由到 Ollama / OpenAI 兼容实现。
 *
 * <p>每个用途缓存一份客户端，缓存键为配置指纹（提供商+地址+模型+Key+超时）。
 * 配置保存后 {@code AiSettingsService} 会刷新快照，指纹变化时下一个请求自动重建客户端，
 * 因此无需重启即可生效。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiChatClientFactory {

    private final AiChatClientBuilder clientBuilder;
    private final AiSettingsService settingsService;

    private final AtomicReference<CachedClient> textCache = new AtomicReference<>();
    private final AtomicReference<CachedClient> visionCache = new AtomicReference<>();

    /** 当前文本对话客户端。 */
    public AiChatClient textClient() {
        return resolve(AiPurpose.TEXT, textCache);
    }

    /** 当前票据视觉识别客户端。 */
    public AiChatClient visionClient() {
        return resolve(AiPurpose.VISION, visionCache);
    }

    /** 构建一次性客户端（管理员连接测试用，不进入缓存）。 */
    public AiChatClient create(AiPurposeConfig config) {
        return clientBuilder.create(config);
    }

    private AiChatClient resolve(AiPurpose purpose, AtomicReference<CachedClient> cache) {
        AiPurposeConfig config = settingsService.purposeConfig(purpose);
        String fingerprint = fingerprint(config);
        CachedClient cached = cache.get();
        if (cached != null && Objects.equals(cached.fingerprint(), fingerprint)) {
            return cached.client();
        }
        synchronized (cache) {
            cached = cache.get();
            if (cached != null && Objects.equals(cached.fingerprint(), fingerprint)) {
                return cached.client();
            }
            AiChatClient client = clientBuilder.create(config);
            cache.set(new CachedClient(fingerprint, client));
            log.info(
                    "已按最新配置创建 AI 客户端：purpose={}, provider={}, model={}",
                    purpose,
                    config.getProvider(),
                    config.getModel());
            return client;
        }
    }

    /**
     * 配置指纹：只用于判断是否需要重建客户端，不写日志，避免泄露 Key。
     */
    private String fingerprint(AiPurposeConfig config) {
        return String.join(
                "|",
                config.getProvider() == null ? "" : config.getProvider().name(),
                config.getBaseUrl() == null ? "" : config.getBaseUrl(),
                config.getModel() == null ? "" : config.getModel(),
                config.getApiKey() == null ? "" : config.getApiKey(),
                String.valueOf(config.getTimeoutSeconds()));
    }

    /** 缓存项：指纹 + 客户端。 */
    private record CachedClient(String fingerprint, AiChatClient client) {}
}
