package com.ceams.module.hr.controller;

import com.ceams.common.response.ApiResponse;
import com.ceams.module.hr.dto.JobLevelCreateRequest;
import com.ceams.module.hr.dto.JobLevelUpdateRequest;
import com.ceams.module.hr.service.HrJobLevelService;
import com.ceams.module.hr.vo.JobLevelVO;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** HR 职级字典接口。 */
@RestController
@RequestMapping("/api/v1/hr/job-levels")
@RequiredArgsConstructor
public class HrJobLevelController {

    private final HrJobLevelService jobLevelService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<JobLevelVO>> list() {
        return ApiResponse.success(jobLevelService.list());
    }

    @GetMapping("/enabled")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<JobLevelVO>> enabledList() {
        return ApiResponse.success(jobLevelService.enabledList());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SYS_ADMIN')")
    public ApiResponse<String> create(@Valid @RequestBody JobLevelCreateRequest request) {
        return ApiResponse.success(jobLevelService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SYS_ADMIN')")
    public ApiResponse<Void> update(
            @PathVariable Long id, @Valid @RequestBody JobLevelUpdateRequest request) {
        jobLevelService.update(id, request);
        return ApiResponse.success();
    }
}
