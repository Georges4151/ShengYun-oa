package com.ceams.module.seal.controller;

import com.ceams.common.response.ApiResponse;
import com.ceams.common.response.PageResult;
import com.ceams.module.seal.service.ElectronicSealUsageService;
import com.ceams.module.seal.vo.ElectronicSealUsageVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 印章使用审计接口。
 */
@RestController
@RequestMapping("/api/v1/seal/usages")
@RequiredArgsConstructor
public class ElectronicSealUsageController {

    private final ElectronicSealUsageService electronicSealUsageService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<PageResult<ElectronicSealUsageVO>> page(@RequestParam(defaultValue = "1") long page,
                                                               @RequestParam(defaultValue = "20") long size,
                                                               @RequestParam(required = false) Long sealId,
                                                               @RequestParam(required = false) Long userId) {
        return ApiResponse.success(electronicSealUsageService.page(page, size, sealId, userId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<ElectronicSealUsageVO> detail(@PathVariable Long id) {
        return ApiResponse.success(electronicSealUsageService.detail(id));
    }
}
