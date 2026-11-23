package com.ceams.module.hr.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.ceams.common.crypto.SensitiveTypeHandler;
import com.ceams.common.exception.BusinessException;
import com.ceams.module.bpm.entity.BpmInstance;
import com.ceams.module.bpm.enums.BpmInstanceStatus;
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
import com.ceams.module.hr.mapper.HrJobLevelMapper;
import com.ceams.module.hr.mapper.HrPositionMapper;
import com.ceams.module.hr.support.HrResignBpmCallback;
import com.ceams.module.iam.mapper.DeptMapper;
import com.ceams.module.iam.mapper.UserMapper;
import java.time.LocalDate;
import java.util.List;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** 离职下属强制转交：申请校验、审批通过后的批量改挂与变更记录。 */
@ExtendWith(MockitoExtension.class)
class HrResignTransferTest {

    private static final Long EMPLOYEE_ID = 300L;
    private static final Long SUBORDINATE_ID = 310L;
    private static final Long TARGET_ID = 320L;

    @Mock private HrEmployeeMapper employeeMapper;
    @Mock private HrEmployeeChangeMapper changeMapper;
    @Mock private HrJobLevelMapper jobLevelMapper;
    @Mock private HrPositionMapper positionMapper;
    @Mock private BpmInstanceService bpmInstanceService;
    @Mock private DeptMapper deptMapper;
    @Mock private UserMapper userMapper;

    private HrEmployeeServiceImpl employeeService;
    private HrResignBpmCallback resignCallback;

    @BeforeAll
    static void initTableInfoCache() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        configuration.getTypeHandlerRegistry().register(new SensitiveTypeHandler(null));
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
        TableInfoHelper.initTableInfo(assistant, HrEmployee.class);
    }

    @BeforeEach
    void setUp() {
        employeeService =
                new HrEmployeeServiceImpl(
                        employeeMapper,
                        changeMapper,
                        jobLevelMapper,
                        positionMapper,
                        Mappers.getMapper(HrEmployeeConverter.class),
                        Mappers.getMapper(HrEmployeeChangeConverter.class),
                        bpmInstanceService,
                        deptMapper,
                        userMapper);
        resignCallback = new HrResignBpmCallback(changeMapper, employeeMapper, userMapper);
    }

    // ------------------------------------------------------------------
    // 发起离职申请
    // ------------------------------------------------------------------

    @Test
    void resignShouldRejectWhenSubordinatesExistWithoutTarget() {
        when(employeeMapper.selectById(EMPLOYEE_ID)).thenReturn(employee(EMPLOYEE_ID, EmployeeStatus.ACTIVE));
        when(employeeMapper.selectList(any())).thenReturn(List.of(employee(SUBORDINATE_ID, EmployeeStatus.ACTIVE)));

        assertThatThrownBy(() -> employeeService.resign(EMPLOYEE_ID, resignRequest(null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("必须指定下属转交上级");
        verify(changeMapper, never()).insert(any(HrEmployeeChange.class));
    }

    @Test
    void resignShouldRejectTerminatedTransferTarget() {
        when(employeeMapper.selectById(EMPLOYEE_ID)).thenReturn(employee(EMPLOYEE_ID, EmployeeStatus.ACTIVE));
        when(employeeMapper.selectList(any())).thenReturn(List.of(employee(SUBORDINATE_ID, EmployeeStatus.ACTIVE)));
        when(employeeMapper.selectById(TARGET_ID))
                .thenReturn(employee(TARGET_ID, EmployeeStatus.TERMINATED));

        assertThatThrownBy(() -> employeeService.resign(EMPLOYEE_ID, resignRequest(TARGET_ID)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("已离职");
    }

    @Test
    void resignShouldRejectTransferTargetAsSubordinate() {
        when(employeeMapper.selectById(EMPLOYEE_ID)).thenReturn(employee(EMPLOYEE_ID, EmployeeStatus.ACTIVE));
        when(employeeMapper.selectList(any())).thenReturn(List.of(employee(SUBORDINATE_ID, EmployeeStatus.ACTIVE)));
        when(employeeMapper.selectById(SUBORDINATE_ID))
                .thenReturn(employee(SUBORDINATE_ID, EmployeeStatus.ACTIVE));

        assertThatThrownBy(() -> employeeService.resign(EMPLOYEE_ID, resignRequest(SUBORDINATE_ID)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不能是待转交的下属");
    }

    @Test
    void resignShouldPersistTransferTarget() {
        when(employeeMapper.selectById(EMPLOYEE_ID)).thenReturn(employee(EMPLOYEE_ID, EmployeeStatus.ACTIVE));
        when(employeeMapper.selectList(any())).thenReturn(List.of(employee(SUBORDINATE_ID, EmployeeStatus.ACTIVE)));
        when(employeeMapper.selectById(TARGET_ID)).thenReturn(employee(TARGET_ID, EmployeeStatus.ACTIVE));
        when(bpmInstanceService.start(any())).thenReturn("7001");

        employeeService.resign(EMPLOYEE_ID, resignRequest(TARGET_ID));

        ArgumentCaptor<HrEmployeeChange> captor = ArgumentCaptor.forClass(HrEmployeeChange.class);
        verify(changeMapper).insert(captor.capture());
        assertThat(captor.getValue().getResignLeaderId()).isEqualTo(TARGET_ID);
        assertThat(captor.getValue().getChangeType()).isEqualTo(ChangeType.RESIGN);
    }

    @Test
    void resignShouldSkipTransferWhenNoSubordinates() {
        when(employeeMapper.selectById(EMPLOYEE_ID)).thenReturn(employee(EMPLOYEE_ID, EmployeeStatus.ACTIVE));
        when(employeeMapper.selectList(any())).thenReturn(List.of());
        when(bpmInstanceService.start(any())).thenReturn("7002");

        employeeService.resign(EMPLOYEE_ID, resignRequest(null));

        ArgumentCaptor<HrEmployeeChange> captor = ArgumentCaptor.forClass(HrEmployeeChange.class);
        verify(changeMapper).insert(captor.capture());
        assertThat(captor.getValue().getResignLeaderId()).isNull();
    }

    // ------------------------------------------------------------------
    // 审批通过后的自动转交
    // ------------------------------------------------------------------

    @Test
    void approvedShouldTransferSubordinatesAndWriteRecords() {
        HrEmployeeChange change = resignChange(TARGET_ID);
        when(changeMapper.selectById(change.getId())).thenReturn(change);
        when(employeeMapper.selectById(EMPLOYEE_ID)).thenReturn(employee(EMPLOYEE_ID, EmployeeStatus.ACTIVE));
        when(employeeMapper.selectList(any())).thenReturn(List.of(employee(SUBORDINATE_ID, EmployeeStatus.ACTIVE)));
        when(employeeMapper.selectById(TARGET_ID)).thenReturn(employee(TARGET_ID, EmployeeStatus.ACTIVE));

        resignCallback.onInstanceFinished(instance(change.getId()), BpmInstanceStatus.APPROVED);

        ArgumentCaptor<HrEmployee> employeeCaptor = ArgumentCaptor.forClass(HrEmployee.class);
        verify(employeeMapper, org.mockito.Mockito.atLeast(2)).updateById(employeeCaptor.capture());
        HrEmployee transferred = employeeCaptor.getAllValues().stream()
                .filter(item -> SUBORDINATE_ID.equals(item.getId()))
                .findFirst()
                .orElseThrow();
        assertThat(transferred.getLeaderId()).isEqualTo(TARGET_ID);

        ArgumentCaptor<HrEmployeeChange> recordCaptor =
                ArgumentCaptor.forClass(HrEmployeeChange.class);
        verify(changeMapper, org.mockito.Mockito.atLeastOnce()).insert(recordCaptor.capture());
        HrEmployeeChange record = recordCaptor.getAllValues().stream()
                .filter(item -> item.getChangeType() == ChangeType.LEADER_TRANSFER)
                .findFirst()
                .orElseThrow();
        assertThat(record.getEmployeeId()).isEqualTo(SUBORDINATE_ID);
        assertThat(record.getResignLeaderId()).isEqualTo(TARGET_ID);
        assertThat(record.getApprovalStatus()).isEqualTo(HrApprovalStatus.APPROVED);
        assertThat(change.getApprovalStatus()).isEqualTo(HrApprovalStatus.APPROVED);
    }

    @Test
    void approvedShouldFailWhenSubordinatesExistButNoTarget() {
        HrEmployeeChange change = resignChange(null);
        when(changeMapper.selectById(change.getId())).thenReturn(change);
        when(employeeMapper.selectById(EMPLOYEE_ID)).thenReturn(employee(EMPLOYEE_ID, EmployeeStatus.ACTIVE));
        when(employeeMapper.selectList(any())).thenReturn(List.of(employee(SUBORDINATE_ID, EmployeeStatus.ACTIVE)));

        assertThatThrownBy(
                        () ->
                                resignCallback.onInstanceFinished(
                                        instance(change.getId()), BpmInstanceStatus.APPROVED))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("未指定转交上级");
    }

    @Test
    void approvedAgainShouldNotTransferTwice() {
        HrEmployeeChange change = resignChange(TARGET_ID);
        when(changeMapper.selectById(change.getId())).thenReturn(change);
        when(employeeMapper.selectById(EMPLOYEE_ID))
                .thenReturn(employee(EMPLOYEE_ID, EmployeeStatus.TERMINATED));

        resignCallback.onInstanceFinished(instance(change.getId()), BpmInstanceStatus.APPROVED);

        verify(employeeMapper, never()).selectList(any());
        verify(changeMapper, never()).insert(any(HrEmployeeChange.class));
    }

    @Test
    void rejectedShouldNotTransfer() {
        HrEmployeeChange change = resignChange(TARGET_ID);
        when(changeMapper.selectById(change.getId())).thenReturn(change);

        resignCallback.onInstanceFinished(instance(change.getId()), BpmInstanceStatus.REJECTED);

        verify(employeeMapper, never()).selectList(any());
        assertThat(change.getApprovalStatus()).isEqualTo(HrApprovalStatus.REJECTED);
    }

    // ------------------------------------------------------------------
    // 构造辅助
    // ------------------------------------------------------------------

    private HrEmployee employee(Long id, EmployeeStatus status) {
        HrEmployee employee = new HrEmployee();
        employee.setId(id);
        employee.setName("员工" + id);
        employee.setEmployeeNo("EMP-" + id);
        employee.setStatus(status);
        employee.setUserId(id + 1000);
        return employee;
    }

    private EmployeeChangeRequest resignRequest(Long resignLeaderId) {
        EmployeeChangeRequest request = new EmployeeChangeRequest();
        request.setEffectiveDate(LocalDate.of(2026, 12, 1));
        request.setReason("个人原因");
        request.setResignLeaderId(resignLeaderId);
        return request;
    }

    private HrEmployeeChange resignChange(Long resignLeaderId) {
        HrEmployeeChange change = new HrEmployeeChange();
        change.setId(8001L);
        change.setEmployeeId(EMPLOYEE_ID);
        change.setChangeType(ChangeType.RESIGN);
        change.setEffectiveDate(LocalDate.of(2026, 12, 1));
        change.setResignLeaderId(resignLeaderId);
        change.setApprovalStatus(HrApprovalStatus.PENDING);
        return change;
    }

    private BpmInstance instance(Long bizId) {
        BpmInstance instance = new BpmInstance();
        instance.setId(9001L);
        instance.setBizId(bizId);
        return instance;
    }
}
