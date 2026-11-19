package com.ceams.module.bpm.controller;

import com.ceams.common.response.ApiResponse;
import com.ceams.common.response.PageResult;
import com.ceams.module.bpm.dto.DelegateCreateRequest;
import com.ceams.module.bpm.service.BpmDelegateService;
import com.ceams.module.bpm.vo.DelegateVO;
import com.ceams.module.bpm.vo.UserOptionVO;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 用户自助代理接口（/api/v1/bpm/delegates），仅操作本人代理。 */
@RestController
@RequestMapping("/api/v1/bpm/delegates")
@RequiredArgsConstructor
public class BpmDelegateController {

    private final BpmDelegateService delegateService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<PageResult<DelegateVO>> pageMine(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size) {
        return ApiResponse.success(delegateService.pageMine(page, size));
    }

    @GetMapping("/candidates")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<UserOptionVO>> candidates(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "20") int limit) {
        return ApiResponse.success(delegateService.candidates(keyword, limit));
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<String> create(@Valid @RequestBody DelegateCreateRequest request) {
        return ApiResponse.success(String.valueOf(delegateService.create(request)));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Void> cancel(@PathVariable Long id) {
        delegateService.cancel(id);
        return ApiResponse.success();
    }
}
