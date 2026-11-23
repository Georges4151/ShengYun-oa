package com.ceams.module.ai.client;

import com.ceams.module.ai.enums.AiPurpose;
import com.ceams.module.ai.service.AiSettingsService;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 路由客户端：上层服务唯一注入的 {@link AiChatClient} 实现。
 *
 * <p>文本对话走 TEXT 配置，带图对话走 VISION 配置，两者可分别使用 Ollama 或 OpenAI 兼容接口。
 */
@Component
@RequiredArgsConstructor
public class RoutingAiChatClient implements AiChatClient {

    private final AiChatClientFactory clientFactory;
    private final AiSettingsService settingsService;

    @Override
    public boolean isEnabled() {
        return settingsService.isEnabled();
    }

    @Override
    public String textModel() {
        return settingsService.purposeConfig(AiPurpose.TEXT).getModel();
    }

    @Override
    public String visionModel() {
        return settingsService.purposeConfig(AiPurpose.VISION).getModel();
    }

    @Override
    public int timeoutSeconds() {
        return settingsService.purposeConfig(AiPurpose.TEXT).getTimeoutSeconds();
    }

    @Override
    public long configRevision() {
        return settingsService.revision();
    }

    @Override
    public boolean isReachable() {
        return clientFactory.textClient().isReachable();
    }

    @Override
    public AiProbeResult probe() {
        return clientFactory.textClient().probe();
    }

    @Override
    public OllamaChatResult chatStream(
            List<ChatMessage> messages, List<Map<String, Object>> tools, Consumer<String> onDelta) {
        return clientFactory.textClient().chatStream(messages, tools, onDelta);
    }

    @Override
    public String chatWithImages(String model, String prompt, List<String> images) {
        return clientFactory.visionClient().chatWithImages(model, prompt, images);
    }
}
