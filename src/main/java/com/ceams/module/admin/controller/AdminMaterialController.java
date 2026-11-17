package com.ceams.module.admin.controller;

import com.ceams.common.response.ApiResponse;
import com.ceams.common.response.PageResult;
import com.ceams.module.admin.dto.MaterialApplyRequest;
import com.ceams.module.admin.dto.MaterialCreateRequest;
import com.ceams.module.admin.dto.MaterialStockInRequest;
import com.ceams.module.admin.dto.MaterialUpdateRequest;
import com.ceams.module.admin.enums.MaterialStatus;
import com.ceams.module.admin.service.AdminMaterialService;
import com.ceams.module.admin.vo.MaterialApplyVO;
import com.ceams.module.admin.vo.MaterialVO;
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

/** 行政易耗品接口（统一前缀 /api/v1/admin/materials）。 */
@RestController
@RequestMapping("/api/v1/admin/materials")
@RequiredArgsConstructor
public class AdminMaterialController {

    private final AdminMaterialService materialService;

    /** 物料分页查询，支持 keyword（编码 / 名称）、status。 */
    @GetMapping
    @PreAuthorize("hasAnyRole('SYS_ADMIN')")
    public ApiResponse<PageResult<MaterialVO>> page(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) MaterialStatus status,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size) {
        return ApiResponse.success(materialService.page(keyword, status, page, size));
    }

    /** 物料建档。 */
    @PostMapping
    @PreAuthorize("hasAnyRole('SYS_ADMIN')")
    public ApiResponse<String> create(@Valid @RequestBody MaterialCreateRequest request) {
        return ApiResponse.success(materialService.create(request));
    }

    /** 物料编辑。 */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SYS_ADMIN')")
    public ApiResponse<Void> update(
            @PathVariable Long id, @Valid @RequestBody MaterialUpdateRequest request) {
        materialService.update(id, request);
        return ApiResponse.success();
    }

    /** 入库。 */
    @PostMapping("/{id}/stock-in")
    @PreAuthorize("hasAnyRole('SYS_ADMIN')")
    public ApiResponse<Void> stockIn(
            @PathVariable Long id, @Valid @RequestBody MaterialStockInRequest request) {
        materialService.stockIn(id, request);
        return ApiResponse.success();
    }

    /** 发起申领（BPM）。 */
    @PostMapping("/apply")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<String> apply(@Valid @RequestBody MaterialApplyRequest request) {
        return ApiResponse.success(materialService.apply(request));
    }

    /** 本人申领记录。 */
    @GetMapping("/applies/my")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<MaterialApplyVO>> myApplies() {
        return ApiResponse.success(materialService.myApplies());
    }

    /** 确认发放。 */
    @PutMapping("/applies/{id}/issue")
    @PreAuthorize("hasAnyRole('SYS_ADMIN')")
    public ApiResponse<Void> issue(@PathVariable Long id) {
        materialService.issue(id);
        return ApiResponse.success();
    }

    /** 低于安全库存的启用物料。 */
    @GetMapping("/alerts")
    @PreAuthorize("hasAnyRole('SYS_ADMIN')")
    public ApiResponse<List<MaterialVO>> alerts() {
        return ApiResponse.success(materialService.alerts());
    }
}
