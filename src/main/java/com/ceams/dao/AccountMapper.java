package com.ceams.dao;

import com.ceams.model.entity.Account;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Mapper
public interface AccountMapper {
        /**
         * 根据ID查询账目
         * 
         * @param id 账目ID
         * @return 账目信息
         */
        Account selectById(Integer id);

        /**
         * 根据项目ID查询账目
         * 
         * @param projectId 项目ID
         * @return 账目列表
         */
        List<Account> selectByProjectId(@Param("projectId") Integer projectId);

        /**
         * 根据日期范围查询账目
         * 
         * @param startDate 开始日期
         * @param endDate   结束日期
         * @return 账目列表
         */
        List<Account> selectByDateRange(@Param("startDate") Date startDate, @Param("endDate") Date endDate);

        /**
         * 根据类型查询账目
         * 
         * @param type 账目类型
         * @return 账目列表
         */
        List<Account> selectByType(@Param("type") String type);

        /**
         * 获取账目汇总信息
         * 
         * @param projectId 项目ID
         * @param startDate 开始日期
         * @param endDate   结束日期
         * @return 汇总信息
         */
        Map<String, Object> getAccountSummary(@Param("projectId") Integer projectId, @Param("startDate") Date startDate,
                        @Param("endDate") Date endDate);

        /**
         * 获取月度统计
         * 
         * @param year 年份
         * @return 月度统计列表
         */
        List<Map<String, Object>> getMonthlyStats(@Param("year") Integer year);

        /**
         * 插入账目
         * 
         * @param account 账目信息
         * @return 影响行数
         */
        int insert(Account account);

        /**
         * 更新账目
         * 
         * @param account 账目信息
         * @return 影响行数
         */
        int update(Account account);

        /**
         * 删除账目
         * 
         * @param id 账目ID
         * @return 影响行数
         */
        int deleteById(Integer id);

        /**
         * 查询所有账目
         * 
         * @return 账目列表
         */
        List<Account> selectAll();

        /**
         * 分页查询所有账目记录
         * 
         * @param pageSize 每页大小
         * @param offset   偏移量
         * @return 账目列表
         */
        List<Account> selectAllWithPagination(@Param("pageSize") int pageSize, @Param("offset") int offset);

        /**
         * 分页查询账目记录（支持条件）
         * 
         * @param projectId 项目ID
         * @param type      账目类型
         * @param startDate 开始日期
         * @param endDate   结束日期
         * @param pageSize  每页大小
         * @param offset    偏移量
         * @return 账目列表
         */
        List<Account> selectByConditionsWithPagination(
                        @Param("projectId") Integer projectId,
                        @Param("type") String type,
                        @Param("startDate") Date startDate,
                        @Param("endDate") Date endDate,
                        @Param("pageSize") int pageSize,
                        @Param("offset") int offset);

        /**
         * 获取账目记录总数（支持条件）
         * 
         * @param projectId 项目ID
         * @param type      账目类型
         * @param startDate 开始日期
         * @param endDate   结束日期
         * @return 账目记录总数
         */
        int countByConditions(
                        @Param("projectId") Integer projectId,
                        @Param("type") String type,
                        @Param("startDate") Date startDate,
                        @Param("endDate") Date endDate);

        /**
         * 获取所有账目记录总数
         * 
         * @return 账目记录总数
         */
        long count();

        /**
         * 根据合同ID查询账目记录
         * 
         * @param contractId 合同ID
         * @return 账目记录列表
         */
        List<Account> selectByContractId(Integer contractId);

        /**
         * 根据关键词搜索账目
         * 
         * @param keyword 关键词
         * @return 账目列表
         */
        List<Account> searchByKeyword(@Param("keyword") String keyword);
}