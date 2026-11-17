package com.ceams.module.doc.controller;

import com.ceams.common.response.ApiResponse;
import com.ceams.module.doc.service.DocVersionService;
import com.ceams.module.doc.vo.DocFileVO;
import com.ceams.module.doc.vo.DocVersionVO;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 文档版本接口（统一前缀 /api/v1/doc/files/{id}/versions）。
 *
 * <p>列表要求文件 PREVIEW 权；回滚要求 {@code doc:file:edit} 或文件 EDIT 权。
 */
@RestController
@RequestMapping("/api/v1/doc/files/{id}/versions")
@RequiredArgsConstructor
public class DocVersionController {

    private final DocVersionService versionService;

    /** 版本列表（versionNo 倒序）。 */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<DocVersionVO>> list(@PathVariable Long id) {
        return ApiResponse.success(versionService.list(id));
    }

    /** 回滚到指定版本（复制内容生成新版本，不删除历史）。 */
    @PostMapping("/{no}/rollback")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<DocFileVO> rollback(@PathVariable Long id, @PathVariable int no) {
        return ApiResponse.success(versionService.rollback(id, no));
    }
}
