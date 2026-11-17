package com.ceams.module.admin.controller;

import com.ceams.common.response.ApiResponse;
import com.ceams.common.response.PageResult;
import com.ceams.module.admin.dto.VisitorCheckinRequest;
import com.ceams.module.admin.dto.VisitorCreateRequest;
import com.ceams.module.admin.enums.VisitorStatus;
import com.ceams.module.admin.service.AdminVisitorService;
import com.ceams.module.admin.vo.VisitorQrVO;
import com.ceams.module.admin.vo.VisitorVO;
import jakarta.validation.Valid;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 行政访客接口（统一前缀 /api/v1/admin/visitors）。 */
@RestController
@RequestMapping("/api/v1/admin/visitors")
@RequiredArgsConstructor
public class AdminVisitorController {

    private final AdminVisitorService visitorService;

    /** 访客登记，返回一次性二维码内容。 */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<VisitorQrVO> create(@Valid @RequestBody VisitorCreateRequest request) {
        return ApiResponse.success(visitorService.create(request));
    }

    /** 访客列表，支持 date（来访日期）、status。 */
    @GetMapping
    @PreAuthorize("hasAnyRole('SYS_ADMIN')")
    public ApiResponse<PageResult<VisitorVO>> list(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate date,
            @RequestParam(required = false) VisitorStatus status,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size) {
        return ApiResponse.success(visitorService.list(date, status, page, size));
    }

    /** 重新获取二维码内容（被访人或管理员）。 */
    @GetMapping("/{id}/qr")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<VisitorQrVO> qr(@PathVariable Long id) {
        return ApiResponse.success(visitorService.qr(id));
    }

    /** 按 token 核销入场。 */
    @PostMapping("/checkin")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Void> checkin(@Valid @RequestBody VisitorCheckinRequest request) {
        visitorService.checkin(request);
        return ApiResponse.success();
    }

    /** 离场登记（被访人或管理员）。 */
    @PostMapping("/{id}/checkout")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Void> checkout(@PathVariable Long id) {
        visitorService.checkout(id);
        return ApiResponse.success();
    }

    /** 取消访客登记（被访人或管理员）。 */
    @PostMapping("/{id}/cancel")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Void> cancel(@PathVariable Long id) {
        visitorService.cancel(id);
        return ApiResponse.success();
    }
}
