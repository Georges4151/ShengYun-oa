package com.ceams.module.iam.controller;

import com.ceams.common.response.ApiResponse;
import com.ceams.module.iam.dto.permission.PermissionCreateRequest;
import com.ceams.module.iam.dto.permission.PermissionUpdateRequest;
import com.ceams.module.iam.service.PermissionService;
import com.ceams.module.iam.vo.PermissionVO;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 权限查询与维护接口。 */
@RestController
@RequestMapping("/api/v1/permissions")
@RequiredArgsConstructor
public class PermissionController {

    private final PermissionService permissionService;

    /** 权限树（管理员赋权用）。 */
    @GetMapping
    @PreAuthorize("hasRole('SYS_ADMIN')")
    public ApiResponse<List<PermissionVO>> tree() {
        return ApiResponse.success(permissionService.tree());
    }

    /** 当前登录用户权限码集合（前端按钮级控制）。 */
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<String>> me() {
        return ApiResponse.success(permissionService.currentUserPermissionCodes());
    }

    /** 新建权限，返回新权限 ID。 */
    @PostMapping
    @PreAuthorize("hasRole('SYS_ADMIN')")
    public ApiResponse<String> create(@Valid @RequestBody PermissionCreateRequest request) {
        return ApiResponse.success(permissionService.create(request));
    }

    /** 更新权限。 */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SYS_ADMIN')")
    public ApiResponse<Void> update(
            @PathVariable Long id, @Valid @RequestBody PermissionUpdateRequest request) {
        permissionService.update(id, request);
        return ApiResponse.success();
    }

    /** 删除权限（存在子权限时拒绝）。 */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SYS_ADMIN')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        permissionService.delete(id);
        return ApiResponse.success();
    }
}
