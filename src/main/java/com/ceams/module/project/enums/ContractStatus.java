package com.ceams.module.project.enums;

/**
 * 分包合同状态（biz_subcontractor_contract.status）。
 *
 * <p>枚举 name 即 varchar 代码值，与契约 SubcontractorContractVO.status 一致。
 */
public enum ContractStatus {
    DRAFT,
    ACTIVE,
    COMPLETED,
    TERMINATED
}
