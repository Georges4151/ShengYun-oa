package com.ceams.module.masterdata.controller;

import com.ceams.common.response.ApiResponse;
import com.ceams.common.response.PageResult;
import com.ceams.module.masterdata.dto.lease.EquipmentLeaseCreateRequest;
import com.ceams.module.masterdata.dto.lease.EquipmentLeaseUpdateRequest;
import com.ceams.module.masterdata.service.EquipmentLeaseService;
import com.ceams.module.masterdata.vo.EquipmentLeaseVO;
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
 * 设备租赁管理接口。
 */
@RestController
@RequestMapping("/api/v1/equipment-leases")
@RequiredArgsConstructor
public class EquipmentLeaseController {

    private final EquipmentLeaseService equipmentLeaseService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<PageResult<EquipmentLeaseVO>> page(@RequestParam(defaultValue = "1") long page,
                                                          @RequestParam(defaultValue = "20") long size,
                                                          @RequestParam(required = false) Long equipmentId,
                                                          @RequestParam(required = false) Long projectId) {
        return ApiResponse.success(equipmentLeaseService.page(page, size, equipmentId, projectId));
    }

    @PostMapping
    @PreAuthorize("hasRole('SYS_ADMIN')")
    public ApiResponse<String> create(@Valid @RequestBody EquipmentLeaseCreateRequest request) {
        return ApiResponse.success(equipmentLeaseService.create(request));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<EquipmentLeaseVO> detail(@PathVariable Long id) {
        return ApiResponse.success(equipmentLeaseService.detail(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SYS_ADMIN')")
    public ApiResponse<Void> update(@PathVariable Long id, @Valid @RequestBody EquipmentLeaseUpdateRequest request) {
        equipmentLeaseService.update(id, request);
        return ApiResponse.success();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SYS_ADMIN')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        equipmentLeaseService.delete(id);
        return ApiResponse.success();
    }
}