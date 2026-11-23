package com.ceams.module.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ceams.common.exception.BusinessException;
import com.ceams.common.exception.ErrorCode;
import com.ceams.common.response.PageResult;
import com.ceams.module.expense.entity.Receipt;
import com.ceams.module.expense.mapper.ReceiptMapper;
import com.ceams.module.payment.converter.PaymentTransactionConverter;
import com.ceams.module.payment.dto.PaymentTransactionCreateRequest;
import com.ceams.module.payment.dto.PaymentTransactionUpdateRequest;
import com.ceams.module.payment.entity.PaymentAccount;
import com.ceams.module.payment.entity.PaymentTransaction;
import com.ceams.module.payment.enums.AccountType;
import com.ceams.module.payment.mapper.PaymentAccountMapper;
import com.ceams.module.payment.mapper.PaymentTransactionMapper;
import com.ceams.module.payment.service.PaymentAccountQueryService;
import com.ceams.module.payment.service.PaymentTransactionService;
import com.ceams.module.payment.util.PageUtil;
import com.ceams.module.payment.vo.PaymentTransactionVO;
import com.ceams.module.project.entity.Project;
import com.ceams.module.project.mapper.ProjectMapper;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
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
    private final PaymentAccountMapper paymentAccountMapper;
    private final ReceiptMapper receiptMapper;
    private final ProjectMapper projectMapper;

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
        List<PaymentTransactionVO> records =
                paymentTransactionConverter.toVOList(result.getRecords());
        fillRelatedNames(records);
        return new PageResult<>(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    public PaymentTransactionVO detail(Long id) {
        PaymentTransactionVO vo = paymentTransactionConverter.toVO(requireTransaction(id));
        fillRelatedNames(List.of(vo));
        return vo;
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

    /** 批量回填关联名称（付款账户 / 票据编号 / 项目名称），避免逐条查询造成 N+1。 */
    private void fillRelatedNames(List<PaymentTransactionVO> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        List<Long> accountIds = distinctIds(records, PaymentTransactionVO::getAccountId);
        List<Long> receiptIds = distinctIds(records, PaymentTransactionVO::getReceiptId);
        List<Long> projectIds = distinctIds(records, PaymentTransactionVO::getProjectId);

        Map<Long, String> accountNames = new HashMap<>();
        if (!accountIds.isEmpty()) {
            for (PaymentAccount account : paymentAccountMapper.selectBatchIds(accountIds)) {
                accountNames.put(account.getId(), account.getName());
            }
        }
        Map<Long, String> receiptNumbers = new HashMap<>();
        if (!receiptIds.isEmpty()) {
            for (Receipt receipt : receiptMapper.selectBatchIds(receiptIds)) {
                receiptNumbers.put(receipt.getId(), receipt.getReceiptNumber());
            }
        }
        Map<Long, String> projectNames = new HashMap<>();
        if (!projectIds.isEmpty()) {
            for (Project project : projectMapper.selectBatchIds(projectIds)) {
                projectNames.put(project.getId(), project.getProjectName());
            }
        }

        for (PaymentTransactionVO record : records) {
            if (record.getAccountId() != null) {
                record.setAccountName(accountNames.get(record.getAccountId()));
            }
            if (record.getReceiptId() != null) {
                record.setReceiptNumber(receiptNumbers.get(record.getReceiptId()));
            }
            if (record.getProjectId() != null) {
                record.setProjectName(projectNames.get(record.getProjectId()));
            }
        }
    }

    /** 收集去重后的非空关联 ID。 */
    private static <T> List<Long> distinctIds(List<T> records, Function<T, Long> idGetter) {
        return records.stream()
                .map(idGetter)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    private PaymentTransaction requireTransaction(Long id) {
        PaymentTransaction transaction = paymentTransactionMapper.selectById(id);
        if (transaction == null) {
            throw BusinessException.of(ErrorCode.BIZ_NOT_FOUND, "付款交易不存在");
        }
        return transaction;
    }
}
