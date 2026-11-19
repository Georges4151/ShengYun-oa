package com.ceams.module.hr.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ceams.module.bpm.entity.BpmInstance;
import com.ceams.module.bpm.enums.BpmInstanceStatus;
import com.ceams.module.hr.entity.HrEmployee;
import com.ceams.module.hr.entity.HrEmployeeChange;
import com.ceams.module.hr.entity.HrLeaveRequest;
import com.ceams.module.hr.enums.ChangeType;
import com.ceams.module.hr.enums.EmployeeStatus;
import com.ceams.module.hr.enums.HrApprovalStatus;
import com.ceams.module.hr.enums.LeaveType;
import com.ceams.module.hr.mapper.HrEmployeeChangeMapper;
import com.ceams.module.hr.mapper.HrEmployeeMapper;
import com.ceams.module.hr.mapper.HrLeaveRequestMapper;
import com.ceams.module.hr.service.HrLeaveQuotaService;
import com.ceams.module.iam.entity.SysUser;
import com.ceams.module.iam.enums.UserStatus;
import com.ceams.module.iam.mapper.UserMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** HR 审批回调单元测试：转正/调岗/离职的业务状态派生与请假额度扣减。 */
@ExtendWith(MockitoExtension.class)
class HrBpmCallbackTest {

    private static final Long BIZ_ID = 6001L;

    @Mock private HrEmployeeChangeMapper changeMapper;
    @Mock private HrEmployeeMapper employeeMapper;
    @Mock private UserMapper userMapper;
    @Mock private HrLeaveRequestMapper leaveRequestMapper;
    @Mock private HrLeaveQuotaService leaveQuotaService;

    @Test
    void regularApprovedShouldActivateEmployee() {
        HrRegularBpmCallback callback = new HrRegularBpmCallback(changeMapper, employeeMapper);
        HrEmployeeChange change = change(ChangeType.REGULAR);
        HrEmployee employee = employee(11L, 21L);
        when(changeMapper.selectById(BIZ_ID)).thenReturn(change);
        when(employeeMapper.selectById(11L)).thenReturn(employee);

        callback.onInstanceFinished(instance(), BpmInstanceStatus.APPROVED);

        ArgumentCaptor<HrEmployee> employeeCaptor = ArgumentCaptor.forClass(HrEmployee.class);
        verify(employeeMapper).updateById(employeeCaptor.capture());
        assertThat(employeeCaptor.getValue().getStatus()).isEqualTo(EmployeeStatus.ACTIVE);
        assertThat(employeeCaptor.getValue().getRegularDate()).isEqualTo(LocalDate.of(2026, 11, 1));

        ArgumentCaptor<HrEmployeeChange> changeCaptor =
                ArgumentCaptor.forClass(HrEmployeeChange.class);
        verify(changeMapper).updateById(changeCaptor.capture());
        assertThat(changeCaptor.getValue().getApprovalStatus())
                .isEqualTo(HrApprovalStatus.APPROVED);
    }

    @Test
    void regularRejectedShouldOnlyUpdateChangeStatus() {
        HrRegularBpmCallback callback = new HrRegularBpmCallback(changeMapper, employeeMapper);
        HrEmployeeChange change = change(ChangeType.REGULAR);
        when(changeMapper.selectById(BIZ_ID)).thenReturn(change);

        callback.onInstanceFinished(instance(), BpmInstanceStatus.REJECTED);

        verify(employeeMapper, never()).updateById(any(HrEmployee.class));
        assertThat(change.getApprovalStatus()).isEqualTo(HrApprovalStatus.REJECTED);
    }

    @Test
    void resignApprovedShouldTerminateEmployeeAndDisableUser() {
        HrResignBpmCallback callback =
                new HrResignBpmCallback(changeMapper, employeeMapper, userMapper);
        HrEmployeeChange change = change(ChangeType.RESIGN);
        HrEmployee employee = employee(12L, 22L);
        SysUser user = new SysUser();
        user.setId(22L);
        user.setStatus(UserStatus.ACTIVE);
        when(changeMapper.selectById(BIZ_ID)).thenReturn(change);
        when(employeeMapper.selectById(12L)).thenReturn(employee);
        when(userMapper.selectById(22L)).thenReturn(user);

        callback.onInstanceFinished(instance(), BpmInstanceStatus.APPROVED);

        assertThat(employee.getStatus()).isEqualTo(EmployeeStatus.TERMINATED);
        assertThat(employee.getLeaveDate()).isEqualTo(LocalDate.of(2026, 11, 1));
        assertThat(user.getStatus()).isEqualTo(UserStatus.DISABLED);
        assertThat(change.getApprovalStatus()).isEqualTo(HrApprovalStatus.APPROVED);
    }

    @Test
    void terminatedInstanceShouldBeDerivedAsCancelled() {
        HrResignBpmCallback callback =
                new HrResignBpmCallback(changeMapper, employeeMapper, userMapper);
        HrEmployeeChange change = change(ChangeType.RESIGN);
        when(changeMapper.selectById(BIZ_ID)).thenReturn(change);

        callback.onInstanceFinished(instance(), BpmInstanceStatus.TERMINATED);

        assertThat(change.getApprovalStatus()).isEqualTo(HrApprovalStatus.CANCELLED);
        verify(employeeMapper, never()).updateById(any(HrEmployee.class));
    }

    @Test
    void transferApprovedShouldSyncEmployeeAndUserDept() {
        HrTransferBpmCallback callback =
                new HrTransferBpmCallback(changeMapper, employeeMapper, userMapper);
        HrEmployeeChange change = change(ChangeType.TRANSFER);
        change.setTargetDeptId(31L);
        change.setTargetPositionId(41L);
        HrEmployee employee = employee(13L, 23L);
        SysUser user = new SysUser();
        user.setId(23L);
        user.setDeptId(30L);
        when(changeMapper.selectById(BIZ_ID)).thenReturn(change);
        when(employeeMapper.selectById(13L)).thenReturn(employee);
        when(userMapper.selectById(23L)).thenReturn(user);

        callback.onInstanceFinished(instance(), BpmInstanceStatus.APPROVED);

        assertThat(employee.getDeptId()).isEqualTo(31L);
        assertThat(employee.getPositionId()).isEqualTo(41L);
        assertThat(user.getDeptId()).isEqualTo(31L);
        assertThat(change.getApprovalStatus()).isEqualTo(HrApprovalStatus.APPROVED);
    }

    @Test
    void leaveApprovedShouldDeductQuota() {
        HrLeaveBpmCallback callback =
                new HrLeaveBpmCallback(leaveRequestMapper, leaveQuotaService);
        HrLeaveRequest leaveRequest = new HrLeaveRequest();
        leaveRequest.setId(BIZ_ID);
        leaveRequest.setEmployeeId(14L);
        leaveRequest.setLeaveType(LeaveType.ANNUAL);
        leaveRequest.setStartDate(LocalDate.of(2026, 11, 2));
        leaveRequest.setEndDate(LocalDate.of(2026, 11, 4));
        leaveRequest.setDays(new BigDecimal("3.0"));
        when(leaveRequestMapper.selectById(BIZ_ID)).thenReturn(leaveRequest);

        callback.onInstanceFinished(instance(), BpmInstanceStatus.APPROVED);

        verify(leaveQuotaService)
                .deduct(14L, 2026, LeaveType.ANNUAL, new BigDecimal("3.0"));
        assertThat(leaveRequest.getApprovalStatus()).isEqualTo(HrApprovalStatus.APPROVED);
    }

    @Test
    void leaveRejectedShouldNotDeductQuota() {
        HrLeaveBpmCallback callback =
                new HrLeaveBpmCallback(leaveRequestMapper, leaveQuotaService);
        HrLeaveRequest leaveRequest = new HrLeaveRequest();
        leaveRequest.setId(BIZ_ID);
        leaveRequest.setEmployeeId(15L);
        leaveRequest.setLeaveType(LeaveType.PERSONAL);
        leaveRequest.setStartDate(LocalDate.of(2026, 11, 5));
        leaveRequest.setDays(new BigDecimal("1.0"));
        when(leaveRequestMapper.selectById(BIZ_ID)).thenReturn(leaveRequest);

        callback.onInstanceFinished(instance(), BpmInstanceStatus.WITHDRAWN);

        verify(leaveQuotaService, never())
                .deduct(
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any());
        assertThat(leaveRequest.getApprovalStatus()).isEqualTo(HrApprovalStatus.WITHDRAWN);
    }

    private BpmInstance instance() {
        BpmInstance instance = new BpmInstance();
        instance.setId(9001L);
        instance.setBizId(BIZ_ID);
        return instance;
    }

    private HrEmployeeChange change(ChangeType type) {
        HrEmployeeChange change = new HrEmployeeChange();
        change.setId(BIZ_ID);
        change.setEmployeeId(changeEmployeeId(type));
        change.setChangeType(type);
        change.setEffectiveDate(LocalDate.of(2026, 11, 1));
        change.setApprovalStatus(HrApprovalStatus.PENDING);
        return change;
    }

    private Long changeEmployeeId(ChangeType type) {
        return switch (type) {
            case REGULAR -> 11L;
            case TRANSFER -> 13L;
            case RESIGN -> 12L;
        };
    }

    private HrEmployee employee(Long id, Long userId) {
        HrEmployee employee = new HrEmployee();
        employee.setId(id);
        employee.setUserId(userId);
        employee.setEmployeeNo("E" + id);
        employee.setName("员工" + id);
        employee.setStatus(EmployeeStatus.PROBATION);
        return employee;
    }
}
