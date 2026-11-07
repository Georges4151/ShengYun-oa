package com.ceams.module.ai.vo;

import com.ceams.module.project.enums.ContractStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import lombok.Data;

/** 合同要点与风险提示视图对象（只读，不修改合同）。 */
@Data
public class ContractRiskHintVO {

    /** 合同 ID（雪花 ID 序列化为字符串）。 */
    private String contractId;

    /** 合同编号。 */
    private String contractNumber;

    /** 合同名称。 */
    private String contractName;

    /** 分包商 ID（雪花 ID 序列化为字符串）。 */
    private String subcontractorId;

    /** 项目 ID（雪花 ID 序列化为字符串，可为空）。 */
    private String projectId;

    /** 合同状态。 */
    private ContractStatus status;

    /** 合同金额。 */
    private BigDecimal contractAmount;

    /** 已付金额。 */
    private BigDecimal paidAmount;

    /** 未付金额。 */
    private BigDecimal remainingAmount;

    /** 付款比例（百分数，保留两位小数）。 */
    private BigDecimal paidRatio;

    /** 生效日期。 */
    private LocalDate startDate;

    /** 截止日期。 */
    private LocalDate endDate;

    /** 合同要点清单。 */
    private List<ContractClausePointVO> clausePoints;

    /** 风险提示清单（按等级降序、同级按固定顺序）。 */
    private List<ContractRiskVO> risks;

    /** 最高风险等级：HIGH / MEDIUM / LOW / NONE。 */
    private String riskLevel;

    /** AI 复述结果（未启用、失败或校验不通过时为 null）。 */
    private String aiNote;

    /** AI 能力是否已开启（不代表一定产出 note）。 */
    private boolean aiEnabled;

    /** 只读提示。 */
    private String disclaimer;
}
