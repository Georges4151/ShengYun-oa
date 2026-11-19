package com.ceams.module.ai.tool;

import com.ceams.module.ai.config.AiProperties;
import com.ceams.module.doc.service.DocKnowledgeSearchService;
import com.ceams.module.doc.support.KnowledgeHit;
import com.ceams.module.doc.support.KnowledgeQuery;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 文档知识库检索只读工具。
 *
 * <p>返回片段已由 {@code DocKnowledgeSearchService} 完成「先收窄再召回」与 PREVIEW 复核，
 * 只含文件名、目录路径与片段文本；模型必须逐条标注引用编号，不得编造未命中的内容。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SearchKnowledgeBaseTool implements ReadOnlyTool {

    /** 默认返回片段数。 */
    private static final int DEFAULT_TOP_K = 5;

    /** 片段数上限。 */
    private static final int MAX_TOP_K = 10;

    private final DocKnowledgeSearchService knowledgeSearchService;
    private final AiProperties aiProperties;

    @Override
    public String name() {
        return "searchKnowledgeBase";
    }

    @Override
    public String description() {
        return "检索文档知识库原文片段（制度、规范、流程、合同条款等）。"
                + "query 为检索关键词或用户问题；libraryType 可选 PERSONAL/DEPT/PROJECT/PUBLIC；"
                + "projectId 可选；topK 默认 5、最大 10。"
                + "回答必须引用返回片段的编号（如 [1]），不得编造片段之外的内容；"
                + "检索不到时必须明确说明未找到相关文档。";
    }

    @Override
    public Map<String, Object> parameterSchema() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("query", ToolSchemas.text("检索关键词或用户问题，必填"));
        properties.put(
                "libraryType",
                ToolSchemas.choice(
                        "库类型过滤，可选", "PERSONAL", "DEPT", "PROJECT", "PUBLIC"));
        properties.put("projectId", ToolSchemas.text("项目 ID，可传数字或数字字符串，可选"));
        properties.put("topK", ToolSchemas.integer("返回片段数，默认 5，最大 10", 1, MAX_TOP_K));
        return ToolSchemas.object(properties, "query");
    }

    @Override
    public ToolResult execute(Map<String, Object> args) {
        if (ToolSecurity.currentUserId() == null) {
            return ToolResult.denied("请先登录后再检索知识库");
        }
        String query = ToolArgs.text(args, "query");
        if (query == null) {
            return ToolResult.denied("请提供检索关键词");
        }
        Integer topK = ToolArgs.integer(args, "topK");
        int limit = topK == null ? DEFAULT_TOP_K : Math.min(Math.max(topK, 1), MAX_TOP_K);
        String libraryType = ToolArgs.text(args, "libraryType");
        Long projectId = ToolArgs.longValue(args, "projectId");

        List<KnowledgeHit> hits =
                knowledgeSearchService.search(
                        new KnowledgeQuery(query, limit, libraryType, projectId));
        List<Map<String, Object>> rows = new ArrayList<>();
        int citationNo = 1;
        for (KnowledgeHit hit : hits) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("citationNo", citationNo++);
            row.put("fileId", hit.fileId());
            row.put("dirId", hit.dirId());
            row.put("libraryType", hit.libraryType());
            row.put("fileName", hit.fileName());
            row.put("directoryPath", hit.directoryPath());
            row.put("snippet", hit.snippet());
            rows.add(row);
        }
        if (rows.isEmpty()) {
            log.debug("知识库检索无命中：query={}", query);
            return ToolResult.empty();
        }
        return ToolResult.capped(rows, rows.size(), aiProperties.getMaxToolRows());
    }
}
