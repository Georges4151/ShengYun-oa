package com.ceams.module.hr.controller;

import com.ceams.common.response.ApiResponse;
import com.ceams.common.response.PageResult;
import com.ceams.module.hr.dto.ContractCreateRequest;
import com.ceams.module.hr.dto.ContractUpdateRequest;
import com.ceams.module.hr.enums.ContractStatus;
import com.ceams.module.hr.service.HrContractService;
import com.ceams.module.hr.vo.ContractVO;
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

/** HR 劳动合同接口。 */
@RestController
@RequestMapping("/api/v1/hr/contracts")
@RequiredArgsConstructor
public class HrContractController {

    private final HrContractService contractService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<PageResult<ContractVO>> page(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size,
            @RequestParam(required = false) Long employeeId,
            @RequestParam(required = false) ContractStatus status) {
        return ApiResponse.success(contractService.page(page, size, employeeId, status));
    }

    @GetMapping("/expiring")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<ContractVO>> expiring(@RequestParam(defaultValue = "30") int days) {
        return ApiResponse.success(contractService.expiring(days));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SYS_ADMIN')")
    public ApiResponse<String> create(@Valid @RequestBody ContractCreateRequest request) {
        return ApiResponse.success(contractService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SYS_ADMIN')")
    public ApiResponse<Void> update(
            @PathVariable Long id, @Valid @RequestBody ContractUpdateRequest request) {
        contractService.update(id, request);
        return ApiResponse.success();
    }
}
