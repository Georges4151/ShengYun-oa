package com.ceams.module.project.vo;

import com.ceams.common.enums.ContractApprovalStatus;
import com.ceams.module.project.enums.ContractStatus;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

/** 分包合同视图对象。 */
@Data
public class SubcontractorContractVO {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long subcontractorId;

    private String subcontractorName;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long projectId;

    private String projectName;

    private String contractName;

    private String contractNumber;

    private BigDecimal contractAmount;

    private LocalDate startDate;

    private LocalDate endDate;

    private ContractStatus status;

    /** 审批状态快照。 */
    private ContractApprovalStatus approvalStatus;

    /** 关联 BPM 流程实例 ID。 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long bpmInstanceId;

    private String description;

    private String filePath;

    private String fileName;

    private BigDecimal paidAmount;

    private LocalDateTime createTime;
}
