package com.ceams.module.bpm.vo;

import com.ceams.module.bpm.enums.TemplateVersionStatus;
import java.time.LocalDateTime;
import lombok.Data;

/** 流程模板版本视图。 */
@Data
public class TemplateVersionVO {

    private Long id;
    private Long templateId;
    private Integer versionNo;
    private String schemaJson;
    private TemplateVersionStatus status;
    private LocalDateTime effectiveTime;
    private LocalDateTime createTime;
}
