package com.ceams.model.event;

import com.ceams.model.entity.PaymentTransaction;

/**
 * 支付审批通过事件
 * 当支付审批通过时发布此事件，触发自动记账功能
 */
public class PaymentApprovedEvent {
    private final Object source;
    private final PaymentTransaction paymentTransaction;
    private final Integer contractId;
    private final String paymentType;

    /**
     * 构造函数
     * @param source 事件源
     * @param paymentTransaction 支付交易记录
     * @param contractId 合同ID
     * @param paymentType 支付类型
     */
    public PaymentApprovedEvent(Object source, PaymentTransaction paymentTransaction, Integer contractId, String paymentType) {
        this.source = source;
        this.paymentTransaction = paymentTransaction;
        this.contractId = contractId;
        this.paymentType = paymentType;
    }

    public Object getSource() {
        return source;
    }

    public PaymentTransaction getPaymentTransaction() {
        return paymentTransaction;
    }

    public Integer getContractId() {
        return contractId;
    }

    public String getPaymentType() {
        return paymentType;
    }
}
