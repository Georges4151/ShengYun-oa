package com.ceams.module.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ceams.common.exception.BusinessException;
import com.ceams.common.exception.ErrorCode;
import com.ceams.common.response.PageResult;
import com.ceams.module.payment.converter.AccountConverter;
import com.ceams.module.payment.dto.AccountCreateRequest;
import com.ceams.module.payment.dto.AccountUpdateRequest;
import com.ceams.module.payment.entity.Account;
import com.ceams.module.payment.enums.AccountStatus;
import com.ceams.module.payment.enums.AccountType;
import com.ceams.module.payment.enums.AllocationStatus;
import com.ceams.module.payment.mapper.AccountMapper;
import com.ceams.module.payment.service.AccountService;
import com.ceams.module.payment.service.PaymentAccountQueryService;
import com.ceams.module.payment.util.PageUtil;
import com.ceams.module.payment.vo.AccountVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 收支流水服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

    private final AccountMapper accountMapper;
    private final AccountConverter accountConverter;
    private final PaymentAccountQueryService paymentAccountQueryService;

    @Override
    public PageResult<AccountVO> page(long page, long size, Long projectId, AccountType type, AccountStatus status) {
        Page<Account> p = PageUtil.resolve(page, size);
        LambdaQueryWrapper<Account> wrapper = new LambdaQueryWrapper<>();
        if (projectId != null) {
            wrapper.eq(Account::getProjectId, projectId);
        }
        if (type != null) {
            wrapper.eq(Account::getType, type);
        }
        if (status != null) {
            wrapper.eq(Account::getStatus, status);
        }
        wrapper.orderByDesc(Account::getCreateTime);
        IPage<Account> result = accountMapper.selectPage(p, wrapper);
        return new PageResult<>(accountConverter.toVOList(result.getRecords()),
                result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    public AccountVO detail(Long id) {
        return accountConverter.toVO(requireAccount(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String create(AccountCreateRequest request) {
        if (request.getPaymentAccountId() != null
                && !paymentAccountQueryService.exists(request.getPaymentAccountId())) {
            throw BusinessException.of(ErrorCode.BIZ_NOT_FOUND, "付款账户不存在");
        }
        Account account = new Account();
        account.setProjectId(request.getProjectId());
        account.setTransactionDate(request.getTransactionDate());
        account.setAmount(request.getAmount());
        account.setType(request.getType());
        account.setReceiptId(request.getReceiptId());
        account.setDescription(request.getDescription());
        account.setPaymentAccountId(request.getPaymentAccountId());
        account.setContractId(request.getContractId());
        account.setPaymentType(request.getPaymentType());
        account.setCategoryId(request.getCategoryId());
        account.setContactId(request.getContactId());
        account.setMaterialId(request.getMaterialId());
        account.setSubcontractorId(request.getSubcontractorId());
        account.setStatus(AccountStatus.DRAFT);
        account.setAllocationStatus(AllocationStatus.UNALLOCATED);
        accountMapper.insert(account);
        return String.valueOf(account.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, AccountUpdateRequest request) {
        Account account = requireAccount(id);
        requireEditable(account);
        if (request.getProjectId() != null) {
            account.setProjectId(request.getProjectId());
        }
        if (request.getTransactionDate() != null) {
            account.setTransactionDate(request.getTransactionDate());
        }
        if (request.getAmount() != null) {
            account.setAmount(request.getAmount());
        }
        if (request.getType() != null) {
            account.setType(request.getType());
        }
        if (request.getReceiptId() != null) {
            account.setReceiptId(request.getReceiptId());
        }
        if (request.getDescription() != null) {
            account.setDescription(request.getDescription());
        }
        if (request.getPaymentAccountId() != null) {
            if (!paymentAccountQueryService.exists(request.getPaymentAccountId())) {
                throw BusinessException.of(ErrorCode.BIZ_NOT_FOUND, "付款账户不存在");
            }
            account.setPaymentAccountId(request.getPaymentAccountId());
        }
        if (request.getContractId() != null) {
            account.setContractId(request.getContractId());
        }
        if (request.getPaymentType() != null) {
            account.setPaymentType(request.getPaymentType());
        }
        if (request.getCategoryId() != null) {
            account.setCategoryId(request.getCategoryId());
        }
        if (request.getContactId() != null) {
            account.setContactId(request.getContactId());
        }
        if (request.getMaterialId() != null) {
            account.setMaterialId(request.getMaterialId());
        }
        if (request.getSubcontractorId() != null) {
            account.setSubcontractorId(request.getSubcontractorId());
        }
        accountMapper.updateById(account);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        Account account = requireAccount(id);
        requireEditable(account);
        accountMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirm(Long id) {
        Account account = requireAccount(id);
        if (account.getStatus() != AccountStatus.DRAFT) {
            throw BusinessException.of(ErrorCode.BIZ_STATE_CONFLICT, "仅草稿态流水可确认");
        }
        account.setStatus(AccountStatus.CONFIRMED);
        accountMapper.updateById(account);
        log.info("收支流水 {} 已确认", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long id) {
        Account account = requireAccount(id);
        if (account.getStatus() != AccountStatus.DRAFT) {
            throw BusinessException.of(ErrorCode.BIZ_STATE_CONFLICT, "仅草稿态流水可取消");
        }
        account.setStatus(AccountStatus.CANCELLED);
        accountMapper.updateById(account);
        log.info("收支流水 {} 已取消", id);
    }

    private Account requireAccount(Long id) {
        Account account = accountMapper.selectById(id);
        if (account == null) {
            throw BusinessException.of(ErrorCode.BIZ_NOT_FOUND, "收支流水不存在");
        }
        return account;
    }

    private void requireEditable(Account account) {
        if (account.getStatus() != AccountStatus.DRAFT) {
            throw BusinessException.of(ErrorCode.BIZ_STATE_CONFLICT, "仅草稿态流水可编辑");
        }
    }
}
