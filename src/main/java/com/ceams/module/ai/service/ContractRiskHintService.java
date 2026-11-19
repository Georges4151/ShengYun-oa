package com.ceams.module.ai.service;

import com.ceams.module.ai.vo.ContractRiskHintVO;

/** 合同要点与风险提示服务（只读规则提示，绝不修改合同）。 */
public interface ContractRiskHintService {

    /** 单份分包合同的要点与风险提示；合同不存在时透传 {@code BIZ_NOT_FOUND}。 */
    ContractRiskHintVO riskHints(Long contractId);
}
