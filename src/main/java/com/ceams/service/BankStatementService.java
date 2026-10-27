package com.ceams.service;

import com.ceams.dao.BankStatementMapper;
import com.ceams.model.Pagination;
import com.ceams.model.entity.BankStatement;
import com.ceams.util.BankStatementParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Service
public class BankStatementService {

    @Autowired
    private BankStatementMapper bankStatementMapper;

    /**
     * 根据ID获取银行流水
     * 
     * @param id 银行流水ID
     * @return 银行流水信息
     */
    public BankStatement getBankStatementById(Integer id) {
        return bankStatementMapper.selectById(id);
    }

    /**
     * 根据账户ID获取银行流水
     * 
     * @param accountId 账户ID
     * @return 银行流水列表
     */
    public List<BankStatement> getBankStatementsByAccountId(Integer accountId) {
        return bankStatementMapper.selectByAccountId(accountId);
    }

    /**
     * 根据日期范围获取银行流水
     * 
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @return 银行流水列表
     */
    public List<BankStatement> getBankStatementsByDateRange(Date startDate, Date endDate) {
        return bankStatementMapper.selectByDateRange(startDate, endDate);
    }

    /**
     * 根据账户ID和日期范围获取银行流水
     * 
     * @param accountId 账户ID
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @return 银行流水列表
     */
    public List<BankStatement> getBankStatementsByAccountIdAndDateRange(Integer accountId, Date startDate,
            Date endDate) {
        return bankStatementMapper.selectByAccountIdAndDateRange(accountId, startDate, endDate);
    }

    /**
     * 根据条件获取银行流水
     * 
     * @param accountId   账户ID
     * @param type        交易类型
     * @param startDate   开始日期
     * @param endDate     结束日期
     * @param description 交易描述
     * @return 银行流水列表
     */
    public List<BankStatement> getBankStatementsByConditions(Integer accountId, String type, Date startDate,
            Date endDate, String description) {
        return bankStatementMapper.selectByConditions(accountId, type, startDate, endDate, description);
    }

    /**
     * 获取银行流水汇总信息
     * 
     * @param accountId 账户ID
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @return 汇总信息
     */
    public Map<String, Object> getBankStatementSummary(Integer accountId, Date startDate, Date endDate) {
        return bankStatementMapper.getStatementSummary(accountId, startDate, endDate);
    }

    /**
     * 添加银行流水
     * 
     * @param bankStatement 银行流水信息
     * @return 是否添加成功
     */
    @Transactional
    public boolean addBankStatement(BankStatement bankStatement) {
        // 设置默认值
        if (bankStatement.getStatus() == null) {
            bankStatement.setStatus("unreconciled");
        }
        return bankStatementMapper.insert(bankStatement) > 0;
    }

    /**
     * 批量添加银行流水
     * 
     * @param bankStatements 银行流水列表
     * @return 是否添加成功
     */
    @Transactional
    public boolean batchAddBankStatements(List<BankStatement> bankStatements) {
        if (bankStatements == null || bankStatements.isEmpty()) {
            return false;
        }
        // 设置默认值
        for (BankStatement statement : bankStatements) {
            if (statement.getStatus() == null) {
                statement.setStatus("unreconciled");
            }
        }
        return bankStatementMapper.batchInsert(bankStatements) > 0;
    }

    /**
     * 批量保存银行流水（与batchAddBankStatements方法功能相同，为了兼容FXBankReconciliationController中的调用）
     * 
     * @param bankStatements 银行流水列表
     * @return 保存的记录数
     */
    @Transactional
    public int batchSaveBankStatements(List<BankStatement> bankStatements) {
        if (bankStatements == null || bankStatements.isEmpty()) {
            return 0;
        }
        // 设置默认值
        for (BankStatement statement : bankStatements) {
            if (statement.getStatus() == null) {
                statement.setStatus("unreconciled");
            }
        }
        return bankStatementMapper.batchInsert(bankStatements);
    }

    /**
     * 更新银行流水
     * 
     * @param bankStatement 银行流水信息
     * @return 是否更新成功
     */
    @Transactional
    public boolean updateBankStatement(BankStatement bankStatement) {
        return bankStatementMapper.update(bankStatement) > 0;
    }

    /**
     * 更新银行流水状态
     * 
     * @param id     银行流水ID
     * @param status 状态
     * @return 是否更新成功
     */
    @Transactional
    public boolean updateBankStatementStatus(Integer id, String status) {
        return bankStatementMapper.updateStatus(id, status) > 0;
    }

    /**
     * 删除银行流水
     * 
     * @param id 银行流水ID
     * @return 是否删除成功
     */
    @Transactional
    public boolean deleteBankStatement(Integer id) {
        return bankStatementMapper.deleteById(id) > 0;
    }

    /**
     * 根据账户ID删除银行流水
     * 
     * @param accountId 账户ID
     * @return 是否删除成功
     */
    @Transactional
    public boolean deleteBankStatementsByAccountId(Integer accountId) {
        return bankStatementMapper.deleteByAccountId(accountId) > 0;
    }

    /**
     * 获取所有银行流水
     * 
     * @return 银行流水列表
     */
    public List<BankStatement> getAllBankStatements() {
        return bankStatementMapper.selectAll();
    }

    /**
     * 分页获取银行流水
     * 
     * @param pagination 分页参数
     * @return 银行流水列表
     */
    public List<BankStatement> getBankStatementsWithPagination(Pagination pagination) {
        return bankStatementMapper.selectAllWithPagination(pagination.getPageSize(), pagination.getOffset());
    }

    /**
     * 分页根据条件获取银行流水
     * 
     * @param accountId   账户ID
     * @param type        交易类型
     * @param startDate   开始日期
     * @param endDate     结束日期
     * @param description 交易描述
     * @param pagination  分页参数
     * @return 银行流水列表
     */
    public List<BankStatement> getBankStatementsByConditionsWithPagination(
            Integer accountId, String type, Date startDate, Date endDate, String description, Pagination pagination) {
        return bankStatementMapper.selectByConditionsWithPagination(
                accountId, type, startDate, endDate, description, pagination.getPageSize(), pagination.getOffset());
    }

    /**
     * 获取银行流水数量
     * 
     * @param accountId   账户ID
     * @param type        交易类型
     * @param startDate   开始日期
     * @param endDate     结束日期
     * @param description 交易描述
     * @return 银行流水数量
     */
    public int getBankStatementCountByConditions(
            Integer accountId, String type, Date startDate, Date endDate, String description) {
        return bankStatementMapper.countByConditions(accountId, type, startDate, endDate, description);
    }

    /**
     * 从文件导入银行流水
     * 
     * @param inputStream 文件输入流
     * @param fileType    文件类型（csv, excel）
     * @param accountId   账户ID
     * @return 导入的银行流水列表
     * @throws Exception 导入异常
     */
    @Transactional
    public List<BankStatement> importBankStatements(InputStream inputStream, String fileType, Integer accountId)
            throws Exception {
        // 这里将通过FXBankReconciliationController中的文件上传功能实现
        // 解析文件，获取银行流水记录
        List<BankStatement> statements = new ArrayList<>();

        // 批量保存到数据库
        if (!statements.isEmpty()) {
            batchAddBankStatements(statements);
        }

        return statements;
    }

    /**
     * 智能匹配银行流水与系统交易
     * 
     * @param accountId 账户ID
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @return 匹配结果
     */
    public Map<String, Object> autoMatchBankStatements(Integer accountId, Date startDate, Date endDate) {
        // 这里将在ReconciliationService中实现
        return null;
    }

    /**
     * 清理重复的银行流水
     * 
     * @param accountId 账户ID
     * @return 清理的记录数
     */
    @Transactional
    public int cleanDuplicateBankStatements(Integer accountId) {
        // 实现清理重复记录的逻辑
        // 可以基于金额、日期、描述等字段的组合来判断重复
        return 0;
    }

    /**
     * 导出银行流水为Excel
     * 
     * @param accountId 账户ID
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @return Excel文件字节数组
     * @throws Exception 导出异常
     */
    public byte[] exportBankStatements(Integer accountId, Date startDate, Date endDate) throws Exception {
        // 实现导出逻辑
        return null;
    }
}
