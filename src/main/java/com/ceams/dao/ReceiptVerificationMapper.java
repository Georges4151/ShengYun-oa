package com.ceams.dao;

import com.ceams.model.entity.ReceiptVerification;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Date;

@Mapper
public interface ReceiptVerificationMapper {
    /**
     * 根据ID查询核销记录
     * 
     * @param id 核销记录ID
     * @return 核销记录
     */
    ReceiptVerification selectById(Integer id);

    /**
     * 根据票据ID查询核销记录
     * 
     * @param receiptId 票据ID
     * @return 核销记录列表
     */
    List<ReceiptVerification> selectByReceiptId(@Param("receiptId") Integer receiptId);

    /**
     * 根据日期范围查询核销记录
     * 
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @return 核销记录列表
     */
    List<ReceiptVerification> selectByDateRange(@Param("startDate") Date startDate, @Param("endDate") Date endDate);

    /**
     * 根据支付账户ID查询核销记录
     * 
     * @param paymentAccountId 支付账户ID
     * @return 核销记录列表
     */
    List<ReceiptVerification> selectByPaymentAccountId(@Param("paymentAccountId") Integer paymentAccountId);

    /**
     * 插入核销记录
     * 
     * @param verification 核销记录信息
     * @return 影响行数
     */
    int insert(ReceiptVerification verification);

    /**
     * 更新核销记录
     * 
     * @param verification 核销记录信息
     * @return 影响行数
     */
    int update(ReceiptVerification verification);

    /**
     * 删除核销记录
     * 
     * @param id 核销记录ID
     * @return 影响行数
     */
    int deleteById(Integer id);

    /**
     * 查询所有核销记录
     * 
     * @return 核销记录列表
     */
    List<ReceiptVerification> selectAll();
}
