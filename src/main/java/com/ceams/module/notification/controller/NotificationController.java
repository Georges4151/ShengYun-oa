package com.ceams.module.notification.controller;

import com.ceams.common.response.ApiResponse;
import com.ceams.common.response.PageResult;
import com.ceams.module.notification.dto.NotificationCreateRequest;
import com.ceams.module.notification.dto.NotificationPublishRequest;
import com.ceams.module.notification.dto.NotificationRecipientRoleVO;
import com.ceams.module.notification.dto.NotificationRecipientUserVO;
import com.ceams.module.notification.dto.NotificationUpdateRequest;
import com.ceams.module.notification.enums.NotificationPriority;
import com.ceams.module.notification.enums.NotificationStatus;
import com.ceams.module.notification.enums.NotificationType;
import com.ceams.module.notification.service.NotificationService;
import com.ceams.module.notification.vo.NotificationVO;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 通知管理接口。 */
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<PageResult<NotificationVO>> page(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size,
            @RequestParam(required = false) NotificationType type,
            @RequestParam(required = false) NotificationStatus status,
            @RequestParam(required = false) NotificationPriority priority,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    LocalDateTime endTime) {
        return ApiResponse.success(
                notificationService.page(page, size, type, status, priority, startTime, endTime));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SYS_ADMIN','FINANCE')")
    public ApiResponse<String> create(@Valid @RequestBody NotificationCreateRequest request) {
        return ApiResponse.success(notificationService.create(request));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<NotificationVO> detail(@PathVariable Long id) {
        return ApiResponse.success(notificationService.detail(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SYS_ADMIN','FINANCE')")
    public ApiResponse<Void> update(
            @PathVariable Long id, @Valid @RequestBody NotificationUpdateRequest request) {
        notificationService.update(id, request);
        return ApiResponse.success();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SYS_ADMIN','FINANCE')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        notificationService.delete(id);
        return ApiResponse.success();
    }

    /**
     * 发布通知。
     *
     * <p>请求体可选：传 scope 时按指定接收范围直接生成站内信；不传时沿用旧的按用户偏好分发行为。
     *
     * @return 实际送达人数
     */
    @PostMapping("/{id}/publish")
    @PreAuthorize("hasAnyRole('SYS_ADMIN','FINANCE')")
    public ApiResponse<Integer> publish(
            @PathVariable Long id,
            @Valid @RequestBody(required = false) NotificationPublishRequest request) {
        return ApiResponse.success(notificationService.publish(id, request));
    }

    /** 发布范围-启用角色列表。 */
    @GetMapping("/recipients/roles")
    @PreAuthorize("hasAnyRole('SYS_ADMIN','FINANCE')")
    public ApiResponse<List<NotificationRecipientRoleVO>> recipientRoles() {
        return ApiResponse.success(notificationService.listRecipientRoles());
    }

    /** 发布范围-启用用户模糊搜索（最多 50 条）。 */
    @GetMapping("/recipients/users")
    @PreAuthorize("hasAnyRole('SYS_ADMIN','FINANCE')")
    public ApiResponse<List<NotificationRecipientUserVO>> recipientUsers(
            @RequestParam(required = false) String keyword) {
        return ApiResponse.success(notificationService.searchRecipientUsers(keyword));
    }

    @PostMapping("/{id}/revoke")
    @PreAuthorize("hasAnyRole('SYS_ADMIN','FINANCE')")
    public ApiResponse<Void> revoke(@PathVariable Long id) {
        notificationService.revoke(id);
        return ApiResponse.success();
    }
}
