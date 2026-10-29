package com.ceams.module.bank.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ceams.common.exception.BusinessException;
import com.ceams.common.exception.ErrorCode;
import com.ceams.common.response.PageResult;
import com.ceams.module.bank.converter.GeneralLedgerConverter;
import com.ceams.module.bank.dto.GeneralLedgerCreateRequest;
import com.ceams.module.bank.dto.GeneralLedgerUpdateRequest;
import com.ceams.module.bank.entity.GeneralLedger;
import com.ceams.module.bank.enums.TransactionType;
import com.ceams.module.bank.mapper.GeneralLedgerMapper;
import com.ceams.module.bank.service.GeneralLedgerService;
import com.ceams.module.bank.util.PageUtil;
import com.ceams.module.bank.vo.GeneralLedgerVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 总账服务实现。 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GeneralLedgerServiceImpl implements GeneralLedgerService {

    private final GeneralLedgerMapper generalLedgerMapper;
    private final GeneralLedgerConverter generalLedgerConverter;

    @Override
    public PageResult<GeneralLedgerVO> page(
            long page,
            long size,
            String accountName,
            TransactionType transactionType,
            String status) {
        Page<GeneralLedger> p = PageUtil.resolve(page, size);
        LambdaQueryWrapper<GeneralLedger> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.isNotBlank(accountName)) {
            wrapper.like(GeneralLedger::getAccountName, accountName);
        }
        if (transactionType != null) {
            wrapper.eq(GeneralLedger::getTransactionType, transactionType);
        }
        if (StringUtils.isNotBlank(status)) {
            wrapper.eq(GeneralLedger::getStatus, status);
        }
        wrapper.orderByDesc(GeneralLedger::getCreateTime);
        IPage<GeneralLedger> result = generalLedgerMapper.selectPage(p, wrapper);
        return new PageResult<>(
                generalLedgerConverter.toVOList(result.getRecords()),
                result.getTotal(),
                result.getCurrent(),
                result.getSize());
    }

    @Override
    public GeneralLedgerVO detail(Long id) {
        return generalLedgerConverter.toVO(requireLedger(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String create(GeneralLedgerCreateRequest request) {
        GeneralLedger ledger = new GeneralLedger();
        ledger.setAccountName(request.getAccountName());
        ledger.setAccountNumber(request.getAccountNumber());
        ledger.setBankName(request.getBankName());
        ledger.setBalance(request.getBalance());
        ledger.setAccountType(request.getAccountType());
        ledger.setSummary(request.getSummary());
        ledger.setTransactionTime(request.getTransactionTime());
        ledger.setTransactionType(request.getTransactionType());
        ledger.setAmount(request.getAmount());
        ledger.setStatus(request.getStatus());
        generalLedgerMapper.insert(ledger);
        return String.valueOf(ledger.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, GeneralLedgerUpdateRequest request) {
        GeneralLedger ledger = requireLedger(id);
        if (request.getAccountName() != null) {
            ledger.setAccountName(request.getAccountName());
        }
        if (request.getAccountNumber() != null) {
            ledger.setAccountNumber(request.getAccountNumber());
        }
        if (request.getBankName() != null) {
            ledger.setBankName(request.getBankName());
        }
        if (request.getBalance() != null) {
            ledger.setBalance(request.getBalance());
        }
        if (request.getAccountType() != null) {
            ledger.setAccountType(request.getAccountType());
        }
        if (request.getSummary() != null) {
            ledger.setSummary(request.getSummary());
        }
        if (request.getTransactionTime() != null) {
            ledger.setTransactionTime(request.getTransactionTime());
        }
        if (request.getTransactionType() != null) {
            ledger.setTransactionType(request.getTransactionType());
        }
        if (request.getAmount() != null) {
            ledger.setAmount(request.getAmount());
        }
        if (request.getStatus() != null) {
            ledger.setStatus(request.getStatus());
        }
        generalLedgerMapper.updateById(ledger);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        requireLedger(id);
        generalLedgerMapper.deleteById(id);
    }

    private GeneralLedger requireLedger(Long id) {
        GeneralLedger ledger = generalLedgerMapper.selectById(id);
        if (ledger == null) {
            throw BusinessException.of(ErrorCode.BIZ_NOT_FOUND, "总账记录不存在");
        }
        return ledger;
    }
}
