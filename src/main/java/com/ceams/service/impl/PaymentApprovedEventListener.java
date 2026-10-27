package com.ceams.service.impl;

import com.ceams.model.entity.Account;
import com.ceams.model.event.PaymentApprovedEvent;
import com.ceams.service.AccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 支付审批通过事件监听器
 * 监听支付审批通过事件，触发自动记账功能
 */
@Component
public class PaymentApprovedEventListener {

    @Autowired
    private AccountService accountService;

    /**
     * 处理支付审批通过事件
     * @param event 支付审批通过事件
     */
    @EventListener
    public void handlePaymentApprovedEvent(PaymentApprovedEvent event) {
        // 从事件中获取支付交易记录
        var paymentTransaction = event.getPaymentTransaction();
        var contractId = event.getContractId();
        var paymentType = event.getPaymentType();

        // 创建账目记录
        Account account = new Account();
        account.setProjectId(paymentTransaction.getProjectId());
        account.setContractId(contractId);
        account.setTransactionDate(paymentTransaction.getTransactionDate());
        account.setAmount(paymentTransaction.getAmount());
        account.setType("expense");
        account.setPaymentAccountId(paymentTransaction.getAccountId());
        account.setPaymentType(paymentType);
        account.setDescription(paymentTransaction.getDescription());
        account.setStatus("completed");

        // 保存账目记录
        accountService.addAccountFromContract(account);

        // 打印日志
        System.out.println("自动记账成功：" + paymentTransaction.getDescription());
    }
}
