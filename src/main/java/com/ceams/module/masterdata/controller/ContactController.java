package com.ceams.module.masterdata.controller;

import com.ceams.common.response.ApiResponse;
import com.ceams.common.response.PageResult;
import com.ceams.module.masterdata.dto.contact.ContactCreateRequest;
import com.ceams.module.masterdata.dto.contact.ContactUpdateRequest;
import com.ceams.module.masterdata.enums.ContactType;
import com.ceams.module.masterdata.service.ContactService;
import com.ceams.module.masterdata.vo.ContactVO;
import com.ceams.module.masterdata.vo.IdResponse;
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
 * 往来单位管理接口。
 */
@RestController
@RequestMapping("/api/v1/contacts")
@RequiredArgsConstructor
public class ContactController {

    private final ContactService contactService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<PageResult<ContactVO>> page(@RequestParam(defaultValue = "1") long page,
                                                   @RequestParam(defaultValue = "20") long size,
                                                   @RequestParam(required = false) String keyword,
                                                   @RequestParam(required = false) ContactType type) {
        return ApiResponse.success(contactService.page(page, size, keyword, type));
    }

    @PostMapping
    @PreAuthorize("hasRole('SYS_ADMIN')")
    public ApiResponse<IdResponse> create(@Valid @RequestBody ContactCreateRequest request) {
        return ApiResponse.success(contactService.create(request));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<ContactVO> detail(@PathVariable Long id) {
        return ApiResponse.success(contactService.detail(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SYS_ADMIN')")
    public ApiResponse<Void> update(@PathVariable Long id, @Valid @RequestBody ContactUpdateRequest request) {
        contactService.update(id, request);
        return ApiResponse.success();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SYS_ADMIN')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        contactService.delete(id);
        return ApiResponse.success();
    }
}