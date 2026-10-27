package com.ceams.service.impl;

import com.ceams.dao.ReceiptVerificationMapper;
import com.ceams.dao.ReceiptMapper;
import com.ceams.dao.PaymentAccountMapper;
import com.ceams.exception.BusinessException;
import com.ceams.model.entity.ReceiptVerification;
import com.ceams.model.entity.Receipt;
import com.ceams.model.entity.PaymentAccount;
import com.ceams.model.entity.PaymentTransaction;
import com.ceams.model.entity.Account;
import com.ceams.service.ReceiptVerificationService;
import com.ceams.service.PaymentTransactionService;
import com.ceams.service.AccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Date;
import java.math.BigDecimal;

@Service
public class ReceiptVerificationServiceImpl implements ReceiptVerificationService {

    @Autowired
    private ReceiptVerificationMapper receiptVerificationMapper;

    @Autowired
    private ReceiptMapper receiptMapper;

    @Autowired
    private PaymentAccountMapper paymentAccountMapper;

    @Autowired
    private PaymentTransactionService paymentTransactionService;

    @Autowired
    private AccountService accountService;

    @Override
    public int createVerification(ReceiptVerification verification) {
        verification.setVerificationTime(new Date());
        return receiptVerificationMapper.insert(verification);
    }

    @Override
    public int updateVerification(ReceiptVerification verification) {
        return receiptVerificationMapper.update(verification);
    }

    @Override
    public int deleteVerification(Integer id) {
        return receiptVerificationMapper.deleteById(id);
    }

    @Override
    public ReceiptVerification getVerificationById(Integer id) {
        return receiptVerificationMapper.selectById(id);
    }

    @Override
    public List<ReceiptVerification> getVerificationsByReceiptId(Integer receiptId) {
        System.out.println("获取核销记录，票据ID: " + receiptId);
        List<ReceiptVerification> verifications = receiptVerificationMapper.selectByReceiptId(receiptId);
        System.out.println("获取到的核销记录数量: " + verifications.size());
        for (ReceiptVerification verification : verifications) {
            System.out.println("核销记录: ID=" + verification.getId() + ", 金额=" + verification.getAmount() + ", 支付方式="
                    + verification.getPaymentMethod());
        }
        return verifications;
    }

    @Override
    public List<ReceiptVerification> getVerificationsByDateRange(Date startDate, Date endDate) {
        return receiptVerificationMapper.selectByDateRange(startDate, endDate);
    }

    @Override
    public List<ReceiptVerification> getVerificationsByPaymentAccountId(Integer paymentAccountId) {
        return receiptVerificationMapper.selectByPaymentAccountId(paymentAccountId);
    }

    @Override
    public List<ReceiptVerification> getAllVerifications() {
        return receiptVerificationMapper.selectAll();
    }

    @Transactional
    @Override
    public boolean processReceiptVerification(Integer receiptId, Integer paymentAccountId,
            BigDecimal amount, Integer verifiedBy, String remark) {
        try {
            if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException("INVALID_AMOUNT", "核销金额必须大于0");
            }

            Receipt receipt = receiptMapper.selectByIdForUpdate(receiptId);
            if (receipt == null) {
                throw new BusinessException("RECEIPT_NOT_FOUND", "票据不存在");
            }

            PaymentAccount paymentAccount = paymentAccountMapper.selectById(paymentAccountId);
            if (paymentAccount == null) {
                throw new BusinessException("ACCOUNT_NOT_FOUND", "支付账户不存在");
            }

            BigDecimal totalAmount = receipt.getAmount();
            if (totalAmount == null || totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException("INVALID_RECEIPT_AMOUNT", "票据金额无效");
            }

            BigDecimal currentPaidAmount = receipt.getPaidAmount() == null ? BigDecimal.ZERO : receipt.getPaidAmount();
            BigDecimal remainingAmount = totalAmount.subtract(currentPaidAmount);

            if (remainingAmount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException("RECEIPT_FULLY_PAID", "该票据已全额支付，无需再次核销");
            }

            if (amount.compareTo(remainingAmount) > 0) {
                throw new BusinessException("EXCESSIVE_PAYMENT",
                        String.format("核销金额超出剩余未付金额。剩余未付: %.2f，尝试核销: %.2f",
                                remainingAmount, amount));
            }

            String paymentMethod = paymentAccount.getType();

            ReceiptVerification verification = new ReceiptVerification();
            verification.setReceiptId(receiptId);
            verification.setPaymentAccountId(paymentAccountId);
            verification.setAmount(amount);
            verification.setPaymentMethod(paymentMethod);
            verification.setVerificationTime(new Date());
            verification.setVerifiedBy(verifiedBy);
            verification.setRemark(remark);
            int insertResult = receiptVerificationMapper.insert(verification);
            if (insertResult <= 0) {
                throw new BusinessException("VERIFICATION_INSERT_FAILED", "核销记录插入失败");
            }

            BigDecimal newPaidAmount = currentPaidAmount.add(amount);
            receipt.setPaidAmount(newPaidAmount);

            if (newPaidAmount.compareTo(totalAmount) == 0) {
                receipt.setPaymentStatus("paid");
            } else if (newPaidAmount.compareTo(BigDecimal.ZERO) > 0) {
                receipt.setPaymentStatus("partially_paid");
            } else {
                receipt.setPaymentStatus("unpaid");
            }

            receipt.setPaymentMethod(paymentMethod);
            receipt.setPaidAt(new Date());

            receiptMapper.updatePaymentStatus(receipt);

            PaymentTransaction transaction = new PaymentTransaction();
            transaction.setAccountId(paymentAccountId);
            transaction.setReceiptId(receiptId);
            transaction.setAmount(amount);
            transaction.setType("expense");
            transaction.setTransactionDate(new Date());
            transaction.setDescription("票据核销: 票据ID=" + receiptId + ", 核销金额=" + amount);
            paymentTransactionService.addTransaction(transaction);

            BigDecimal currentBalance = paymentAccount.getBalance() == null ? BigDecimal.ZERO
                    : paymentAccount.getBalance();
            if (currentBalance.compareTo(amount) < 0) {
                throw new BusinessException("INSUFFICIENT_BALANCE",
                        String.format("支付账户余额不足。当前余额: %.2f，核销金额: %.2f",
                                currentBalance, amount));
            }
            paymentAccount.setBalance(currentBalance.subtract(amount));
            paymentAccountMapper.update(paymentAccount);

            Account account = new Account();
            account.setUserId(receipt.getUserId());
            account.setProjectId(receipt.getProjectId());
            account.setTransactionDate(new Date());
            account.setAmount(amount);
            account.setType("expense");
            account.setReceiptId(receiptId);
            account.setPaymentAccountId(paymentAccountId);
            account.setDescription("票据核销: " + receipt.getType() + " #" + receiptId);
            account.setStatus("approved");
            accountService.addAccount(account, false);

            System.out.println(String.format("核销成功: 票据ID=%d, 核销金额=%.2f, 累计已付=%.2f, 总金额=%.2f",
                    receiptId, amount, newPaidAmount, totalAmount));

            return true;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            throw new BusinessException("VERIFICATION_ERROR", "核销处理失败: " + e.getMessage());
        }
    }
}
