package com.ceams.module.project.controller;

import com.ceams.common.response.ApiResponse;
import com.ceams.module.project.service.SubcontractorContractService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 分包合同审批接口（项目域路径）。
 *
 * <p>与 {@code /api/v1/subcontractor-contracts/{id}/submit-approval} 等价，
 * 供 Web 与小程序按项目域前缀调用，实际逻辑统一委托分包合同服务。
 */
@RestController
@RequestMapping("/api/v1/project/subcontractor-contracts")
@RequiredArgsConstructor
public class ProjectSubcontractorContractApprovalController {

    private final SubcontractorContractService subcontractorContractService;

    /** 提交分包合同审批，返回 BPM 流程实例 ID（字符串）。 */
    @PostMapping("/{id}/submit-approval")
    @PreAuthorize("hasRole('SYS_ADMIN')")
    public ApiResponse<String> submitApproval(@PathVariable Long id) {
        return ApiResponse.success(subcontractorContractService.submitApproval(id));
    }
}
