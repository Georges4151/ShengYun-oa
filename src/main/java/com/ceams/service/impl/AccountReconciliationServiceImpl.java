package com.ceams.service.impl;

import com.ceams.service.AccountReconciliationService;
import com.ceams.service.AccountService;
import com.ceams.service.SubcontractorService;
import com.ceams.model.entity.Account;
import com.ceams.model.entity.SubcontractorContract;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AccountReconciliationServiceImpl implements AccountReconciliationService {

    @Autowired
    private AccountService accountService;

    @Autowired
    private SubcontractorService subcontractorService;

    @Override
    public Map<String, Object> reconcileAllContracts() {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> discrepancies = new ArrayList<>();
        int totalContracts = 0;
        int correctContracts = 0;

        // 获取所有合同
        List<SubcontractorContract> contracts = subcontractorService.getAllContracts();
        totalContracts = contracts.size();

        for (SubcontractorContract contract : contracts) {
            Map<String, Object> reconciliationResult = reconcileContract(contract.getId());
            if ((boolean) reconciliationResult.get("hasDiscrepancy")) {
                discrepancies.add(reconciliationResult);
            } else {
                correctContracts++;
            }
        }

        result.put("totalContracts", totalContracts);
        result.put("correctContracts", correctContracts);
        result.put("discrepancies", discrepancies);
        result.put("discrepancyCount", discrepancies.size());
        result.put("status", discrepancies.isEmpty() ? "success" : "warning");

        return result;
    }

    @Override
    public Map<String, Object> reconcileContract(Integer contractId) {
        Map<String, Object> result = new HashMap<>();

        // 获取合同
        SubcontractorContract contract = subcontractorService.getContractDetail(contractId);
        if (contract == null) {
            result.put("status", "error");
            result.put("message", "合同不存在");
            return result;
        }

        // 获取合同相关的账目记录
        List<Account> accounts = accountService.getByContractId(contractId);

        // 计算账目记录中的已支付金额
        BigDecimal accountPaidAmount = BigDecimal.ZERO;
        for (Account account : accounts) {
            if ("expense".equals(account.getType()) && "completed".equals(account.getStatus())) {
                accountPaidAmount = accountPaidAmount.add(account.getAmount());
            }
        }

        // 获取合同中记录的已支付金额
        BigDecimal contractPaidAmount = contract.getPaidAmount() != null ? contract.getPaidAmount() : BigDecimal.ZERO;

        // 检查是否有差异
        boolean hasDiscrepancy = accountPaidAmount.compareTo(contractPaidAmount) != 0;

        result.put("contractId", contractId);
        result.put("contractName", contract.getContractName());
        result.put("accountPaidAmount", accountPaidAmount);
        result.put("contractPaidAmount", contractPaidAmount);
        result.put("discrepancyAmount", accountPaidAmount.subtract(contractPaidAmount));
        result.put("hasDiscrepancy", hasDiscrepancy);
        result.put("status", hasDiscrepancy ? "warning" : "success");

        return result;
    }

    @Override
    public Map<String, Object> generateReconciliationReport(String startDate, String endDate) {
        Map<String, Object> result = new HashMap<>();

        // 执行所有合同的核对
        Map<String, Object> reconciliationResult = reconcileAllContracts();

        // 生成报告
        result.put("reconciliationResult", reconciliationResult);
        result.put("startDate", startDate);
        result.put("endDate", endDate);
        result.put("reportDate", new java.util.Date().toString());

        return result;
    }

    @Override
    public List<Map<String, Object>> getDiscrepancies() {
        Map<String, Object> reconciliationResult = reconcileAllContracts();
        Object discrepancies = reconciliationResult.get("discrepancies");
        if (discrepancies instanceof List<?>) {
            List<?> list = (List<?>) discrepancies;
            List<Map<String, Object>> result = new ArrayList<>();
            for (Object item : list) {
                if (item instanceof Map<?, ?>) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> map = (Map<String, Object>) item;
                    result.add(map);
                }
            }
            return result;
        }
        return new ArrayList<>();
    }

    @Override
    public boolean fixDiscrepancy(Integer contractId) {
        // 获取合同
        SubcontractorContract contract = subcontractorService.getContractDetail(contractId);
        if (contract == null) {
            return false;
        }

        // 获取合同相关的账目记录
        List<Account> accounts = accountService.getByContractId(contractId);

        // 计算账目记录中的已支付金额
        BigDecimal accountPaidAmount = BigDecimal.ZERO;
        for (Account account : accounts) {
            if ("expense".equals(account.getType()) && "completed".equals(account.getStatus())) {
                accountPaidAmount = accountPaidAmount.add(account.getAmount());
            }
        }

        // 更新合同的已支付金额
        contract.setPaidAmount(accountPaidAmount);
        return subcontractorService.updateContract(contract);
    }
}
