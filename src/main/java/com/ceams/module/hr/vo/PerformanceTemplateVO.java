package com.ceams.module.hr.vo;

import com.ceams.module.hr.dto.PerformanceItem;
import com.ceams.module.hr.dto.PerformanceScoreRule;
import com.ceams.module.hr.enums.PeriodType;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

/** 绩效模板视图对象（items / scoreRules 已从 JSON 解析为结构化模型）。 */
@Data
public class PerformanceTemplateVO {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    private String code;

    private String name;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long positionId;

    private PeriodType periodType;

    /** 考评项。 */
    private List<PerformanceItem> items;

    /** 系数规则。 */
    private List<PerformanceScoreRule> scoreRules;

    private Integer totalWeight;

    private String status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
