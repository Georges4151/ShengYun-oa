package com.ceams.module.notification.controller;

import com.ceams.common.response.ApiResponse;
import com.ceams.module.notification.dto.UserNotificationPreferenceUpsertRequest;
import com.ceams.module.notification.service.UserNotificationPreferenceService;
import com.ceams.module.notification.vo.UserNotificationPreferenceVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 用户通知偏好接口。
 */
@RestController
@RequestMapping("/api/v1/notifications/preferences")
@RequiredArgsConstructor
public class UserNotificationPreferenceController {

    private final UserNotificationPreferenceService userNotificationPreferenceService;

    @GetMapping("/mine")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<UserNotificationPreferenceVO>> getMine() {
        return ApiResponse.success(userNotificationPreferenceService.getMine());
    }

    @PutMapping
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<String> upsert(@Valid @RequestBody UserNotificationPreferenceUpsertRequest request) {
        return ApiResponse.success(userNotificationPreferenceService.upsert(request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        userNotificationPreferenceService.delete(id);
        return ApiResponse.success();
    }
}
