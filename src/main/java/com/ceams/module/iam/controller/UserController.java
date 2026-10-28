package com.ceams.module.iam.controller;

import com.ceams.common.response.ApiResponse;
import com.ceams.common.response.PageResult;
import com.ceams.module.iam.dto.user.AssignRolesRequest;
import com.ceams.module.iam.dto.user.ResetPasswordRequest;
import com.ceams.module.iam.dto.user.UserCreateRequest;
import com.ceams.module.iam.dto.user.UserUpdateRequest;
import com.ceams.module.iam.service.UserService;
import com.ceams.module.iam.vo.UserVO;
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
 * 用户管理接口。
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SYS_ADMIN')")
public class UserController {

    private final UserService userService;

    @GetMapping
    public ApiResponse<PageResult<UserVO>> page(@RequestParam(defaultValue = "1") long page,
                                                @RequestParam(defaultValue = "20") long size,
                                                @RequestParam(required = false) String keyword,
                                                @RequestParam(required = false) String status) {
        return ApiResponse.success(userService.page(page, size, keyword, status));
    }

    @PostMapping
    public ApiResponse<String> create(@Valid @RequestBody UserCreateRequest request) {
        return ApiResponse.success(userService.create(request));
    }

    @GetMapping("/{id}")
    public ApiResponse<UserVO> detail(@PathVariable Long id) {
        return ApiResponse.success(userService.detail(id));
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> update(@PathVariable Long id, @Valid @RequestBody UserUpdateRequest request) {
        userService.update(id, request);
        return ApiResponse.success();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return ApiResponse.success();
    }

    @PutMapping("/{id}/roles")
    public ApiResponse<Void> assignRoles(@PathVariable Long id, @Valid @RequestBody AssignRolesRequest request) {
        userService.assignRoles(id, request.getRoleIds());
        return ApiResponse.success();
    }

    @PutMapping("/{id}/password")
    public ApiResponse<Void> resetPassword(@PathVariable Long id, @Valid @RequestBody ResetPasswordRequest request) {
        userService.resetPassword(id, request);
        return ApiResponse.success();
    }
}