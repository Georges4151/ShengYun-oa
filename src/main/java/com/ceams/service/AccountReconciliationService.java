package com.ceams.service;

import com.ceams.model.entity.Account;
import com.ceams.model.entity.SubcontractorContract;

import java.util.List;
import java.util.Map;

public interface AccountReconciliationService {
    /**
     * 核对所有合同的已支付金额与账目记录的一致性
     * @return 核对结果
     */
    Map<String, Object> reconcileAllContracts();

    /**
     * 核对单个合同的已支付金额与账目记录的一致性
     * @param contractId 合同ID
     * @return 核对结果
     */
    Map<String, Object> reconcileContract(Integer contractId);

    /**
     * 生成核对报告
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 核对报告
     */
    Map<String, Object> generateReconciliationReport(String startDate, String endDate);

    /**
     * 获取差异记录
     * @return 差异记录列表
     */
    List<Map<String, Object>> getDiscrepancies();

    /**
     * 修复差异
     * @param contractId 合同ID
     * @return 修复结果
     */
    boolean fixDiscrepancy(Integer contractId);
}
