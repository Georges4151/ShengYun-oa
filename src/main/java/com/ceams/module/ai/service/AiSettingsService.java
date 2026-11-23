package com.ceams.module.ai.service;

import com.ceams.module.ai.config.AiPurposeConfig;
import com.ceams.module.ai.dto.AiSettingsTestRequest;
import com.ceams.module.ai.dto.AiSettingsUpdateRequest;
import com.ceams.module.ai.enums.AiPurpose;
import com.ceams.module.ai.vo.AiSettingsTestVO;
import com.ceams.module.ai.vo.AiSettingsVO;

/** AI 提供商配置服务：读写配置、全局开关、连接测试与进程内缓存。 */
public interface AiSettingsService {

    /** 读取配置（API Key 仅返回掩码）。 */
    AiSettingsVO getSettings();

    /** 保存配置并立即刷新缓存。 */
    AiSettingsVO updateSettings(AiSettingsUpdateRequest request);

    /** 连接测试：失败不抛异常，返回中文原因。 */
    AiSettingsTestVO testConnection(AiSettingsTestRequest request);

    /** 全局 AI 开关：库中配置优先，缺失时回退 {@code ceams.ai.enabled}。 */
    boolean isEnabled();

    /** 取指定用途的运行时配置（含解密后的 API Key）。 */
    AiPurposeConfig purposeConfig(AiPurpose purpose);

    /** 配置修订号：每次保存后递增。 */
    long revision();

    /** 主动失效缓存（保存后调用）。 */
    void refresh();
}
