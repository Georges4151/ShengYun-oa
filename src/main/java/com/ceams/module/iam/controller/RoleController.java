package com.ceams.module.iam.controller;

import com.ceams.common.response.ApiResponse;
import com.ceams.common.response.PageResult;
import com.ceams.module.iam.dto.role.AssignPermissionsRequest;
import com.ceams.module.iam.dto.role.RoleCreateRequest;
import com.ceams.module.iam.dto.role.RoleUpdateRequest;
import com.ceams.module.iam.service.RoleService;
import com.ceams.module.iam.vo.RoleVO;
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
 * 角色管理接口。
 */
@RestController
@RequestMapping("/api/v1/roles")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SYS_ADMIN')")
public class RoleController {

    private final RoleService roleService;

    @GetMapping
    public ApiResponse<PageResult<RoleVO>> page(@RequestParam(defaultValue = "1") long page,
                                                @RequestParam(defaultValue = "20") long size,
                                                @RequestParam(required = false) String keyword) {
        return ApiResponse.success(roleService.page(page, size, keyword));
    }

    @PostMapping
    public ApiResponse<String> create(@Valid @RequestBody RoleCreateRequest request) {
        return ApiResponse.success(roleService.create(request));
    }

    @GetMapping("/{id}")
    public ApiResponse<RoleVO> detail(@PathVariable Long id) {
        return ApiResponse.success(roleService.detail(id));
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> update(@PathVariable Long id, @Valid @RequestBody RoleUpdateRequest request) {
        roleService.update(id, request);
        return ApiResponse.success();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        roleService.delete(id);
        return ApiResponse.success();
    }

    @PutMapping("/{id}/permissions")
    public ApiResponse<Void> assignPermissions(@PathVariable Long id, @Valid @RequestBody AssignPermissionsRequest request) {
        roleService.assignPermissions(id, request.getPermissionIds());
        return ApiResponse.success();
    }
}