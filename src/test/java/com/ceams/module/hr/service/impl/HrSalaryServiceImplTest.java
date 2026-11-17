package com.ceams.module.hr.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.ceams.common.exception.BusinessException;
import com.ceams.common.exception.ErrorCode;
import com.ceams.module.hr.dto.SalaryCreateRequest;
import com.ceams.module.hr.entity.HrEmployee;
import com.ceams.module.hr.entity.HrSalary;
import com.ceams.module.hr.enums.EmployeeStatus;
import com.ceams.module.hr.enums.SalaryStatus;
import com.ceams.module.hr.mapper.HrEmployeeMapper;
import com.ceams.module.hr.mapper.HrSalaryMapper;
import com.ceams.module.hr.service.HrEmployeeService;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** 薪资台账单元测试：首次建档、重复建档拦截、无档案查询 404 语义。 */
@ExtendWith(MockitoExtension.class)
class HrSalaryServiceImplTest {

    private static final Long EMPLOYEE_ID = 900L;

    @Mock private HrSalaryMapper salaryMapper;
    @Mock private HrEmployeeMapper employeeMapper;
    @Mock private HrEmployeeService employeeService;

    private HrSalaryServiceImpl salaryService;

    @BeforeEach
    void setUp() {
        salaryService = new HrSalaryServiceImpl(salaryMapper, employeeMapper, employeeService);
    }

    @Test
    void createSalaryShouldInsertActiveWhenNoActiveExists() {
        when(employeeMapper.selectById(EMPLOYEE_ID)).thenReturn(employee());
        when(salaryMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        when(salaryMapper.insert(any(HrSalary.class)))
                .thenAnswer(
                        invocation -> {
                            HrSalary salary = invocation.getArgument(0);
                            salary.setId(5001L);
                            return 1;
                        });

        String id = salaryService.createSalary(createRequest());

        assertThat(id).isEqualTo("5001");
        ArgumentCaptor<HrSalary> captor = ArgumentCaptor.forClass(HrSalary.class);
        verify(salaryMapper).insert(captor.capture());
        HrSalary saved = captor.getValue();
        assertThat(saved.getEmployeeId()).isEqualTo(EMPLOYEE_ID);
        assertThat(saved.getStatus()).isEqualTo(SalaryStatus.ACTIVE);
        assertThat(new BigDecimal(saved.getBaseSalaryEnc())).isEqualByComparingTo("10000.00");
        assertThat(new BigDecimal(saved.getPostSalaryEnc())).isEqualByComparingTo("5000.00");
        assertThat(saved.getEffectiveDate()).isEqualTo(LocalDate.of(2026, 11, 1));
    }

    @Test
    void createSalaryShouldRejectWhenActiveAlreadyExists() {
        when(employeeMapper.selectById(EMPLOYEE_ID)).thenReturn(employee());
        when(salaryMapper.selectCount(any(Wrapper.class))).thenReturn(1L);

        assertThatThrownBy(() -> salaryService.createSalary(createRequest()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.BIZ_STATE_CONFLICT);
        verify(salaryMapper, never()).insert(any(HrSalary.class));
    }

    @Test
    void mySalaryShouldFailWithNotFoundWhenCurrentUserHasNoEmployeeProfile() {
        when(employeeService.currentEmployeeId())
                .thenThrow(
                        BusinessException.of(
                                ErrorCode.BIZ_NOT_FOUND, "当前账号未绑定员工档案"));

        assertThatThrownBy(() -> salaryService.mySalary())
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.BIZ_NOT_FOUND);
        verify(salaryMapper, never()).selectOne(any(Wrapper.class));
    }

    private SalaryCreateRequest createRequest() {
        SalaryCreateRequest request = new SalaryCreateRequest();
        request.setEmployeeId(EMPLOYEE_ID);
        request.setEffectiveDate(LocalDate.of(2026, 11, 1));
        request.setBaseSalary(new BigDecimal("10000"));
        request.setPostSalary(new BigDecimal("5000"));
        request.setRemark("首次建档");
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
}
