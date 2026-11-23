package com.ceams.module.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.ceams.common.crypto.SensitiveEncryptor;
import com.ceams.common.exception.BusinessException;
import com.ceams.common.exception.ErrorCode;
import com.ceams.common.security.SecurityUtils;
import com.ceams.module.ai.client.AiChatClient;
import com.ceams.module.ai.client.AiChatClientBuilder;
import com.ceams.module.ai.client.AiProbeResult;
import com.ceams.module.ai.config.AiProperties;
import com.ceams.module.ai.config.AiPurposeConfig;
import com.ceams.module.ai.config.AiSettingsSnapshot;
import com.ceams.module.ai.dto.AiSettingsTestRequest;
import com.ceams.module.ai.dto.AiSettingsUpdateRequest;
import com.ceams.module.ai.entity.SysAiProvider;
import com.ceams.module.ai.entity.SysConfig;
import com.ceams.module.ai.enums.AiPreset;
import com.ceams.module.ai.enums.AiProvider;
import com.ceams.module.ai.enums.AiPurpose;
import com.ceams.module.ai.mapper.SysAiProviderMapper;
import com.ceams.module.ai.mapper.SysConfigMapper;
import com.ceams.module.ai.service.AiSettingsService;
import com.ceams.module.ai.support.AiConfigSupport;
import com.ceams.module.ai.vo.AiPresetVO;
import com.ceams.module.ai.vo.AiProviderSettingsVO;
import com.ceams.module.ai.vo.AiSettingsTestVO;
import com.ceams.module.ai.vo.AiSettingsVO;
import com.ceams.module.iam.entity.OperationLog;
import com.ceams.module.iam.enums.OperationType;
import com.ceams.module.iam.mapper.OperationLogMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * AI 配置服务实现。
 *
 * <p>关键行为：
 *
 * <ul>
 *   <li>配置读取使用 30 秒 TTL 的进程内快照，保存后主动 {@link #refresh()}，修改立即生效；
 *   <li>全局开关优先读 sys_config（key=ai.enabled），缺失时回退 {@code ceams.ai.enabled}；
 *   <li>API Key 使用 {@link SensitiveEncryptor} 加密入库，对外只返回掩码；
 *   <li>PUT 时 apiKey 为 null 保持原值、空串清空、非空更新；
 *   <li>配置变更写入操作日志（复用 iam 的 sys_operation_log）。
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiSettingsServiceImpl implements AiSettingsService {

    /** 全局开关在 sys_config 中的键。 */
    public static final String AI_ENABLED_CONFIG_KEY = "ai.enabled";

    /** 快照有效期：30 秒。 */
    private static final long CACHE_TTL_MILLIS = 30_000L;

    /** 超时下限（秒）。 */
    private static final int MIN_TIMEOUT_SECONDS = 10;

    /** 超时上限（秒）。 */
    private static final int MAX_TIMEOUT_SECONDS = 600;

    private final SysAiProviderMapper providerMapper;
    private final SysConfigMapper configMapper;
    private final SensitiveEncryptor sensitiveEncryptor;
    private final AiProperties aiProperties;
    private final AiChatClientBuilder clientBuilder;
    private final OperationLogMapper operationLogMapper;

    /** 当前快照。 */
    private volatile AiSettingsSnapshot snapshot;

    /** 快照加载时间（毫秒）。 */
    private volatile long snapshotLoadedAt;

    /** 配置修订号。 */
    private final AtomicLong revision = new AtomicLong();

    @Override
    public AiSettingsVO getSettings() {
        AiSettingsSnapshot current = snapshot();
        AiSettingsVO vo = new AiSettingsVO();
        vo.setEnabled(current.enabled());
        vo.setText(toVO(current.text()));
        vo.setVision(toVO(current.vision()));
        List<AiPresetVO> presets = new ArrayList<>(AiPreset.values().length);
        for (AiPreset preset : AiPreset.values()) {
            AiPresetVO item = new AiPresetVO();
            item.setCode(preset.name());
            item.setName(preset.getDisplayName());
            item.setBaseUrl(preset.getBaseUrl());
            item.setTextModel(preset.getTextModel());
            item.setVisionModel(preset.getVisionModel());
            presets.add(item);
        }
        vo.setPresets(presets);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AiSettingsVO updateSettings(AiSettingsUpdateRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.VALID_ILLEGAL_ARGUMENT, "请求不能为空");
        }
        if (request.getEnabled() == null) {
            throw new BusinessException(ErrorCode.VALID_ILLEGAL_ARGUMENT, "全局开关不能为空");
        }
        if (request.getText() == null) {
            throw new BusinessException(ErrorCode.VALID_ILLEGAL_ARGUMENT, "文本对话配置不能为空");
        }
        if (request.getVision() == null) {
            throw new BusinessException(ErrorCode.VALID_ILLEGAL_ARGUMENT, "票据视觉识别配置不能为空");
        }

        savePurpose(AiPurpose.TEXT, request.getText(), "文本对话");
        savePurpose(AiPurpose.VISION, request.getVision(), "票据视觉识别");
        saveEnabled(request.getEnabled());

        refresh();
        recordAudit();
        return getSettings();
    }

    @Override
    public AiSettingsTestVO testConnection(AiSettingsTestRequest request) {
        long startNanos = System.nanoTime();
        try {
            AiPurpose purpose = request == null ? null : AiPurpose.from(request.getPurpose());
            if (purpose == null) {
                return fail("用途不合法，仅支持 TEXT / VISION", startNanos);
            }
            AiPurposeConfig config = mergeForTest(purpose, request);
            AiChatClient client = clientBuilder.create(config);
            AiProbeResult result = client.probe();
            return new AiSettingsTestVO(
                    result.success(), result.message(), elapsedMillis(startNanos));
        } catch (IllegalArgumentException e) {
            return fail(e.getMessage(), startNanos);
        } catch (Exception e) {
            log.warn("AI 连接测试异常：{}", e.getMessage());
            return fail("连接测试失败：" + e.getMessage(), startNanos);
        }
    }

    @Override
    public boolean isEnabled() {
        return snapshot().enabled();
    }

    @Override
    public AiPurposeConfig purposeConfig(AiPurpose purpose) {
        AiSettingsSnapshot current = snapshot();
        return current.purposeConfig(purpose == null ? AiPurpose.TEXT : purpose);
    }

    @Override
    public long revision() {
        return revision.get();
    }

    @Override
    public void refresh() {
        snapshot = null;
        revision.incrementAndGet();
    }

    // ------------------------------------------------------------------
    // 快照加载
    // ------------------------------------------------------------------

    /** 取有效快照；过期时重新加载。 */
    private AiSettingsSnapshot snapshot() {
        AiSettingsSnapshot current = snapshot;
        if (current != null && System.currentTimeMillis() - snapshotLoadedAt < CACHE_TTL_MILLIS) {
            return current;
        }
        synchronized (this) {
            current = snapshot;
            if (current != null && System.currentTimeMillis() - snapshotLoadedAt < CACHE_TTL_MILLIS) {
                return current;
            }
            AiSettingsSnapshot loaded =
                    new AiSettingsSnapshot(
                            loadEnabled(), loadPurpose(AiPurpose.TEXT), loadPurpose(AiPurpose.VISION));
            snapshot = loaded;
            snapshotLoadedAt = System.currentTimeMillis();
            return loaded;
        }
    }

    /** 读取全局开关，异常或缺失时回退环境配置。 */
    private boolean loadEnabled() {
        try {
            SysConfig config =
                    configMapper.selectOne(
                            new QueryWrapper<SysConfig>()
                                    .eq("config_key", AI_ENABLED_CONFIG_KEY)
                                    .last("LIMIT 1"));
            if (config == null || !StringUtils.hasText(config.getConfigValue())) {
                return aiProperties.isEnabled();
            }
            return Boolean.parseBoolean(config.getConfigValue().trim());
        } catch (Exception e) {
            log.warn("读取 AI 全局开关失败，回退 ceams.ai.enabled：{}", e.getMessage());
            return aiProperties.isEnabled();
        }
    }

    /** 读取单个用途配置；缺失或异常时回退环境配置。 */
    private AiPurposeConfig loadPurpose(AiPurpose purpose) {
        try {
            SysAiProvider row =
                    providerMapper.selectOne(
                            new QueryWrapper<SysAiProvider>()
                                    .eq("purpose", purpose.name())
                                    .last("LIMIT 1"));
            return row == null ? fallback(purpose) : fromRow(row, purpose);
        } catch (Exception e) {
            log.warn("读取 AI 配置失败（purpose={}），回退环境配置：{}", purpose, e.getMessage());
            return fallback(purpose);
        }
    }

    /** 环境默认配置：保持升级前行为。 */
    private AiPurposeConfig fallback(AiPurpose purpose) {
        AiPurposeConfig config = new AiPurposeConfig();
        config.setPurpose(purpose);
        config.setProvider(AiProvider.OLLAMA);
        config.setPreset(AiPreset.CUSTOM);
        config.setBaseUrl(AiConfigSupport.normalizeBaseUrl(aiProperties.getBaseUrl()));
        config.setModel(
                purpose == AiPurpose.VISION
                        ? aiProperties.getVisionModel()
                        : aiProperties.getModel());
        config.setTimeoutSeconds(Math.max(1, aiProperties.getTimeoutSeconds()));
        config.setApiKey(null);
        return config;
    }

    /** 数据库行转运行时配置。 */
    private AiPurposeConfig fromRow(SysAiProvider row, AiPurpose purpose) {
        AiPurposeConfig config = new AiPurposeConfig();
        config.setPurpose(purpose);
        config.setProvider(defaultProvider(AiProvider.from(row.getProvider())));
        config.setPreset(defaultPreset(AiPreset.from(row.getPreset())));
        String baseUrl = AiConfigSupport.normalizeBaseUrl(row.getBaseUrl());
        config.setBaseUrl(
                StringUtils.hasText(baseUrl)
                        ? baseUrl
                        : AiConfigSupport.normalizeBaseUrl(aiProperties.getBaseUrl()));
        String model = StringUtils.hasText(row.getModel()) ? row.getModel().trim() : null;
        if (!StringUtils.hasText(model)) {
            model =
                    purpose == AiPurpose.VISION
                            ? aiProperties.getVisionModel()
                            : aiProperties.getModel();
        }
        config.setModel(model);
        Integer timeout = row.getTimeoutSeconds();
        config.setTimeoutSeconds(
                timeout != null && timeout >= MIN_TIMEOUT_SECONDS && timeout <= MAX_TIMEOUT_SECONDS
                        ? timeout
                        : Math.max(1, aiProperties.getTimeoutSeconds()));
        config.setApiKey(decryptQuietly(row.getApiKeyCipher(), purpose));
        return config;
    }

    private AiProvider defaultProvider(AiProvider provider) {
        return provider == null ? AiProvider.OLLAMA : provider;
    }

    private AiPreset defaultPreset(AiPreset preset) {
        return preset == null ? AiPreset.CUSTOM : preset;
    }

    /** 解密失败只记录用途，不输出密文或 Key。 */
    private String decryptQuietly(String cipher, AiPurpose purpose) {
        if (!StringUtils.hasText(cipher)) {
            return null;
        }
        try {
            return sensitiveEncryptor.decrypt(cipher);
        } catch (Exception e) {
            log.warn("AI 配置 API Key 解密失败（purpose={}），按未配置处理", purpose);
            return null;
        }
    }

    // ------------------------------------------------------------------
    // 保存
    // ------------------------------------------------------------------

    /** 校验并保存单个用途配置。 */
    private void savePurpose(
            AiPurpose purpose,
            AiSettingsUpdateRequest.ProviderSettings settings,
            String label) {
        AiProvider provider = AiProvider.from(settings.getProvider());
        if (provider == null) {
            throw new BusinessException(
                    ErrorCode.VALID_ILLEGAL_ARGUMENT,
                    label + "的提供商不合法，仅支持 OLLAMA / OPENAI_COMPAT");
        }
        AiPreset preset = AiPreset.from(settings.getPreset());
        if (preset == null) {
            if (StringUtils.hasText(settings.getPreset())) {
                throw new BusinessException(
                        ErrorCode.VALID_ILLEGAL_ARGUMENT, label + "的厂商预设不合法");
            }
            preset = AiPreset.CUSTOM;
        }
        String baseUrl = AiConfigSupport.normalizeBaseUrl(settings.getBaseUrl());
        if (!StringUtils.hasText(baseUrl)) {
            throw new BusinessException(
                    ErrorCode.VALID_ILLEGAL_ARGUMENT, label + "的服务地址不能为空");
        }
        if (!AiConfigSupport.isHttpUrl(baseUrl)) {
            throw new BusinessException(
                    ErrorCode.VALID_ILLEGAL_ARGUMENT,
                    label + "的服务地址必须以 http:// 或 https:// 开头");
        }
        String model = settings.getModel() == null ? "" : settings.getModel().trim();
        if (!StringUtils.hasText(model)) {
            throw new BusinessException(ErrorCode.VALID_ILLEGAL_ARGUMENT, label + "的模型名称不能为空");
        }
        Integer timeout = settings.getTimeoutSeconds();
        if (timeout == null || timeout < MIN_TIMEOUT_SECONDS || timeout > MAX_TIMEOUT_SECONDS) {
            throw new BusinessException(
                    ErrorCode.VALID_ILLEGAL_ARGUMENT, label + "的超时时间必须在 10~600 秒之间");
        }

        SysAiProvider existing = selectPurpose(purpose);
        String cipher = resolveCipher(existing, settings.getApiKey(), label);
        if (existing == null) {
            SysAiProvider entity = new SysAiProvider();
            entity.setPurpose(purpose.name());
            entity.setProvider(provider.name());
            entity.setPreset(preset.name());
            entity.setBaseUrl(baseUrl);
            entity.setModel(model);
            entity.setTimeoutSeconds(timeout);
            entity.setApiKeyCipher(cipher);
            providerMapper.insert(entity);
            return;
        }

        UpdateWrapper<SysAiProvider> update =
                new UpdateWrapper<SysAiProvider>()
                        .eq("purpose", purpose.name())
                        .set("provider", provider.name())
                        .set("preset", preset.name())
                        .set("base_url", baseUrl)
                        .set("model", model)
                        .set("timeout_seconds", timeout)
                        .set("api_key_cipher", cipher);
        SysAiProvider patch = new SysAiProvider();
        patch.setUpdateBy(SecurityUtils.getCurrentUserIdOrDefault());
        providerMapper.update(patch, update);
    }

    /**
     * 解析 API Key 密文：null 保持原值；空串清空；非空加密。
     *
     * @param existing 现有配置（可能为 null）
     * @param apiKey 请求中的 apiKey
     * @param label 用途中文名（用于异常提示）
     * @return 应写入的密文
     */
    private String resolveCipher(SysAiProvider existing, String apiKey, String label) {
        if (apiKey == null) {
            return existing == null ? null : existing.getApiKeyCipher();
        }
        if (!StringUtils.hasText(apiKey)) {
            return null;
        }
        try {
            return sensitiveEncryptor.encrypt(apiKey.trim());
        } catch (Exception e) {
            log.warn("AI 配置 API Key 加密失败（{}）", label);
            throw new BusinessException(ErrorCode.SYSTEM_INTERNAL_ERROR, "API Key 加密失败");
        }
    }

    private SysAiProvider selectPurpose(AiPurpose purpose) {
        return providerMapper.selectOne(
                new QueryWrapper<SysAiProvider>()
                        .eq("purpose", purpose.name())
                        .last("LIMIT 1"));
    }

    /** 保存全局开关到 sys_config。 */
    private void saveEnabled(boolean enabled) {
        String value = Boolean.toString(enabled);
        SysConfig existing =
                configMapper.selectOne(
                        new QueryWrapper<SysConfig>()
                                .eq("config_key", AI_ENABLED_CONFIG_KEY)
                                .last("LIMIT 1"));
        if (existing == null) {
            SysConfig config = new SysConfig();
            config.setConfigKey(AI_ENABLED_CONFIG_KEY);
            config.setConfigValue(value);
            config.setConfigType("BOOL");
            config.setDescription("AI 全局开关");
            configMapper.insert(config);
            return;
        }
        existing.setConfigValue(value);
        configMapper.updateById(existing);
    }

    /** 组装连接测试配置：请求字段优先，缺省取当前保存值。 */
    private AiPurposeConfig mergeForTest(AiPurpose purpose, AiSettingsTestRequest request) {
        AiPurposeConfig current = purposeConfig(purpose);
        AiPurposeConfig merged = new AiPurposeConfig();
        merged.setPurpose(purpose);

        AiProvider provider =
                StringUtils.hasText(request.getProvider())
                        ? AiProvider.from(request.getProvider())
                        : current.getProvider();
        if (provider == null) {
            throw new IllegalArgumentException("提供商不合法，仅支持 OLLAMA / OPENAI_COMPAT");
        }
        merged.setProvider(provider);

        if (StringUtils.hasText(request.getPreset())) {
            AiPreset preset = AiPreset.from(request.getPreset());
            if (preset == null) {
                throw new IllegalArgumentException("厂商预设不合法");
            }
            merged.setPreset(preset);
        } else {
            merged.setPreset(current.getPreset());
        }

        String baseUrl = AiConfigSupport.normalizeBaseUrl(request.getBaseUrl());
        if (baseUrl == null) {
            baseUrl = current.getBaseUrl();
        }
        if (!AiConfigSupport.isHttpUrl(baseUrl)) {
            throw new IllegalArgumentException("服务地址必须以 http:// 或 https:// 开头");
        }
        merged.setBaseUrl(baseUrl);

        merged.setModel(
                StringUtils.hasText(request.getModel()) ? request.getModel().trim() : current.getModel());
        Integer timeout = request.getTimeoutSeconds();
        merged.setTimeoutSeconds(
                timeout != null && timeout >= MIN_TIMEOUT_SECONDS && timeout <= MAX_TIMEOUT_SECONDS
                        ? timeout
                        : current.getTimeoutSeconds());
        merged.setApiKey(
                StringUtils.hasText(request.getApiKey()) ? request.getApiKey().trim() : current.getApiKey());
        return merged;
    }

    private AiSettingsTestVO fail(String message, long startNanos) {
        return new AiSettingsTestVO(false, message, elapsedMillis(startNanos));
    }

    private long elapsedMillis(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000L;
    }

    // ------------------------------------------------------------------
    // 展示与审计
    // ------------------------------------------------------------------

    private AiProviderSettingsVO toVO(AiPurposeConfig config) {
        AiProviderSettingsVO vo = new AiProviderSettingsVO();
        vo.setProvider(config.getProvider().name());
        vo.setPreset(config.getPreset().name());
        vo.setBaseUrl(config.getBaseUrl());
        vo.setModel(config.getModel());
        vo.setTimeoutSeconds(config.getTimeoutSeconds());
        boolean openAiCompatible = config.getProvider() == AiProvider.OPENAI_COMPAT;
        boolean keySet = openAiCompatible && StringUtils.hasText(config.getApiKey());
        vo.setApiKeySet(keySet);
        vo.setApiKeyMasked(keySet ? AiConfigSupport.maskApiKey(config.getApiKey()) : null);
        return vo;
    }

    /** 写入操作日志：失败仅告警，不影响配置保存结果。 */
    private void recordAudit() {
        try {
            OperationLog log = new OperationLog();
            log.setUserId(SecurityUtils.getCurrentUserIdOrDefault());
            log.setOperationType(OperationType.UPDATE);
            log.setOperationDescription("修改 AI 提供商配置（TEXT/VISION）");
            log.setTargetType("sys_ai_provider");
            log.setStatus("SUCCESS");
            RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
            if (attributes instanceof ServletRequestAttributes servletAttributes) {
                HttpServletRequest httpRequest = servletAttributes.getRequest();
                log.setRequestMethod(httpRequest.getMethod());
                log.setRequestUri(httpRequest.getRequestURI());
                log.setIpAddress(httpRequest.getRemoteAddr());
                log.setUserAgent(httpRequest.getHeader("User-Agent"));
            }
            operationLogMapper.insert(log);
        } catch (Exception e) {
            log.warn("写入 AI 配置变更审计日志失败：{}", e.getMessage());
        }
    }
}
