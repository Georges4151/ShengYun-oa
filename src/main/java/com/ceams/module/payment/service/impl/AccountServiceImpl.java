package com.ceams.module.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ceams.common.exception.BusinessException;
import com.ceams.common.exception.ErrorCode;
import com.ceams.common.response.PageResult;
import com.ceams.module.expense.entity.Receipt;
import com.ceams.module.expense.mapper.ReceiptMapper;
import com.ceams.module.payment.converter.AccountConverter;
import com.ceams.module.payment.dto.AccountCreateRequest;
import com.ceams.module.payment.dto.AccountUpdateRequest;
import com.ceams.module.payment.entity.Account;
import com.ceams.module.payment.entity.PaymentAccount;
import com.ceams.module.payment.enums.AccountStatus;
import com.ceams.module.payment.enums.AccountType;
import com.ceams.module.payment.enums.AllocationStatus;
import com.ceams.module.payment.mapper.AccountMapper;
import com.ceams.module.payment.mapper.PaymentAccountMapper;
import com.ceams.module.payment.service.AccountService;
import com.ceams.module.payment.service.PaymentAccountQueryService;
import com.ceams.module.payment.util.PageUtil;
import com.ceams.module.payment.vo.AccountVO;
import com.ceams.module.project.entity.Project;
import com.ceams.module.project.entity.SubcontractorContract;
import com.ceams.module.project.mapper.ProjectMapper;
import com.ceams.module.project.mapper.SubcontractorContractMapper;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 收支流水服务实现。 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

    private final AccountMapper accountMapper;
    private final AccountConverter accountConverter;
    private final PaymentAccountQueryService paymentAccountQueryService;
    private final ProjectMapper projectMapper;
    private final PaymentAccountMapper paymentAccountMapper;
    private final ReceiptMapper receiptMapper;
    private final SubcontractorContractMapper subcontractorContractMapper;

    @Override
    public PageResult<AccountVO> page(
            long page, long size, Long projectId, AccountType type, AccountStatus status) {
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
        List<AccountVO> records = accountConverter.toVOList(result.getRecords());
        fillRelatedNames(records);
        return new PageResult<>(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    public AccountVO detail(Long id) {
        AccountVO vo = accountConverter.toVO(requireAccount(id));
        fillRelatedNames(List.of(vo));
        return vo;
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

    /** 批量回填关联名称（项目 / 付款账户 / 票据 / 分包合同），避免逐条查询造成 N+1。 */
    private void fillRelatedNames(List<AccountVO> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        List<Long> projectIds = distinctIds(records, AccountVO::getProjectId);
        List<Long> paymentAccountIds = distinctIds(records, AccountVO::getPaymentAccountId);
        List<Long> receiptIds = distinctIds(records, AccountVO::getReceiptId);
        List<Long> contractIds = distinctIds(records, AccountVO::getContractId);

        Map<Long, String> projectNames = new HashMap<>();
        if (!projectIds.isEmpty()) {
            for (Project project : projectMapper.selectBatchIds(projectIds)) {
                projectNames.put(project.getId(), project.getProjectName());
            }
        }
        Map<Long, String> paymentAccountNames = new HashMap<>();
        if (!paymentAccountIds.isEmpty()) {
            for (PaymentAccount paymentAccount :
                    paymentAccountMapper.selectBatchIds(paymentAccountIds)) {
                paymentAccountNames.put(paymentAccount.getId(), paymentAccount.getName());
            }
        }
        Map<Long, String> receiptNumbers = new HashMap<>();
        if (!receiptIds.isEmpty()) {
            for (Receipt receipt : receiptMapper.selectBatchIds(receiptIds)) {
                receiptNumbers.put(receipt.getId(), receipt.getReceiptNumber());
            }
        }
        Map<Long, String> contractNames = new HashMap<>();
        if (!contractIds.isEmpty()) {
            for (SubcontractorContract contract :
                    subcontractorContractMapper.selectBatchIds(contractIds)) {
                contractNames.put(contract.getId(), contract.getContractName());
            }
        }

        for (AccountVO record : records) {
            if (record.getProjectId() != null) {
                record.setProjectName(projectNames.get(record.getProjectId()));
            }
            if (record.getPaymentAccountId() != null) {
                record.setPaymentAccountName(
                        paymentAccountNames.get(record.getPaymentAccountId()));
            }
            if (record.getReceiptId() != null) {
                record.setReceiptNumber(receiptNumbers.get(record.getReceiptId()));
            }
            if (record.getContractId() != null) {
                record.setContractName(contractNames.get(record.getContractId()));
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
