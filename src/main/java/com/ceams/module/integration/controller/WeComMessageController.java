package com.ceams.module.integration.controller;

import com.ceams.common.response.ApiResponse;
import com.ceams.common.response.PageResult;
import com.ceams.module.integration.service.WeComMessageService;
import com.ceams.module.integration.vo.WeComMessageVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 企业微信应用消息（发件箱）接口，前缀 /api/v1/integration/wecom/messages。
 *
 * <p>权限码在服务层校验：分页查询要求 {@code integration:wecom:view}，手工重投要求
 * {@code integration:wecom:sync}；Controller 仅做登录态校验。
 */
@RestController
@RequestMapping("/api/v1/integration/wecom/messages")
@RequiredArgsConstructor
public class WeComMessageController {

    private final WeComMessageService weComMessageService;

    /** 发件箱分页查询，支持按状态过滤。 */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<PageResult<WeComMessageVO>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status) {
        return ApiResponse.success(weComMessageService.page(page, size, status));
    }

    /** 手工重投失败消息（仅 FAILED 可重投）。 */
    @PostMapping("/{id}/retry")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Void> retry(@PathVariable Long id) {
        weComMessageService.retry(id);
        return ApiResponse.success();
    }
}
