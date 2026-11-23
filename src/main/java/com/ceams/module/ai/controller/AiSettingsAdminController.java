package com.ceams.module.ai.controller;

import com.ceams.common.response.ApiResponse;
import com.ceams.module.ai.dto.AiSettingsTestRequest;
import com.ceams.module.ai.dto.AiSettingsUpdateRequest;
import com.ceams.module.ai.service.AiSettingsService;
import com.ceams.module.ai.vo.AiSettingsTestVO;
import com.ceams.module.ai.vo.AiSettingsVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理员 AI 提供商配置接口。
 *
 * <p>仅 SYS_ADMIN 可读写（权限码 system:ai:config）。
 */
@RestController
@RequestMapping("/api/v1/admin/ai-settings")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SYS_ADMIN')")
public class AiSettingsAdminController {

    private final AiSettingsService aiSettingsService;

    /** 读取当前 AI 配置（API Key 仅返回掩码）。 */
    @GetMapping
    public ApiResponse<AiSettingsVO> getSettings() {
        return ApiResponse.success(aiSettingsService.getSettings());
    }

    /** 保存 AI 配置，保存后立即生效。 */
    @PutMapping
    public ApiResponse<AiSettingsVO> updateSettings(
            @RequestBody AiSettingsUpdateRequest request) {
        return ApiResponse.success(aiSettingsService.updateSettings(request));
    }

    /** 连接测试：失败也返回 200 + success=false，message 为中文原因。 */
    @PostMapping("/test")
    public ApiResponse<AiSettingsTestVO> testConnection(
            @RequestBody(required = false) AiSettingsTestRequest request) {
        return ApiResponse.success(aiSettingsService.testConnection(request));
    }
}
