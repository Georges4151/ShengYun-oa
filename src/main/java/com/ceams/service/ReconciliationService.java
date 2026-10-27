package com.ceams.service;

import com.ceams.model.entity.BankStatement;
import com.ceams.model.entity.PaymentTransaction;
import com.ceams.model.entity.ReconciliationResult;
import com.ceams.util.ReconciliationMatcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReconciliationService {

    @Autowired
    private BankStatementService bankStatementService;

    @Autowired
    private PaymentTransactionService paymentTransactionService;

    @Autowired
    private ReconciliationMatcher reconciliationMatcher;

    /**
     * 执行银行对账
     * 
     * @param accountId 账户ID
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @return 对账结果
     */
    @Transactional
    public Map<String, Object> reconcileBankStatements(Integer accountId, Date startDate, Date endDate) {
        Map<String, Object> result = new HashMap<>();

        // 1. 获取银行流水
        List<BankStatement> bankStatements = bankStatementService.getBankStatementsByAccountIdAndDateRange(accountId,
                startDate, endDate);

        // 2. 获取系统交易记录
        List<PaymentTransaction> transactions = paymentTransactionService
                .getTransactionsByAccountIdAndDateRange(accountId, startDate, endDate);

        // 3. 执行匹配算法
        List<ReconciliationResult> reconciliationResults = reconcileBankStatementsWithTransactions(
                bankStatements, transactions, accountId, startDate, endDate);

        // 4. 分析匹配结果
        Map<String, Object> analysis = analyzeReconciliationResults(reconciliationResults);

        // 5. 构建返回结果
        result.put("bankStatements", bankStatements);
        result.put("transactions", transactions);
        result.put("reconciliationResults", reconciliationResults);
        result.put("analysis", analysis);
        result.put("accountId", accountId);
        result.put("startDate", startDate);
        result.put("endDate", endDate);

        return result;
    }

    /**
     * 执行银行流水与系统交易的匹配
     * 
     * @param bankStatements 银行流水列表
     * @param transactions   系统交易列表
     * @param accountId      账户ID
     * @param startDate      开始日期
     * @param endDate        结束日期
     * @return 对账结果列表
     */
    private List<ReconciliationResult> reconcileBankStatementsWithTransactions(
            List<BankStatement> bankStatements, List<PaymentTransaction> transactions,
            Integer accountId, Date startDate, Date endDate) {
        List<ReconciliationResult> results = new ArrayList<>();

        // 1. 标记已处理的记录
        List<BankStatement> unmatchedBankStatements = new ArrayList<>(bankStatements);
        List<PaymentTransaction> unmatchedTransactions = new ArrayList<>(transactions);

        // 2. 使用匹配器执行智能匹配
        List<Map<String, Object>> matches = reconciliationMatcher.match(bankStatements, transactions);

        // 3. 处理匹配结果
        for (Map<String, Object> match : matches) {
            BankStatement statement = (BankStatement) match.get("statement");
            PaymentTransaction transaction = (PaymentTransaction) match.get("transaction");
            String matchStatus = (String) match.get("status");
            String discrepancyType = (String) match.get("discrepancyType");

            // 创建对账结果
            ReconciliationResult result = createReconciliationResult(
                    accountId, statement, transaction, matchStatus, discrepancyType);
            results.add(result);

            // 从待匹配列表中移除已匹配的记录
            unmatchedBankStatements.remove(statement);
            unmatchedTransactions.remove(transaction);

            // 更新银行流水状态
            if (statement != null) {
                bankStatementService.updateBankStatementStatus(statement.getId(), "reconciled");
            }
        }

        // 4. 处理未匹配的银行流水
        for (BankStatement statement : unmatchedBankStatements) {
            ReconciliationResult result = createReconciliationResult(
                    accountId, statement, null, "unmatched", "missing_in_system");
            results.add(result);

            // 更新银行流水状态
            bankStatementService.updateBankStatementStatus(statement.getId(), "unreconciled");
        }

        // 5. 处理未匹配的系统交易
        for (PaymentTransaction transaction : unmatchedTransactions) {
            ReconciliationResult result = createReconciliationResult(
                    accountId, null, transaction, "unmatched", "missing_in_bank");
            results.add(result);
        }

        return results;
    }

    /**
     * 创建对账结果
     * 
     * @param accountId       账户ID
     * @param statement       银行流水
     * @param transaction     系统交易
     * @param status          匹配状态
     * @param discrepancyType 差异类型
     * @return 对账结果
     */
    private ReconciliationResult createReconciliationResult(
            Integer accountId, BankStatement statement, PaymentTransaction transaction,
            String status, String discrepancyType) {
        ReconciliationResult result = new ReconciliationResult();

        result.setAccountId(accountId);
        result.setStatus(status);
        result.setDiscrepancyType(discrepancyType);
        result.setResolutionStatus("unresolved");
        result.setReconciliationDate(new Date());

        if (statement != null) {
            result.setStatementId(statement.getId());
            result.setBankAmount(statement.getAmount());
            result.setBankDate(statement.getTransactionDate());
            result.setBankDescription(statement.getDescription());
            result.setStatementReference(statement.getBankReference());
        }

        if (transaction != null) {
            result.setTransactionId(transaction.getId());
            result.setSystemAmount(transaction.getAmount());
            result.setSystemDate(transaction.getTransactionDate());
            result.setSystemDescription(transaction.getDescription());
            result.setTransactionReference(transaction.getReceiptNumber());
        }

        return result;
    }

    /**
     * 分析对账结果
     * 
     * @param reconciliationResults 对账结果列表
     * @return 分析结果
     */
    private Map<String, Object> analyzeReconciliationResults(List<ReconciliationResult> reconciliationResults) {
        Map<String, Object> analysis = new HashMap<>();

        int totalRecords = reconciliationResults.size();
        int matchedRecords = 0;
        int unmatchedRecords = 0;
        int partiallyMatchedRecords = 0;

        int missingInBank = 0;
        int missingInSystem = 0;
        int amountMismatch = 0;
        int dateMismatch = 0;
        int descriptionMismatch = 0;

        BigDecimal totalBankAmount = BigDecimal.ZERO;
        BigDecimal totalSystemAmount = BigDecimal.ZERO;

        for (ReconciliationResult result : reconciliationResults) {
            if ("matched".equals(result.getStatus())) {
                matchedRecords++;
            } else if ("partially_matched".equals(result.getStatus())) {
                partiallyMatchedRecords++;
            } else if ("unmatched".equals(result.getStatus())) {
                unmatchedRecords++;
            }

            if ("missing_in_bank".equals(result.getDiscrepancyType())) {
                missingInBank++;
            } else if ("missing_in_system".equals(result.getDiscrepancyType())) {
                missingInSystem++;
            } else if ("amount_mismatch".equals(result.getDiscrepancyType())) {
                amountMismatch++;
            } else if ("date_mismatch".equals(result.getDiscrepancyType())) {
                dateMismatch++;
            } else if ("description_mismatch".equals(result.getDiscrepancyType())) {
                descriptionMismatch++;
            }

            if (result.getBankAmount() != null) {
                totalBankAmount = totalBankAmount.add(result.getBankAmount());
            }

            if (result.getSystemAmount() != null) {
                totalSystemAmount = totalSystemAmount.add(result.getSystemAmount());
            }
        }

        BigDecimal amountDifference = totalBankAmount.subtract(totalSystemAmount);

        analysis.put("totalRecords", totalRecords);
        analysis.put("matchedRecords", matchedRecords);
        analysis.put("unmatchedRecords", unmatchedRecords);
        analysis.put("partiallyMatchedRecords", partiallyMatchedRecords);
        analysis.put("missingInBank", missingInBank);
        analysis.put("missingInSystem", missingInSystem);
        analysis.put("amountMismatch", amountMismatch);
        analysis.put("dateMismatch", dateMismatch);
        analysis.put("descriptionMismatch", descriptionMismatch);
        analysis.put("totalBankAmount", totalBankAmount);
        analysis.put("totalSystemAmount", totalSystemAmount);
        analysis.put("amountDifference", amountDifference);

        return analysis;
    }

    /**
     * 手动匹配银行流水与系统交易
     * 
     * @param statementId   银行流水ID
     * @param transactionId 系统交易ID
     * @return 是否匹配成功
     */
    @Transactional
    public boolean manuallyMatchBankStatementWithTransaction(Integer statementId, Integer transactionId) {
        // 1. 获取银行流水
        BankStatement statement = bankStatementService.getBankStatementById(statementId);
        if (statement == null) {
            return false;
        }

        // 2. 获取系统交易
        PaymentTransaction transaction = paymentTransactionService.getTransactionById(transactionId);
        if (transaction == null) {
            return false;
        }

        // 3. 验证是否属于同一账户
        if (!statement.getAccountId().equals(transaction.getAccountId())) {
            return false;
        }

        // 4. 更新银行流水状态
        bankStatementService.updateBankStatementStatus(statementId, "reconciled");

        return true;
    }

    /**
     * 标记对账差异为已处理
     * 
     * @param reconciliationId 对账结果ID
     * @param resolutionNotes  处理备注
     * @return 是否标记成功
     */
    public boolean markDiscrepancyAsResolved(Integer reconciliationId, String resolutionNotes) {
        // 实现标记差异为已处理的逻辑
        return false;
    }

    /**
     * 从银行流水创建系统交易
     * 
     * @param statementId 银行流水ID
     * @return 是否创建成功
     */
    @Transactional
    public boolean createTransactionFromBankStatement(Integer statementId) {
        // 1. 获取银行流水
        BankStatement statement = bankStatementService.getBankStatementById(statementId);
        if (statement == null) {
            return false;
        }

        // 2. 创建系统交易
        PaymentTransaction transaction = new PaymentTransaction();
        transaction.setAccountId(statement.getAccountId());
        transaction.setAmount(statement.getAmount());
        transaction.setType(statement.getType());
        transaction.setTransactionDate(statement.getTransactionDate());
        transaction.setDescription(statement.getDescription());

        // 3. 保存系统交易
        boolean saved = paymentTransactionService.addTransaction(transaction);

        if (saved) {
            // 4. 更新银行流水状态
            bankStatementService.updateBankStatementStatus(statementId, "reconciled");
        }

        return saved;
    }

    /**
     * 导出对账结果
     * 
     * @param accountId 账户ID
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @return 导出的字节数组
     * @throws Exception 导出异常
     */
    public byte[] exportReconciliationResult(Integer accountId, Date startDate, Date endDate) throws Exception {
        // 实现导出对账结果的逻辑
        return null;
    }
}
