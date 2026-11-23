package com.ceams.module.hr.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ceams.module.bpm.entity.BpmInstance;
import com.ceams.module.bpm.enums.BpmInstanceStatus;
import com.ceams.module.hr.entity.HrEmployee;
import com.ceams.module.hr.entity.HrEmployeeChange;
import com.ceams.module.hr.enums.ChangeType;
import com.ceams.module.hr.enums.EmployeeStatus;
import com.ceams.module.hr.enums.HrApprovalStatus;
import com.ceams.module.hr.mapper.HrEmployeeChangeMapper;
import com.ceams.module.hr.mapper.HrEmployeeMapper;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** 晋升审批回调单元测试：通过即生效、驳回/撤回只改状态、单据缺失安全跳过。 */
@ExtendWith(MockitoExtension.class)
class HrPromotionBpmCallbackTest {

    private static final Long CHANGE_ID = 6001L;
    private static final Long EMPLOYEE_ID = 11L;
    private static final Long TARGET_LEVEL_ID = 9L;

    @Mock private HrEmployeeChangeMapper changeMapper;
    @Mock private HrEmployeeMapper employeeMapper;

    private HrPromotionBpmCallback callback;

    @BeforeEach
    void setUp() {
        callback = new HrPromotionBpmCallback(changeMapper, employeeMapper);
    }

    @Test
    void approvedShouldApplyTargetLevelImmediatelyIgnoringEffectiveDate() {
        HrEmployeeChange change = change();
        // 生效日期在未来，审批通过仍应立即写入职级
        change.setEffectiveDate(LocalDate.of(2027, 1, 1));
        HrEmployee employee = employee();
        when(changeMapper.selectById(CHANGE_ID)).thenReturn(change);
        when(employeeMapper.selectById(EMPLOYEE_ID)).thenReturn(employee);

        callback.onInstanceFinished(instance(), BpmInstanceStatus.APPROVED);

        ArgumentCaptor<HrEmployee> captor = ArgumentCaptor.forClass(HrEmployee.class);
        verify(employeeMapper).updateById(captor.capture());
        assertThat(captor.getValue().getLevelId()).isEqualTo(TARGET_LEVEL_ID);
        assertThat(change.getApprovalStatus()).isEqualTo(HrApprovalStatus.APPROVED);
    }

    @Test
    void rejectedShouldOnlyUpdateChangeStatus() {
        HrEmployeeChange change = change();
        when(changeMapper.selectById(CHANGE_ID)).thenReturn(change);

        callback.onInstanceFinished(instance(), BpmInstanceStatus.REJECTED);

        verify(employeeMapper, never()).updateById(any(HrEmployee.class));
        assertThat(change.getApprovalStatus()).isEqualTo(HrApprovalStatus.REJECTED);
    }

    @Test
    void withdrawnShouldOnlyUpdateChangeStatus() {
        HrEmployeeChange change = change();
        when(changeMapper.selectById(CHANGE_ID)).thenReturn(change);

        callback.onInstanceFinished(instance(), BpmInstanceStatus.WITHDRAWN);

        verify(employeeMapper, never()).updateById(any(HrEmployee.class));
        assertThat(change.getApprovalStatus()).isEqualTo(HrApprovalStatus.WITHDRAWN);
    }

    @Test
    void runningInstanceShouldNotWriteAnything() {
        HrEmployeeChange change = change();
        when(changeMapper.selectById(CHANGE_ID)).thenReturn(change);

        callback.onInstanceFinished(instance(), BpmInstanceStatus.RUNNING);

        verify(employeeMapper, never()).selectById(any());
        verify(employeeMapper, never()).updateById(any(HrEmployee.class));
        verify(changeMapper, never()).updateById(any(HrEmployeeChange.class));
        assertThat(change.getApprovalStatus()).isEqualTo(HrApprovalStatus.PENDING);
    }

    @Test
    void missingChangeShouldSkipSafely() {
        when(changeMapper.selectById(CHANGE_ID)).thenReturn(null);

        assertThatCode(() -> callback.onInstanceFinished(instance(), BpmInstanceStatus.APPROVED))
                .doesNotThrowAnyException();
        verify(employeeMapper, never()).selectById(any());
        verify(changeMapper, never()).updateById(any(HrEmployeeChange.class));
    }

    @Test
    void missingEmployeeShouldStillFinishChangeStatus() {
        HrEmployeeChange change = change();
        when(changeMapper.selectById(CHANGE_ID)).thenReturn(change);
        when(employeeMapper.selectById(EMPLOYEE_ID)).thenReturn(null);

        assertThatCode(() -> callback.onInstanceFinished(instance(), BpmInstanceStatus.APPROVED))
                .doesNotThrowAnyException();
        // 员工档案缺失时跳过职级回写，但申请单仍需落终态，避免长期占用待审批唯一校验
        verify(employeeMapper, never()).updateById(any(HrEmployee.class));
        ArgumentCaptor<HrEmployeeChange> captor = ArgumentCaptor.forClass(HrEmployeeChange.class);
        verify(changeMapper).updateById(captor.capture());
        assertThat(captor.getValue().getApprovalStatus()).isEqualTo(HrApprovalStatus.APPROVED);
        assertThat(change.getApprovalStatus()).isEqualTo(HrApprovalStatus.APPROVED);
    }

    @Test
    void approvedWithoutTargetLevelShouldKeepLevelButMarkApproved() {
        HrEmployeeChange change = change();
        change.setTargetLevelId(null);
        HrEmployee employee = employee();
        when(changeMapper.selectById(CHANGE_ID)).thenReturn(change);
        when(employeeMapper.selectById(EMPLOYEE_ID)).thenReturn(employee);

        callback.onInstanceFinished(instance(), BpmInstanceStatus.APPROVED);

        verify(employeeMapper, never()).updateById(any(HrEmployee.class));
        assertThat(employee.getLevelId()).isEqualTo(3L);
        verify(changeMapper).updateById(change);
        assertThat(change.getApprovalStatus()).isEqualTo(HrApprovalStatus.APPROVED);
    }

    // ------------------------------------------------------------------
    // 夹具
    // ------------------------------------------------------------------

    private BpmInstance instance() {
        BpmInstance instance = new BpmInstance();
        instance.setId(9001L);
        instance.setBizId(CHANGE_ID);
        return instance;
    }

    private HrEmployeeChange change() {
        HrEmployeeChange change = new HrEmployeeChange();
        change.setId(CHANGE_ID);
        change.setEmployeeId(EMPLOYEE_ID);
        change.setChangeType(ChangeType.PROMOTION);
        change.setTargetLevelId(TARGET_LEVEL_ID);
        change.setEffectiveDate(LocalDate.of(2026, 12, 1));
        change.setApprovalStatus(HrApprovalStatus.PENDING);
        return change;
    }

    private HrEmployee employee() {
        HrEmployee employee = new HrEmployee();
        employee.setId(EMPLOYEE_ID);
        employee.setEmployeeNo("E0011");
        employee.setName("员工11");
        employee.setLevelId(3L);
        employee.setStatus(EmployeeStatus.ACTIVE);
        return employee;
    }
}
