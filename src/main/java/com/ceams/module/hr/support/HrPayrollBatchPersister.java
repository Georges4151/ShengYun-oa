package com.ceams.module.hr.support;

import com.ceams.module.hr.entity.HrSalaryCalc;
import com.ceams.module.hr.mapper.HrSalaryCalcMapper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 月度工资核算分片落库器。
 *
 * <p>批量核算按 200 条/批调用本类，每个批次使用独立事务（{@link Propagation#REQUIRES_NEW}），
 * 避免单条长事务长时间占用连接与行锁；某批失败只回滚当前批，其余批次已落库结果可重算覆盖。
 *
 * <p>日志只输出批次计数，禁止输出任何金额。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HrPayrollBatchPersister {

    private final HrSalaryCalcMapper calcMapper;

    /**
     * 落库一个批次：无 ID 视为新建，有 ID 视为覆盖重算。
     *
     * @param calculations 已计算完成的核算实体
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void persistBatch(List<HrSalaryCalc> calculations) {
        int inserted = 0;
        int updated = 0;
        for (HrSalaryCalc calc : calculations) {
            if (calc.getId() == null) {
                calcMapper.insert(calc);
                inserted++;
            } else {
                calcMapper.updateById(calc);
                updated++;
            }
        }
        log.info("月度工资核算落库批次完成：新增 {} 条，重算 {} 条", inserted, updated);
    }
}
