package com.ceams.module.iam.controller;

import com.ceams.common.response.ApiResponse;
import com.ceams.common.response.PageResult;
import com.ceams.module.iam.dto.dept.DeptCreateRequest;
import com.ceams.module.iam.dto.dept.DeptUpdateRequest;
import com.ceams.module.iam.service.DeptService;
import com.ceams.module.iam.vo.DeptVO;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 部门维护接口（最小组织树）。
 *
 * <p>细粒度权限由服务层校验：查询需 {@code iam:dept:view}，增删改需 {@code iam:dept:manage}；
 * 控制器仅要求已登录，保持与现有 IAM 接口的权限注解风格一致。
 */
@RestController
@RequestMapping("/api/v1/iam/depts")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class DeptController {

    private final DeptService deptService;

    @GetMapping("/tree")
    public ApiResponse<List<DeptVO>> tree() {
        return ApiResponse.success(deptService.tree());
    }

    @GetMapping
    public ApiResponse<PageResult<DeptVO>> page(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size,
            @RequestParam(required = false) String keyword) {
        return ApiResponse.success(deptService.page(page, size, keyword));
    }

    @PostMapping
    public ApiResponse<String> create(@Valid @RequestBody DeptCreateRequest request) {
        return ApiResponse.success(deptService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> update(
            @PathVariable Long id, @Valid @RequestBody DeptUpdateRequest request) {
        deptService.update(id, request);
        return ApiResponse.success();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        deptService.delete(id);
        return ApiResponse.success();
    }
}
