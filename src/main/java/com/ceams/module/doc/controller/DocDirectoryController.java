package com.ceams.module.doc.controller;

import com.ceams.common.response.ApiResponse;
import com.ceams.module.doc.dto.DirectoryCreateRequest;
import com.ceams.module.doc.dto.DirectoryMoveRequest;
import com.ceams.module.doc.dto.DirectoryUpdateRequest;
import com.ceams.module.doc.service.DocDirectoryService;
import com.ceams.module.doc.vo.DirectoryNodeVO;
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

/**
 * 文档目录接口（统一前缀 /api/v1/doc/directories）。
 *
 * <p>服务层负责数据权限：PUBLIC 库仅全权用户可建，PERSONAL / DEPT / PROJECT 按库默认规则放行，
 * 编辑 / 移动 / 删除要求 {@code doc:dir:manage} 或目录级对应动作。
 */
@RestController
@RequestMapping("/api/v1/doc/directories")
@RequiredArgsConstructor
public class DocDirectoryController {

    private final DocDirectoryService directoryService;

    /** 目录树（按可见性过滤）。 */
    @GetMapping("/tree")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<DirectoryNodeVO>> tree(
            @RequestParam(required = false) String libraryType,
            @RequestParam(required = false) Long ownerId) {
        return ApiResponse.success(directoryService.tree(libraryType, ownerId));
    }

    /** 新建目录。 */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<String> create(@Valid @RequestBody DirectoryCreateRequest request) {
        return ApiResponse.success(directoryService.create(request));
    }

    /** 改名 / 排序。 */
    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Void> update(
            @PathVariable Long id, @Valid @RequestBody DirectoryUpdateRequest request) {
        directoryService.update(id, request);
        return ApiResponse.success();
    }

    /** 移动目录。 */
    @PutMapping("/{id}/move")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Void> move(
            @PathVariable Long id, @Valid @RequestBody DirectoryMoveRequest request) {
        directoryService.move(id, request);
        return ApiResponse.success();
    }

    /** 删除目录（非空拒绝）。 */
    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        directoryService.delete(id);
        return ApiResponse.success();
    }
}
