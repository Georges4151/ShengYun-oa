package com.ceams.module.doc.controller;

import com.ceams.common.exception.BusinessException;
import com.ceams.common.exception.ErrorCode;
import com.ceams.common.response.ApiResponse;
import com.ceams.module.doc.service.DocIndexService;
import com.ceams.module.doc.service.DocKnowledgeSearchService;
import com.ceams.module.doc.support.DocAccessResolver;
import com.ceams.module.doc.support.KnowledgeHit;
import com.ceams.module.doc.support.KnowledgeQuery;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 文档知识库接口（检索与索引重建）。
 *
 * <p>安全口径：
 *
 * <ul>
 *   <li>{@code GET /api/v1/doc/search} 仅要求登录，权限由服务层「先收窄再召回」保证，
 *       返回片段不含存储键 / 校验和 / 物理路径；
 *   <li>{@code POST /api/v1/doc/knowledge/reindex} 要求文档权限管理权限（服务层校验），
 *       异步全量重建，接口只负责提交任务。
 * </ul>
 */
@RestController
@RequiredArgsConstructor
public class DocKnowledgeController {

    private final DocKnowledgeSearchService knowledgeSearchService;
    private final DocIndexService docIndexService;
    private final DocAccessResolver docAccessResolver;

    /**
     * 检索知识库片段。
     *
     * @param q 检索关键词（空白返回空列表）
     * @param limit 返回条数（默认 5，服务层夹在 1 ~ 10）
     * @param libraryType 库类型过滤（PERSONAL/DEPT/PROJECT/PUBLIC，可空）
     * @param projectId 项目过滤（可空）
     * @return 命中片段（相关度倒序，每个文件最多一条）
     */
    @GetMapping("/api/v1/doc/search")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<KnowledgeHit>> search(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "5") int limit,
            @RequestParam(required = false) String libraryType,
            @RequestParam(required = false) Long projectId) {
        KnowledgeQuery query = new KnowledgeQuery(q, limit, libraryType, projectId);
        return ApiResponse.success(knowledgeSearchService.search(query));
    }

    /** 全量重建知识库索引（异步，需要文档权限管理权限，服务层校验）。 */
    @PostMapping("/api/v1/doc/knowledge/reindex")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Void> reindex() {
        if (!docAccessResolver.isFullAccess()) {
            throw BusinessException.of(ErrorCode.AUTH_ACCESS_DENIED, "无权重建知识库索引");
        }
        docIndexService.reindexAll();
        return ApiResponse.success();
    }
}
