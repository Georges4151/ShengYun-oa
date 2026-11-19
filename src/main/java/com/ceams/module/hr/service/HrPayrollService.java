package com.ceams.module.hr.service;

import com.ceams.common.response.PageResult;
import com.ceams.module.hr.dto.PayrollAdjustRequest;
import com.ceams.module.hr.dto.PayrollCalcRequest;
import com.ceams.module.hr.dto.PayrollConfirmRequest;
import com.ceams.module.hr.enums.PayrollStatus;
import com.ceams.module.hr.vo.PayrollCalcResultVO;
import com.ceams.module.hr.vo.PayrollCostSummaryVO;
import com.ceams.module.hr.vo.SalaryCalcVO;

/**
 * 月度工资核算服务。
 *
 * <p>状态机：DRAFT → CONFIRMED → LOCKED。DRAFT 可重算与手工调整；CONFIRMED/LOCKED 在批量核算时跳过；
 * 本期不提供解锁与更正接口。
 */
public interface HrPayrollService {

    /** 按月份初始化 / 重算工资核算（幂等：DRAFT 覆盖重算，CONFIRMED/LOCKED 跳过）。 */
    PayrollCalcResultVO calculate(PayrollCalcRequest request);

    /** 核算明细分页（支持核算月、状态、员工过滤）。 */
    PageResult<SalaryCalcVO> page(
            long page, long size, String calcMonth, PayrollStatus status, Long employeeId);

    /** 手工调整单项金额 / 绩效系数（仅 DRAFT），调整后重算应发与实发。 */
    SalaryCalcVO adjust(Long id, PayrollAdjustRequest request);

    /** 按月批量确认 DRAFT 记录，返回确认条数；LOCKED 不受影响。 */
    int confirm(PayrollConfirmRequest request);

    /**
     * 人力成本汇总（只读聚合，无事务）。
     *
     * <p>口径：仅统计 {@code hr_salary_calc} 中 {@code calcMonth=month} 且状态非 DRAFT 的记录
     * （即 CONFIRMED / LOCKED，工资已确认才进入成本口径）；加密金额字段经
     * {@code SensitiveTypeHandler} 读出明文后在 Java 内逐条累加，禁止 SQL SUM 加密列。
     * headcount 为 distinct employeeId 数，avgNetPay = netPay / headcount（headcount 为 0 时取 0）。
     * 所有金额输出保留 2 位小数。
     *
     * @param month 核算月（yyyy-MM），为空或格式非法抛 BusinessException
     * @param deptId 部门 ID，非空时先解析该部门员工集合再过滤
     * @return 人力成本汇总
     */
    PayrollCostSummaryVO costSummary(String month, Long deptId);
}
