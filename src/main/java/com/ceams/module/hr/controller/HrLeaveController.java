package com.ceams.module.hr.controller;

import com.ceams.common.response.ApiResponse;
import com.ceams.module.hr.dto.LeaveQuotaAdjustRequest;
import com.ceams.module.hr.dto.LeaveQuotaInitRequest;
import com.ceams.module.hr.dto.LeaveRequestCreateRequest;
import com.ceams.module.hr.service.HrLeaveQuotaService;
import com.ceams.module.hr.service.HrLeaveRequestService;
import com.ceams.module.hr.vo.LeaveQuotaVO;
import com.ceams.module.hr.vo.LeaveRequestVO;
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

/** HR 假期额度 / 请假申请接口。 */
@RestController
@RequestMapping("/api/v1/hr")
@RequiredArgsConstructor
public class HrLeaveController {

    private final HrLeaveQuotaService leaveQuotaService;
    private final HrLeaveRequestService leaveRequestService;

    // ------------------------------------------------------------------
    // 假期额度
    // ------------------------------------------------------------------

    @GetMapping("/leave-quotas")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<LeaveQuotaVO>> listQuotas(
            @RequestParam(required = false) Long employeeId,
            @RequestParam(required = false) Integer year) {
        return ApiResponse.success(leaveQuotaService.list(employeeId, year));
    }

    @PutMapping("/leave-quotas/{id}")
    @PreAuthorize("hasAnyRole('SYS_ADMIN')")
    public ApiResponse<Void> adjustQuota(
            @PathVariable Long id, @Valid @RequestBody LeaveQuotaAdjustRequest request) {
        leaveQuotaService.adjust(id, request.getTotalDays());
        return ApiResponse.success();
    }

    @PostMapping("/leave-quotas/init")
    @PreAuthorize("hasAnyRole('SYS_ADMIN')")
    public ApiResponse<Void> initQuotas(@Valid @RequestBody LeaveQuotaInitRequest request) {
        leaveQuotaService.init(request.getEmployeeId(), request.getYear());
        return ApiResponse.success();
    }

    // ------------------------------------------------------------------
    // 请假申请
    // ------------------------------------------------------------------

    @PostMapping("/leave-requests")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<String> createLeaveRequest(
            @Valid @RequestBody LeaveRequestCreateRequest request) {
        return ApiResponse.success(leaveRequestService.create(request));
    }

    @GetMapping("/leave-requests/my")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<LeaveRequestVO>> myLeaveRequests() {
        return ApiResponse.success(leaveRequestService.my());
    }
}
