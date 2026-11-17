package com.ceams.module.admin.controller;

import com.ceams.common.response.ApiResponse;
import com.ceams.common.response.PageResult;
import com.ceams.module.admin.dto.AssetBorrowRequest;
import com.ceams.module.admin.dto.AssetCreateRequest;
import com.ceams.module.admin.dto.AssetInventoryRequest;
import com.ceams.module.admin.dto.AssetScrapRequest;
import com.ceams.module.admin.dto.AssetUpdateRequest;
import com.ceams.module.admin.enums.AssetStatus;
import com.ceams.module.admin.service.AdminAssetService;
import com.ceams.module.admin.vo.AssetBorrowVO;
import com.ceams.module.admin.vo.AssetVO;
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

/** 行政资产接口（统一前缀 /api/v1/admin/assets）。 */
@RestController
@RequestMapping("/api/v1/admin/assets")
@RequiredArgsConstructor
public class AdminAssetController {

    private final AdminAssetService assetService;

    /** 资产分页查询，支持 keyword（编号 / 名称）、status、deptId。 */
    @GetMapping
    @PreAuthorize("hasAnyRole('SYS_ADMIN')")
    public ApiResponse<PageResult<AssetVO>> page(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) AssetStatus status,
            @RequestParam(required = false) Long deptId,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size) {
        return ApiResponse.success(assetService.page(keyword, status, deptId, page, size));
    }

    /** 资产详情。 */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SYS_ADMIN')")
    public ApiResponse<AssetVO> detail(@PathVariable Long id) {
        return ApiResponse.success(assetService.detail(id));
    }

    /** 资产建档。 */
    @PostMapping
    @PreAuthorize("hasAnyRole('SYS_ADMIN')")
    public ApiResponse<String> create(@Valid @RequestBody AssetCreateRequest request) {
        return ApiResponse.success(assetService.create(request));
    }

    /** 资产编辑。 */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SYS_ADMIN')")
    public ApiResponse<Void> update(
            @PathVariable Long id, @Valid @RequestBody AssetUpdateRequest request) {
        assetService.update(id, request);
        return ApiResponse.success();
    }

    /** 发起领用（BPM）。 */
    @PostMapping("/{id}/borrow")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<String> borrow(
            @PathVariable Long id, @Valid @RequestBody AssetBorrowRequest request) {
        return ApiResponse.success(assetService.borrow(id, request));
    }

    /** 本人领用记录。 */
    @GetMapping("/borrows/my")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<AssetBorrowVO>> myBorrows() {
        return ApiResponse.success(assetService.myBorrows());
    }

    /** 归还登记。 */
    @PostMapping("/{id}/return")
    @PreAuthorize("hasAnyRole('SYS_ADMIN')")
    public ApiResponse<Void> returnAsset(
            @PathVariable Long id, @RequestBody(required = false) AssetScrapRequest request) {
        assetService.returnAsset(id, request == null ? null : request.getRemark());
        return ApiResponse.success();
    }

    /** 报废。 */
    @PostMapping("/{id}/scrap")
    @PreAuthorize("hasAnyRole('SYS_ADMIN')")
    public ApiResponse<Void> scrap(
            @PathVariable Long id, @RequestBody(required = false) AssetScrapRequest request) {
        assetService.scrap(id, request);
        return ApiResponse.success();
    }

    /** 批量盘点，返回实际变更条数。 */
    @PostMapping("/inventory")
    @PreAuthorize("hasAnyRole('SYS_ADMIN')")
    public ApiResponse<Integer> inventory(@Valid @RequestBody AssetInventoryRequest request) {
        return ApiResponse.success(assetService.inventory(request));
    }
}
