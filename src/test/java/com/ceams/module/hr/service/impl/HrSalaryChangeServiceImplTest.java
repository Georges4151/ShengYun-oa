package com.ceams.module.hr.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.ceams.common.crypto.SensitiveEncryptor;
import com.ceams.module.bpm.dto.InstanceStartRequest;
import com.ceams.module.bpm.service.BpmInstanceService;
import com.ceams.module.hr.dto.SalaryChangeApplyRequest;
import com.ceams.module.hr.entity.HrEmployee;
import com.ceams.module.hr.entity.HrSalary;
import com.ceams.module.hr.entity.HrSalaryChangeApply;
import com.ceams.module.hr.enums.EmployeeStatus;
import com.ceams.module.hr.enums.SalaryChangeStatus;
import com.ceams.module.hr.enums.SalaryChangeType;
import com.ceams.module.hr.enums.SalaryStatus;
import com.ceams.module.hr.mapper.HrEmployeeMapper;
import com.ceams.module.hr.mapper.HrSalaryChangeApplyMapper;
import com.ceams.module.hr.mapper.HrSalaryMapper;
import com.ceams.module.hr.support.SalarySnapshotCodec;
import com.ceams.module.iam.mapper.UserMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Base64;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.annotation.Transactional;

/** 薪资变动申请单元测试：发起成功回写实例 ID、金额密文快照、发起失败的事务边界。 */
@ExtendWith(MockitoExtension.class)
class HrSalaryChangeServiceImplTest {

    private static final Long EMPLOYEE_ID = 900L;
    private static final Long APPLY_ID = 7001L;

    @Mock private HrSalaryChangeApplyMapper changeApplyMapper;
    @Mock private HrSalaryMapper salaryMapper;
    @Mock private HrEmployeeMapper employeeMapper;
    @Mock private UserMapper userMapper;
    @Mock private BpmInstanceService bpmInstanceService;

    private SalarySnapshotCodec snapshotCodec;
    private HrSalaryChangeServiceImpl changeService;

    @BeforeEach
    void setUp() {
        String secret =
                Base64.getEncoder()
                        .encodeToString(
                                "0123456789abcdef0123456789abcdef"
                                        .getBytes(StandardCharsets.UTF_8));
        snapshotCodec = new SalarySnapshotCodec(new SensitiveEncryptor(secret), new ObjectMapper());
        changeService =
                new HrSalaryChangeServiceImpl(
                        changeApplyMapper,
                        salaryMapper,
                        employeeMapper,
                        userMapper,
                        snapshotCodec,
                        bpmInstanceService);
    }

    @Test
    void applyChangeShouldCreateApplyAndWriteBackInstanceId() {
        when(employeeMapper.selectById(EMPLOYEE_ID)).thenReturn(employee());
        when(salaryMapper.selectOne(any(Wrapper.class))).thenReturn(activeSalary());
        when(changeApplyMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        when(changeApplyMapper.insert(any(HrSalaryChangeApply.class)))
                .thenAnswer(
                        invocation -> {
                            HrSalaryChangeApply apply = invocation.getArgument(0);
                            apply.setId(APPLY_ID);
                            return 1;
                        });
        when(bpmInstanceService.start(any(InstanceStartRequest.class))).thenReturn("8008");

        String applyId = changeService.applyChange(changeRequest());

        assertThat(applyId).isEqualTo("7001");
        ArgumentCaptor<InstanceStartRequest> startCaptor =
                ArgumentCaptor.forClass(InstanceStartRequest.class);
        verify(bpmInstanceService).start(startCaptor.capture());
        InstanceStartRequest start = startCaptor.getValue();
        assertThat(start.getTemplateCode()).isEqualTo("HR_SALARY_CHANGE");
        assertThat(start.getBizType()).isEqualTo("HR_SALARY_CHANGE");
        assertThat(start.getBizId()).isEqualTo(APPLY_ID);
        assertThat(start.getVariables())
                .containsEntry("employeeId", String.valueOf(EMPLOYEE_ID))
                .containsEntry("changeType", SalaryChangeType.ADJUST.name())
                .containsEntry("effectiveDate", "2026-12-01");

        ArgumentCaptor<HrSalaryChangeApply> insertCaptor =
                ArgumentCaptor.forClass(HrSalaryChangeApply.class);
        verify(changeApplyMapper).insert(insertCaptor.capture());
        HrSalaryChangeApply inserted = insertCaptor.getValue();
        assertThat(inserted.getStatus()).isEqualTo(SalaryChangeStatus.APPROVING);
        assertThat(inserted.getChangeType()).isEqualTo(SalaryChangeType.ADJUST);

        // 快照中的金额必须以密文承载：Base64 密文不含 "."，可安全断言明文小数不出现
        assertThat(inserted.getBeforeSnapshot()).doesNotContain("8000.00").doesNotContain("4000.00");
        assertThat(inserted.getAfterSnapshot()).doesNotContain("9000.00").doesNotContain("4000.00");
        SalarySnapshotCodec.SalarySnapshot after = snapshotCodec.decode(inserted.getAfterSnapshot());
        assertThat(after.baseSalary()).isEqualByComparingTo("9000.00");
        assertThat(after.postSalary()).isEqualByComparingTo("4000.00");

        ArgumentCaptor<HrSalaryChangeApply> updateCaptor =
                ArgumentCaptor.forClass(HrSalaryChangeApply.class);
        verify(changeApplyMapper).updateById(updateCaptor.capture());
        assertThat(updateCaptor.getValue().getBpmInstanceId()).isEqualTo(8008L);
    }

    @Test
    void applyChangeShouldPropagateBpmFailureAndKeepTransactionalBoundary() throws Exception {
        when(employeeMapper.selectById(EMPLOYEE_ID)).thenReturn(employee());
        when(salaryMapper.selectOne(any(Wrapper.class))).thenReturn(activeSalary());
        when(changeApplyMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        when(bpmInstanceService.start(any(InstanceStartRequest.class)))
                .thenThrow(new IllegalStateException("BPM 不可用"));

        assertThatThrownBy(() -> changeService.applyChange(changeRequest()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("BPM 不可用");

        // 申请单已落库，但 applyChange 标注 @Transactional，异常会回滚，不残留孤儿申请单
        verify(changeApplyMapper).insert(any(HrSalaryChangeApply.class));
        verify(changeApplyMapper, never()).updateById(any(HrSalaryChangeApply.class));
        Method method =
                HrSalaryChangeServiceImpl.class.getMethod(
                        "applyChange", SalaryChangeApplyRequest.class);
        assertThat(method.isAnnotationPresent(Transactional.class)).isTrue();
    }

    private SalaryChangeApplyRequest changeRequest() {
        SalaryChangeApplyRequest request = new SalaryChangeApplyRequest();
        request.setEmployeeId(EMPLOYEE_ID);
        request.setChangeType(SalaryChangeType.ADJUST);
        request.setEffectiveDate(LocalDate.of(2026, 12, 1));
        request.setBaseSalary(new BigDecimal("9000"));
        request.setReason("年度调薪");
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

    private HrSalary activeSalary() {
        HrSalary salary = new HrSalary();
        salary.setId(4001L);
        salary.setEmployeeId(EMPLOYEE_ID);
        salary.setEffectiveDate(LocalDate.of(2026, 1, 1));
        salary.setBaseSalaryEnc("8000.00");
        salary.setPostSalaryEnc("4000.00");
        salary.setStatus(SalaryStatus.ACTIVE);
        return salary;
    }
}
