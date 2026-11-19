package com.ceams.module.doc.support;

/**
 * 知识库检索命中片段（对外只读视图）。
 *
 * <p>安全约定：只暴露文件 / 目录标识、文件名、目录路径与片段内容，绝不包含 storageKey、
 * checksum 或物理路径；且每条命中在返回前均已通过 {@code DocAction.PREVIEW} 权限判定。
 *
 * @param fileId 文件 ID（字符串，避免前端 / 模型精度丢失）
 * @param dirId 目录 ID（字符串）
 * @param libraryType 库类型（PERSONAL/DEPT/PROJECT/PUBLIC，可空）
 * @param fileName 文件名（含扩展名）
 * @param directoryPath 目录路径（目录名按层级用 " / " 连接）
 * @param chunkNo 分块序号（同一文件内从 0 递增）
 * @param snippet 命中片段（已压缩空白并按关键词截取）
 * @param score 相关度（全文检索得分，LIKE 兜底与文件名补充项为 0）
 */
public record KnowledgeHit(
        String fileId,
        String dirId,
        String libraryType,
        String fileName,
        String directoryPath,
        int chunkNo,
        String snippet,
        double score) {}
