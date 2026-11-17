package com.ceams.module.doc.controller;

import com.ceams.common.response.ApiResponse;
import com.ceams.common.response.PageResult;
import com.ceams.module.doc.dto.FileMoveRequest;
import com.ceams.module.doc.dto.FileRenameRequest;
import com.ceams.module.doc.dto.PermissionGrantRequest;
import com.ceams.module.doc.service.DocDownloadService;
import com.ceams.module.doc.service.DocFileService;
import com.ceams.module.doc.service.DocLockService;
import com.ceams.module.doc.service.DocPermissionService;
import com.ceams.module.doc.service.OnlyOfficeService;
import com.ceams.module.doc.vo.DocFileVO;
import com.ceams.module.doc.vo.DocLockVO;
import com.ceams.module.doc.vo.EditorConfigVO;
import com.ceams.module.doc.vo.PermissionVO;
import jakarta.servlet.http.HttpServletResponse;
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
 * 文档文件接口（统一前缀 /api/v1/doc/files）。
 *
 * <p>列表要求目录 PREVIEW 权；重命名 / 移动 / 编辑锁 / 回滚要求 {@code doc:file:edit} 或文件 EDIT 权；
 * 删除要求 {@code doc:file:delete} 或文件 DELETE 权；授权接口要求 {@code doc:permission:manage}。
 *
 * <p>{@code GET /{id}/download?token=} 为一次性 token 端点，<b>匿名可访问</b>
 * （Security 放行 {@code /api/v1/doc/files/*\/download}，鉴权完全依赖 token 与 Redis 一次性标记）。
 */
@RestController
@RequestMapping("/api/v1/doc/files")
@RequiredArgsConstructor
public class DocFileController {

    private final DocFileService fileService;
    private final DocPermissionService permissionService;
    private final DocLockService lockService;
    private final DocDownloadService downloadService;
    private final OnlyOfficeService onlyOfficeService;

    /** 文件分页列表。 */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<PageResult<DocFileVO>> list(
            @RequestParam Long dirId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(fileService.list(dirId, keyword, page, size));
    }

    /** 重命名。 */
    @PutMapping("/{id}/rename")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Void> rename(
            @PathVariable Long id, @Valid @RequestBody FileRenameRequest request) {
        fileService.rename(id, request);
        return ApiResponse.success();
    }

    /** 移动。 */
    @PutMapping("/{id}/move")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Void> move(@PathVariable Long id, @Valid @RequestBody FileMoveRequest request) {
        fileService.move(id, request);
        return ApiResponse.success();
    }

    /** 删除。 */
    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        fileService.delete(id);
        return ApiResponse.success();
    }

    /** 签发一次性下载 token（300s）。 */
    @PostMapping("/{id}/download-token")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<String> downloadToken(@PathVariable Long id) {
        return ApiResponse.success(downloadService.issueToken(id));
    }

    /** 一次性下载（匿名，凭 token；同一 token 仅可用一次）。 */
    @GetMapping("/{id}/download")
    public void download(
            @PathVariable Long id,
            @RequestParam String token,
            HttpServletResponse response) {
        downloadService.download(id, token, response);
    }

    /** 浏览器内联预览（图片 / PDF / 文本；Office 需在线编辑）。 */
    @GetMapping("/{id}/preview")
    @PreAuthorize("isAuthenticated()")
    public void preview(@PathVariable Long id, HttpServletResponse response) {
        downloadService.preview(id, response);
    }

    /** 加锁。 */
    @PostMapping("/{id}/lock")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Void> lock(@PathVariable Long id) {
        lockService.lock(id);
        return ApiResponse.success();
    }

    /** 释放锁。 */
    @PostMapping("/{id}/unlock")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Void> unlock(@PathVariable Long id) {
        lockService.unlock(id);
        return ApiResponse.success();
    }

    /** 锁续期。 */
    @PostMapping("/{id}/lock/renew")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Void> renewLock(@PathVariable Long id) {
        lockService.renew(id);
        return ApiResponse.success();
    }

    /** 查询锁状态。 */
    @GetMapping("/{id}/lock")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<DocLockVO> lockStatus(@PathVariable Long id) {
        return ApiResponse.success(lockService.status(id));
    }

    /** OnlyOffice 编辑器配置（含 JWT；不可用时返回 503 降级为下载）。 */
    @GetMapping("/{id}/editor-config")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<EditorConfigVO> editorConfig(@PathVariable Long id) {
        return ApiResponse.success(onlyOfficeService.buildEditorConfig(id));
    }

    /** 授权列表。 */
    @GetMapping("/{id}/permissions")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<PermissionVO>> permissions(@PathVariable Long id) {
        return ApiResponse.success(permissionService.list(id));
    }

    /** 新增 / 覆盖授权。 */
    @PostMapping("/{id}/permissions")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<String> grant(
            @PathVariable Long id, @Valid @RequestBody PermissionGrantRequest request) {
        return ApiResponse.success(permissionService.grant(id, request));
    }

    /** 取消授权。 */
    @DeleteMapping("/{id}/permissions/{permId}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Void> revoke(@PathVariable Long id, @PathVariable Long permId) {
        permissionService.revoke(id, permId);
        return ApiResponse.success();
    }
}
