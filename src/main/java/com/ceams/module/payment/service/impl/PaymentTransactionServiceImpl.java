package com.ceams.module.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ceams.common.exception.BusinessException;
import com.ceams.common.exception.ErrorCode;
import com.ceams.common.response.PageResult;
import com.ceams.module.payment.converter.PaymentTransactionConverter;
import com.ceams.module.payment.dto.PaymentTransactionCreateRequest;
import com.ceams.module.payment.dto.PaymentTransactionUpdateRequest;
import com.ceams.module.payment.entity.PaymentTransaction;
import com.ceams.module.payment.enums.AccountType;
import com.ceams.module.payment.mapper.PaymentTransactionMapper;
import com.ceams.module.payment.service.PaymentAccountQueryService;
import com.ceams.module.payment.service.PaymentTransactionService;
import com.ceams.module.payment.util.PageUtil;
import com.ceams.module.payment.vo.PaymentTransactionVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 付款交易服务实现。 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentTransactionServiceImpl implements PaymentTransactionService {

    private final PaymentTransactionMapper paymentTransactionMapper;
    private final PaymentTransactionConverter paymentTransactionConverter;
    private final PaymentAccountQueryService paymentAccountQueryService;

    @Override
    public PageResult<PaymentTransactionVO> page(
            long page, long size, Long accountId, AccountType type) {
        Page<PaymentTransaction> p = PageUtil.resolve(page, size);
        LambdaQueryWrapper<PaymentTransaction> wrapper = new LambdaQueryWrapper<>();
        if (accountId != null) {
            wrapper.eq(PaymentTransaction::getAccountId, accountId);
        }
        if (type != null) {
            wrapper.eq(PaymentTransaction::getType, type);
        }
        wrapper.orderByDesc(PaymentTransaction::getCreateTime);
        IPage<PaymentTransaction> result = paymentTransactionMapper.selectPage(p, wrapper);
        return new PageResult<>(
                paymentTransactionConverter.toVOList(result.getRecords()),
                result.getTotal(),
                result.getCurrent(),
                result.getSize());
    }

    @Override
    public PaymentTransactionVO detail(Long id) {
        return paymentTransactionConverter.toVO(requireTransaction(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String create(PaymentTransactionCreateRequest request) {
        if (!paymentAccountQueryService.exists(request.getAccountId())) {
            throw BusinessException.of(ErrorCode.BIZ_NOT_FOUND, "付款账户不存在");
        }
        PaymentTransaction transaction = new PaymentTransaction();
        transaction.setAccountId(request.getAccountId());
        transaction.setReceiptId(request.getReceiptId());
        transaction.setProjectId(request.getProjectId());
        transaction.setAmount(request.getAmount());
        transaction.setType(request.getType());
        transaction.setTransactionDate(request.getTransactionDate());
        transaction.setDescription(request.getDescription());
        paymentTransactionMapper.insert(transaction);
        return String.valueOf(transaction.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, PaymentTransactionUpdateRequest request) {
        PaymentTransaction transaction = requireTransaction(id);
        if (request.getAccountId() != null) {
            if (!paymentAccountQueryService.exists(request.getAccountId())) {
                throw BusinessException.of(ErrorCode.BIZ_NOT_FOUND, "付款账户不存在");
            }
            transaction.setAccountId(request.getAccountId());
        }
        if (request.getReceiptId() != null) {
            transaction.setReceiptId(request.getReceiptId());
        }
        if (request.getProjectId() != null) {
            transaction.setProjectId(request.getProjectId());
        }
        if (request.getAmount() != null) {
            transaction.setAmount(request.getAmount());
        }
        if (request.getType() != null) {
            transaction.setType(request.getType());
        }
        if (request.getTransactionDate() != null) {
            transaction.setTransactionDate(request.getTransactionDate());
        }
        if (request.getDescription() != null) {
            transaction.setDescription(request.getDescription());
        }
        paymentTransactionMapper.updateById(transaction);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        requireTransaction(id);
        paymentTransactionMapper.deleteById(id);
    }

    private PaymentTransaction requireTransaction(Long id) {
        PaymentTransaction transaction = paymentTransactionMapper.selectById(id);
        if (transaction == null) {
            throw BusinessException.of(ErrorCode.BIZ_NOT_FOUND, "付款交易不存在");
        }
        return transaction;
    }
}
