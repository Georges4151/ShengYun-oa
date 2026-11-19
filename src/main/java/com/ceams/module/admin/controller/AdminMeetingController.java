package com.ceams.module.admin.controller;

import com.ceams.common.response.ApiResponse;
import com.ceams.module.admin.dto.MeetingApplyRequest;
import com.ceams.module.admin.dto.MeetingRoomCreateRequest;
import com.ceams.module.admin.dto.MeetingRoomUpdateRequest;
import com.ceams.module.admin.service.AdminMeetingService;
import com.ceams.module.admin.vo.MeetingApplyVO;
import com.ceams.module.admin.vo.MeetingAvailabilityVO;
import com.ceams.module.admin.vo.MeetingRoomVO;
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

/** 行政会议室接口（统一前缀 /api/v1/admin/meeting-rooms）。 */
@RestController
@RequestMapping("/api/v1/admin/meeting-rooms")
@RequiredArgsConstructor
public class AdminMeetingController {

    private final AdminMeetingService meetingService;

    /** 会议室列表。 */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<MeetingRoomVO>> listRooms() {
        return ApiResponse.success(meetingService.listRooms());
    }

    /** 会议室建档。 */
    @PostMapping
    @PreAuthorize("hasAnyRole('SYS_ADMIN')")
    public ApiResponse<String> createRoom(@Valid @RequestBody MeetingRoomCreateRequest request) {
        return ApiResponse.success(meetingService.createRoom(request));
    }

    /** 会议室编辑。 */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SYS_ADMIN')")
    public ApiResponse<Void> updateRoom(
            @PathVariable Long id, @Valid @RequestBody MeetingRoomUpdateRequest request) {
        meetingService.updateRoom(id, request);
        return ApiResponse.success();
    }

    /** 发起会议室预定（BPM）。 */
    @PostMapping("/apply")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<String> apply(@Valid @RequestBody MeetingApplyRequest request) {
        return ApiResponse.success(meetingService.apply(request));
    }

    /** 本人会议室预定记录。 */
    @GetMapping("/applies/my")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<MeetingApplyVO>> myApplies() {
        return ApiResponse.success(meetingService.myApplies());
    }

    /** 指定会议室指定日期的已占用区间。 */
    @GetMapping("/availability")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<MeetingAvailabilityVO> availability(
            @RequestParam Long roomId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ApiResponse.success(meetingService.availability(roomId, date));
    }
}
