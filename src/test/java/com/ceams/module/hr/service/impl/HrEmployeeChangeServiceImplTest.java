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
import com.ceams.module.bpm.dto.InstanceStartRequest;
import com.ceams.module.bpm.service.BpmInstanceService;
import com.ceams.module.hr.converter.HrEmployeeChangeConverter;
import com.ceams.module.hr.converter.HrEmployeeConverter;
import com.ceams.module.hr.dto.EmployeeChangeRequest;
import com.ceams.module.hr.entity.HrEmployee;
import com.ceams.module.hr.entity.HrEmployeeChange;
import com.ceams.module.hr.enums.ChangeType;
import com.ceams.module.hr.enums.EmployeeStatus;
import com.ceams.module.hr.enums.HrApprovalStatus;
import com.ceams.module.hr.mapper.HrEmployeeChangeMapper;
import com.ceams.module.hr.mapper.HrEmployeeMapper;
import com.ceams.module.iam.mapper.DeptMapper;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** 入转调离申请单元测试：重复 PENDING 阻断、发起 BPM 后回写实例 ID。 */
@ExtendWith(MockitoExtension.class)
class HrEmployeeChangeServiceImplTest {

    private static final Long EMPLOYEE_ID = 900L;

    @Mock private HrEmployeeMapper employeeMapper;
    @Mock private HrEmployeeChangeMapper changeMapper;
    @Mock private BpmInstanceService bpmInstanceService;
    @Mock private DeptMapper deptMapper;

    private HrEmployeeServiceImpl employeeService;

    @BeforeEach
    void setUp() {
        employeeService =
                new HrEmployeeServiceImpl(
                        employeeMapper,
                        changeMapper,
                        Mappers.getMapper(HrEmployeeConverter.class),
                        Mappers.getMapper(HrEmployeeChangeConverter.class),
                        bpmInstanceService,
                        deptMapper);
    }

    @Test
    void regularShouldRejectWhenPendingChangeExists() {
        HrEmployee employee = activeEmployee();
        when(employeeMapper.selectById(EMPLOYEE_ID)).thenReturn(employee);
        when(changeMapper.selectCount(any(Wrapper.class))).thenReturn(1L);

        EmployeeChangeRequest request = changeRequest(LocalDate.of(2026, 11, 1));

        assertThatThrownBy(() -> employeeService.regular(EMPLOYEE_ID, request))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.BIZ_STATE_CONFLICT);
        verify(changeMapper, never()).insert(any(HrEmployeeChange.class));
        verify(bpmInstanceService, never()).start(any(InstanceStartRequest.class));
    }

    @Test
    void transferShouldStartBpmAndWriteBackInstanceId() {
        HrEmployee employee = activeEmployee();
        employee.setUserId(66L);
        when(employeeMapper.selectById(EMPLOYEE_ID)).thenReturn(employee);
        when(changeMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        when(changeMapper.insert(any(HrEmployeeChange.class)))
                .thenAnswer(
                        invocation -> {
                            HrEmployeeChange change = invocation.getArgument(0);
                            change.setId(7001L);
                            return 1;
                        });
        when(bpmInstanceService.start(any(InstanceStartRequest.class))).thenReturn("8008");

        EmployeeChangeRequest request = changeRequest(LocalDate.of(2026, 12, 1));
        request.setTargetDeptId(31L);
        request.setTargetPositionId(41L);

        String changeId = employeeService.transfer(EMPLOYEE_ID, request);

        assertThat(changeId).isEqualTo("7001");
        ArgumentCaptor<InstanceStartRequest> startCaptor =
                ArgumentCaptor.forClass(InstanceStartRequest.class);
        verify(bpmInstanceService).start(startCaptor.capture());
        InstanceStartRequest start = startCaptor.getValue();
        assertThat(start.getTemplateCode()).isEqualTo("HR_TRANSFER");
        assertThat(start.getBizType()).isEqualTo("HR_TRANSFER");
        assertThat(start.getBizId()).isEqualTo(7001L);
        assertThat(start.getVariables())
                .containsEntry("employeeId", String.valueOf(EMPLOYEE_ID))
                .containsEntry("changeType", ChangeType.TRANSFER.name())
                .containsEntry("targetDeptId", "31")
                .containsEntry("targetPositionId", "41")
                .containsEntry("effectiveDate", "2026-12-01");

        ArgumentCaptor<HrEmployeeChange> updateCaptor =
                ArgumentCaptor.forClass(HrEmployeeChange.class);
        verify(changeMapper).updateById(updateCaptor.capture());
        assertThat(updateCaptor.getValue().getBpmInstanceId()).isEqualTo(8008L);
        assertThat(updateCaptor.getValue().getApprovalStatus())
                .isEqualTo(HrApprovalStatus.PENDING);
    }

    @Test
    void changeShouldBeRejectedWhenEmployeeTerminated() {
        HrEmployee employee = activeEmployee();
        employee.setStatus(EmployeeStatus.TERMINATED);
        when(employeeMapper.selectById(EMPLOYEE_ID)).thenReturn(employee);

        EmployeeChangeRequest request = changeRequest(LocalDate.of(2026, 11, 20));

        assertThatThrownBy(() -> employeeService.resign(EMPLOYEE_ID, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("员工已离职");
        verify(changeMapper, never()).insert(any(HrEmployeeChange.class));
    }

    private HrEmployee activeEmployee() {
        HrEmployee employee = new HrEmployee();
        employee.setId(EMPLOYEE_ID);
        employee.setEmployeeNo("E0900");
        employee.setName("赵七");
        employee.setStatus(EmployeeStatus.ACTIVE);
        return employee;
    }

    private EmployeeChangeRequest changeRequest(LocalDate effectiveDate) {
        EmployeeChangeRequest request = new EmployeeChangeRequest();
        request.setEffectiveDate(effectiveDate);
        request.setReason("业务调整");
        return request;
    }
}
