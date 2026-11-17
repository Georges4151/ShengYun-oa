package com.ceams.module.hr.vo;

import com.ceams.module.hr.dto.PerformanceItem;
import com.ceams.module.hr.dto.PerformanceScoreItem;
import com.ceams.module.hr.enums.PerformanceReviewStatus;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

/** 绩效考评单视图对象。 */
@Data
public class PerformanceReviewVO {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long employeeId;

    private String employeeName;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long templateId;

    private String templateName;

    /** 模板考评项快照，供自评/上级打分表单渲染。 */
    private List<PerformanceItem> templateItems;

    private String period;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long leaderId;

    /** 自评明细。 */
    private List<PerformanceScoreItem> selfItems;

    private BigDecimal selfScore;

    private String selfComment;

    private LocalDateTime selfSubmittedAt;

    /** 上级评分明细。 */
    private List<PerformanceScoreItem> leaderItems;

    private BigDecimal leaderScore;

    private String leaderComment;

    private BigDecimal finalScore;

    private BigDecimal coefficient;

    private PerformanceReviewStatus status;

    /** 当前登录员工是否可提交自评（只读派生字段，不落库）。 */
    private Boolean canSelfEvaluate;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long bpmInstanceId;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
