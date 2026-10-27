package com.ceams.dao;

import com.ceams.model.entity.PaymentTransaction;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Mapper
public interface PaymentTransactionMapper {
        /**
         * 根据ID查询交易记录
         * 
         * @param id 交易记录ID
         * @return 交易记录信息
         */
        PaymentTransaction selectById(Integer id);

        /**
         * 根据账户ID查询交易记录
         * 
         * @param accountId 账户ID
         * @return 交易记录列表
         */
        List<PaymentTransaction> selectByAccountId(@Param("accountId") Integer accountId);

        /**
         * 根据票据ID查询交易记录
         * 
         * @param receiptId 票据ID
         * @return 交易记录列表
         */
        List<PaymentTransaction> selectByReceiptId(@Param("receiptId") Integer receiptId);

        /**
         * 根据日期范围查询交易记录
         * 
         * @param startDate 开始日期
         * @param endDate   结束日期
         * @return 交易记录列表
         */
        List<PaymentTransaction> selectByDateRange(@Param("startDate") Date startDate, @Param("endDate") Date endDate);

        /**
         * 根据账户ID和日期范围查询交易记录
         * 
         * @param accountId 账户ID
         * @param startDate 开始日期
         * @param endDate   结束日期
         * @return 交易记录列表
         */
        List<PaymentTransaction> selectByAccountIdAndDateRange(
                        @Param("accountId") Integer accountId,
                        @Param("startDate") Date startDate,
                        @Param("endDate") Date endDate);

        /**
         * 根据条件查询交易记录
         * 
         * @param accountId   账户ID
         * @param receiptId   票据ID
         * @param type        交易类型
         * @param startDate   开始日期
         * @param endDate     结束日期
         * @param description 交易描述
         * @return 交易记录列表
         */
        List<PaymentTransaction> selectByConditions(
                        @Param("accountId") Integer accountId,
                        @Param("receiptId") Integer receiptId,
                        @Param("type") String type,
                        @Param("startDate") Date startDate,
                        @Param("endDate") Date endDate,
                        @Param("description") String description);

        /**
         * 获取交易记录汇总信息
         * 
         * @param accountId 账户ID
         * @param startDate 开始日期
         * @param endDate   结束日期
         * @return 汇总信息
         */
        Map<String, Object> getTransactionSummary(
                        @Param("accountId") Integer accountId,
                        @Param("startDate") Date startDate,
                        @Param("endDate") Date endDate);

        /**
         * 插入交易记录
         * 
         * @param transaction 交易记录信息
         * @return 影响行数
         */
        int insert(PaymentTransaction transaction);

        /**
         * 更新交易记录
         * 
         * @param transaction 交易记录信息
         * @return 影响行数
         */
        int update(PaymentTransaction transaction);

        /**
         * 删除交易记录
         * 
         * @param id 交易记录ID
         * @return 影响行数
         */
        int deleteById(Integer id);

        /**
         * 查询所有交易记录
         * 
         * @return 交易记录列表
         */
        List<PaymentTransaction> selectAll();

        /**
         * 分页查询所有交易记录
         * 
         * @param pageSize 每页大小
         * @param offset   偏移量
         * @return 交易记录列表
         */
        List<PaymentTransaction> selectAllWithPagination(@Param("pageSize") int pageSize, @Param("offset") int offset);

        /**
         * 分页查询交易记录（支持条件）
         * 
         * @param accountId   账户ID
         * @param receiptId   票据ID
         * @param type        交易类型
         * @param startDate   开始日期
         * @param endDate     结束日期
         * @param description 交易描述
         * @param pageSize    每页大小
         * @param offset      偏移量
         * @return 交易记录列表
         */
        List<PaymentTransaction> selectByConditionsWithPagination(
                        @Param("accountId") Integer accountId,
                        @Param("receiptId") Integer receiptId,
                        @Param("type") String type,
                        @Param("startDate") Date startDate,
                        @Param("endDate") Date endDate,
                        @Param("description") String description,
                        @Param("pageSize") int pageSize,
                        @Param("offset") int offset);

        /**
         * 获取交易记录总数（支持条件）
         * 
         * @param accountId   账户ID
         * @param receiptId   票据ID
         * @param type        交易类型
         * @param startDate   开始日期
         * @param endDate     结束日期
         * @param description 交易描述
         * @return 交易记录总数
         */
        int countByConditions(
                        @Param("accountId") Integer accountId,
                        @Param("receiptId") Integer receiptId,
                        @Param("type") String type,
                        @Param("startDate") Date startDate,
                        @Param("endDate") Date endDate,
                        @Param("description") String description);
}