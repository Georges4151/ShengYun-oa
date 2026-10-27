package com.ceams.service;

import com.ceams.dao.PaymentTransactionMapper;
import com.ceams.model.Pagination;
import com.ceams.model.entity.PaymentTransaction;
import com.ceams.model.event.AccountDataChangedEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Service
public class PaymentTransactionService {

    @Autowired
    private PaymentTransactionMapper paymentTransactionMapper;

    @Autowired
    private PaymentAccountService paymentAccountService;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    /**
     * 获取所有交易记录
     * 
     * @return 交易记录列表
     */
    public List<PaymentTransaction> getAllTransactions() {
        return paymentTransactionMapper.selectAll();
    }

    /**
     * 根据账户ID获取交易记录
     * 
     * @param accountId 账户ID
     * @return 交易记录列表
     */
    public List<PaymentTransaction> getTransactionsByAccountId(Integer accountId) {
        return paymentTransactionMapper.selectByAccountId(accountId);
    }

    /**
     * 根据票据ID获取交易记录
     * 
     * @param receiptId 票据ID
     * @return 交易记录列表
     */
    public List<PaymentTransaction> getTransactionsByReceiptId(Integer receiptId) {
        return paymentTransactionMapper.selectByReceiptId(receiptId);
    }

    /**
     * 根据日期范围获取交易记录
     * 
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @return 交易记录列表
     */
    public List<PaymentTransaction> getTransactionsByDateRange(Date startDate, Date endDate) {
        // 添加日志，查看查询条件
        System.out.println("查询交易记录: 开始日期=" + startDate + ", 结束日期=" + endDate);

        // 如果开始日期或结束日期为null，查询所有交易记录
        if (startDate == null || endDate == null) {
            System.out.println("开始日期或结束日期为null，查询所有交易记录");
            return paymentTransactionMapper.selectAll();
        }

        return paymentTransactionMapper.selectByDateRange(startDate, endDate);
    }

    /**
     * 根据账户ID和日期范围获取交易记录
     * 
     * @param accountId 账户ID
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @return 交易记录列表
     */
    public List<PaymentTransaction> getTransactionsByAccountIdAndDateRange(
            Integer accountId, Date startDate, Date endDate) {
        // 添加日志，查看查询条件
        System.out.println("查询交易记录: 账户ID=" + accountId + ", 开始日期=" + startDate + ", 结束日期=" + endDate);

        // 如果开始日期或结束日期为null，根据账户ID查询所有交易记录
        if (startDate == null || endDate == null) {
            System.out.println("开始日期或结束日期为null，根据账户ID查询所有交易记录");
            return paymentTransactionMapper.selectByAccountId(accountId);
        }

        return paymentTransactionMapper.selectByAccountIdAndDateRange(accountId, startDate, endDate);
    }

    /**
     * 根据条件查询交易记录
     * 
     * @param accountId   账户ID
     * @param receiptId   票据ID
     * @param type        交易类型
     * @param startDate   开始日期
     * @param endDate     结束日期
     * @param description 交易描述
     * @return 交易记录列表
     */
    public List<PaymentTransaction> getTransactionsByConditions(
            Integer accountId,
            Integer receiptId,
            String type,
            Date startDate,
            Date endDate,
            String description) {
        // 添加日志，查看查询条件
        System.out.println("查询交易记录: 账户ID=" + accountId + ", 票据ID=" + receiptId + ", 类型=" + type + ", 开始日期=" + startDate
                + ", 结束日期=" + endDate + ", 描述=" + description);

        // 当所有条件都为null时，直接查询所有记录
        if (accountId == null && receiptId == null && (type == null || type.isEmpty())
                && startDate == null && endDate == null && (description == null || description.isEmpty())) {
            System.out.println("所有查询条件为空，直接查询所有交易记录");
            return paymentTransactionMapper.selectAll();
        }

        return paymentTransactionMapper.selectByConditions(accountId, receiptId, type, startDate, endDate, description);
    }

    /**
     * 根据ID获取交易记录
     * 
     * @param id 交易记录ID
     * @return 交易记录信息
     */
    public PaymentTransaction getTransactionById(Integer id) {
        return paymentTransactionMapper.selectById(id);
    }

    /**
     * 获取交易记录汇总信息
     * 
     * @param accountId 账户ID
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @return 汇总信息
     */
    public Map<String, Object> getTransactionSummary(
            Integer accountId, Date startDate, Date endDate) {
        return paymentTransactionMapper.getTransactionSummary(accountId, startDate, endDate);
    }

    /**
     * 添加交易记录
     * 
     * @param transaction 交易记录信息
     * @return 是否添加成功
     */
    @Transactional
    public boolean addTransaction(PaymentTransaction transaction) {
        // 插入交易记录
        int result = paymentTransactionMapper.insert(transaction);
        if (result > 0) {
            // 更新账户余额
            BigDecimal amount = transaction.getAmount();
            if ("expense".equals(transaction.getType())) {
                amount = amount.negate(); // 支出时减少余额
            }
            paymentAccountService.updateBalance(transaction.getAccountId(), amount);
            // 发布账目数据变化事件
            eventPublisher.publishEvent(new AccountDataChangedEvent(this, "添加了新交易记录"));
        }
        return result > 0;
    }

    /**
     * 更新交易记录
     * 
     * @param transaction 交易记录信息
     * @return 是否更新成功
     */
    @Transactional
    public boolean updateTransaction(PaymentTransaction transaction) {
        // 获取原交易记录
        PaymentTransaction oldTransaction = paymentTransactionMapper.selectById(transaction.getId());
        if (oldTransaction == null) {
            return false;
        }

        // 计算余额变动
        BigDecimal oldAmount = oldTransaction.getAmount();
        if ("expense".equals(oldTransaction.getType())) {
            oldAmount = oldAmount.negate();
        }

        BigDecimal newAmount = transaction.getAmount();
        if ("expense".equals(transaction.getType())) {
            newAmount = newAmount.negate();
        }

        BigDecimal balanceChange = newAmount.subtract(oldAmount);

        // 更新交易记录
        int result = paymentTransactionMapper.update(transaction);
        if (result > 0) {
            if (balanceChange.compareTo(BigDecimal.ZERO) != 0) {
                // 更新账户余额
                paymentAccountService.updateBalance(transaction.getAccountId(), balanceChange);
            }
            // 发布账目数据变化事件
            eventPublisher.publishEvent(new AccountDataChangedEvent(this, "更新了交易记录"));
        }
        return result > 0;
    }

    /**
     * 删除交易记录
     * 
     * @param id 交易记录ID
     * @return 是否删除成功
     */
    @Transactional
    public boolean deleteTransaction(Integer id) {
        // 获取原交易记录
        PaymentTransaction transaction = paymentTransactionMapper.selectById(id);
        if (transaction == null) {
            return false;
        }

        // 计算需要恢复的余额
        BigDecimal amount = transaction.getAmount();
        if ("income".equals(transaction.getType())) {
            amount = amount.negate(); // 收入时需要减少余额
        }

        // 删除交易记录
        int result = paymentTransactionMapper.deleteById(id);
        if (result > 0) {
            // 更新账户余额
            paymentAccountService.updateBalance(transaction.getAccountId(), amount);
            // 发布账目数据变化事件
            eventPublisher.publishEvent(new AccountDataChangedEvent(this, "删除了交易记录"));
        }
        return result > 0;
    }

    /**
     * 分页获取所有交易记录
     * 
     * @param pagination 分页参数
     * @return 交易记录列表
     */
    public List<PaymentTransaction> getTransactionsWithPagination(Pagination pagination) {
        return paymentTransactionMapper.selectAllWithPagination(pagination.getPageSize(), pagination.getOffset());
    }

    /**
     * 分页获取交易记录（支持条件）
     * 
     * @param accountId   账户ID
     * @param receiptId   票据ID
     * @param type        交易类型
     * @param startDate   开始日期
     * @param endDate     结束日期
     * @param description 交易描述
     * @param pagination  分页参数
     * @return 交易记录列表
     */
    public List<PaymentTransaction> getTransactionsByConditionsWithPagination(
            Integer accountId, Integer receiptId, String type, Date startDate, Date endDate, String description,
            Pagination pagination) {
        return paymentTransactionMapper.selectByConditionsWithPagination(
                accountId, receiptId, type, startDate, endDate, description, pagination.getPageSize(),
                pagination.getOffset());
    }

    /**
     * 获取交易记录总数（支持条件）
     * 
     * @param accountId   账户ID
     * @param receiptId   票据ID
     * @param type        交易类型
     * @param startDate   开始日期
     * @param endDate     结束日期
     * @param description 交易描述
     * @return 交易记录总数
     */
    public int getTransactionCountByConditions(
            Integer accountId, Integer receiptId, String type, Date startDate, Date endDate, String description) {
        return paymentTransactionMapper.countByConditions(accountId, receiptId, type, startDate, endDate, description);
    }
}