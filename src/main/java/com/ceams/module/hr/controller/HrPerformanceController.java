package com.ceams.module.hr.controller;

import com.ceams.common.response.ApiResponse;
import com.ceams.common.response.PageResult;
import com.ceams.module.hr.dto.PerformanceLeaderRequest;
import com.ceams.module.hr.dto.PerformanceReviewBatchRequest;
import com.ceams.module.hr.dto.PerformanceReviewRequest;
import com.ceams.module.hr.dto.PerformanceSelfRequest;
import com.ceams.module.hr.dto.PerformanceTemplateSaveRequest;
import com.ceams.module.hr.enums.PerformanceReviewStatus;
import com.ceams.module.hr.service.HrPerformanceService;
import com.ceams.module.hr.vo.PerformanceBatchResultVO;
import com.ceams.module.hr.vo.PerformanceReviewVO;
import com.ceams.module.hr.vo.PerformanceTemplateVO;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * HR 绩效模板与考评单接口。
 *
 * <p>权限口径与薪资模块一致：JWT 仅携带 ROLE_*，故使用等价的角色表达式。绩效模板查看额外放行
 * FINANCE（便于财务核对绩效系数），模板写入与考评单管理仅 SYS_ADMIN；本人自评 / 上级打分仅要求
 * 登录，归属校验在服务层完成。
 */
@RestController
@RequestMapping("/api/v1/hr/performance")
@RequiredArgsConstructor
public class HrPerformanceController {

    private final HrPerformanceService performanceService;

    // ------------------------------------------------------------------
    // 模板
    // ------------------------------------------------------------------

    @GetMapping("/templates")
    @PreAuthorize("hasAnyRole('SYS_ADMIN','FINANCE')")
    public ApiResponse<List<PerformanceTemplateVO>> listTemplates() {
        return ApiResponse.success(performanceService.listTemplates());
    }

    @PostMapping("/templates")
    @PreAuthorize("hasAnyRole('SYS_ADMIN')")
    public ApiResponse<String> createTemplate(
            @Valid @RequestBody PerformanceTemplateSaveRequest request) {
        return ApiResponse.success(performanceService.saveTemplate(null, request));
    }

    @PutMapping("/templates/{id}")
    @PreAuthorize("hasAnyRole('SYS_ADMIN')")
    public ApiResponse<String> updateTemplate(
            @PathVariable Long id, @Valid @RequestBody PerformanceTemplateSaveRequest request) {
        return ApiResponse.success(performanceService.saveTemplate(id, request));
    }

    // ------------------------------------------------------------------
    // 考评单
    // ------------------------------------------------------------------

    @PostMapping("/reviews/batch")
    @PreAuthorize("hasAnyRole('SYS_ADMIN')")
    public ApiResponse<PerformanceBatchResultVO> batchGenerate(
            @Valid @RequestBody PerformanceReviewBatchRequest request) {
        return ApiResponse.success(performanceService.batchGenerate(request));
    }

    @GetMapping("/reviews")
    @PreAuthorize("hasAnyRole('SYS_ADMIN')")
    public ApiResponse<PageResult<PerformanceReviewVO>> pageReviews(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size,
            @RequestParam(required = false) Long templateId,
            @RequestParam(required = false) String period,
            @RequestParam(required = false) PerformanceReviewStatus status,
            @RequestParam(required = false) Long employeeId) {
        return ApiResponse.success(
                performanceService.pageReviews(page, size, templateId, period, status, employeeId));
    }

    @GetMapping("/reviews/my")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<PageResult<PerformanceReviewVO>> myReviews(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size) {
        return ApiResponse.success(performanceService.myReviews(page, size));
    }

    @PostMapping("/reviews/{id}/self")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Void> submitSelf(
            @PathVariable Long id, @Valid @RequestBody PerformanceSelfRequest request) {
        performanceService.submitSelf(id, request);
        return ApiResponse.success();
    }

    @PostMapping("/reviews/{id}/leader")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Void> submitLeader(
            @PathVariable Long id, @Valid @RequestBody PerformanceLeaderRequest request) {
        performanceService.submitLeader(id, request);
        return ApiResponse.success();
    }

    @PostMapping("/reviews/{id}/review")
    @PreAuthorize("hasAnyRole('SYS_ADMIN')")
    public ApiResponse<Void> review(
            @PathVariable Long id, @Valid @RequestBody PerformanceReviewRequest request) {
        performanceService.review(id, request);
        return ApiResponse.success();
    }
}
