package com.ceams.module.project.vo;

import com.ceams.module.project.enums.ApprovalStatus;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

/** 进度款视图对象。 */
@Data
public class ProgressPaymentVO {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long projectId;

    /** 项目名称（列表/详情展示用，非持久化字段）。 */
    private String projectName;

    private String paymentPeriod;

    private BigDecimal applicationAmount;

    private BigDecimal approvedAmount;

    private BigDecimal paymentAmount;

    private LocalDate applicationDate;

    private LocalDate approvalDate;

    private LocalDate paymentDate;

    private ApprovalStatus status;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long applicationBy;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long approvedBy;

    private LocalDateTime createTime;
}
