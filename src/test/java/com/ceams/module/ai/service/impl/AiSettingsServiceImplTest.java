package com.ceams.module.ai.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.ceams.common.crypto.SensitiveEncryptor;
import com.ceams.common.exception.BusinessException;
import com.ceams.module.ai.client.AiChatClientBuilder;
import com.ceams.module.ai.config.AiProperties;
import com.ceams.module.ai.dto.AiSettingsUpdateRequest;
import com.ceams.module.ai.entity.SysAiProvider;
import com.ceams.module.ai.enums.AiPreset;
import com.ceams.module.ai.enums.AiProvider;
import com.ceams.module.ai.mapper.SysAiProviderMapper;
import com.ceams.module.ai.mapper.SysConfigMapper;
import com.ceams.module.ai.vo.AiProviderSettingsVO;
import com.ceams.module.ai.vo.AiSettingsVO;
import com.ceams.module.iam.mapper.OperationLogMapper;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** {@link AiSettingsServiceImpl} 单元测试：默认值回退、Key 加密/脱敏、PUT 时 Key 空值语义与校验。 */
@ExtendWith(MockitoExtension.class)
class AiSettingsServiceImplTest {

    private static final String SECRET =
            Base64.getEncoder()
                    .encodeToString("0123456789abcdef0123456789abcdef".getBytes(StandardCharsets.UTF_8));

    @Mock private SysAiProviderMapper providerMapper;
    @Mock private SysConfigMapper configMapper;
    @Mock private AiChatClientBuilder clientBuilder;
    @Mock private OperationLogMapper operationLogMapper;

    private final SensitiveEncryptor encryptor = new SensitiveEncryptor(SECRET);

    private final AiProperties properties = defaultProperties();

    private AiSettingsServiceImpl service() {
        return new AiSettingsServiceImpl(
                providerMapper, configMapper, encryptor, properties, clientBuilder, operationLogMapper);
    }

    @Test
    void 库中无记录_回退环境配置并返回内置预设() {
        when(providerMapper.selectOne(any())).thenReturn(null);
        when(configMapper.selectOne(any())).thenReturn(null);

        AiSettingsVO vo = service().getSettings();

        assertTrue(vo.isEnabled());
        assertEquals("OLLAMA", vo.getText().getProvider());
        assertEquals("CUSTOM", vo.getText().getPreset());
        assertEquals("http://localhost:11434", vo.getText().getBaseUrl());
        assertEquals("qwen2.5:7b", vo.getText().getModel());
        assertEquals("qwen2.5vl:7b", vo.getVision().getModel());
        assertEquals(120, vo.getText().getTimeoutSeconds());
        assertFalse(vo.getText().isApiKeySet());
        assertNull(vo.getText().getApiKeyMasked());
        assertEquals(5, vo.getPresets().size());
        assertEquals("DEEPSEEK", vo.getPresets().get(0).getCode());
        assertEquals("CUSTOM", vo.getPresets().get(4).getCode());
    }

    @Test
    void 读取已保存Key_仅返回脱敏串() {
        SysAiProvider text = row(AiProvider.OPENAI_COMPAT, "https://api.deepseek.com/v1", "deepseek-chat");
        text.setApiKeyCipher(encryptor.encrypt("sk-abcdefgh"));
        SysAiProvider vision = row(AiProvider.OLLAMA, "http://localhost:11434", "qwen2.5vl:7b");
        when(providerMapper.selectOne(any())).thenReturn(text, vision);
        when(configMapper.selectOne(any())).thenReturn(null);

        AiSettingsVO vo = service().getSettings();

        assertTrue(vo.getText().isApiKeySet());
        assertEquals("sk-****efgh", vo.getText().getApiKeyMasked());
        assertFalse(vo.getVision().isApiKeySet());
        assertNull(vo.getVision().getApiKeyMasked());
    }

    @Test
    void 保存新Key_加密入库且解密可还原() {
        when(providerMapper.selectOne(any())).thenReturn(null);
        when(configMapper.selectOne(any())).thenReturn(null);

        service().updateSettings(requestWithKey("sk-abcdefgh"));

        ArgumentCaptor<SysAiProvider> captor = ArgumentCaptor.forClass(SysAiProvider.class);
        verify(providerMapper, times(2)).insert(captor.capture());
        SysAiProvider textRow =
                captor.getAllValues().stream()
                        .filter(item -> "TEXT".equals(item.getPurpose()))
                        .findFirst()
                        .orElseThrow();
        assertNotNull(textRow.getApiKeyCipher());
        assertNotEquals("sk-abcdefgh", textRow.getApiKeyCipher());
        assertTrue(textRow.getApiKeyCipher().startsWith("v1:"));
        assertEquals("sk-abcdefgh", encryptor.decrypt(textRow.getApiKeyCipher()));
    }

    @Test
    void 保存时apiKey为null_保持原密文不变() {
        SysAiProvider text = row(AiProvider.OPENAI_COMPAT, "https://api.deepseek.com/v1", "deepseek-chat");
        String oldCipher = encryptor.encrypt("sk-old-key");
        text.setApiKeyCipher(oldCipher);
        SysAiProvider vision = row(AiProvider.OLLAMA, "http://localhost:11434", "qwen2.5vl:7b");
        when(providerMapper.selectOne(any())).thenReturn(text, vision, text, vision);
        when(configMapper.selectOne(any())).thenReturn(null);

        service().updateSettings(requestWithKey(null));

        List<UpdateWrapper<SysAiProvider>> wrappers = captureUpdates(2);
        Collection<Object> textValues = wrappers.get(0).getParamNameValuePairs().values();
        assertTrue(textValues.contains(oldCipher));
        verify(providerMapper, never()).insert(any(SysAiProvider.class));
    }

    @Test
    void 保存时apiKey为空串_清空密文() {
        SysAiProvider text = row(AiProvider.OPENAI_COMPAT, "https://api.deepseek.com/v1", "deepseek-chat");
        text.setApiKeyCipher(encryptor.encrypt("sk-old-key"));
        SysAiProvider vision = row(AiProvider.OLLAMA, "http://localhost:11434", "qwen2.5vl:7b");
        when(providerMapper.selectOne(any())).thenReturn(text, vision, text, vision);
        when(configMapper.selectOne(any())).thenReturn(null);

        service().updateSettings(requestWithKey(""));

        List<UpdateWrapper<SysAiProvider>> wrappers = captureUpdates(2);
        Collection<Object> textValues = wrappers.get(0).getParamNameValuePairs().values();
        assertTrue(textValues.stream().anyMatch(Objects::isNull));
    }

    @Test
    void 校验_OpenAI兼容地址必须为http或https() {
        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () ->
                                service()
                                        .updateSettings(
                                                requestWithKey(
                                                        "OPENAI_COMPAT",
                                                        "https://api.deepseek.com/v1",
                                                        "deepseek-chat",
                                                        "sk-test",
                                                        "ftp://api.deepseek.com")));

        assertTrue(exception.getMessage().contains("http"));
    }

    @Test
    void 校验_超时时间必须在10到600秒之间() {
        assertThrows(
                BusinessException.class,
                () ->
                        service()
                                .updateSettings(
                                        requestWithTimeout(
                                                "OPENAI_COMPAT",
                                                "https://api.deepseek.com/v1",
                                                "deepseek-chat",
                                                "sk-test",
                                                9)));
    }

    private List<UpdateWrapper<SysAiProvider>> captureUpdates(int expected) {
        @SuppressWarnings("unchecked")
        ArgumentCaptor<UpdateWrapper<SysAiProvider>> captor =
                ArgumentCaptor.forClass(UpdateWrapper.class);
        verify(providerMapper, times(expected)).update(any(), captor.capture());
        return captor.getAllValues();
    }

    private AiSettingsUpdateRequest requestWithKey(String textApiKey) {
        return requestWithKey(
                "OPENAI_COMPAT",
                "https://api.deepseek.com/v1",
                "deepseek-chat",
                textApiKey,
                null);
    }

    private AiSettingsUpdateRequest requestWithKey(
            String provider, String baseUrl, String model, String textApiKey, String overrideBaseUrl) {
        AiSettingsUpdateRequest request = new AiSettingsUpdateRequest();
        request.setEnabled(true);
        AiSettingsUpdateRequest.ProviderSettings text = new AiSettingsUpdateRequest.ProviderSettings();
        text.setProvider(provider);
        text.setPreset(AiPreset.DEEPSEEK.name());
        text.setBaseUrl(overrideBaseUrl == null ? baseUrl : overrideBaseUrl);
        text.setModel(model);
        text.setTimeoutSeconds(120);
        text.setApiKey(textApiKey);
        request.setText(text);

        AiSettingsUpdateRequest.ProviderSettings vision =
                new AiSettingsUpdateRequest.ProviderSettings();
        vision.setProvider("OLLAMA");
        vision.setPreset(AiPreset.CUSTOM.name());
        vision.setBaseUrl("http://localhost:11434");
        vision.setModel("qwen2.5vl:7b");
        vision.setTimeoutSeconds(120);
        vision.setApiKey(null);
        request.setVision(vision);
        return request;
    }

    private AiSettingsUpdateRequest requestWithTimeout(
            String provider, String baseUrl, String model, String textApiKey, Integer timeoutSeconds) {
        AiSettingsUpdateRequest request =
                requestWithKey(provider, baseUrl, model, textApiKey, null);
        request.getText().setTimeoutSeconds(timeoutSeconds);
        return request;
    }

    private static SysAiProvider row(AiProvider provider, String baseUrl, String model) {
        SysAiProvider row = new SysAiProvider();
        row.setPurpose("TEXT");
        row.setProvider(provider.name());
        row.setPreset(AiPreset.CUSTOM.name());
        row.setBaseUrl(baseUrl);
        row.setModel(model);
        row.setTimeoutSeconds(120);
        return row;
    }

    private static AiProperties defaultProperties() {
        AiProperties properties = new AiProperties();
        properties.setEnabled(true);
        properties.setBaseUrl("http://localhost:11434");
        properties.setModel("qwen2.5:7b");
        properties.setVisionModel("qwen2.5vl:7b");
        properties.setTimeoutSeconds(120);
        return properties;
    }
}
