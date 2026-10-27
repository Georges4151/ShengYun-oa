package com.ceams.module.masterdata.controller;

import com.ceams.common.response.ApiResponse;
import com.ceams.common.response.PageResult;
import com.ceams.module.masterdata.dto.material.MaterialCreateRequest;
import com.ceams.module.masterdata.dto.material.MaterialUpdateRequest;
import com.ceams.module.masterdata.service.MaterialService;
import com.ceams.module.masterdata.vo.IdResponse;
import com.ceams.module.masterdata.vo.MaterialVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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

/**
 * 材料库管理接口。
 */
@RestController
@RequestMapping("/api/v1/materials")
@RequiredArgsConstructor
public class MaterialController {

    private final MaterialService materialService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<PageResult<MaterialVO>> page(@RequestParam(defaultValue = "1") long page,
                                                    @RequestParam(defaultValue = "20") long size,
                                                    @RequestParam(required = false) String keyword,
                                                    @RequestParam(required = false) Long categoryId) {
        return ApiResponse.success(materialService.page(page, size, keyword, categoryId));
    }

    @PostMapping
    @PreAuthorize("hasRole('SYS_ADMIN')")
    public ApiResponse<IdResponse> create(@Valid @RequestBody MaterialCreateRequest request) {
        return ApiResponse.success(materialService.create(request));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<MaterialVO> detail(@PathVariable Long id) {
        return ApiResponse.success(materialService.detail(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SYS_ADMIN')")
    public ApiResponse<Void> update(@PathVariable Long id, @Valid @RequestBody MaterialUpdateRequest request) {
        materialService.update(id, request);
        return ApiResponse.success();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SYS_ADMIN')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        materialService.delete(id);
        return ApiResponse.success();
    }
}