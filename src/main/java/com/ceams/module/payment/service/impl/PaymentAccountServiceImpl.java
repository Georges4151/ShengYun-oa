package com.ceams.module.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ceams.common.exception.BusinessException;
import com.ceams.common.exception.ErrorCode;
import com.ceams.common.response.PageResult;
import com.ceams.module.payment.converter.PaymentAccountConverter;
import com.ceams.module.payment.dto.PaymentAccountCreateRequest;
import com.ceams.module.payment.dto.PaymentAccountUpdateRequest;
import com.ceams.module.payment.entity.PaymentAccount;
import com.ceams.module.payment.mapper.PaymentAccountMapper;
import com.ceams.module.payment.service.PaymentAccountService;
import com.ceams.module.payment.util.PageUtil;
import com.ceams.module.payment.vo.PaymentAccountVO;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 付款账户服务实现。 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentAccountServiceImpl implements PaymentAccountService {

    private final PaymentAccountMapper paymentAccountMapper;
    private final PaymentAccountConverter paymentAccountConverter;

    @Override
    public PageResult<PaymentAccountVO> page(long page, long size, String name, String type) {
        Page<PaymentAccount> p = PageUtil.resolve(page, size);
        LambdaQueryWrapper<PaymentAccount> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.isNotBlank(name)) {
            wrapper.like(PaymentAccount::getName, name);
        }
        if (StringUtils.isNotBlank(type)) {
            wrapper.eq(PaymentAccount::getType, type);
        }
        wrapper.orderByDesc(PaymentAccount::getCreateTime);
        IPage<PaymentAccount> result = paymentAccountMapper.selectPage(p, wrapper);
        return new PageResult<>(
                paymentAccountConverter.toVOList(result.getRecords()),
                result.getTotal(),
                result.getCurrent(),
                result.getSize());
    }

    @Override
    public PaymentAccountVO detail(Long id) {
        return paymentAccountConverter.toVO(requireAccount(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String create(PaymentAccountCreateRequest request) {
        PaymentAccount account = new PaymentAccount();
        account.setName(request.getName());
        account.setType(request.getType());
        account.setBalance(request.getBalance() == null ? BigDecimal.ZERO : request.getBalance());
        account.setAccountNumber(request.getAccountNumber());
        account.setDescription(request.getDescription());
        paymentAccountMapper.insert(account);
        return String.valueOf(account.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, PaymentAccountUpdateRequest request) {
        PaymentAccount account = requireAccount(id);
        if (request.getName() != null) {
            account.setName(request.getName());
        }
        if (request.getType() != null) {
            account.setType(request.getType());
        }
        if (request.getAccountNumber() != null) {
            account.setAccountNumber(request.getAccountNumber());
        }
        if (request.getDescription() != null) {
            account.setDescription(request.getDescription());
        }
        paymentAccountMapper.updateById(account);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        requireAccount(id);
        paymentAccountMapper.deleteById(id);
    }

    private PaymentAccount requireAccount(Long id) {
        PaymentAccount account = paymentAccountMapper.selectById(id);
        if (account == null) {
            throw BusinessException.of(ErrorCode.BIZ_NOT_FOUND, "付款账户不存在");
        }
        return account;
    }
}
