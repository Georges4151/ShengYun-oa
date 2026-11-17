package com.ceams.module.bpm.enums;

/** 流程模板版本状态（bpm_template_version.status）。 */
public enum TemplateVersionStatus {
    /** 草稿：不可用于发起。 */
    DRAFT,
    /** 已发布：可作为当前版本发起新实例。 */
    PUBLISHED,
    /** 已归档：历史实例仍锁定该版本，但不再作为新实例版本。 */
    ARCHIVED
}
