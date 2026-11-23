package com.ceams.module.admin.vo;

import com.ceams.module.admin.enums.MaterialApplyStatus;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/** 易耗品申领单展示对象。 */
@Data
public class MaterialApplyVO {

    /** 申领单 ID。 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /** 物料 ID。 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long materialId;

    /** 物料编码。 */
    private String materialCode;

    /** 物料名称。 */
    private String materialName;

    /** 单位。 */
    private String unit;

    /** 申请人 ID。 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long applicantId;

    /** 申请人姓名。 */
    private String applicantName;

    /** 申领数量。 */
    private BigDecimal quantity;

    /** 申领事由。 */
    private String reason;

    /** BPM 实例 ID。 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long bpmInstanceId;

    /** 状态。 */
    private MaterialApplyStatus status;

    /** 发放时间。 */
    private LocalDateTime issuedAt;

    /** 发放人 ID。 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long issuedBy;

    /** 创建时间。 */
    private LocalDateTime createTime;
}
