package com.ceams.module.bpm.vo;

import com.ceams.module.bpm.enums.TemplateStatus;
import java.time.LocalDateTime;
import lombok.Data;

/** 流程模板视图。 */
@Data
public class TemplateVO {

    private Long id;
    private String code;
    private String name;
    private String bizType;
    private Long currentVersionId;
    private TemplateStatus status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private TemplateVersionVO currentVersion;
}
