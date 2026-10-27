package com.ceams.service.impl;

import com.ceams.dao.GeneralLedgerMapper;
import com.ceams.model.entity.GeneralLedger;
import com.ceams.service.GeneralLedgerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/**
 * 总账管理服务实现类
 */
@Service
public class GeneralLedgerServiceImpl implements GeneralLedgerService {

    private static final Logger log = LoggerFactory.getLogger(GeneralLedgerServiceImpl.class);

    @Autowired
    private GeneralLedgerMapper generalLedgerMapper;

    @Override
    public List<GeneralLedger> getAllLedgers() {
        log.info("查询所有总账记录");
        return generalLedgerMapper.selectAll();
    }

    @Override
    public GeneralLedger getLedgerById(Long id) {
        log.info("根据ID查询总账记录, id: {}", id);
        if (id == null) {
            log.warn("查询总账记录时ID为空");
            return null;
        }
        return generalLedgerMapper.selectById(id);
    }

    @Override
    public List<GeneralLedger> getLedgersByAccountName(String accountName) {
        log.info("根据账户名称查询总账记录, accountName: {}", accountName);
        if (accountName == null || accountName.trim().isEmpty()) {
            log.warn("查询总账记录时账户名称为空");
            return List.of();
        }
        return generalLedgerMapper.selectByAccountName(accountName);
    }

    @Override
    public List<GeneralLedger> getLedgersByDateRange(Date startDate, Date endDate) {
        log.info("按日期范围查询总账记录, startDate: {}, endDate: {}", startDate, endDate);
        if (startDate == null || endDate == null) {
            log.warn("查询总账记录时日期范围为空");
            return List.of();
        }
        return generalLedgerMapper.selectByDateRange(startDate, endDate);
    }

    @Override
    public List<GeneralLedger> getLedgersByStatus(Integer status) {
        log.info("按状态查询总账记录, status: {}", status);
        if (status == null) {
            log.warn("查询总账记录时状态为空");
            return List.of();
        }
        return generalLedgerMapper.selectByStatus(status);
    }

    @Override
    public List<GeneralLedger> getLedgersWithPagination(int pageNum, int pageSize) {
        log.info("分页查询总账记录, pageNum: {}, pageSize: {}", pageNum, pageSize);
        if (pageNum < 1 || pageSize < 1) {
            log.warn("分页参数无效, pageNum: {}, pageSize: {}", pageNum, pageSize);
            return List.of();
        }
        int offset = (pageNum - 1) * pageSize;
        return generalLedgerMapper.selectByPage(offset, pageSize);
    }

    @Override
    public long count() {
        log.info("统计总账记录总数");
        return generalLedgerMapper.count();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean addLedger(GeneralLedger ledger) {
        log.info("添加总账记录, accountName: {}", ledger != null ? ledger.getAccountName() : null);
        if (ledger == null) {
            log.warn("添加总账记录时参数为空");
            return false;
        }
        int rows = generalLedgerMapper.insert(ledger);
        log.info("添加总账记录结果, 影响行数: {}", rows);
        return rows > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateLedger(GeneralLedger ledger) {
        log.info("更新总账记录, id: {}", ledger != null ? ledger.getId() : null);
        if (ledger == null || ledger.getId() == null) {
            log.warn("更新总账记录时参数无效");
            return false;
        }
        int rows = generalLedgerMapper.update(ledger);
        log.info("更新总账记录结果, 影响行数: {}", rows);
        return rows > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteLedger(Long id) {
        log.info("删除总账记录, id: {}", id);
        if (id == null) {
            log.warn("删除总账记录时ID为空");
            return false;
        }
        int rows = generalLedgerMapper.deleteById(id);
        log.info("删除总账记录结果, 影响行数: {}", rows);
        return rows > 0;
    }
}
