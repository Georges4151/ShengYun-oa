package com.ceams.service;

import com.ceams.model.entity.GeneralLedger;

import java.util.Date;
import java.util.List;

/**
 * 总账管理服务接口
 */
public interface GeneralLedgerService {

    /**
     * 获取所有总账记录
     *
     * @return 总账列表
     */
    List<GeneralLedger> getAllLedgers();

    /**
     * 根据ID获取总账记录
     *
     * @param id 总账ID
     * @return 总账信息
     */
    GeneralLedger getLedgerById(Long id);

    /**
     * 根据账户名称查询
     *
     * @param accountName 账户名称
     * @return 总账列表
     */
    List<GeneralLedger> getLedgersByAccountName(String accountName);

    /**
     * 按日期范围查询
     *
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @return 总账列表
     */
    List<GeneralLedger> getLedgersByDateRange(Date startDate, Date endDate);

    /**
     * 按状态查询
     *
     * @param status 状态
     * @return 总账列表
     */
    List<GeneralLedger> getLedgersByStatus(Integer status);

    /**
     * 分页查询
     *
     * @param pageNum  页码
     * @param pageSize 每页数量
     * @return 总账列表
     */
    List<GeneralLedger> getLedgersWithPagination(int pageNum, int pageSize);

    /**
     * 统计总数
     *
     * @return 总记录数
     */
    long count();

    /**
     * 添加总账记录
     *
     * @param ledger 总账信息
     * @return 操作结果
     */
    boolean addLedger(GeneralLedger ledger);

    /**
     * 更新总账记录
     *
     * @param ledger 总账信息
     * @return 操作结果
     */
    boolean updateLedger(GeneralLedger ledger);

    /**
     * 删除总账记录
     *
     * @param id 总账ID
     * @return 操作结果
     */
    boolean deleteLedger(Long id);
}
