package com.ceams.module.bank.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ceams.common.exception.BusinessException;
import com.ceams.common.exception.ErrorCode;
import com.ceams.common.response.PageResult;
import com.ceams.module.bank.converter.BankStatementConverter;
import com.ceams.module.bank.dto.BankStatementCreateRequest;
import com.ceams.module.bank.dto.BankStatementUpdateRequest;
import com.ceams.module.bank.entity.BankStatement;
import com.ceams.module.bank.enums.BankStatementStatus;
import com.ceams.module.bank.enums.TransactionType;
import com.ceams.module.bank.mapper.BankStatementMapper;
import com.ceams.module.bank.service.BankStatementService;
import com.ceams.module.bank.util.PageUtil;
import com.ceams.module.bank.vo.BankStatementVO;
import com.ceams.module.payment.service.PaymentAccountQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 银行流水服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BankStatementServiceImpl implements BankStatementService {

    private final BankStatementMapper bankStatementMapper;
    private final BankStatementConverter bankStatementConverter;
    private final PaymentAccountQueryService paymentAccountQueryService;

    @Override
    public PageResult<BankStatementVO> page(long page, long size, Long accountId, BankStatementStatus status,
                                            TransactionType type, LocalDateTime startDate, LocalDateTime endDate) {
        Page<BankStatement> p = PageUtil.resolve(page, size);
        LambdaQueryWrapper<BankStatement> wrapper = new LambdaQueryWrapper<>();
        if (accountId != null) {
            wrapper.eq(BankStatement::getAccountId, accountId);
        }
        if (status != null) {
            wrapper.eq(BankStatement::getStatus, status);
        }
        if (type != null) {
            wrapper.eq(BankStatement::getType, type);
        }
        if (startDate != null) {
            wrapper.ge(BankStatement::getTransactionDate, startDate);
        }
        if (endDate != null) {
            wrapper.le(BankStatement::getTransactionDate, endDate);
        }
        wrapper.orderByDesc(BankStatement::getCreateTime);
        IPage<BankStatement> result = bankStatementMapper.selectPage(p, wrapper);
        return new PageResult<>(bankStatementConverter.toVOList(result.getRecords()),
                result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    public BankStatementVO detail(Long id) {
        return bankStatementConverter.toVO(requireStatement(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String create(BankStatementCreateRequest request) {
        if (!paymentAccountQueryService.exists(request.getAccountId())) {
            throw BusinessException.of(ErrorCode.BIZ_NOT_FOUND, "付款账户不存在");
        }
        BankStatement statement = new BankStatement();
        statement.setAccountId(request.getAccountId());
        statement.setTransactionDate(request.getTransactionDate());
        statement.setAmount(request.getAmount());
        statement.setType(request.getType());
        statement.setDescription(request.getDescription());
        statement.setCounterparty(request.getCounterparty());
        statement.setBankReference(request.getBankReference());
        statement.setBalance(request.getBalance());
        statement.setStatus(BankStatementStatus.UNRECONCILED);
        bankStatementMapper.insert(statement);
        return String.valueOf(statement.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, BankStatementUpdateRequest request) {
        BankStatement statement = requireStatement(id);
        if (request.getAccountId() != null) {
            if (!paymentAccountQueryService.exists(request.getAccountId())) {
                throw BusinessException.of(ErrorCode.BIZ_NOT_FOUND, "付款账户不存在");
            }
            statement.setAccountId(request.getAccountId());
        }
        if (request.getTransactionDate() != null) {
            statement.setTransactionDate(request.getTransactionDate());
        }
        if (request.getAmount() != null) {
            statement.setAmount(request.getAmount());
        }
        if (request.getType() != null) {
            statement.setType(request.getType());
        }
        if (request.getDescription() != null) {
            statement.setDescription(request.getDescription());
        }
        if (request.getCounterparty() != null) {
            statement.setCounterparty(request.getCounterparty());
        }
        if (request.getBankReference() != null) {
            statement.setBankReference(request.getBankReference());
        }
        if (request.getBalance() != null) {
            statement.setBalance(request.getBalance());
        }
        bankStatementMapper.updateById(statement);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        requireStatement(id);
        bankStatementMapper.deleteById(id);
    }

    private BankStatement requireStatement(Long id) {
        BankStatement statement = bankStatementMapper.selectById(id);
        if (statement == null) {
            throw BusinessException.of(ErrorCode.BIZ_NOT_FOUND, "银行流水不存在");
        }
        return statement;
    }
}
