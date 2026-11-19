package com.ceams.module.bpm.enums;

/** 流程模板状态（bpm_template.status）。 */
public enum TemplateStatus {
    /** 已启用：允许新实例发起。 */
    ENABLED,
    /** 已停用：禁止新实例发起，历史实例不受影响。 */
    DISABLED
}
