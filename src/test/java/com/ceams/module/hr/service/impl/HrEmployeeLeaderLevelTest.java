package com.ceams.module.hr.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.ceams.common.crypto.SensitiveTypeHandler;
import com.ceams.common.exception.BusinessException;
import com.ceams.common.exception.ErrorCode;
import com.ceams.module.bpm.dto.InstanceStartRequest;
import com.ceams.module.bpm.service.BpmInstanceService;
import com.ceams.module.hr.converter.HrEmployeeChangeConverter;
import com.ceams.module.hr.converter.HrEmployeeConverter;
import com.ceams.module.hr.dto.EmployeeChangeRequest;
import com.ceams.module.hr.dto.EmployeeCreateRequest;
import com.ceams.module.hr.dto.EmployeeUpdateRequest;
import com.ceams.module.hr.entity.HrEmployee;
import com.ceams.module.hr.entity.HrEmployeeChange;
import com.ceams.module.hr.entity.HrJobLevel;
import com.ceams.module.hr.enums.ChangeType;
import com.ceams.module.hr.enums.EmployeeStatus;
import com.ceams.module.hr.enums.HrApprovalStatus;
import com.ceams.module.hr.mapper.HrEmployeeChangeMapper;
import com.ceams.module.hr.mapper.HrEmployeeMapper;
import com.ceams.module.hr.mapper.HrJobLevelMapper;
import com.ceams.module.hr.mapper.HrPositionMapper;
import com.ceams.module.iam.mapper.DeptMapper;
import com.ceams.module.iam.mapper.UserMapper;
import java.time.LocalDate;
import java.util.Map;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** 员工直属上级与职级单元测试：上级校验、环路阻断、清空上级、晋升即时生效流程。 */
@ExtendWith(MockitoExtension.class)
class HrEmployeeLeaderLevelTest {

    private static final Long EMPLOYEE_ID = 200L;

    @Mock private HrEmployeeMapper employeeMapper;
    @Mock private HrEmployeeChangeMapper changeMapper;
    @Mock private HrJobLevelMapper jobLevelMapper;
    @Mock private HrPositionMapper positionMapper;
    @Mock private BpmInstanceService bpmInstanceService;
    @Mock private DeptMapper deptMapper;
    @Mock private UserMapper userMapper;

    private HrEmployeeServiceImpl employeeService;

    /** MyBatis-Plus Lambda 列缓存依赖 TableInfo，清空上级的 LambdaUpdateWrapper 需要实体元数据。 */
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
    }

    // ------------------------------------------------------------------
    // create：直属上级与初始职级
    // ------------------------------------------------------------------

    @Test
    void createShouldRejectLeaderNotExists() {
        EmployeeCreateRequest request = createRequest();
        request.setLeaderId(50L);
        when(employeeMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        when(employeeMapper.selectById(50L)).thenReturn(null);

        assertThatThrownBy(() -> employeeService.create(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("直属上级员工不存在")
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.BIZ_NOT_FOUND);
        verify(employeeMapper, never()).insert(any(HrEmployee.class));
    }

    @Test
    void createShouldRejectTerminatedLeader() {
        EmployeeCreateRequest request = createRequest();
        request.setLeaderId(50L);
        when(employeeMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        when(employeeMapper.selectById(50L))
                .thenReturn(employee(50L, null, EmployeeStatus.TERMINATED, 21L));

        assertThatThrownBy(() -> employeeService.create(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("直属上级已离职")
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.BIZ_STATE_CONFLICT);
        verify(employeeMapper, never()).insert(any(HrEmployee.class));
    }

    @Test
    void createShouldRejectLeaderWithoutBoundUser() {
        EmployeeCreateRequest request = createRequest();
        request.setLeaderId(50L);
        when(employeeMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        when(employeeMapper.selectById(50L))
                .thenReturn(employee(50L, null, EmployeeStatus.ACTIVE, null));

        assertThatThrownBy(() -> employeeService.create(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("直属上级必须绑定系统账号")
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.BIZ_STATE_CONFLICT);
        verify(employeeMapper, never()).insert(any(HrEmployee.class));
    }

    @Test
    void createShouldRejectCyclicLeaderChain() {
        EmployeeCreateRequest request = createRequest();
        request.setLeaderId(50L);
        when(employeeMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        // 50 -> 51 -> 50 形成环
        when(employeeMapper.selectById(50L))
                .thenReturn(employee(50L, 51L, EmployeeStatus.ACTIVE, 21L));
        when(employeeMapper.selectById(51L))
                .thenReturn(employee(51L, 50L, EmployeeStatus.ACTIVE, 22L));

        assertThatThrownBy(() -> employeeService.create(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("循环引用")
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.BIZ_STATE_CONFLICT);
        verify(employeeMapper, never()).insert(any(HrEmployee.class));
    }

    @Test
    void createShouldAcceptMultiLevelLeaderChain() {
        EmployeeCreateRequest request = createRequest();
        request.setLeaderId(50L);
        when(employeeMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        // 50 -> 51 -> 无上级，正常多级链路不应误判为环
        when(employeeMapper.selectById(50L))
                .thenReturn(employee(50L, 51L, EmployeeStatus.ACTIVE, 21L));
        when(employeeMapper.selectById(51L))
                .thenReturn(employee(51L, null, EmployeeStatus.ACTIVE, 22L));

        employeeService.create(request);

        ArgumentCaptor<HrEmployee> captor = ArgumentCaptor.forClass(HrEmployee.class);
        verify(employeeMapper).insert(captor.capture());
        assertThat(captor.getValue().getLeaderId()).isEqualTo(50L);
    }

    @Test
    void createShouldRejectUnknownLevel() {
        EmployeeCreateRequest request = createRequest();
        request.setLevelId(9L);
        when(employeeMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        when(jobLevelMapper.selectById(9L)).thenReturn(null);

        assertThatThrownBy(() -> employeeService.create(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("职级不存在")
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.BIZ_NOT_FOUND);
        verify(employeeMapper, never()).insert(any(HrEmployee.class));
    }

    @Test
    void createShouldPersistLeaderAndLevel() {
        EmployeeCreateRequest request = createRequest();
        request.setLeaderId(50L);
        request.setLevelId(9L);
        when(employeeMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        when(employeeMapper.selectById(50L))
                .thenReturn(employee(50L, null, EmployeeStatus.ACTIVE, 21L));
        when(jobLevelMapper.selectById(9L)).thenReturn(jobLevel(9L, "P6"));

        employeeService.create(request);

        ArgumentCaptor<HrEmployee> captor = ArgumentCaptor.forClass(HrEmployee.class);
        verify(employeeMapper).insert(captor.capture());
        HrEmployee inserted = captor.getValue();
        assertThat(inserted.getLeaderId()).isEqualTo(50L);
        assertThat(inserted.getLevelId()).isEqualTo(9L);
        assertThat(inserted.getStatus()).isEqualTo(EmployeeStatus.PROBATION);
    }

    // ------------------------------------------------------------------
    // update：直属上级维护与 level_id 保护
    // ------------------------------------------------------------------

    @Test
    void updateShouldKeepLeaderWhenLeaderIdAbsent() {
        HrEmployee employee = existingEmployee();
        employee.setLeaderId(50L);
        when(employeeMapper.selectById(EMPLOYEE_ID)).thenReturn(employee);

        EmployeeUpdateRequest request = new EmployeeUpdateRequest();
        request.setName("新名字");

        employeeService.update(EMPLOYEE_ID, request);

        ArgumentCaptor<HrEmployee> captor = ArgumentCaptor.forClass(HrEmployee.class);
        verify(employeeMapper).updateById(captor.capture());
        assertThat(captor.getValue().getLeaderId()).isEqualTo(50L);
        assertThat(captor.getValue().getName()).isEqualTo("新名字");
        // 未传上级不触发清空语句
        verify(employeeMapper, never()).update(isNull(), any(Wrapper.class));
    }

    @Test
    void updateShouldClearLeaderWhenZeroPassed() {
        HrEmployee employee = existingEmployee();
        employee.setLeaderId(50L);
        when(employeeMapper.selectById(EMPLOYEE_ID)).thenReturn(employee);

        EmployeeUpdateRequest request = new EmployeeUpdateRequest();
        request.setLeaderId(0L);

        employeeService.update(EMPLOYEE_ID, request);

        // updateById 默认忽略 null，清空上级需显式 update wrapper
        verify(employeeMapper).update(isNull(), any(Wrapper.class));
    }

    @Test
    void updateShouldRejectSelfAsLeader() {
        HrEmployee employee = existingEmployee();
        when(employeeMapper.selectById(EMPLOYEE_ID)).thenReturn(employee);

        EmployeeUpdateRequest request = new EmployeeUpdateRequest();
        request.setLeaderId(EMPLOYEE_ID);

        assertThatThrownBy(() -> employeeService.update(EMPLOYEE_ID, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("直属上级不能是本人")
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.BIZ_STATE_CONFLICT);
        verify(employeeMapper, never()).updateById(any(HrEmployee.class));
    }

    @Test
    void updateShouldRejectUnknownLeader() {
        when(employeeMapper.selectById(EMPLOYEE_ID)).thenReturn(existingEmployee());
        when(employeeMapper.selectById(77L)).thenReturn(null);

        EmployeeUpdateRequest request = new EmployeeUpdateRequest();
        request.setLeaderId(77L);

        assertThatThrownBy(() -> employeeService.update(EMPLOYEE_ID, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("直属上级员工不存在")
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.BIZ_NOT_FOUND);
        verify(employeeMapper, never()).updateById(any(HrEmployee.class));
    }

    @Test
    void updateShouldNotChangeLevelId() {
        HrEmployee employee = existingEmployee();
        employee.setLevelId(3L);
        when(employeeMapper.selectById(EMPLOYEE_ID)).thenReturn(employee);

        EmployeeUpdateRequest request = new EmployeeUpdateRequest();
        request.setDeptId(7L);

        employeeService.update(EMPLOYEE_ID, request);

        ArgumentCaptor<HrEmployee> captor = ArgumentCaptor.forClass(HrEmployee.class);
        verify(employeeMapper).updateById(captor.capture());
        // 普通更新不接受职级变更，level_id 必须保持不变
        assertThat(captor.getValue().getLevelId()).isEqualTo(3L);
        assertThat(captor.getValue().getDeptId()).isEqualTo(7L);
    }

    // ------------------------------------------------------------------
    // promotion：申请单落库 + BPM 发起
    // ------------------------------------------------------------------

    @Test
    void promotionShouldRecordTargetLevelAndStartBpm() {
        when(employeeMapper.selectById(EMPLOYEE_ID)).thenReturn(activeEmployee());
        when(jobLevelMapper.selectById(9L)).thenReturn(jobLevel(9L, "P6"));
        when(changeMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        when(changeMapper.insert(any(HrEmployeeChange.class)))
                .thenAnswer(
                        invocation -> {
                            HrEmployeeChange change = invocation.getArgument(0);
                            change.setId(7001L);
                            return 1;
                        });
        when(bpmInstanceService.start(any(InstanceStartRequest.class))).thenReturn("8008");

        EmployeeChangeRequest request = new EmployeeChangeRequest();
        request.setEffectiveDate(LocalDate.of(2026, 12, 1));
        request.setTargetLevelId(9L);

        String changeId = employeeService.promotion(EMPLOYEE_ID, request);

        assertThat(changeId).isEqualTo("7001");
        ArgumentCaptor<HrEmployeeChange> changeCaptor =
                ArgumentCaptor.forClass(HrEmployeeChange.class);
        verify(changeMapper).insert(changeCaptor.capture());
        HrEmployeeChange inserted = changeCaptor.getValue();
        assertThat(inserted.getEmployeeId()).isEqualTo(EMPLOYEE_ID);
        assertThat(inserted.getChangeType()).isEqualTo(ChangeType.PROMOTION);
        assertThat(inserted.getTargetLevelId()).isEqualTo(9L);
        assertThat(inserted.getApprovalStatus()).isEqualTo(HrApprovalStatus.PENDING);

        ArgumentCaptor<InstanceStartRequest> startCaptor =
                ArgumentCaptor.forClass(InstanceStartRequest.class);
        verify(bpmInstanceService).start(startCaptor.capture());
        InstanceStartRequest start = startCaptor.getValue();
        assertThat(start.getTemplateCode()).isEqualTo("HR_PROMOTION");
        assertThat(start.getBizType()).isEqualTo("HR_PROMOTION");
        assertThat(start.getBizId()).isEqualTo(7001L);
        assertThat(start.getVariables())
                .containsEntry("employeeId", String.valueOf(EMPLOYEE_ID))
                .containsEntry("changeType", ChangeType.PROMOTION.name())
                .containsEntry("targetLevelId", "9");

        ArgumentCaptor<HrEmployeeChange> updateCaptor =
                ArgumentCaptor.forClass(HrEmployeeChange.class);
        verify(changeMapper).updateById(updateCaptor.capture());
        assertThat(updateCaptor.getValue().getBpmInstanceId()).isEqualTo(8008L);
        assertThat(updateCaptor.getValue().getApprovalStatus())
                .isEqualTo(HrApprovalStatus.PENDING);
        assertThat(updateCaptor.getValue().getEffectiveDate())
                .isEqualTo(LocalDate.of(2026, 12, 1));
        Map<String, String> variables = start.getVariables();
        assertThat(variables).containsEntry("effectiveDate", "2026-12-01");
    }

    @Test
    void promotionShouldRejectTerminatedEmployee() {
        HrEmployee employee = activeEmployee();
        employee.setStatus(EmployeeStatus.TERMINATED);
        when(employeeMapper.selectById(EMPLOYEE_ID)).thenReturn(employee);

        EmployeeChangeRequest request = new EmployeeChangeRequest();
        request.setEffectiveDate(LocalDate.of(2026, 12, 1));
        request.setTargetLevelId(9L);

        assertThatThrownBy(() -> employeeService.promotion(EMPLOYEE_ID, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("员工已离职")
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.BIZ_STATE_CONFLICT);
        verify(changeMapper, never()).insert(any(HrEmployeeChange.class));
        verify(bpmInstanceService, never()).start(any(InstanceStartRequest.class));
    }

    @Test
    void promotionShouldRejectMissingTargetLevel() {
        when(employeeMapper.selectById(EMPLOYEE_ID)).thenReturn(activeEmployee());

        EmployeeChangeRequest request = new EmployeeChangeRequest();
        request.setEffectiveDate(LocalDate.of(2026, 12, 1));

        assertThatThrownBy(() -> employeeService.promotion(EMPLOYEE_ID, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("晋升申请需指定目标职级")
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.VALID_ILLEGAL_ARGUMENT);
        verify(changeMapper, never()).insert(any(HrEmployeeChange.class));
    }

    @Test
    void promotionShouldRejectUnknownTargetLevel() {
        when(employeeMapper.selectById(EMPLOYEE_ID)).thenReturn(activeEmployee());
        when(jobLevelMapper.selectById(9L)).thenReturn(null);

        EmployeeChangeRequest request = new EmployeeChangeRequest();
        request.setEffectiveDate(LocalDate.of(2026, 12, 1));
        request.setTargetLevelId(9L);

        assertThatThrownBy(() -> employeeService.promotion(EMPLOYEE_ID, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("目标职级不存在")
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.BIZ_NOT_FOUND);
        verify(changeMapper, never()).insert(any(HrEmployeeChange.class));
    }

    // ------------------------------------------------------------------
    // 夹具
    // ------------------------------------------------------------------

    private EmployeeCreateRequest createRequest() {
        EmployeeCreateRequest request = new EmployeeCreateRequest();
        request.setEmployeeNo(" E0100 ");
        request.setName(" 张三 ");
        return request;
    }

    private HrEmployee existingEmployee() {
        HrEmployee employee = new HrEmployee();
        employee.setId(EMPLOYEE_ID);
        employee.setEmployeeNo("E0200");
        employee.setName("李四");
        employee.setUserId(31L);
        employee.setStatus(EmployeeStatus.ACTIVE);
        return employee;
    }

    private HrEmployee activeEmployee() {
        return existingEmployee();
    }

    private HrEmployee employee(Long id, Long leaderId, EmployeeStatus status, Long userId) {
        HrEmployee employee = new HrEmployee();
        employee.setId(id);
        employee.setEmployeeNo("E" + id);
        employee.setName("员工" + id);
        employee.setLeaderId(leaderId);
        employee.setStatus(status);
        employee.setUserId(userId);
        return employee;
    }

    private HrJobLevel jobLevel(Long id, String code) {
        HrJobLevel jobLevel = new HrJobLevel();
        jobLevel.setId(id);
        jobLevel.setCode(code);
        jobLevel.setName(code);
        jobLevel.setStatus("ENABLED");
        return jobLevel;
    }
}
