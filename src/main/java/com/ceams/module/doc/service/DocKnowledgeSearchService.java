package com.ceams.module.doc.service;

import com.ceams.module.doc.support.KnowledgeHit;
import com.ceams.module.doc.support.KnowledgeQuery;
import java.util.List;

/**
 * 文档知识库检索服务（只读）。
 *
 * <p>权限口径为「先收窄再召回」：先通过 {@code DocAccessResolver#accessibleFileIds} 得到当前用户
 * 可访问的文件集合，召回 SQL 只在集合内检索；入选结果再逐文件复核 {@code DocAction.PREVIEW}。
 * 绝不返回 storageKey、checksum 或物理路径。
 */
public interface DocKnowledgeSearchService {

    /**
     * 检索知识库片段。
     *
     * @param query 检索条件（{@code text} 空白或未登录返回空列表）
     * @return 命中片段列表（相关度倒序，每个文件最多一条）
     */
    List<KnowledgeHit> search(KnowledgeQuery query);
}
