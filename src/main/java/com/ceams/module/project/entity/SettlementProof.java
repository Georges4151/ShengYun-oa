package com.ceams.module.project.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ceams.common.base.BaseEntity;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 结算凭证（biz_settlement_proof）。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_settlement_proof")
public class SettlementProof extends BaseEntity {

    /** 分包合同 ID（逻辑引用 biz_subcontractor_contract.id）。 */
    private Long contractId;

    /** 分包商 ID（逻辑引用 biz_subcontractor.id）。 */
    private Long subcontractorId;

    /** 凭证名。 */
    private String proofName;

    /** 凭证编号。 */
    private String proofNumber;

    /** 凭证日期。 */
    private LocalDateTime proofDate;

    /** 结算金额。 */
    private BigDecimal settlementAmount;

    /** 凭证文件路径。 */
    private String filePath;

    /** 凭证文件名。 */
    private String fileName;

    /** 备注。 */
    private String description;
}
