package com.ceams.module.hr.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ceams.common.crypto.SensitiveEncryptor;
import com.ceams.common.exception.BusinessException;
import com.ceams.common.exception.ErrorCode;
import com.ceams.module.bpm.enums.AuditAction;
import com.ceams.module.bpm.service.BpmAuditService;
import com.ceams.module.hr.config.HrPayrollProperties;
import com.ceams.module.hr.dto.PayrollAdjustRequest;
import com.ceams.module.hr.dto.PayrollCalcRequest;
import com.ceams.module.hr.dto.PayrollConfirmRequest;
import com.ceams.module.hr.entity.HrEmployee;
import com.ceams.module.hr.entity.HrSalary;
import com.ceams.module.hr.entity.HrSalaryCalc;
import com.ceams.module.hr.enums.EmployeeStatus;
import com.ceams.module.hr.enums.PayrollStatus;
import com.ceams.module.hr.enums.SalaryStatus;
import com.ceams.module.hr.mapper.HrCheckinMapper;
import com.ceams.module.hr.mapper.HrEmployeeMapper;
import com.ceams.module.hr.mapper.HrLeaveRequestMapper;
import com.ceams.module.hr.mapper.HrPerformanceReviewMapper;
import com.ceams.module.hr.mapper.HrSalaryCalcMapper;
import com.ceams.module.hr.mapper.HrSalaryMapper;
import com.ceams.module.hr.support.HrPayrollBatchPersister;
import com.ceams.module.hr.vo.PayrollCalcResultVO;
import com.ceams.module.hr.vo.SalaryCalcVO;
import com.ceams.module.iam.mapper.DeptMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** 月度工资核算服务单元测试：幂等重算、锁定跳过、无薪资跳过、调整重算与批量确认。 */
@ExtendWith(MockitoExtension.class)
class HrPayrollServiceImplTest {

    private static final Long EMPLOYEE_ID = 900L;

    private static final Long CALC_ID = 7001L;

    @Mock private HrSalaryCalcMapper calcMapper;
    @Mock private HrSalaryMapper salaryMapper;
    @Mock private HrEmployeeMapper employeeMapper;
    @Mock private HrCheckinMapper checkinMapper;
    @Mock private HrLeaveRequestMapper leaveRequestMapper;
    @Mock private HrPerformanceReviewMapper reviewMapper;
    @Mock private BpmAuditService bpmAuditService;
    @Mock private SensitiveEncryptor encryptor;
    @Mock private DeptMapper deptMapper;

    private HrPayrollProperties properties;
    private HrPayrollServiceImpl payrollService;

    @BeforeEach
    void setUp() {
        properties = new HrPayrollProperties();
        payrollService =
                new HrPayrollServiceImpl(
                        calcMapper,
                        salaryMapper,
                        employeeMapper,
                        checkinMapper,
                        leaveRequestMapper,
                        reviewMapper,
                        new HrPayrollBatchPersister(calcMapper),
                        properties,
                        bpmAuditService,
                        encryptor,
                        deptMapper);
    }

    @Test
    void calculateShouldRecalculateExistingDraft() {
        when(employeeMapper.selectList(any())).thenReturn(List.of(employee()));
        when(calcMapper.selectList(any()))
                .thenReturn(List.of(existingCalc(PayrollStatus.DRAFT)));
        when(salaryMapper.selectList(any())).thenReturn(List.of(salary()));
        when(checkinMapper.selectList(any())).thenReturn(List.of());
        when(leaveRequestMapper.selectList(any())).thenReturn(List.of());
        when(reviewMapper.selectList(any())).thenReturn(List.of());
        when(calcMapper.updateById(any(HrSalaryCalc.class))).thenReturn(1);

        PayrollCalcResultVO result = payrollService.calculate(calcRequest());

        assertThat(result.getCalcMonth()).isEqualTo("2026-11");
        assertThat(result.getTotal()).isEqualTo(1);
        assertThat(result.getCreated()).isZero();
        assertThat(result.getRecalculated()).isEqualTo(1);
        assertThat(result.getSkipped()).isZero();
        assertThat(result.getLocked()).isZero();

        ArgumentCaptor<HrSalaryCalc> captor = ArgumentCaptor.forClass(HrSalaryCalc.class);
        verify(calcMapper).updateById(captor.capture());
        verify(calcMapper, never()).insert(any(HrSalaryCalc.class));
        HrSalaryCalc saved = captor.getValue();
        assertThat(saved.getId()).isEqualTo(CALC_ID);
        assertThat(saved.getStatus()).isEqualTo(PayrollStatus.DRAFT);
        assertThat(saved.getAttendanceDays()).isEqualByComparingTo("21.75");
        assertThat(saved.getAbsentDays()).isEqualByComparingTo("0.00");
        assertThat(saved.getLateCount()).isZero();
        assertThat(saved.getPerformanceCoefficient()).isEqualByComparingTo("1.0000");
        assertThat(new BigDecimal(saved.getGrossPayEnc())).isEqualByComparingTo("17000.00");
        assertThat(new BigDecimal(saved.getNetPayEnc())).isEqualByComparingTo("13647.50");
    }

    @Test
    void calculateShouldSkipConfirmedRecord() {
        when(employeeMapper.selectList(any())).thenReturn(List.of(employee()));
        when(calcMapper.selectList(any()))
                .thenReturn(List.of(existingCalc(PayrollStatus.CONFIRMED)));

        PayrollCalcResultVO result = payrollService.calculate(calcRequest());

        assertThat(result.getLocked()).isEqualTo(1);
        assertThat(result.getCreated()).isZero();
        assertThat(result.getRecalculated()).isZero();
        assertThat(result.getWarnings()).anyMatch(warning -> warning.contains("跳过重算"));
        verify(calcMapper, never()).insert(any(HrSalaryCalc.class));
        verify(calcMapper, never()).updateById(any(HrSalaryCalc.class));
    }

    @Test
    void calculateShouldSkipEmployeeWithoutActiveSalary() {
        when(employeeMapper.selectList(any())).thenReturn(List.of(employee()));
        when(calcMapper.selectList(any())).thenReturn(List.of());
        when(salaryMapper.selectList(any())).thenReturn(List.of());

        PayrollCalcResultVO result = payrollService.calculate(calcRequest());

        assertThat(result.getSkipped()).isEqualTo(1);
        assertThat(result.getCreated()).isZero();
        assertThat(result.getWarnings()).anyMatch(warning -> warning.contains("无生效薪资"));
        verify(calcMapper, never()).insert(any(HrSalaryCalc.class));
    }

    @Test
    void adjustShouldRecomputeGrossAndNet() {
        when(calcMapper.selectById(CALC_ID)).thenReturn(existingCalc(PayrollStatus.DRAFT));
        when(salaryMapper.selectOne(any())).thenReturn(salary());
        when(encryptor.encrypt(anyString())).thenReturn("cipher");
        when(calcMapper.updateById(any(HrSalaryCalc.class))).thenReturn(1);

        PayrollAdjustRequest request = new PayrollAdjustRequest();
        request.setAllowance(new BigDecimal("2000"));
        request.setPerformanceCoefficient(new BigDecimal("1.2"));

        SalaryCalcVO vo = payrollService.adjust(CALC_ID, request);

        assertThat(vo.getPerformanceAmount()).isEqualByComparingTo("2400.00");
        assertThat(vo.getGrossPay()).isEqualByComparingTo("19400.00");
        // 未显式指定个税时按调整后的应发重算：(19400-1575-1050-5000)*10%-210=967.50
        assertThat(vo.getTax()).isEqualByComparingTo("967.50");
        assertThat(vo.getNetPay()).isEqualByComparingTo("15807.50");

        ArgumentCaptor<HrSalaryCalc> captor = ArgumentCaptor.forClass(HrSalaryCalc.class);
        verify(calcMapper).updateById(captor.capture());
        HrSalaryCalc saved = captor.getValue();
        assertThat(new BigDecimal(saved.getAllowanceEnc())).isEqualByComparingTo("2000.00");
        assertThat(new BigDecimal(saved.getPerformanceAmountEnc())).isEqualByComparingTo("2400.00");
        assertThat(new BigDecimal(saved.getGrossPayEnc())).isEqualByComparingTo("19400.00");
        assertThat(new BigDecimal(saved.getNetPayEnc())).isEqualByComparingTo("15807.50");
        assertThat(saved.getPerformanceCoefficient()).isEqualByComparingTo("1.2000");
        verify(bpmAuditService)
                .record(
                        eq("HR_SALARY_CALC"),
                        eq(CALC_ID),
                        eq(AuditAction.ADJUST),
                        any(),
                        any(),
                        anyString(),
                        anyLong());
    }

    @Test
    void adjustShouldRejectNonDraftRecord() {
        when(calcMapper.selectById(CALC_ID)).thenReturn(existingCalc(PayrollStatus.CONFIRMED));

        PayrollAdjustRequest request = new PayrollAdjustRequest();
        request.setAllowance(new BigDecimal("100"));

        assertThatThrownBy(() -> payrollService.adjust(CALC_ID, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.BIZ_STATE_CONFLICT);
        verify(calcMapper, never()).updateById(any(HrSalaryCalc.class));
    }

    @Test
    void confirmShouldOnlyConfirmDraftRecords() {
        HrSalaryCalc first = existingCalc(PayrollStatus.DRAFT);
        HrSalaryCalc second = existingCalc(PayrollStatus.DRAFT);
        second.setId(7002L);
        second.setEmployeeId(901L);
        when(calcMapper.selectList(any())).thenReturn(List.of(first, second));
        when(calcMapper.updateById(any(HrSalaryCalc.class))).thenReturn(1);

        PayrollConfirmRequest request = new PayrollConfirmRequest();
        request.setCalcMonth("2026-11");

        int confirmed = payrollService.confirm(request);

        assertThat(confirmed).isEqualTo(2);
        ArgumentCaptor<HrSalaryCalc> captor = ArgumentCaptor.forClass(HrSalaryCalc.class);
        verify(calcMapper, times(2)).updateById(captor.capture());
        assertThat(captor.getAllValues())
                .allSatisfy(
                        calc -> {
                            assertThat(calc.getStatus()).isEqualTo(PayrollStatus.CONFIRMED);
                            assertThat(calc.getConfirmedBy()).isEqualTo(0L);
                            assertThat(calc.getConfirmedAt()).isNotNull();
                        });
        verify(bpmAuditService)
                .record(
                        eq("HR_SALARY_CALC"),
                        isNull(),
                        eq(AuditAction.CONFIRM),
                        isNull(),
                        any(),
                        isNull(),
                        eq(0L));
    }

    private PayrollCalcRequest calcRequest() {
        PayrollCalcRequest request = new PayrollCalcRequest();
        request.setCalcMonth("2026-11");
        return request;
    }

    private HrEmployee employee() {
        HrEmployee employee = new HrEmployee();
        employee.setId(EMPLOYEE_ID);
        employee.setEmployeeNo("E0900");
        employee.setName("赵七");
        employee.setStatus(EmployeeStatus.ACTIVE);
        return employee;
    }

    private HrSalary salary() {
        HrSalary salary = new HrSalary();
        salary.setEmployeeId(EMPLOYEE_ID);
        salary.setEffectiveDate(LocalDate.of(2026, 1, 1));
        salary.setBaseSalaryEnc("10000");
        salary.setPostSalaryEnc("5000");
        salary.setPerformanceBaseEnc("2000");
        salary.setAllowanceEnc("0");
        salary.setStatus(SalaryStatus.ACTIVE);
        return salary;
    }

    private HrSalaryCalc existingCalc(PayrollStatus status) {
        HrSalaryCalc calc = new HrSalaryCalc();
        calc.setId(CALC_ID);
        calc.setEmployeeId(EMPLOYEE_ID);
        calc.setCalcMonth("2026-11");
        calc.setBaseSalaryEnc("10000");
        calc.setPostSalaryEnc("5000");
        calc.setAttendanceDays(new BigDecimal("21.75"));
        calc.setAbsentDays(new BigDecimal("0.00"));
        calc.setLateCount(0);
        calc.setAttendanceDeductionEnc("0.00");
        calc.setPerformanceCoefficient(new BigDecimal("1.0000"));
        calc.setPerformanceAmountEnc("2000.00");
        calc.setAllowanceEnc("1000.00");
        calc.setSocialInsuranceEnc("1575.00");
        calc.setHousingFundEnc("1050.00");
        calc.setTaxEnc("727.50");
        calc.setGrossPayEnc("18000.00");
        calc.setNetPayEnc("14647.50");
        calc.setStatus(status);
        return calc;
    }
}
