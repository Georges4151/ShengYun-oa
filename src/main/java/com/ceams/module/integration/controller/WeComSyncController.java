package com.ceams.module.integration.controller;

import com.ceams.common.response.ApiResponse;
import com.ceams.common.response.PageResult;
import com.ceams.module.integration.enums.WeComSyncType;
import com.ceams.module.integration.service.WeComSyncService;
import com.ceams.module.integration.vo.WeComSyncLogVO;
import com.ceams.module.integration.vo.WeComSyncResultVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 企业微信集成接口（统一前缀 /api/v1/integration/wecom）。
 *
 * <p>权限码在服务层校验：手动同步要求 {@code integration:wecom:sync}，日志查询要求
 * {@code integration:wecom:view}；Controller 仅做登录态校验。
 */
@RestController
@RequestMapping("/api/v1/integration/wecom")
@RequiredArgsConstructor
public class WeComSyncController {

    private final WeComSyncService weComSyncService;

    /** 手动触发组织同步（企业微信接口失败时仍返回批次号与 FAILED 状态）。 */
    @PostMapping("/sync")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<WeComSyncResultVO> sync() {
        String batchNo = weComSyncService.sync(WeComSyncType.MANUAL);
        return ApiResponse.success(weComSyncService.result(batchNo));
    }

    /** 同步日志分页查询，支持按状态过滤。 */
    @GetMapping("/sync/logs")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<PageResult<WeComSyncLogVO>> logs(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status) {
        return ApiResponse.success(weComSyncService.logs(page, size, status));
    }
}
