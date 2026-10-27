package com.ceams.dao;

import com.ceams.model.entity.GeneralLedger;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

@Mapper
public interface GeneralLedgerMapper {

    /**
     * 查询所有总账记录
     *
     * @return 总账列表
     */
    List<GeneralLedger> selectAll();

    /**
     * 根据ID查询
     *
     * @param id 总账ID
     * @return 总账信息
     */
    GeneralLedger selectById(Long id);

    /**
     * 根据账户名称查询
     *
     * @param accountName 账户名称
     * @return 总账列表
     */
    List<GeneralLedger> selectByAccountName(@Param("accountName") String accountName);

    /**
     * 按日期范围查询
     *
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @return 总账列表
     */
    List<GeneralLedger> selectByDateRange(@Param("startDate") Date startDate, @Param("endDate") Date endDate);

    /**
     * 按状态查询
     *
     * @param status 状态
     * @return 总账列表
     */
    List<GeneralLedger> selectByStatus(@Param("status") Integer status);

    /**
     * 分页查询
     *
     * @param offset 偏移量
     * @param limit  每页数量
     * @return 总账列表
     */
    List<GeneralLedger> selectByPage(@Param("offset") int offset, @Param("limit") int limit);

    /**
     * 统计总数
     *
     * @return 总记录数
     */
    long count();

    /**
     * 插入记录
     *
     * @param ledger 总账信息
     * @return 影响行数
     */
    int insert(GeneralLedger ledger);

    /**
     * 更新记录
     *
     * @param ledger 总账信息
     * @return 影响行数
     */
    int update(GeneralLedger ledger);

    /**
     * 根据ID删除
     *
     * @param id 总账ID
     * @return 影响行数
     */
    int deleteById(Long id);
}
