package com.ceams.module.admin.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.ceams.common.exception.BusinessException;
import com.ceams.common.exception.ErrorCode;
import com.ceams.module.admin.config.AdminProperties;
import com.ceams.module.admin.dto.VehicleApplyRequest;
import com.ceams.module.admin.dto.VehicleExpenseRequest;
import com.ceams.module.admin.dto.VehicleReturnRequest;
import com.ceams.module.admin.entity.AdminVehicle;
import com.ceams.module.admin.entity.AdminVehicleApply;
import com.ceams.module.admin.entity.AdminVehicleExpense;
import com.ceams.module.admin.enums.VehicleApplyStatus;
import com.ceams.module.admin.enums.VehicleExpenseType;
import com.ceams.module.admin.enums.VehicleStatus;
import com.ceams.module.admin.mapper.AdminVehicleApplyMapper;
import com.ceams.module.admin.mapper.AdminVehicleExpenseMapper;
import com.ceams.module.admin.mapper.AdminVehicleMapper;
import com.ceams.module.admin.support.AdminConflictGuard;
import com.ceams.module.admin.vo.VehicleVO;
import com.ceams.module.bpm.dto.InstanceStartRequest;
import com.ceams.module.bpm.service.BpmInstanceService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** 车辆服务单元测试：申请时段、还车、费用、到期预警。 */
@ExtendWith(MockitoExtension.class)
class AdminVehicleServiceImplTest {

    private static final Long VEHICLE_ID = 4001L;
    private static final Long APPLY_ID = 4101L;
    private static final Long EXPENSE_ID = 4201L;

    @Mock private AdminVehicleMapper vehicleMapper;
    @Mock private AdminVehicleApplyMapper applyMapper;
    @Mock private AdminVehicleExpenseMapper expenseMapper;
    @Mock private AdminConflictGuard conflictGuard;
    @Mock private BpmInstanceService bpmInstanceService;

    private AdminProperties adminProperties;
    private AdminVehicleServiceImpl vehicleService;

    @BeforeEach
    void setUp() {
        adminProperties = new AdminProperties();
        adminProperties.setAlertDays(30);
        vehicleService =
                new AdminVehicleServiceImpl(
                        vehicleMapper,
                        applyMapper,
                        expenseMapper,
                        conflictGuard,
                        bpmInstanceService,
                        adminProperties);
    }

    @Test
    void applyShouldRejectWhenTimeRangeInvalid() {
        when(vehicleMapper.selectById(VEHICLE_ID)).thenReturn(vehicle(VehicleStatus.IDLE));
        LocalDateTime start = LocalDateTime.of(2026, 11, 20, 9, 0);
        VehicleApplyRequest request = applyRequest(start, start);

        assertThatThrownBy(() -> vehicleService.apply(request))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.VALID_ILLEGAL_ARGUMENT);
        verify(applyMapper, never()).insert(any(AdminVehicleApply.class));
    }

    @Test
    void applyShouldRejectWhenVehicleAlreadyOccupied() {
        when(vehicleMapper.selectById(VEHICLE_ID)).thenReturn(vehicle(VehicleStatus.IDLE));
        when(conflictGuard.hasVehicleConflict(eq(VEHICLE_ID), any(), any(), isNull()))
                .thenReturn(true);

        assertThatThrownBy(
                        () ->
                                vehicleService.apply(
                                        applyRequest(
                                                LocalDateTime.of(2026, 11, 20, 9, 0),
                                                LocalDateTime.of(2026, 11, 20, 12, 0))))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.BIZ_STATE_CONFLICT);
        verify(applyMapper, never()).insert(any(AdminVehicleApply.class));
        verify(bpmInstanceService, never()).start(any(InstanceStartRequest.class));
    }

    @Test
    void applyShouldCreateApprovingRecordAndStartBpm() {
        when(vehicleMapper.selectById(VEHICLE_ID)).thenReturn(vehicle(VehicleStatus.IDLE));
        when(conflictGuard.hasVehicleConflict(eq(VEHICLE_ID), any(), any(), isNull()))
                .thenReturn(false);
        when(applyMapper.insert(any(AdminVehicleApply.class)))
                .thenAnswer(
                        invocation -> {
                            AdminVehicleApply record = invocation.getArgument(0);
                            record.setId(APPLY_ID);
                            return 1;
                        });
        when(bpmInstanceService.start(any(InstanceStartRequest.class))).thenReturn("9100");

        String id =
                vehicleService.apply(
                        applyRequest(
                                LocalDateTime.of(2026, 11, 20, 9, 0),
                                LocalDateTime.of(2026, 11, 20, 12, 0)));

        assertThat(id).isEqualTo(String.valueOf(APPLY_ID));
        ArgumentCaptor<AdminVehicleApply> insertCaptor =
                ArgumentCaptor.forClass(AdminVehicleApply.class);
        verify(applyMapper).insert(insertCaptor.capture());
        assertThat(insertCaptor.getValue().getStatus()).isEqualTo(VehicleApplyStatus.APPROVING);
        assertThat(insertCaptor.getValue().getApplicantId()).isEqualTo(0L);

        ArgumentCaptor<InstanceStartRequest> startCaptor =
                ArgumentCaptor.forClass(InstanceStartRequest.class);
        verify(bpmInstanceService).start(startCaptor.capture());
        assertThat(startCaptor.getValue().getTemplateCode()).isEqualTo("ADMIN_VEHICLE_APPLY");
        assertThat(startCaptor.getValue().getBizId()).isEqualTo(APPLY_ID);

        ArgumentCaptor<AdminVehicleApply> updateCaptor =
                ArgumentCaptor.forClass(AdminVehicleApply.class);
        verify(applyMapper).updateById(updateCaptor.capture());
        assertThat(updateCaptor.getValue().getBpmInstanceId()).isEqualTo(9100L);
    }

    @Test
    void returnShouldRejectWhenStatusNotReturnable() {
        AdminVehicleApply apply = apply(VEHICLE_ID, VehicleApplyStatus.REJECTED, 0L);
        when(applyMapper.selectById(APPLY_ID)).thenReturn(apply);

        assertThatThrownBy(() -> vehicleService.returnVehicle(APPLY_ID, returnRequest("100")))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.BIZ_STATE_CONFLICT);
        verify(applyMapper, never()).update(any(AdminVehicleApply.class), any(Wrapper.class));
        verify(vehicleMapper, never()).update(any(AdminVehicle.class), any(Wrapper.class));
    }

    @Test
    void returnShouldRejectNegativeMileage() {
        AdminVehicleApply apply = apply(VEHICLE_ID, VehicleApplyStatus.APPROVED, 0L);
        when(applyMapper.selectById(APPLY_ID)).thenReturn(apply);

        assertThatThrownBy(() -> vehicleService.returnVehicle(APPLY_ID, returnRequest("-1")))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.VALID_ILLEGAL_ARGUMENT);
        verify(applyMapper, never()).update(any(AdminVehicleApply.class), any(Wrapper.class));
    }

    @Test
    void returnShouldMarkReturnedAndVehicleIdle() {
        AdminVehicleApply apply = apply(VEHICLE_ID, VehicleApplyStatus.APPROVED, 0L);
        when(applyMapper.selectById(APPLY_ID)).thenReturn(apply);
        when(applyMapper.update(any(AdminVehicleApply.class), any(Wrapper.class))).thenReturn(1);
        when(vehicleMapper.update(any(AdminVehicle.class), any(Wrapper.class))).thenReturn(1);

        vehicleService.returnVehicle(APPLY_ID, returnRequest("120.5"));

        ArgumentCaptor<AdminVehicleApply> applyCaptor =
                ArgumentCaptor.forClass(AdminVehicleApply.class);
        verify(applyMapper).update(applyCaptor.capture(), any(Wrapper.class));
        assertThat(applyCaptor.getValue().getStatus()).isEqualTo(VehicleApplyStatus.RETURNED);
        assertThat(applyCaptor.getValue().getReturnTime()).isNotNull();
        assertThat(applyCaptor.getValue().getMileage()).isEqualByComparingTo("120.5");
        assertThat(applyCaptor.getValue().getVersion()).isNull();

        ArgumentCaptor<AdminVehicle> vehicleCaptor =
                ArgumentCaptor.forClass(AdminVehicle.class);
        verify(vehicleMapper).update(vehicleCaptor.capture(), any(Wrapper.class));
        assertThat(vehicleCaptor.getValue().getStatus()).isEqualTo(VehicleStatus.IDLE);
    }

    @Test
    void returnShouldRejectWhenNotOwnerAndNotAdmin() {
        AdminVehicleApply apply = apply(VEHICLE_ID, VehicleApplyStatus.APPROVED, 99L);
        when(applyMapper.selectById(APPLY_ID)).thenReturn(apply);

        assertThatThrownBy(() -> vehicleService.returnVehicle(APPLY_ID, returnRequest("100")))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.AUTH_ACCESS_DENIED);
        verify(applyMapper, never()).update(any(AdminVehicleApply.class), any(Wrapper.class));
    }

    @Test
    void addExpenseShouldRejectNonPositiveAmount() {
        when(vehicleMapper.selectById(VEHICLE_ID)).thenReturn(vehicle(VehicleStatus.IDLE));
        VehicleExpenseRequest request = expenseRequest(new BigDecimal("0.00"));

        assertThatThrownBy(() -> vehicleService.addExpense(VEHICLE_ID, request))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.VALID_ILLEGAL_ARGUMENT);
        verify(expenseMapper, never()).insert(any(AdminVehicleExpense.class));
    }

    @Test
    void addExpenseShouldInsertRecord() {
        when(vehicleMapper.selectById(VEHICLE_ID)).thenReturn(vehicle(VehicleStatus.IDLE));
        when(expenseMapper.insert(any(AdminVehicleExpense.class)))
                .thenAnswer(
                        invocation -> {
                            AdminVehicleExpense expense = invocation.getArgument(0);
                            expense.setId(EXPENSE_ID);
                            return 1;
                        });

        String id = vehicleService.addExpense(VEHICLE_ID, expenseRequest(new BigDecimal("350.50")));

        assertThat(id).isEqualTo(String.valueOf(EXPENSE_ID));
        ArgumentCaptor<AdminVehicleExpense> captor =
                ArgumentCaptor.forClass(AdminVehicleExpense.class);
        verify(expenseMapper).insert(captor.capture());
        assertThat(captor.getValue().getExpenseType()).isEqualTo(VehicleExpenseType.FUEL);
        assertThat(captor.getValue().getAmount()).isEqualByComparingTo("350.50");
        assertThat(captor.getValue().getVehicleId()).isEqualTo(VEHICLE_ID);
    }

    @Test
    void alertsShouldIncludeDay30BoundaryAndExpiredVehicles() {
        LocalDate today = LocalDate.now();
        AdminVehicle boundary = vehicle(VehicleStatus.IDLE);
        boundary.setPlateNo("沪A00001");
        boundary.setInsuranceExpire(today.plusDays(30));
        boundary.setInspectionExpire(today.plusDays(31));
        AdminVehicle expired = vehicle(VehicleStatus.IDLE);
        expired.setId(VEHICLE_ID + 1);
        expired.setPlateNo("沪A00002");
        expired.setInsuranceExpire(today.minusDays(1));
        when(vehicleMapper.selectList(any(Wrapper.class))).thenReturn(List.of(boundary, expired));

        List<VehicleVO> alerts = vehicleService.alerts();

        assertThat(alerts).hasSize(2);
        VehicleVO boundaryVO =
                alerts.stream()
                        .filter(vo -> "沪A00001".equals(vo.getPlateNo()))
                        .findFirst()
                        .orElseThrow();
        assertThat(boundaryVO.getInsuranceRemainDays()).isEqualTo(30L);
        assertThat(boundaryVO.getInspectionRemainDays()).isEqualTo(31L);
        VehicleVO expiredVO =
                alerts.stream()
                        .filter(vo -> "沪A00002".equals(vo.getPlateNo()))
                        .findFirst()
                        .orElseThrow();
        assertThat(expiredVO.getInsuranceRemainDays()).isEqualTo(-1L);
    }

    private AdminVehicle vehicle(VehicleStatus status) {
        AdminVehicle vehicle = new AdminVehicle();
        vehicle.setId(VEHICLE_ID);
        vehicle.setPlateNo("沪A12345");
        vehicle.setBrandModel("别克 GL8");
        vehicle.setStatus(status);
        return vehicle;
    }

    private AdminVehicleApply apply(Long vehicleId, VehicleApplyStatus status, Long applicantId) {
        AdminVehicleApply apply = new AdminVehicleApply();
        apply.setId(APPLY_ID);
        apply.setVehicleId(vehicleId);
        apply.setApplicantId(applicantId);
        apply.setStatus(status);
        apply.setStartTime(LocalDateTime.of(2026, 11, 20, 9, 0));
        apply.setEndTime(LocalDateTime.of(2026, 11, 20, 12, 0));
        return apply;
    }

    private VehicleApplyRequest applyRequest(LocalDateTime start, LocalDateTime end) {
        VehicleApplyRequest request = new VehicleApplyRequest();
        request.setVehicleId(VEHICLE_ID);
        request.setPurpose("客户拜访");
        request.setDestination("浦东");
        request.setStartTime(start);
        request.setEndTime(end);
        return request;
    }

    private VehicleReturnRequest returnRequest(String mileage) {
        VehicleReturnRequest request = new VehicleReturnRequest();
        request.setMileage(new BigDecimal(mileage));
        return request;
    }

    private VehicleExpenseRequest expenseRequest(BigDecimal amount) {
        VehicleExpenseRequest request = new VehicleExpenseRequest();
        request.setExpenseType(VehicleExpenseType.FUEL);
        request.setAmount(amount);
        request.setOccurDate(LocalDate.of(2026, 11, 13));
        return request;
    }
}
