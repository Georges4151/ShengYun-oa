package com.ceams.module.bpm.controller;

import com.ceams.common.response.ApiResponse;
import com.ceams.common.response.PageResult;
import com.ceams.common.sign.HmacSigned;
import com.ceams.module.bpm.dto.TemplateCreateRequest;
import com.ceams.module.bpm.dto.TemplatePublishRequest;
import com.ceams.module.bpm.dto.TemplateUpdateRequest;
import com.ceams.module.bpm.enums.TemplateStatus;
import com.ceams.module.bpm.service.BpmTemplateService;
import com.ceams.module.bpm.vo.TemplateVO;
import com.ceams.module.bpm.vo.TemplateVersionVO;
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

/** 流程模板接口（/api/v1/bpm/templates）。 */
@RestController
@RequestMapping("/api/v1/bpm/templates")
@RequiredArgsConstructor
public class BpmTemplateController {

    private final BpmTemplateService templateService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<PageResult<TemplateVO>> page(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size,
            @RequestParam(required = false) String bizType,
            @RequestParam(required = false) TemplateStatus status,
            @RequestParam(required = false) String keyword) {
        return ApiResponse.success(templateService.page(page, size, bizType, status, keyword));
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<String> create(@Valid @RequestBody TemplateCreateRequest request) {
        return ApiResponse.success(templateService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Void> update(
            @PathVariable Long id, @Valid @RequestBody TemplateUpdateRequest request) {
        templateService.update(id, request);
        return ApiResponse.success();
    }

    @PutMapping("/{id}/enable")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Void> enable(@PathVariable Long id) {
        templateService.enable(id);
        return ApiResponse.success();
    }

    @PutMapping("/{id}/disable")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Void> disable(@PathVariable Long id) {
        templateService.disable(id);
        return ApiResponse.success();
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<TemplateVO> detail(@PathVariable Long id) {
        return ApiResponse.success(templateService.detail(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        templateService.delete(id);
        return ApiResponse.success();
    }

    @GetMapping("/{id}/versions")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<TemplateVersionVO>> versions(@PathVariable Long id) {
        return ApiResponse.success(templateService.versions(id));
    }

    @HmacSigned
    @PostMapping("/{id}/publish")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Long> publish(
            @PathVariable Long id, @Valid @RequestBody TemplatePublishRequest request) {
        return ApiResponse.success(templateService.publish(id, request));
    }
}
