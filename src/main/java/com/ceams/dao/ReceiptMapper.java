package com.ceams.dao;

import com.ceams.model.entity.Receipt;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ReceiptMapper {
        /**
         * 根据ID查询票据
         * 
         * @param id 票据ID
         * @return 票据信息
         */
        Receipt selectById(Integer id);

        /**
         * 根据ID查询票据（带悲观锁，用于并发控制）
         * 使用 SELECT ... FOR UPDATE 锁定记录
         * 
         * @param id 票据ID
         * @return 票据信息
         */
        Receipt selectByIdForUpdate(@Param("id") Integer id);

        /**
         * 根据用户ID查询票据
         * 
         * @param userId 用户ID
         * @return 票据列表
         */
        List<Receipt> selectByUserId(@Param("userId") Integer userId);

        /**
         * 根据验证状态查询票据
         * 
         * @param status 验证状态
         * @return 票据列表
         */
        List<Receipt> selectByVerificationStatus(@Param("status") String status);

        /**
         * 根据日期范围查询票据
         * 
         * @param startDate 开始日期
         * @param endDate   结束日期
         * @return 票据列表
         */
        List<Receipt> selectByDateRange(@Param("startDate") java.util.Date startDate,
                        @Param("endDate") java.util.Date endDate);

        /**
         * 根据类型查询票据
         * 
         * @param type 票据类型
         * @return 票据列表
         */
        List<Receipt> selectByType(@Param("type") String type);

        /**
         * 根据报票人ID查询票据
         * 
         * @param reporterId 报票人ID
         * @return 票据列表
         */
        List<Receipt> selectByReporterId(@Param("reporterId") Integer reporterId);

        /**
         * 根据归属项目ID查询票据
         * 
         * @param projectId 归属项目ID
         * @return 票据列表
         */
        List<Receipt> selectByProjectId(@Param("projectId") Integer projectId);

        /**
         * 根据报票人姓名模糊查询票据
         * 
         * @param reporterName 报票人姓名
         * @return 票据列表
         */
        List<Receipt> selectByReporterName(@Param("reporterName") String reporterName);

        /**
         * 根据项目名称模糊查询票据
         * 
         * @param projectName 项目名称
         * @return 票据列表
         */
        List<Receipt> selectByProjectName(@Param("projectName") String projectName);

        /**
         * 插入票据
         * 
         * @param receipt 票据信息
         * @return 影响行数
         */
        int insert(Receipt receipt);

        /**
         * 更新票据
         * 
         * @param receipt 票据信息
         * @return 影响行数
         */
        int update(Receipt receipt);

        /**
         * 删除票据
         * 
         * @param id 票据ID
         * @return 影响行数
         */
        int deleteById(Integer id);

        /**
         * 查询所有票据
         * 
         * @return 票据列表
         */
        List<Receipt> selectAll();

        /**
         * 更新票据支付状态
         * 
         * @param receipt 票据信息，包含支付状态相关字段
         * @return 影响行数
         */
        int updatePaymentStatus(Receipt receipt);

        /**
         * 更新票据审核状态
         * 
         * @param receipt 票据信息，包含审核状态相关字段
         * @return 影响行数
         */
        int updateApprovalStatus(Receipt receipt);

        /**
         * 分页查询所有票据记录
         * 
         * @param pageSize 每页大小
         * @param offset   偏移量
         * @return 票据列表
         */
        List<Receipt> selectAllWithPagination(@Param("pageSize") int pageSize, @Param("offset") int offset);

        /**
         * 分页查询票据记录（支持条件）
         * 
         * @param userId             用户ID
         * @param reporterId         报票人ID
         * @param projectId          项目ID
         * @param verificationStatus 验证状态
         * @param paymentStatus      支付状态
         * @param type               票据类型
         * @param startDate          开始日期
         * @param endDate            结束日期
         * @param pageSize           每页大小
         * @param offset             偏移量
         * @return 票据列表
         */
        List<Receipt> selectByConditionsWithPagination(
                        @Param("userId") Integer userId,
                        @Param("reporterId") Integer reporterId,
                        @Param("projectId") Integer projectId,
                        @Param("verificationStatus") String verificationStatus,
                        @Param("paymentStatus") String paymentStatus,
                        @Param("type") String type,
                        @Param("startDate") java.util.Date startDate,
                        @Param("endDate") java.util.Date endDate,
                        @Param("pageSize") int pageSize,
                        @Param("offset") int offset);

        /**
         * 获取票据记录总数（支持条件）
         * 
         * @param userId             用户ID
         * @param reporterId         报票人ID
         * @param projectId          项目ID
         * @param verificationStatus 验证状态
         * @param paymentStatus      支付状态
         * @param type               票据类型
         * @param startDate          开始日期
         * @param endDate            结束日期
         * @return 票据记录总数
         */
        int countByConditions(
                        @Param("userId") Integer userId,
                        @Param("reporterId") Integer reporterId,
                        @Param("projectId") Integer projectId,
                        @Param("verificationStatus") String verificationStatus,
                        @Param("paymentStatus") String paymentStatus,
                        @Param("type") String type,
                        @Param("startDate") java.util.Date startDate,
                        @Param("endDate") java.util.Date endDate);

        /**
         * 获取所有票据记录总数
         * 
         * @return 票据记录总数
         */
        long count();

        /**
         * 根据财务审核状态查询票据
         * 
         * @param status 审核状态
         * @return 票据列表
         */
        List<Receipt> selectByFinanceStatus(@Param("status") String status);

        /**
         * 根据项目经理审核状态查询票据
         * 
         * @param status 审核状态
         * @return 票据列表
         */
        List<Receipt> selectByProjectManagerStatus(@Param("status") String status);

        /**
         * 根据经理审核状态查询票据
         * 
         * @param status 审核状态
         * @return 票据列表
         */
        List<Receipt> selectByManagerStatus(@Param("status") String status);

        /**
         * 查询待审核的票据（任意审核级别）
         * 
         * @param limit 限制数量
         * @return 票据列表
         */
        List<Receipt> selectPendingApprovals(@Param("limit") int limit);

        /**
         * 统计本月新增票据数量
         * 
         * @return 本月新增票据数量
         */
        long countThisMonth();

        /**
         * 分页查询待审核票据（按审核级别）
         * 
         * @param approvalLevel 审核级别
         * @param pageSize      每页大小
         * @param offset        偏移量
         * @return 票据列表
         */
        List<Receipt> selectPendingApprovalsByLevelWithPagination(
                        @Param("approvalLevel") Integer approvalLevel,
                        @Param("pageSize") int pageSize,
                        @Param("offset") int offset);

        /**
         * 统计待审核票据数量（按审核级别）
         * 
         * @param approvalLevel 审核级别
         * @return 票据数量
         */
        int countPendingApprovalsByLevel(@Param("approvalLevel") Integer approvalLevel);

        /**
         * 分页查询项目经理待审核票据
         * 
         * @param managerId 项目经理ID
         * @param pageSize  每页大小
         * @param offset    偏移量
         * @return 票据列表
         */
        List<Receipt> selectPendingApprovalsForManagerWithPagination(
                        @Param("managerId") Integer managerId,
                        @Param("pageSize") int pageSize,
                        @Param("offset") int offset);

        /**
         * 统计项目经理待审核票据数量
         * 
         * @param managerId 项目经理ID
         * @return 票据数量
         */
        int countPendingApprovalsForManager(@Param("managerId") Integer managerId);

        /**
         * 根据关键词搜索票据
         * 
         * @param keyword 关键词
         * @return 票据列表
         */
        List<Receipt> searchByKeyword(@Param("keyword") String keyword);

        /**
         * 检查票据是否重复
         * 通过票据编号判断
         * 
         * @param receiptNumber 票据编号
         * @return 重复票据数量
         */
        int checkDuplicateByReceiptNumber(
                        @Param("receiptNumber") String receiptNumber);

        /**
         * 根据文件哈希查询票据
         * 
         * @param fileHash 文件哈希值
         * @return 票据信息
         */
        Receipt selectByFileHash(@Param("fileHash") String fileHash);
}