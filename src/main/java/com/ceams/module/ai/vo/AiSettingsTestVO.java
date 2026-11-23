package com.ceams.module.ai.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

/** AI 连接测试结果。 */
@Data
@AllArgsConstructor
public class AiSettingsTestVO {

    /** 是否成功。 */
    private boolean success;

    /** 中文提示。 */
    private String message;

    /** 耗时（毫秒）。 */
    private long latencyMs;
}
