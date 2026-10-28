package com.ceams.module.seal.controller;

import com.ceams.common.response.ApiResponse;
import com.ceams.common.response.PageResult;
import com.ceams.module.seal.dto.ElectronicSealCreateRequest;
import com.ceams.module.seal.dto.ElectronicSealUpdateRequest;
import com.ceams.module.seal.enums.SealStatus;
import com.ceams.module.seal.enums.StorageType;
import com.ceams.module.seal.service.ElectronicSealService;
import com.ceams.module.seal.vo.ElectronicSealVO;
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
 * 电子印章管理接口。
 */
@RestController
@RequestMapping("/api/v1/seals")
@RequiredArgsConstructor
public class ElectronicSealController {

    private final ElectronicSealService electronicSealService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<PageResult<ElectronicSealVO>> page(@RequestParam(defaultValue = "1") long page,
                                                          @RequestParam(defaultValue = "20") long size,
                                                          @RequestParam(required = false) String name,
                                                          @RequestParam(required = false) SealStatus status,
                                                          @RequestParam(required = false) StorageType storageType) {
        return ApiResponse.success(electronicSealService.page(page, size, name, status, storageType));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SYS_ADMIN','FINANCE')")
    public ApiResponse<String> create(@Valid @RequestBody ElectronicSealCreateRequest request) {
        return ApiResponse.success(electronicSealService.create(request));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<ElectronicSealVO> detail(@PathVariable Long id) {
        return ApiResponse.success(electronicSealService.detail(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SYS_ADMIN','FINANCE')")
    public ApiResponse<Void> update(@PathVariable Long id, @Valid @RequestBody ElectronicSealUpdateRequest request) {
        electronicSealService.update(id, request);
        return ApiResponse.success();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SYS_ADMIN','FINANCE')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        electronicSealService.delete(id);
        return ApiResponse.success();
    }
}
