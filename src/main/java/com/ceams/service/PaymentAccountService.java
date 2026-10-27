package com.ceams.service;

import com.ceams.dao.PaymentAccountMapper;
import com.ceams.model.entity.PaymentAccount;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class PaymentAccountService {

    @Autowired
    private PaymentAccountMapper paymentAccountMapper;

    /**
     * 获取所有账户
     * 
     * @return 账户列表
     */
    public List<PaymentAccount> getAllAccounts() {
        return paymentAccountMapper.selectAll();
    }

    /**
     * 根据类型获取账户
     * 
     * @param type 账户类型
     * @return 账户列表
     */
    public List<PaymentAccount> getAccountsByType(String type) {
        return paymentAccountMapper.selectByType(type);
    }

    /**
     * 根据ID获取账户
     * 
     * @param id 账户ID
     * @return 账户信息
     */
    public PaymentAccount getAccountById(Integer id) {
        return paymentAccountMapper.selectById(id);
    }

    /**
     * 添加账户
     * 
     * @param account 账户信息
     * @return 是否添加成功
     */
    @Transactional
    public boolean addAccount(PaymentAccount account) {
        // 设置默认余额为0
        if (account.getBalance() == null) {
            account.setBalance(BigDecimal.ZERO);
        }
        return paymentAccountMapper.insert(account) > 0;
    }

    /**
     * 更新账户信息
     * 
     * @param account 账户信息
     * @return 是否更新成功
     */
    @Transactional
    public boolean updateAccount(PaymentAccount account) {
        return paymentAccountMapper.update(account) > 0;
    }

    /**
     * 删除账户
     * 
     * @param id 账户ID
     * @return 是否删除成功
     */
    @Transactional
    public boolean deleteAccount(Integer id) {
        return paymentAccountMapper.deleteById(id) > 0;
    }

    /**
     * 更新账户余额
     * 
     * @param accountId 账户ID
     * @param amount    变动金额（正数增加，负数减少）
     * @return 是否更新成功
     */
    @Transactional
    public boolean updateBalance(Integer accountId, BigDecimal amount) {
        return paymentAccountMapper.updateBalance(accountId, amount) > 0;
    }

    /**
     * 检查账户余额是否充足
     * 
     * @param accountId 账户ID
     * @param amount    需要的金额
     * @return 是否充足
     */
    public boolean checkBalance(Integer accountId, BigDecimal amount) {
        PaymentAccount account = paymentAccountMapper.selectById(accountId);
        return account != null && account.getBalance().compareTo(amount) >= 0;
    }
}