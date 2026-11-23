package com.ceams.module.ai.vo;

import lombok.Data;

/** 内置厂商预设展示对象。 */
@Data
public class AiPresetVO {

    /** 预设编码。 */
    private String code;

    /** 展示名称。 */
    private String name;

    /** 默认服务地址。 */
    private String baseUrl;

    /** 默认文本模型。 */
    private String textModel;

    /** 默认视觉模型（可能为 null）。 */
    private String visionModel;
}
