package com.ceams.module.hr.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ceams.common.crypto.SensitiveEncryptor;
import com.ceams.common.exception.BusinessException;
import com.ceams.common.exception.ErrorCode;
import com.ceams.module.bpm.service.BpmAuditService;
import com.ceams.module.hr.config.HrPayrollProperties;
import com.ceams.module.hr.entity.HrSalaryCalc;
import com.ceams.module.hr.enums.PayrollStatus;
import com.ceams.module.hr.mapper.HrCheckinMapper;
import com.ceams.module.hr.mapper.HrEmployeeMapper;
import com.ceams.module.hr.mapper.HrLeaveRequestMapper;
import com.ceams.module.hr.mapper.HrPerformanceReviewMapper;
import com.ceams.module.hr.mapper.HrSalaryCalcMapper;
import com.ceams.module.hr.mapper.HrSalaryMapper;
import com.ceams.module.hr.support.HrPayrollBatchPersister;
import com.ceams.module.hr.vo.PayrollCostSummaryVO;
import com.ceams.module.iam.mapper.DeptMapper;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** 人力成本汇总单元测试：只统计非 DRAFT、逐条求和、人均实发、非法月份。 */
@ExtendWith(MockitoExtension.class)
class HrPayrollCostSummaryTest {

    @Mock private HrSalaryCalcMapper calcMapper;
    @Mock private HrSalaryMapper salaryMapper;
    @Mock private HrEmployeeMapper employeeMapper;
    @Mock private HrCheckinMapper checkinMapper;
    @Mock private HrLeaveRequestMapper leaveRequestMapper;
    @Mock private HrPerformanceReviewMapper reviewMapper;
    @Mock private BpmAuditService bpmAuditService;
    @Mock private SensitiveEncryptor encryptor;
    @Mock private DeptMapper deptMapper;

    private HrPayrollServiceImpl payrollService;

    @BeforeEach
    void setUp() {
        payrollService =
                new HrPayrollServiceImpl(
                        calcMapper,
                        salaryMapper,
                        employeeMapper,
                        checkinMapper,
                        leaveRequestMapper,
                        reviewMapper,
                        new HrPayrollBatchPersister(calcMapper),
                        new HrPayrollProperties(),
                        bpmAuditService,
                        encryptor,
                        deptMapper);
    }

    @Test
    void shouldSumOnlyConfirmedAndLockedRecords() {
        when(calcMapper.selectList(any()))
                .thenReturn(
                        List.of(
                                calc(1L, PayrollStatus.CONFIRMED, "10000.00", "8000.00", "1000.00", "500.00", "500.00"),
                                calc(2L, PayrollStatus.LOCKED, "5000.00", "4000.00", "600.00", "300.00", "100.00"),
                                // 即使 Mapper 误返回草稿，也不计入成本口径
                                calc(3L, PayrollStatus.DRAFT, "99999.00", "88888.00", "0", "0", "0")));

        PayrollCostSummaryVO summary = payrollService.costSummary("2026-11", null);

        assertThat(summary.getMonth()).isEqualTo("2026-11");
        assertThat(summary.getHeadcount()).isEqualTo(2L);
        assertThat(summary.getGrossPay()).isEqualByComparingTo("15000.00");
        assertThat(summary.getNetPay()).isEqualByComparingTo("12000.00");
        assertThat(summary.getSocialInsurance()).isEqualByComparingTo("1600.00");
        assertThat(summary.getHousingFund()).isEqualByComparingTo("800.00");
        assertThat(summary.getTax()).isEqualByComparingTo("600.00");
        assertThat(summary.getAvgNetPay()).isEqualByComparingTo("6000.00");
        assertThat(summary.getGrossPay().scale()).isEqualTo(2);
    }

    @Test
    void shouldReturnZeroAmountsWhenDeptHasNoEmployee() {
        when(employeeMapper.selectList(any())).thenReturn(List.of());

        PayrollCostSummaryVO summary = payrollService.costSummary("2026-11", 8L);

        assertThat(summary.getMonth()).isEqualTo("2026-11");
        assertThat(summary.getHeadcount()).isZero();
        assertThat(summary.getNetPay()).isEqualByComparingTo("0.00");
        assertThat(summary.getAvgNetPay()).isEqualByComparingTo("0.00");
        verify(calcMapper, never()).selectList(any());
    }

    @Test
    void shouldReturnZeroAverageWhenNoRecord() {
        when(calcMapper.selectList(any())).thenReturn(List.of());

        PayrollCostSummaryVO summary = payrollService.costSummary("2026-11", null);

        assertThat(summary.getHeadcount()).isZero();
        assertThat(summary.getAvgNetPay()).isEqualByComparingTo("0.00");
    }

    @Test
    void shouldRejectBlankMonth() {
        assertThatThrownBy(() -> payrollService.costSummary(" ", null))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.VALID_REQUIRED_FIELD);
    }

    @Test
    void shouldRejectIllegalMonthFormat() {
        assertThatThrownBy(() -> payrollService.costSummary("2026/11", null))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.VALID_ILLEGAL_ARGUMENT);
        verify(calcMapper, never()).selectList(any());
    }

    private HrSalaryCalc calc(
            Long employeeId,
            PayrollStatus status,
            String grossPay,
            String netPay,
            String socialInsurance,
            String housingFund,
            String tax) {
        HrSalaryCalc calc = new HrSalaryCalc();
        calc.setEmployeeId(employeeId);
        calc.setCalcMonth("2026-11");
        calc.setStatus(status);
        // 单元测试直接使用明文，模拟 SensitiveTypeHandler 解密后的返回值
        calc.setGrossPayEnc(grossPay);
        calc.setNetPayEnc(netPay);
        calc.setSocialInsuranceEnc(socialInsurance);
        calc.setHousingFundEnc(housingFund);
        calc.setTaxEnc(tax);
        return calc;
    }
}
