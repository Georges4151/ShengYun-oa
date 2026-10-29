package com.ceams.module.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ceams.common.exception.BusinessException;
import com.ceams.common.exception.ErrorCode;
import com.ceams.module.payment.dto.PaymentExecuteRequest;
import com.ceams.module.payment.entity.Account;
import com.ceams.module.payment.entity.PaymentAccount;
import com.ceams.module.payment.entity.PaymentTransaction;
import com.ceams.module.payment.enums.AccountStatus;
import com.ceams.module.payment.enums.AccountType;
import com.ceams.module.payment.enums.AllocationStatus;
import com.ceams.module.payment.mapper.AccountMapper;
import com.ceams.module.payment.mapper.PaymentAccountMapper;
import com.ceams.module.payment.mapper.PaymentTransactionMapper;
import com.ceams.module.payment.service.PaymentService;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 付款执行服务实现。
 *
 * <p>全部操作在一个事务内：校验账户 -> 幂等 -> 余额校验 -> 更新余额 -> 写付款交易 -> 写收支流水（CONFIRMED）。票据回写仅体现为
 * biz_payment_transaction.receipt_id 与 biz_account.receipt_id 的落库， 不直接写 expense 域。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentAccountMapper paymentAccountMapper;
    private final PaymentTransactionMapper paymentTransactionMapper;
    private final AccountMapper accountMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String execute(PaymentExecuteRequest request) {
        // 1. 校验付款账户
        PaymentAccount account = paymentAccountMapper.selectById(request.getAccountId());
        if (account == null) {
            throw BusinessException.of(ErrorCode.BIZ_NOT_FOUND, "付款账户不存在");
        }

        // 2. 幂等：同票据只允许执行一次付款
        if (request.getReceiptId() != null) {
            LambdaQueryWrapper<PaymentTransaction> idemWrapper = new LambdaQueryWrapper<>();
            idemWrapper.eq(PaymentTransaction::getReceiptId, request.getReceiptId());
            if (paymentTransactionMapper.selectCount(idemWrapper) > 0) {
                throw BusinessException.of(ErrorCode.BIZ_STATE_CONFLICT, "该票据已执行过付款");
            }
        }

        BigDecimal amount = request.getAmount();
        AccountType type = request.getType();
        BigDecimal currentBalance =
                account.getBalance() == null ? BigDecimal.ZERO : account.getBalance();

        // 3. 余额校验（支出）
        if (type == AccountType.EXPENSE && currentBalance.compareTo(amount) < 0) {
            throw BusinessException.of(ErrorCode.BIZ_UNPROCESSABLE, "余额不足");
        }

        // 4. 更新余额
        BigDecimal newBalance =
                type == AccountType.EXPENSE
                        ? currentBalance.subtract(amount)
                        : currentBalance.add(amount);
        account.setBalance(newBalance);
        paymentAccountMapper.updateById(account);

        // 5. 插入付款交易
        PaymentTransaction transaction = new PaymentTransaction();
        transaction.setAccountId(request.getAccountId());
        transaction.setReceiptId(request.getReceiptId());
        transaction.setProjectId(request.getProjectId());
        transaction.setAmount(amount);
        transaction.setType(type);
        transaction.setTransactionDate(request.getTransactionDate());
        transaction.setDescription(request.getDescription());
        paymentTransactionMapper.insert(transaction);

        // 6. 插入收支流水（已确认、未分摊）
        Account accountRecord = new Account();
        accountRecord.setProjectId(request.getProjectId());
        accountRecord.setTransactionDate(request.getTransactionDate());
        accountRecord.setAmount(amount);
        accountRecord.setType(type);
        accountRecord.setReceiptId(request.getReceiptId());
        accountRecord.setDescription(request.getDescription());
        accountRecord.setStatus(AccountStatus.CONFIRMED);
        accountRecord.setPaymentAccountId(request.getAccountId());
        accountRecord.setAllocationStatus(AllocationStatus.UNALLOCATED);
        accountMapper.insert(accountRecord);

        log.info(
                "付款执行成功，账户 {}，金额 {}，交易 ID {}", request.getAccountId(), amount, transaction.getId());
        return String.valueOf(transaction.getId());
    }
}
