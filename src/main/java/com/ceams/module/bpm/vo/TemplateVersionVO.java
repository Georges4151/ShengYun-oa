package com.ceams.module.bpm.vo;

import com.ceams.module.bpm.enums.TemplateVersionStatus;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.time.LocalDateTime;
import lombok.Data;

/** 流程模板版本视图。 */
@Data
public class TemplateVersionVO {

    /** 雪花 ID 超出 JS 安全整数范围，统一按字符串输出，避免前端精度丢失。 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long templateId;
    private Integer versionNo;
    private String schemaJson;
    private TemplateVersionStatus status;
    private LocalDateTime effectiveTime;
    private LocalDateTime createTime;
}
