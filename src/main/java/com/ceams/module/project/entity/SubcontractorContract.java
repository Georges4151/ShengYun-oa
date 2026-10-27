package com.ceams.module.project.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ceams.common.base.BaseEntity;
import com.ceams.module.project.enums.ContractStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 分包合同（biz_subcontractor_contract）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_subcontractor_contract")
public class SubcontractorContract extends BaseEntity {

    /** 分包商 ID（逻辑引用 biz_subcontractor.id）。 */
    private Long subcontractorId;

    /** 项目 ID（逻辑引用 biz_project.id）。 */
    private Long projectId;

    /** 合同名。 */
    private String contractName;

    /** 合同编号。 */
    private String contractNumber;

    /** 合同金额。 */
    private BigDecimal contractAmount;

    /** 开始日期。 */
    private LocalDateTime startDate;

    /** 结束日期。 */
    private LocalDateTime endDate;

    /** 合同状态。 */
    private ContractStatus status;

    /** 备注。 */
    private String description;

    /** 合同文件路径。 */
    private String filePath;

    /** 合同文件名。 */
    private String fileName;

    /** 已支付金额。 */
    private BigDecimal paidAmount = BigDecimal.ZERO;
}