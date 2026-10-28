package com.ceams.module.project.controller;

import com.ceams.common.response.ApiResponse;
import com.ceams.common.response.PageResult;
import com.ceams.module.project.dto.project.ProjectCreateRequest;
import com.ceams.module.project.dto.project.ProjectUpdateRequest;
import com.ceams.module.project.enums.ProjectStatus;
import com.ceams.module.project.service.ProjectService;
import com.ceams.module.project.vo.ProjectVO;
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
 * 项目台账管理接口。
 */
@RestController
@RequestMapping("/api/v1/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<PageResult<ProjectVO>> page(@RequestParam(defaultValue = "1") long page,
                                                   @RequestParam(defaultValue = "20") long size,
                                                   @RequestParam(required = false) String keyword,
                                                   @RequestParam(required = false) ProjectStatus status) {
        return ApiResponse.success(projectService.page(page, size, keyword, status));
    }

    @PostMapping
    @PreAuthorize("hasRole('SYS_ADMIN')")
    public ApiResponse<String> create(@Valid @RequestBody ProjectCreateRequest request) {
        return ApiResponse.success(projectService.create(request));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<ProjectVO> detail(@PathVariable Long id) {
        return ApiResponse.success(projectService.detail(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SYS_ADMIN')")
    public ApiResponse<Void> update(@PathVariable Long id, @Valid @RequestBody ProjectUpdateRequest request) {
        projectService.update(id, request);
        return ApiResponse.success();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SYS_ADMIN')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        projectService.delete(id);
        return ApiResponse.success();
    }
}