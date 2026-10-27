package com.ceams.util;

import com.ceams.model.entity.BankStatement;
import com.ceams.model.entity.PaymentTransaction;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class ReconciliationMatcher {

    /**
     * 匹配银行流水与系统交易
     * 
     * @param bankStatements 银行流水列表
     * @param transactions   系统交易列表
     * @return 匹配结果列表
     */
    public List<Map<String, Object>> match(List<BankStatement> bankStatements, List<PaymentTransaction> transactions) {
        List<Map<String, Object>> matches = new ArrayList<>();
        
        // 1. 复制列表以避免修改原始数据
        List<BankStatement> unmatchedStatements = new ArrayList<>(bankStatements);
        List<PaymentTransaction> unmatchedTransactions = new ArrayList<>(transactions);
        
        // 2. 执行精确匹配
        matches.addAll(exactMatch(unmatchedStatements, unmatchedTransactions));
        
        // 3. 执行模糊匹配
        matches.addAll(fuzzyMatch(unmatchedStatements, unmatchedTransactions));
        
        return matches;
    }

    /**
     * 精确匹配
     * 
     * @param unmatchedStatements 未匹配的银行流水
     * @param unmatchedTransactions 未匹配的系统交易
     * @return 匹配结果
     */
    private List<Map<String, Object>> exactMatch(List<BankStatement> unmatchedStatements, List<PaymentTransaction> unmatchedTransactions) {
        List<Map<String, Object>> matches = new ArrayList<>();
        List<BankStatement> matchedStatements = new ArrayList<>();
        List<PaymentTransaction> matchedTransactions = new ArrayList<>();
        
        for (BankStatement statement : unmatchedStatements) {
            for (PaymentTransaction transaction : unmatchedTransactions) {
                if (isExactMatch(statement, transaction)) {
                    Map<String, Object> match = createMatchResult(statement, transaction, "matched", null);
                    matches.add(match);
                    matchedStatements.add(statement);
                    matchedTransactions.add(transaction);
                    break;
                }
            }
        }
        
        // 从待匹配列表中移除已匹配的记录
        unmatchedStatements.removeAll(matchedStatements);
        unmatchedTransactions.removeAll(matchedTransactions);
        
        return matches;
    }

    /**
     * 模糊匹配
     * 
     * @param unmatchedStatements 未匹配的银行流水
     * @param unmatchedTransactions 未匹配的系统交易
     * @return 匹配结果
     */
    private List<Map<String, Object>> fuzzyMatch(List<BankStatement> unmatchedStatements, List<PaymentTransaction> unmatchedTransactions) {
        List<Map<String, Object>> matches = new ArrayList<>();
        List<BankStatement> matchedStatements = new ArrayList<>();
        List<PaymentTransaction> matchedTransactions = new ArrayList<>();
        
        for (BankStatement statement : unmatchedStatements) {
            for (PaymentTransaction transaction : unmatchedTransactions) {
                String matchResult = isFuzzyMatch(statement, transaction);
                if (matchResult != null) {
                    Map<String, Object> match = createMatchResult(statement, transaction, "partially_matched", matchResult);
                    matches.add(match);
                    matchedStatements.add(statement);
                    matchedTransactions.add(transaction);
                    break;
                }
            }
        }
        
        // 从待匹配列表中移除已匹配的记录
        unmatchedStatements.removeAll(matchedStatements);
        unmatchedTransactions.removeAll(matchedTransactions);
        
        return matches;
    }

    /**
     * 检查是否为精确匹配
     * 
     * @param statement 银行流水
     * @param transaction 系统交易
     * @return 是否精确匹配
     */
    private boolean isExactMatch(BankStatement statement, PaymentTransaction transaction) {
        // 检查金额是否匹配
        if (!statement.getAmount().equals(transaction.getAmount())) {
            return false;
        }
        
        // 检查日期是否匹配
        if (!isDateMatch(statement.getTransactionDate(), transaction.getTransactionDate(), 0)) {
            return false;
        }
        
        // 检查描述是否匹配
        if (!isDescriptionMatch(statement.getDescription(), transaction.getDescription())) {
            return false;
        }
        
        return true;
    }

    /**
     * 检查是否为模糊匹配
     * 
     * @param statement 银行流水
     * @param transaction 系统交易
     * @return 匹配类型或null
     */
    private String isFuzzyMatch(BankStatement statement, PaymentTransaction transaction) {
        // 1. 金额匹配但日期不匹配
        if (statement.getAmount().equals(transaction.getAmount())) {
            if (!isDateMatch(statement.getTransactionDate(), transaction.getTransactionDate(), 0)) {
                if (isDateMatch(statement.getTransactionDate(), transaction.getTransactionDate(), 3)) {
                    return "date_mismatch";
                }
            }
        }
        
        // 2. 金额接近且日期匹配
        if (isAmountClose(statement.getAmount(), transaction.getAmount()) &&
            isDateMatch(statement.getTransactionDate(), transaction.getTransactionDate(), 1)) {
            return "amount_mismatch";
        }
        
        // 3. 金额匹配但描述不匹配
        if (statement.getAmount().equals(transaction.getAmount()) &&
            isDateMatch(statement.getTransactionDate(), transaction.getTransactionDate(), 1)) {
            if (!isDescriptionMatch(statement.getDescription(), transaction.getDescription())) {
                return "description_mismatch";
            }
        }
        
        return null;
    }

    /**
     * 检查日期是否匹配
     * 
     * @param date1 日期1
     * @param date2 日期2
     * @param tolerance 允许的天数差异
     * @return 是否匹配
     */
    private boolean isDateMatch(Date date1, Date date2, int tolerance) {
        if (date1 == null || date2 == null) {
            return false;
        }
        
        long diff = Math.abs(date1.getTime() - date2.getTime());
        long diffDays = diff / (1000 * 60 * 60 * 24);
        
        return diffDays <= tolerance;
    }

    /**
     * 检查金额是否接近
     * 
     * @param amount1 金额1
     * @param amount2 金额2
     * @return 是否接近
     */
    private boolean isAmountClose(BigDecimal amount1, BigDecimal amount2) {
        if (amount1 == null || amount2 == null) {
            return false;
        }
        
        BigDecimal diff = amount1.subtract(amount2).abs();
        BigDecimal threshold = amount1.max(amount2).multiply(new BigDecimal("0.01"));
        
        return diff.compareTo(threshold) <= 0;
    }

    /**
     * 检查描述是否匹配
     * 
     * @param desc1 描述1
     * @param desc2 描述2
     * @return 是否匹配
     */
    private boolean isDescriptionMatch(String desc1, String desc2) {
        if (desc1 == null || desc2 == null) {
            return false;
        }
        
        // 标准化描述
        String normalizedDesc1 = normalizeDescription(desc1);
        String normalizedDesc2 = normalizeDescription(desc2);
        
        // 检查是否包含相同的关键词
        return normalizedDesc1.equals(normalizedDesc2) ||
               normalizedDesc1.contains(normalizedDesc2) ||
               normalizedDesc2.contains(normalizedDesc1) ||
               hasCommonKeywords(normalizedDesc1, normalizedDesc2);
    }

    /**
     * 标准化描述
     * 
     * @param description 描述
     * @return 标准化后的描述
     */
    private String normalizeDescription(String description) {
        return description.toLowerCase()
                .replaceAll("\\s+", " ")
                .replaceAll("[\\p{Punct}]", "")
                .trim();
    }

    /**
     * 检查是否有共同的关键词
     * 
     * @param desc1 描述1
     * @param desc2 描述2
     * @return 是否有共同关键词
     */
    private boolean hasCommonKeywords(String desc1, String desc2) {
        List<String> keywords1 = extractKeywords(desc1);
        List<String> keywords2 = extractKeywords(desc2);
        
        return keywords1.stream().anyMatch(keywords2::contains);
    }

    /**
     * 提取关键词
     * 
     * @param description 描述
     * @return 关键词列表
     */
    private List<String> extractKeywords(String description) {
        // 移除常见的无意义词
        List<String> stopWords = List.of("支付", "转账", "交易", "费用", "款项", "金额", "人民币", "元", "整");
        
        return List.of(description.split(" "))
                .stream()
                .filter(word -> word.length() > 1)
                .filter(word -> !stopWords.contains(word))
                .collect(Collectors.toList());
    }

    /**
     * 创建匹配结果
     * 
     * @param statement 银行流水
     * @param transaction 系统交易
     * @param status 匹配状态
     * @param discrepancyType 差异类型
     * @return 匹配结果
     */
    private Map<String, Object> createMatchResult(BankStatement statement, PaymentTransaction transaction, String status, String discrepancyType) {
        Map<String, Object> match = new HashMap<>();
        match.put("statement", statement);
        match.put("transaction", transaction);
        match.put("status", status);
        match.put("discrepancyType", discrepancyType);
        return match;
    }

    /**
     * 基于金额分组匹配
     * 
     * @param bankStatements 银行流水列表
     * @param transactions 系统交易列表
     * @return 按金额分组的匹配结果
     */
    public Map<BigDecimal, List<Map<String, Object>>> matchByAmount(List<BankStatement> bankStatements, List<PaymentTransaction> transactions) {
        Map<BigDecimal, List<BankStatement>> statementsByAmount = bankStatements.stream()
                .collect(Collectors.groupingBy(BankStatement::getAmount));
        
        Map<BigDecimal, List<PaymentTransaction>> transactionsByAmount = transactions.stream()
                .collect(Collectors.groupingBy(PaymentTransaction::getAmount));
        
        Map<BigDecimal, List<Map<String, Object>>> matchesByAmount = new HashMap<>();
        
        for (BigDecimal amount : statementsByAmount.keySet()) {
            if (transactionsByAmount.containsKey(amount)) {
                List<BankStatement> statements = statementsByAmount.get(amount);
                List<PaymentTransaction> txns = transactionsByAmount.get(amount);
                List<Map<String, Object>> matches = new ArrayList<>();
                
                for (BankStatement statement : statements) {
                    for (PaymentTransaction transaction : txns) {
                        if (isDateMatch(statement.getTransactionDate(), transaction.getTransactionDate(), 3)) {
                            Map<String, Object> match = createMatchResult(statement, transaction, "matched", null);
                            matches.add(match);
                        }
                    }
                }
                
                if (!matches.isEmpty()) {
                    matchesByAmount.put(amount, matches);
                }
            }
        }
        
        return matchesByAmount;
    }

    /**
     * 基于日期范围匹配
     * 
     * @param bankStatements 银行流水列表
     * @param transactions 系统交易列表
     * @param dateRange 日期范围（天）
     * @return 匹配结果
     */
    public List<Map<String, Object>> matchByDateRange(List<BankStatement> bankStatements, List<PaymentTransaction> transactions, int dateRange) {
        List<Map<String, Object>> matches = new ArrayList<>();
        
        for (BankStatement statement : bankStatements) {
            for (PaymentTransaction transaction : transactions) {
                if (isDateMatch(statement.getTransactionDate(), transaction.getTransactionDate(), dateRange) &&
                    isAmountClose(statement.getAmount(), transaction.getAmount())) {
                    Map<String, Object> match = createMatchResult(statement, transaction, "partially_matched", "date_range_match");
                    matches.add(match);
                }
            }
        }
        
        return matches;
    }
}
