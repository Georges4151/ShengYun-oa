package com.ceams.module.notification.controller;

import com.ceams.common.response.ApiResponse;
import com.ceams.module.notification.service.UserNotificationService;
import com.ceams.module.notification.vo.UserNotificationMineVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 用户通知（收件箱）接口。 */
@RestController
@RequestMapping("/api/v1/notifications/user-notifications")
@RequiredArgsConstructor
public class UserNotificationController {

    private final UserNotificationService userNotificationService;

    @GetMapping("/mine")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<UserNotificationMineVO> mine(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size) {
        return ApiResponse.success(userNotificationService.mine(page, size));
    }

    @GetMapping("/unread-count")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Long> unreadCount() {
        return ApiResponse.success(userNotificationService.unreadCount());
    }

    @PostMapping("/{id}/read")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Void> read(@PathVariable Long id) {
        userNotificationService.read(id);
        return ApiResponse.success();
    }

    @PostMapping("/read-all")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Void> readAll() {
        userNotificationService.readAll();
        return ApiResponse.success();
    }
}
