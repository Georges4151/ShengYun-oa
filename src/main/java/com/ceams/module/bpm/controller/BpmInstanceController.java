package com.ceams.module.bpm.controller;

import com.ceams.common.response.ApiResponse;
import com.ceams.common.response.PageResult;
import com.ceams.common.sign.HmacSigned;
import com.ceams.module.bpm.dto.InstanceActionRequest;
import com.ceams.module.bpm.dto.InstanceStartRequest;
import com.ceams.module.bpm.enums.BpmInstanceStatus;
import com.ceams.module.bpm.service.BpmInstanceService;
import com.ceams.module.bpm.vo.InstanceDetailVO;
import com.ceams.module.bpm.vo.InstanceVO;
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

/** 流程实例接口（/api/v1/bpm/instances）。 */
@RestController
@RequestMapping("/api/v1/bpm/instances")
@RequiredArgsConstructor
public class BpmInstanceController {

    private final BpmInstanceService instanceService;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<String> start(@Valid @RequestBody InstanceStartRequest request) {
        return ApiResponse.success(instanceService.start(request));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<InstanceDetailVO> detail(@PathVariable Long id) {
        return ApiResponse.success(instanceService.detail(id));
    }

    @GetMapping("/mine")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<PageResult<InstanceVO>> mine(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size,
            @RequestParam(required = false) BpmInstanceStatus status,
            @RequestParam(required = false) String bizType) {
        return ApiResponse.success(instanceService.pageByInitiator(page, size, status, bizType));
    }

    @PostMapping("/{id}/withdraw")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Void> withdraw(
            @PathVariable Long id, @Valid @RequestBody InstanceActionRequest request) {
        instanceService.withdraw(id, request);
        return ApiResponse.success();
    }

    @HmacSigned
    @PostMapping("/{id}/terminate")
    @PreAuthorize("hasRole('SYS_ADMIN')")
    public ApiResponse<Void> terminate(
            @PathVariable Long id, @Valid @RequestBody InstanceActionRequest request) {
        instanceService.terminate(id, request);
        return ApiResponse.success();
    }
}
