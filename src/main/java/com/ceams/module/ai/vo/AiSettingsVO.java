package com.ceams.module.ai.vo;

import java.util.List;
import lombok.Data;

/** AI 配置聚合展示对象。 */
@Data
public class AiSettingsVO {

    /** 全局开关。 */
    private boolean enabled;

    /** 文本对话配置。 */
    private AiProviderSettingsVO text;

    /** 票据视觉识别配置。 */
    private AiProviderSettingsVO vision;

    /** 内置预设列表。 */
    private List<AiPresetVO> presets;
}
