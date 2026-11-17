package com.ceams.module.bpm.controller;

import com.ceams.common.response.ApiResponse;
import com.ceams.common.response.PageResult;
import com.ceams.common.sign.HmacSigned;
import com.ceams.module.bpm.dto.TaskApproveRequest;
import com.ceams.module.bpm.dto.TaskDelegateRequest;
import com.ceams.module.bpm.dto.TaskRejectRequest;
import com.ceams.module.bpm.enums.BpmInstanceStatus;
import com.ceams.module.bpm.service.BpmTaskService;
import com.ceams.module.bpm.vo.CcRecordVO;
import com.ceams.module.bpm.vo.InstanceVO;
import com.ceams.module.bpm.vo.TaskVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 审批任务接口（/api/v1/bpm/tasks），统一待办与四个查询。 */
@RestController
@RequestMapping("/api/v1/bpm/tasks")
@RequiredArgsConstructor
public class BpmTaskController {

    private final BpmTaskService taskService;

    @GetMapping({"/pending", "/todo"})
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<PageResult<TaskVO>> pending(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size,
            @RequestParam(required = false) String bizType,
            @RequestParam(required = false) Boolean overdue) {
        return ApiResponse.success(taskService.pagePending(page, size, bizType, overdue));
    }

    @GetMapping("/initiated")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<PageResult<InstanceVO>> initiated(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size,
            @RequestParam(required = false) BpmInstanceStatus status,
            @RequestParam(required = false) String bizType) {
        return ApiResponse.success(taskService.pageInitiated(page, size, status, bizType));
    }

    @GetMapping("/done")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<PageResult<TaskVO>> done(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size,
            @RequestParam(required = false) String bizType) {
        return ApiResponse.success(taskService.pageDone(page, size, bizType));
    }

    @GetMapping("/cc")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<PageResult<CcRecordVO>> cc(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size,
            @RequestParam(required = false) String bizType,
            @RequestParam(required = false)
                    com.ceams.module.bpm.enums.CcReadStatus readStatus) {
        return ApiResponse.success(taskService.pageCc(page, size, bizType, readStatus));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Void> approve(
            @PathVariable Long id, @Valid @RequestBody TaskApproveRequest request) {
        taskService.approve(id, request);
        return ApiResponse.success();
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Void> reject(
            @PathVariable Long id, @Valid @RequestBody TaskRejectRequest request) {
        taskService.reject(id, request);
        return ApiResponse.success();
    }

    @HmacSigned
    @PostMapping("/{id}/delegate")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Void> delegate(
            @PathVariable Long id, @Valid @RequestBody TaskDelegateRequest request) {
        taskService.delegate(id, request);
        return ApiResponse.success();
    }
}
