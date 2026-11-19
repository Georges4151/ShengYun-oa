package com.ceams.module.hr.controller;

import com.ceams.common.response.ApiResponse;
import com.ceams.common.response.PageResult;
import com.ceams.module.hr.dto.CheckinClockRequest;
import com.ceams.module.hr.dto.CheckinSupplementCreateRequest;
import com.ceams.module.hr.dto.ScheduleBatchRequest;
import com.ceams.module.hr.dto.ScheduleCreateRequest;
import com.ceams.module.hr.dto.ScheduleUpdateRequest;
import com.ceams.module.hr.enums.CheckinStatus;
import com.ceams.module.hr.service.HrCheckinService;
import com.ceams.module.hr.service.HrScheduleService;
import com.ceams.module.hr.vo.CheckinSupplementVO;
import com.ceams.module.hr.vo.CheckinVO;
import com.ceams.module.hr.vo.ScheduleVO;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** HR 排班 / 打卡 / 补卡接口。 */
@RestController
@RequestMapping("/api/v1/hr")
@RequiredArgsConstructor
public class HrAttendanceController {

    private final HrScheduleService scheduleService;
    private final HrCheckinService checkinService;

    // ------------------------------------------------------------------
    // 排班
    // ------------------------------------------------------------------

    @GetMapping("/schedules")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<ScheduleVO>> listSchedules(
            @RequestParam(required = false) Long employeeId,
            @RequestParam(required = false) String month) {
        return ApiResponse.success(scheduleService.list(employeeId, month));
    }

    @PostMapping("/schedules")
    @PreAuthorize("hasAnyRole('SYS_ADMIN')")
    public ApiResponse<String> createSchedule(@Valid @RequestBody ScheduleCreateRequest request) {
        return ApiResponse.success(scheduleService.create(request));
    }

    @PutMapping("/schedules/{id}")
    @PreAuthorize("hasAnyRole('SYS_ADMIN')")
    public ApiResponse<Void> updateSchedule(
            @PathVariable Long id, @Valid @RequestBody ScheduleUpdateRequest request) {
        scheduleService.update(id, request);
        return ApiResponse.success();
    }

    @PostMapping("/schedules/batch")
    @PreAuthorize("hasAnyRole('SYS_ADMIN')")
    public ApiResponse<Integer> batchSchedule(@Valid @RequestBody ScheduleBatchRequest request) {
        return ApiResponse.success(scheduleService.batch(request));
    }

    // ------------------------------------------------------------------
    // 打卡
    // ------------------------------------------------------------------

    @PostMapping("/checkins/clock")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<CheckinVO> clock(@Valid @RequestBody CheckinClockRequest request) {
        return ApiResponse.success(checkinService.clock(request));
    }

    @GetMapping("/checkins/my")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<CheckinVO>> myCheckins(
            @RequestParam(required = false) String month) {
        return ApiResponse.success(checkinService.my(month));
    }

    @GetMapping("/checkins")
    @PreAuthorize("hasAnyRole('SYS_ADMIN')")
    public ApiResponse<PageResult<CheckinVO>> pageCheckins(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size,
            @RequestParam(required = false) Long employeeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate endDate,
            @RequestParam(required = false) CheckinStatus status) {
        return ApiResponse.success(
                checkinService.page(page, size, employeeId, startDate, endDate, status));
    }

    // ------------------------------------------------------------------
    // 补卡
    // ------------------------------------------------------------------

    @PostMapping("/checkins/supplement")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<String> supplement(
            @Valid @RequestBody CheckinSupplementCreateRequest request) {
        return ApiResponse.success(checkinService.createSupplement(request));
    }

    @GetMapping("/checkins/supplements/my")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<CheckinSupplementVO>> mySupplements() {
        return ApiResponse.success(checkinService.mySupplements());
    }
}
