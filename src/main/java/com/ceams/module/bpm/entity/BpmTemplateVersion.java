package com.ceams.module.bpm.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ceams.common.base.BaseEntity;
import com.ceams.module.bpm.enums.TemplateVersionStatus;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 流程模板版本（bpm_template_version）。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("bpm_template_version")
public class BpmTemplateVersion extends BaseEntity {

    /** 模板 ID。 */
    private Long templateId;

    /** 版本号（同一模板内递增）。 */
    private Integer versionNo;

    /** 流程定义 JSON。 */
    private String schemaJson;

    /** 版本状态。 */
    private TemplateVersionStatus status = TemplateVersionStatus.DRAFT;

    /** 生效时间。 */
    private LocalDateTime effectiveTime;
}
