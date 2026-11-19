package com.ceams.module.hr.controller;

import com.ceams.common.response.ApiResponse;
import com.ceams.common.response.PageResult;
import com.ceams.module.hr.dto.EmployeeChangeRequest;
import com.ceams.module.hr.dto.EmployeeCreateRequest;
import com.ceams.module.hr.dto.EmployeeUpdateRequest;
import com.ceams.module.hr.dto.PositionCreateRequest;
import com.ceams.module.hr.dto.PositionUpdateRequest;
import com.ceams.module.hr.enums.EmployeeStatus;
import com.ceams.module.hr.service.HrEmployeeService;
import com.ceams.module.hr.service.HrPositionService;
import com.ceams.module.hr.vo.EmployeeChangeVO;
import com.ceams.module.hr.vo.EmployeeVO;
import com.ceams.module.hr.vo.PositionVO;
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

/** HR 员工档案 / 岗位 / 入转调离接口。 */
@RestController
@RequestMapping("/api/v1/hr")
@RequiredArgsConstructor
public class HrEmployeeController {

    private final HrEmployeeService employeeService;
    private final HrPositionService positionService;

    // ------------------------------------------------------------------
    // 员工档案
    // ------------------------------------------------------------------

    @GetMapping("/employees")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<PageResult<EmployeeVO>> pageEmployees(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size,
            @RequestParam(required = false) Long deptId,
            @RequestParam(required = false) EmployeeStatus status,
            @RequestParam(required = false) String keyword) {
        return ApiResponse.success(employeeService.page(page, size, deptId, status, keyword));
    }

    @GetMapping("/employees/me")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<EmployeeVO> myProfile() {
        return ApiResponse.success(employeeService.currentEmployee());
    }

    @GetMapping("/employees/{id}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<EmployeeVO> employeeDetail(@PathVariable Long id) {
        return ApiResponse.success(employeeService.detail(id));
    }

    @PostMapping("/employees")
    @PreAuthorize("hasAnyRole('SYS_ADMIN')")
    public ApiResponse<String> createEmployee(@Valid @RequestBody EmployeeCreateRequest request) {
        return ApiResponse.success(employeeService.create(request));
    }

    @PutMapping("/employees/{id}")
    @PreAuthorize("hasAnyRole('SYS_ADMIN')")
    public ApiResponse<Void> updateEmployee(
            @PathVariable Long id, @Valid @RequestBody EmployeeUpdateRequest request) {
        employeeService.update(id, request);
        return ApiResponse.success();
    }

    @GetMapping("/employees/{id}/changes")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<EmployeeChangeVO>> employeeChanges(@PathVariable Long id) {
        return ApiResponse.success(employeeService.listChanges(id));
    }

    @PostMapping("/employees/{id}/regular")
    @PreAuthorize("hasAnyRole('SYS_ADMIN')")
    public ApiResponse<String> regular(
            @PathVariable Long id, @Valid @RequestBody EmployeeChangeRequest request) {
        return ApiResponse.success(employeeService.regular(id, request));
    }

    @PostMapping("/employees/{id}/transfer")
    @PreAuthorize("hasAnyRole('SYS_ADMIN')")
    public ApiResponse<String> transfer(
            @PathVariable Long id, @Valid @RequestBody EmployeeChangeRequest request) {
        return ApiResponse.success(employeeService.transfer(id, request));
    }

    @PostMapping("/employees/{id}/resign")
    @PreAuthorize("hasAnyRole('SYS_ADMIN')")
    public ApiResponse<String> resign(
            @PathVariable Long id, @Valid @RequestBody EmployeeChangeRequest request) {
        return ApiResponse.success(employeeService.resign(id, request));
    }

    // ------------------------------------------------------------------
    // 岗位
    // ------------------------------------------------------------------

    @GetMapping("/positions")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<PositionVO>> listPositions() {
        return ApiResponse.success(positionService.list());
    }

    @PostMapping("/positions")
    @PreAuthorize("hasAnyRole('SYS_ADMIN')")
    public ApiResponse<String> createPosition(@Valid @RequestBody PositionCreateRequest request) {
        return ApiResponse.success(positionService.create(request));
    }

    @PutMapping("/positions/{id}")
    @PreAuthorize("hasAnyRole('SYS_ADMIN')")
    public ApiResponse<Void> updatePosition(
            @PathVariable Long id, @Valid @RequestBody PositionUpdateRequest request) {
        positionService.update(id, request);
        return ApiResponse.success();
    }
}
