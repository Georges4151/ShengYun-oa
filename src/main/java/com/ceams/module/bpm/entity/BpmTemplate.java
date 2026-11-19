package com.ceams.module.bpm.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ceams.common.base.BaseEntity;
import com.ceams.module.bpm.enums.TemplateStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 流程模板（bpm_template）。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("bpm_template")
public class BpmTemplate extends BaseEntity {

    /** 模板编码（唯一）。 */
    private String code;

    /** 模板名称。 */
    private String name;

    /** 业务类型（如 EXPENSE/PROJECT_CHANGE）。 */
    private String bizType;

    /** 当前启用版本 ID。 */
    private Long currentVersionId;

    /** 状态。 */
    private TemplateStatus status = TemplateStatus.DISABLED;
}
