package com.ceams.module.doc.support;

/**
 * 知识库检索入参。
 *
 * <p>{@code libraryType} 使用字符串（PERSONAL/DEPT/PROJECT/PUBLIC），便于直接承接 REST 与模型参数；
 * 解析失败由服务层抛 {@code VALID_ILLEGAL_ARGUMENT}，不做静默降级。
 *
 * @param text 检索关键词 / 问题（必填，空白返回空结果）
 * @param limit 期望返回条数（服务层夹在 1 ~ {@code ceams.doc.rag.max-search-limit}）
 * @param libraryType 库类型过滤，可空
 * @param projectId 项目过滤，可空
 */
public record KnowledgeQuery(String text, int limit, String libraryType, Long projectId) {}
