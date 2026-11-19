package com.ceams.module.bpm.vo;

import com.ceams.module.bpm.enums.TemplateStatus;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.time.LocalDateTime;
import lombok.Data;

/** 流程模板视图。 */
@Data
public class TemplateVO {

    /** 雪花 ID 超出 JS 安全整数范围，统一按字符串输出，避免前端精度丢失。 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    private String code;
    private String name;
    private String bizType;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long currentVersionId;
    private TemplateStatus status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private TemplateVersionVO currentVersion;
}
