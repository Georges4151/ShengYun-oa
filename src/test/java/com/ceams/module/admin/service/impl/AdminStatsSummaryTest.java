package com.ceams.module.admin.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.ceams.common.exception.BusinessException;
import com.ceams.common.exception.ErrorCode;
import com.ceams.module.admin.config.AdminProperties;
import com.ceams.module.admin.entity.AdminAsset;
import com.ceams.module.admin.entity.AdminAssetBorrow;
import com.ceams.module.admin.entity.AdminRepair;
import com.ceams.module.admin.entity.AdminVehicle;
import com.ceams.module.admin.entity.AdminVehicleApply;
import com.ceams.module.admin.entity.AdminVehicleExpense;
import com.ceams.module.admin.enums.AssetStatus;
import com.ceams.module.admin.enums.RepairCategory;
import com.ceams.module.admin.enums.RepairStatus;
import com.ceams.module.admin.enums.VehicleApplyStatus;
import com.ceams.module.admin.mapper.AdminAssetBorrowMapper;
import com.ceams.module.admin.mapper.AdminAssetMapper;
import com.ceams.module.admin.mapper.AdminRepairMapper;
import com.ceams.module.admin.mapper.AdminVehicleApplyMapper;
import com.ceams.module.admin.mapper.AdminVehicleExpenseMapper;
import com.ceams.module.admin.mapper.AdminVehicleMapper;
import com.ceams.module.admin.support.AdminConflictGuard;
import com.ceams.module.admin.support.AdminNoGenerator;
import com.ceams.module.admin.vo.AssetInventorySummaryVO;
import com.ceams.module.admin.vo.RepairStatsVO;
import com.ceams.module.admin.vo.VehicleUsageItemVO;
import com.ceams.module.admin.vo.VehicleUsageSummaryVO;
import com.ceams.module.bpm.service.BpmAuditService;
import com.ceams.module.bpm.service.BpmInstanceService;
import com.ceams.module.iam.mapper.UserMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** 行政只读聚合单元测试：资产状态 / 价值 / 逾期、用车里程费用分组、报修完成率与平均时长。 */
@ExtendWith(MockitoExtension.class)
class AdminStatsSummaryTest {

    @Mock private AdminAssetMapper assetMapper;
    @Mock private AdminAssetBorrowMapper borrowMapper;
    @Mock private AdminNoGenerator noGenerator;
    @Mock private BpmInstanceService bpmInstanceService;
    @Mock private BpmAuditService bpmAuditService;
    @Mock private UserMapper userMapper;
    @Mock private AdminVehicleMapper vehicleMapper;
    @Mock private AdminVehicleApplyMapper applyMapper;
    @Mock private AdminVehicleExpenseMapper expenseMapper;
    @Mock private AdminConflictGuard conflictGuard;
    @Mock private AdminRepairMapper repairMapper;

    private AdminAssetServiceImpl assetService;
    private AdminVehicleServiceImpl vehicleService;
    private AdminRepairServiceImpl repairService;

    @BeforeEach
    void setUp() {
        assetService =
                new AdminAssetServiceImpl(
                        assetMapper,
                        borrowMapper,
                        noGenerator,
                        bpmInstanceService,
                        bpmAuditService,
                        userMapper);
        vehicleService =
                new AdminVehicleServiceImpl(
                        vehicleMapper,
                        applyMapper,
                        expenseMapper,
                        conflictGuard,
                        bpmInstanceService,
                        new AdminProperties());
        repairService =
                new AdminRepairServiceImpl(repairMapper, noGenerator, bpmInstanceService, userMapper);
    }

    // ------------------------------------------------------------------
    // 资产
    // ------------------------------------------------------------------

    @Test
    void assetInventoryShouldCountStatusValueAndOverdue() {
        when(assetMapper.selectList(any()))
                .thenReturn(
                        List.of(
                                asset(1L, AssetStatus.IN_STOCK, "1000.00"),
                                asset(2L, AssetStatus.BORROWED, "2500.50"),
                                asset(3L, AssetStatus.REPAIRING, null),
                                asset(4L, AssetStatus.SCRAPPED, "500.00")));
        when(borrowMapper.selectCount(any())).thenReturn(1L);

        AssetInventorySummaryVO summary = assetService.inventorySummary(null);

        assertThat(summary.getTotalCount()).isEqualTo(4L);
        assertThat(summary.getInStockCount()).isEqualTo(1L);
        assertThat(summary.getBorrowedCount()).isEqualTo(1L);
        assertThat(summary.getRepairingCount()).isEqualTo(1L);
        assertThat(summary.getScrappedCount()).isEqualTo(1L);
        assertThat(summary.getTotalValue()).isEqualByComparingTo("4000.50");
        assertThat(summary.getTotalValue().scale()).isEqualTo(2);
        assertThat(summary.getOverdueCount()).isEqualTo(1L);
    }

    @Test
    void assetInventoryShouldRejectIllegalCategory() {
        assertThatThrownBy(() -> assetService.inventorySummary("UNKNOWN"))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.VALID_ILLEGAL_ARGUMENT);
    }

    @Test
    void listOverdueBorrowsShouldReturnRecords() {
        AdminAssetBorrow borrow = new AdminAssetBorrow();
        borrow.setAssetId(9L);
        borrow.setExpectedReturnDate(LocalDate.of(2026, 11, 1));
        when(borrowMapper.selectList(any())).thenReturn(List.of(borrow));

        List<AdminAssetBorrow> result = assetService.listOverdueBorrows(null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getAssetId()).isEqualTo(9L);
    }

    // ------------------------------------------------------------------
    // 车辆
    // ------------------------------------------------------------------

    @Test
    void vehicleUsageShouldAggregateMileageAndExpenseByVehicle() {
        when(applyMapper.selectList(any()))
                .thenReturn(
                        List.of(
                                apply(1L, VehicleApplyStatus.APPROVED, "100.5"),
                                apply(1L, VehicleApplyStatus.RETURNED, "50.00"),
                                apply(2L, VehicleApplyStatus.APPROVED, null),
                                apply(2L, VehicleApplyStatus.APPROVING, null)));
        when(expenseMapper.selectList(any()))
                .thenReturn(List.of(expense(1L, "300.00"), expense(2L, "200.25")));
        when(vehicleMapper.selectBatchIds(any()))
                .thenReturn(List.of(vehicle(1L, "京A11111"), vehicle(2L, "京B22222")));

        VehicleUsageSummaryVO summary =
                vehicleService.usageSummary(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30), null);

        assertThat(summary.getApplyCount()).isEqualTo(4L);
        assertThat(summary.getApprovedCount()).isEqualTo(3L);
        assertThat(summary.getTotalMileage()).isEqualByComparingTo("150.50");
        assertThat(summary.getTotalExpense()).isEqualByComparingTo("500.25");
        assertThat(summary.getItems()).hasSize(2);

        VehicleUsageItemVO first = summary.getItems().get(0);
        assertThat(first.getVehicleId()).isEqualTo(1L);
        assertThat(first.getPlateNo()).isEqualTo("京A11111");
        assertThat(first.getApplyCount()).isEqualTo(2L);
        assertThat(first.getTotalMileage()).isEqualByComparingTo("150.50");
        assertThat(first.getTotalExpense()).isEqualByComparingTo("300.00");

        VehicleUsageItemVO second = summary.getItems().get(1);
        assertThat(second.getVehicleId()).isEqualTo(2L);
        assertThat(second.getApplyCount()).isEqualTo(2L);
        assertThat(second.getTotalMileage()).isEqualByComparingTo("0.00");
        assertThat(second.getTotalExpense()).isEqualByComparingTo("200.25");
    }

    // ------------------------------------------------------------------
    // 报修
    // ------------------------------------------------------------------

    @Test
    void repairStatsShouldComputeCompletionRateAndAvgHandleHours() {
        when(repairMapper.selectList(any()))
                .thenReturn(
                        List.of(
                                repair(RepairStatus.SUBMITTED, null, null),
                                repair(
                                        RepairStatus.ASSIGNED,
                                        LocalDateTime.of(2026, 11, 10, 9, 0),
                                        LocalDateTime.of(2026, 11, 10, 11, 30)),
                                repair(
                                        RepairStatus.ACCEPTED,
                                        LocalDateTime.of(2026, 11, 11, 10, 0),
                                        LocalDateTime.of(2026, 11, 11, 13, 0)),
                                repair(RepairStatus.PROCESSING, null, null)));

        RepairStatsVO stats =
                repairService.stats(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30), null, null);

        assertThat(stats.getTotalCount()).isEqualTo(4L);
        assertThat(stats.getSubmittedCount()).isEqualTo(1L);
        assertThat(stats.getProcessingCount()).isEqualTo(2L);
        assertThat(stats.getCompletedCount()).isEqualTo(1L);
        assertThat(stats.getCompletionRate()).isEqualByComparingTo("0.2500");
        assertThat(stats.getCompletionRate().scale()).isEqualTo(4);
        // (2.5 + 3.0) / 2 = 2.75
        assertThat(stats.getAvgHandleHours()).isEqualByComparingTo("2.75");
    }

    @Test
    void repairStatsShouldReturnZeroWhenNoRecord() {
        when(repairMapper.selectList(any())).thenReturn(List.of());

        RepairStatsVO stats = repairService.stats(null, null, null, null);

        assertThat(stats.getTotalCount()).isZero();
        assertThat(stats.getCompletionRate()).isEqualByComparingTo("0.0000");
        assertThat(stats.getAvgHandleHours()).isEqualByComparingTo("0.00");
    }

    @Test
    void repairStatsShouldRejectIllegalCategory() {
        assertThatThrownBy(() -> repairService.stats(null, null, "UNKNOWN", null))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.VALID_ILLEGAL_ARGUMENT);
    }

    @Test
    void repairStatsShouldAcceptValidCategoryAndReporterFilter() {
        when(repairMapper.selectList(any()))
                .thenReturn(List.of(repair(RepairStatus.SUBMITTED, null, null)));

        RepairStatsVO stats =
                repairService.stats(
                        LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30), "IT", 88L);

        assertThat(stats.getTotalCount()).isEqualTo(1L);
        assertThat(stats.getSubmittedCount()).isEqualTo(1L);
    }

    // ------------------------------------------------------------------
    // 构造辅助
    // ------------------------------------------------------------------

    private AdminAsset asset(Long id, AssetStatus status, String purchasePrice) {
        AdminAsset asset = new AdminAsset();
        asset.setId(id);
        asset.setStatus(status);
        asset.setPurchasePrice(purchasePrice == null ? null : new BigDecimal(purchasePrice));
        return asset;
    }

    private AdminVehicleApply apply(Long vehicleId, VehicleApplyStatus status, String mileage) {
        AdminVehicleApply apply = new AdminVehicleApply();
        apply.setVehicleId(vehicleId);
        apply.setStatus(status);
        apply.setStartTime(LocalDateTime.of(2026, 11, 15, 9, 0));
        apply.setMileage(mileage == null ? null : new BigDecimal(mileage));
        return apply;
    }

    private AdminVehicleExpense expense(Long vehicleId, String amount) {
        AdminVehicleExpense expense = new AdminVehicleExpense();
        expense.setVehicleId(vehicleId);
        expense.setAmount(new BigDecimal(amount));
        expense.setOccurDate(LocalDate.of(2026, 11, 15));
        return expense;
    }

    private AdminVehicle vehicle(Long id, String plateNo) {
        AdminVehicle vehicle = new AdminVehicle();
        vehicle.setId(id);
        vehicle.setPlateNo(plateNo);
        return vehicle;
    }

    private AdminRepair repair(
            RepairStatus status, LocalDateTime assignedAt, LocalDateTime finishedAt) {
        AdminRepair repair = new AdminRepair();
        repair.setStatus(status);
        repair.setAssignedAt(assignedAt);
        repair.setFinishedAt(finishedAt);
        repair.setCategory(RepairCategory.IT);
        repair.setCreateTime(LocalDateTime.of(2026, 11, 10, 8, 0));
        return repair;
    }
}
