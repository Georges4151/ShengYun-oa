package com.ceams.module.admin.service;

import com.ceams.common.response.PageResult;
import com.ceams.module.admin.dto.VehicleApplyRequest;
import com.ceams.module.admin.dto.VehicleCreateRequest;
import com.ceams.module.admin.dto.VehicleExpenseRequest;
import com.ceams.module.admin.dto.VehicleReturnRequest;
import com.ceams.module.admin.dto.VehicleUpdateRequest;
import com.ceams.module.admin.enums.VehicleStatus;
import com.ceams.module.admin.vo.VehicleApplyVO;
import com.ceams.module.admin.vo.VehicleExpenseVO;
import com.ceams.module.admin.vo.VehicleUsageSummaryVO;
import com.ceams.module.admin.vo.VehicleVO;
import java.time.LocalDate;
import java.util.List;

/** 行政车辆服务。 */
public interface AdminVehicleService {

    /** 分页查询车辆台账（keyword 命中车牌号或品牌型号）。 */
    PageResult<VehicleVO> page(String keyword, VehicleStatus status, long page, long size);

    /** 车辆建档，返回车辆 ID。 */
    String create(VehicleCreateRequest request);

    /** 编辑车辆基础信息（状态只由审批回调写入）。 */
    void update(Long id, VehicleUpdateRequest request);

    /** 发起用车申请（BPM），返回申请单 ID。 */
    String apply(VehicleApplyRequest request);

    /** 本人用车记录。 */
    List<VehicleApplyVO> myApplies();

    /** 还车登记（仅 APPROVED/USING 且为申请人本人或管理员）。 */
    void returnVehicle(Long applyId, VehicleReturnRequest request);

    /** 车辆费用登记，返回费用 ID。 */
    String addExpense(Long vehicleId, VehicleExpenseRequest request);

    /** 按车辆查询费用（发生日期倒序）。 */
    List<VehicleExpenseVO> expenses(Long vehicleId);

    /** 保险 / 年检到期预警（含已过期车辆）。 */
    List<VehicleVO> alerts();

    /**
     * 用车汇总（只读聚合，无事务）。
     *
     * <p>口径：申请按 {@code startTime} 的日期落在 [start, end] 过滤，approvedCount 统计
     * status in (APPROVED, USING, RETURNED)；totalMileage 累加非空 mileage；totalExpense
     * 取费用表按 occurDate 落在区间累加 amount；items 按 vehicleId 分组（车牌号批量查询，
     * 查不到回落空串）。所有金额与里程保留 2 位小数。
     *
     * @param start 开始日期，可空
     * @param end 结束日期，可空
     * @param vehicleId 车辆 ID，可空
     * @return 用车汇总
     */
    VehicleUsageSummaryVO usageSummary(LocalDate start, LocalDate end, Long vehicleId);
}
