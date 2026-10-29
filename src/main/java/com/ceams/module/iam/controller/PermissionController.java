package com.ceams.module.iam.controller;

import com.ceams.common.response.ApiResponse;
import com.ceams.module.iam.service.PermissionService;
import com.ceams.module.iam.vo.PermissionVO;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 权限查询接口。 */
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
}
