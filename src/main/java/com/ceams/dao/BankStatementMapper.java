package com.ceams.dao;

import com.ceams.model.entity.BankStatement;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Mapper
public interface BankStatementMapper {
    /**
     * 根据ID查询银行流水
     * 
     * @param id 银行流水ID
     * @return 银行流水信息
     */
    BankStatement selectById(Integer id);

    /**
     * 根据账户ID查询银行流水
     * 
     * @param accountId 账户ID
     * @return 银行流水列表
     */
    List<BankStatement> selectByAccountId(@Param("accountId") Integer accountId);

    /**
     * 根据日期范围查询银行流水
     * 
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @return 银行流水列表
     */
    List<BankStatement> selectByDateRange(@Param("startDate") Date startDate, @Param("endDate") Date endDate);

    /**
     * 根据账户ID和日期范围查询银行流水
     * 
     * @param accountId 账户ID
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @return 银行流水列表
     */
    List<BankStatement> selectByAccountIdAndDateRange(
                    @Param("accountId") Integer accountId,
                    @Param("startDate") Date startDate,
                    @Param("endDate") Date endDate);

    /**
     * 根据条件查询银行流水
     * 
     * @param accountId   账户ID
     * @param type        交易类型
     * @param startDate   开始日期
     * @param endDate     结束日期
     * @param description 交易描述
     * @return 银行流水列表
     */
    List<BankStatement> selectByConditions(
                    @Param("accountId") Integer accountId,
                    @Param("type") String type,
                    @Param("startDate") Date startDate,
                    @Param("endDate") Date endDate,
                    @Param("description") String description);

    /**
     * 获取银行流水汇总信息
     * 
     * @param accountId 账户ID
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @return 汇总信息
     */
    Map<String, Object> getStatementSummary(
                    @Param("accountId") Integer accountId,
                    @Param("startDate") Date startDate,
                    @Param("endDate") Date endDate);

    /**
     * 插入银行流水
     * 
     * @param statement 银行流水信息
     * @return 影响行数
     */
    int insert(BankStatement statement);

    /**
     * 批量插入银行流水
     * 
     * @param statements 银行流水列表
     * @return 影响行数
     */
    int batchInsert(@Param("statements") List<BankStatement> statements);

    /**
     * 更新银行流水
     * 
     * @param statement 银行流水信息
     * @return 影响行数
     */
    int update(BankStatement statement);

    /**
     * 更新银行流水状态
     * 
     * @param id     银行流水ID
     * @param status 状态
     * @return 影响行数
     */
    int updateStatus(@Param("id") Integer id, @Param("status") String status);

    /**
     * 删除银行流水
     * 
     * @param id 银行流水ID
     * @return 影响行数
     */
    int deleteById(Integer id);

    /**
     * 根据账户ID删除银行流水
     * 
     * @param accountId 账户ID
     * @return 影响行数
     */
    int deleteByAccountId(@Param("accountId") Integer accountId);

    /**
     * 查询所有银行流水
     * 
     * @return 银行流水列表
     */
    List<BankStatement> selectAll();

    /**
     * 分页查询所有银行流水
     * 
     * @param pageSize 每页大小
     * @param offset   偏移量
     * @return 银行流水列表
     */
    List<BankStatement> selectAllWithPagination(@Param("pageSize") int pageSize, @Param("offset") int offset);

    /**
     * 分页查询银行流水（支持条件）
     * 
     * @param accountId   账户ID
     * @param type        交易类型
     * @param startDate   开始日期
     * @param endDate     结束日期
     * @param description 交易描述
     * @param pageSize    每页大小
     * @param offset      偏移量
     * @return 银行流水列表
     */
    List<BankStatement> selectByConditionsWithPagination(
                    @Param("accountId") Integer accountId,
                    @Param("type") String type,
                    @Param("startDate") Date startDate,
                    @Param("endDate") Date endDate,
                    @Param("description") String description,
                    @Param("pageSize") int pageSize,
                    @Param("offset") int offset);

    /**
     * 统计银行流水数量
     * 
     * @param accountId   账户ID
     * @param type        交易类型
     * @param startDate   开始日期
     * @param endDate     结束日期
     * @param description 交易描述
     * @return 银行流水数量
     */
    int countByConditions(
                    @Param("accountId") Integer accountId,
                    @Param("type") String type,
                    @Param("startDate") Date startDate,
                    @Param("endDate") Date endDate,
                    @Param("description") String description);
}
