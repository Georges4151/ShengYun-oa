package com.ceams.service;

import com.ceams.model.entity.ReceiptVerification;
import java.util.List;
import java.util.Date;

public interface ReceiptVerificationService {
    /**
     * 创建核销记录
     * 
     * @param verification 核销记录信息
     * @return 影响行数
     */
    int createVerification(ReceiptVerification verification);

    /**
     * 更新核销记录
     * 
     * @param verification 核销记录信息
     * @return 影响行数
     */
    int updateVerification(ReceiptVerification verification);

    /**
     * 删除核销记录
     * 
     * @param id 核销记录ID
     * @return 影响行数
     */
    int deleteVerification(Integer id);

    /**
     * 根据ID查询核销记录
     * 
     * @param id 核销记录ID
     * @return 核销记录
     */
    ReceiptVerification getVerificationById(Integer id);

    /**
     * 根据票据ID查询核销记录
     * 
     * @param receiptId 票据ID
     * @return 核销记录列表
     */
    List<ReceiptVerification> getVerificationsByReceiptId(Integer receiptId);

    /**
     * 根据日期范围查询核销记录
     * 
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @return 核销记录列表
     */
    List<ReceiptVerification> getVerificationsByDateRange(Date startDate, Date endDate);

    /**
     * 根据支付账户ID查询核销记录
     * 
     * @param paymentAccountId 支付账户ID
     * @return 核销记录列表
     */
    List<ReceiptVerification> getVerificationsByPaymentAccountId(Integer paymentAccountId);

    /**
     * 查询所有核销记录
     * 
     * @return 核销记录列表
     */
    List<ReceiptVerification> getAllVerifications();

    /**
     * 处理票据核销，包括创建核销记录和更新票据状态
     * 
     * @param receiptId        票据ID
     * @param paymentAccountId 支付账户ID
     * @param amount           核销金额
     * @param verifiedBy       核销人ID
     * @param remark           备注
     * @return 处理结果
     */
    boolean processReceiptVerification(Integer receiptId, Integer paymentAccountId,
            java.math.BigDecimal amount, Integer verifiedBy, String remark);
}
