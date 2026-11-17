package com.ceams.module.admin.controller;

import com.ceams.common.response.ApiResponse;
import com.ceams.common.response.PageResult;
import com.ceams.module.admin.dto.VehicleApplyRequest;
import com.ceams.module.admin.dto.VehicleCreateRequest;
import com.ceams.module.admin.dto.VehicleExpenseRequest;
import com.ceams.module.admin.dto.VehicleReturnRequest;
import com.ceams.module.admin.dto.VehicleUpdateRequest;
import com.ceams.module.admin.enums.VehicleStatus;
import com.ceams.module.admin.service.AdminVehicleService;
import com.ceams.module.admin.vo.VehicleApplyVO;
import com.ceams.module.admin.vo.VehicleExpenseVO;
import com.ceams.module.admin.vo.VehicleVO;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 行政车辆接口（统一前缀 /api/v1/admin/vehicles）。 */
@RestController
@RequestMapping("/api/v1/admin/vehicles")
@RequiredArgsConstructor
public class AdminVehicleController {

    private final AdminVehicleService vehicleService;

    /** 车辆分页查询，支持 keyword（车牌号 / 品牌型号）、status。 */
    @GetMapping
    @PreAuthorize("hasAnyRole('SYS_ADMIN')")
    public ApiResponse<PageResult<VehicleVO>> page(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) VehicleStatus status,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size) {
        return ApiResponse.success(vehicleService.page(keyword, status, page, size));
    }

    /** 车辆建档。 */
    @PostMapping
    @PreAuthorize("hasAnyRole('SYS_ADMIN')")
    public ApiResponse<String> create(@Valid @RequestBody VehicleCreateRequest request) {
        return ApiResponse.success(vehicleService.create(request));
    }

    /** 车辆编辑。 */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SYS_ADMIN')")
    public ApiResponse<Void> update(
            @PathVariable Long id, @Valid @RequestBody VehicleUpdateRequest request) {
        vehicleService.update(id, request);
        return ApiResponse.success();
    }

    /** 发起用车申请（BPM）。 */
    @PostMapping("/apply")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<String> apply(@Valid @RequestBody VehicleApplyRequest request) {
        return ApiResponse.success(vehicleService.apply(request));
    }

    /** 本人用车记录。 */
    @GetMapping("/applies/my")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<VehicleApplyVO>> myApplies() {
        return ApiResponse.success(vehicleService.myApplies());
    }

    /** 还车登记（仅申请人本人或管理员）。 */
    @PostMapping("/applies/{id}/return")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Void> returnVehicle(
            @PathVariable Long id, @Valid @RequestBody VehicleReturnRequest request) {
        vehicleService.returnVehicle(id, request);
        return ApiResponse.success();
    }

    /** 车辆费用登记。 */
    @PostMapping("/{id}/expenses")
    @PreAuthorize("hasAnyRole('SYS_ADMIN')")
    public ApiResponse<String> addExpense(
            @PathVariable Long id, @Valid @RequestBody VehicleExpenseRequest request) {
        return ApiResponse.success(vehicleService.addExpense(id, request));
    }

    /** 按车辆查询费用。 */
    @GetMapping("/{id}/expenses")
    @PreAuthorize("hasAnyRole('SYS_ADMIN')")
    public ApiResponse<List<VehicleExpenseVO>> expenses(@PathVariable Long id) {
        return ApiResponse.success(vehicleService.expenses(id));
    }

    /** 保险 / 年检到期预警。 */
    @GetMapping("/alerts")
    @PreAuthorize("hasAnyRole('SYS_ADMIN')")
    public ApiResponse<List<VehicleVO>> alerts() {
        return ApiResponse.success(vehicleService.alerts());
    }
}
