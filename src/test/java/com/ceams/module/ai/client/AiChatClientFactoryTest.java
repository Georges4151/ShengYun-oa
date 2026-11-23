package com.ceams.module.ai.client;

import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.ceams.module.ai.config.AiPurposeConfig;
import com.ceams.module.ai.enums.AiPreset;
import com.ceams.module.ai.enums.AiProvider;
import com.ceams.module.ai.enums.AiPurpose;
import com.ceams.module.ai.service.AiSettingsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;

/** {@link AiChatClientFactory} 路由与缓存单元测试。 */
@ExtendWith(MockitoExtension.class)
class AiChatClientFactoryTest {

    @Mock private AiSettingsService settingsService;

    private AiChatClientFactory factory;

    @BeforeEach
    void setUp() {
        AiChatClientBuilder builder =
                new AiChatClientBuilder(RestClient.builder(), new ObjectMapper());
        factory = new AiChatClientFactory(builder, settingsService);
    }

    @Test
    void OLLAMA路由到OllamaClient() {
        when(settingsService.purposeConfig(AiPurpose.TEXT)).thenReturn(config(AiProvider.OLLAMA, "qwen2.5:7b"));

        AiChatClient client = factory.textClient();

        assertTrue(client instanceof OllamaClient);
    }

    @Test
    void OPENAI_COMPAT路由到OpenAiCompatClient() {
        when(settingsService.purposeConfig(AiPurpose.VISION))
                .thenReturn(config(AiProvider.OPENAI_COMPAT, "qwen-vl-max"));

        AiChatClient client = factory.visionClient();

        assertTrue(client instanceof OpenAiCompatClient);
    }

    @Test
    void 配置不变_复用同一客户端实例() {
        when(settingsService.purposeConfig(AiPurpose.TEXT)).thenReturn(config(AiProvider.OLLAMA, "qwen2.5:7b"));

        assertSame(factory.textClient(), factory.textClient());
    }

    @Test
    void 配置变化_重建客户端实例() {
        when(settingsService.purposeConfig(AiPurpose.TEXT))
                .thenReturn(config(AiProvider.OLLAMA, "qwen2.5:7b"))
                .thenReturn(config(AiProvider.OLLAMA, "qwen2.5:14b"));

        AiChatClient first = factory.textClient();
        AiChatClient second = factory.textClient();

        assertNotSame(first, second);
    }

    private static AiPurposeConfig config(AiProvider provider, String model) {
        AiPurposeConfig config = new AiPurposeConfig();
        config.setPurpose(AiPurpose.TEXT);
        config.setProvider(provider);
        config.setPreset(AiPreset.CUSTOM);
        config.setBaseUrl(
                provider == AiProvider.OLLAMA
                        ? "http://localhost:11434"
                        : "https://api.example.com/v1");
        config.setModel(model);
        config.setTimeoutSeconds(120);
        return config;
    }
}
