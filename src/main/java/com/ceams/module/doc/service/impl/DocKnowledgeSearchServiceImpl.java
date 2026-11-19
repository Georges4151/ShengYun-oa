package com.ceams.module.doc.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ceams.common.exception.BusinessException;
import com.ceams.common.exception.ErrorCode;
import com.ceams.common.security.SecurityUtils;
import com.ceams.module.doc.config.DocProperties;
import com.ceams.module.doc.dto.ChunkSearchHit;
import com.ceams.module.doc.entity.DocChunk;
import com.ceams.module.doc.entity.DocDirectory;
import com.ceams.module.doc.entity.DocFile;
import com.ceams.module.doc.entity.DocFileText;
import com.ceams.module.doc.enums.DocAction;
import com.ceams.module.doc.enums.LibraryType;
import com.ceams.module.doc.mapper.DocChunkMapper;
import com.ceams.module.doc.mapper.DocDirectoryMapper;
import com.ceams.module.doc.mapper.DocFileMapper;
import com.ceams.module.doc.mapper.DocFileTextMapper;
import com.ceams.module.doc.service.DocKnowledgeSearchService;
import com.ceams.module.doc.support.DocAccessResolver;
import com.ceams.module.doc.support.KnowledgeHit;
import com.ceams.module.doc.support.KnowledgeQuery;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

/**
 * 文档知识库检索实现（M2：关键词全文 / LIKE 召回，M3 再切换向量检索）。
 *
 * <p>检索链路（严格「先收窄再召回」）：
 *
 * <ol>
 *   <li>未登录、关键词为空或可访问文件集合为空 → 直接返回空，不执行任何召回 SQL；
 *   <li>候选召回只在 {@code accessibleFileIds} 内检索：全文（关键词长度 ≥ 2）优先，
 *       无命中再 LIKE 兜底；
 *   <li>文件名命中作为补充项排在后面（无分块时取抽取文本前 300 字）；
 *   <li>每个文件只取一个片段，按 score 倒序累计到 limit；
 *   <li>入选文件再经 {@code DocAccessResolver#resolve} 复核 PREVIEW，不通过直接丢弃。
 * </ol>
 *
 * <p>输出只含文件名、目录路径与片段文本，绝不返回存储键、校验和或物理路径。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocKnowledgeSearchServiceImpl implements DocKnowledgeSearchService {

    /** 全文检索触发的最短关键词长度（单字改用 LIKE，避免 ngram 噪声）。 */
    private static final int FULLTEXT_MIN_LENGTH = 2;

    /** 文件名补充召回的最大条数。 */
    private static final int FILE_NAME_LIMIT = 20;

    /** 片段总长度（字符）。 */
    private static final int SNIPPET_LENGTH = 260;

    /** 命中词之前保留的字符数。 */
    private static final int SNIPPET_LEAD = 60;

    /** 无分块时取抽取文本的预览长度。 */
    private static final int TEXT_PREVIEW_LENGTH = 300;

    /** 片段截断标记。 */
    private static final String ELLIPSIS = "…";

    /** 目录层级分隔符。 */
    private static final String PATH_SEPARATOR = " / ";

    private final DocAccessResolver accessResolver;
    private final DocChunkMapper chunkMapper;
    private final DocFileMapper fileMapper;
    private final DocFileTextMapper fileTextMapper;
    private final DocDirectoryMapper dirMapper;
    private final DocProperties docProperties;

    @Override
    public List<KnowledgeHit> search(KnowledgeQuery query) {
        if (query == null || StringUtils.isBlank(query.text())) {
            return List.of();
        }
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            return List.of();
        }
        String keyword = query.text().trim();
        int limit = resolveLimit(query.limit());

        LibraryType libraryType = parseLibraryType(query.libraryType());
        Set<Long> accessibleFileIds =
                accessResolver.accessibleFileIds(userId, libraryType, query.projectId());
        if (accessibleFileIds == null || accessibleFileIds.isEmpty()) {
            // 无可见文件时不得先召回后过滤，直接返回空
            return List.of();
        }

        List<Candidate> candidates = recallChunks(keyword, accessibleFileIds);
        candidates.addAll(recallByFileName(keyword, accessibleFileIds));

        List<Candidate> selected = pickOnePerFile(candidates, limit);
        if (selected.isEmpty()) {
            return List.of();
        }

        Map<Long, DocFile> files = loadFiles(selected);
        DirectoryView directories = loadDirectoryView(files.values());

        List<KnowledgeHit> hits = new ArrayList<>(selected.size());
        for (Candidate candidate : selected) {
            DocFile file = files.get(candidate.fileId());
            if (file == null) {
                continue;
            }
            DocDirectory dir = directories.dirs().get(file.getDirId());
            // 防御性复核：与 resolve() 口径一致，防止授权在召回后被收回
            if (!accessResolver.resolve(userId, dir, file).contains(DocAction.PREVIEW)) {
                log.debug("知识库检索命中被权限复核丢弃：fileId={}", candidate.fileId());
                continue;
            }
            hits.add(
                    new KnowledgeHit(
                            String.valueOf(file.getId()),
                            file.getDirId() == null ? null : String.valueOf(file.getDirId()),
                            dir == null || dir.getLibraryType() == null
                                    ? null
                                    : dir.getLibraryType().name(),
                            file.getName(),
                            directories.paths().getOrDefault(file.getDirId(), ""),
                            candidate.chunkNo(),
                            snippet(candidate.content(), keyword),
                            candidate.score()));
        }
        return hits;
    }

    // ------------------------------------------------------------------
    // 召回
    // ------------------------------------------------------------------

    /** 分块召回：关键词长度 ≥ 2 先全文后 LIKE，否则直接 LIKE。 */
    private List<Candidate> recallChunks(String keyword, Set<Long> accessibleFileIds) {
        int candidateLimit = resolveCandidateLimit();
        List<ChunkSearchHit> hits;
        if (keyword.length() >= FULLTEXT_MIN_LENGTH) {
            hits = chunkMapper.searchFulltext(keyword, accessibleFileIds, candidateLimit);
            if (hits == null || hits.isEmpty()) {
                hits = chunkMapper.searchLike(keyword, accessibleFileIds, candidateLimit);
            }
        } else {
            hits = chunkMapper.searchLike(keyword, accessibleFileIds, candidateLimit);
        }
        List<Candidate> candidates = new ArrayList<>();
        if (hits == null) {
            return candidates;
        }
        for (ChunkSearchHit hit : hits) {
            if (hit == null || hit.getFileId() == null) {
                continue;
            }
            candidates.add(
                    new Candidate(
                            hit.getFileId(),
                            hit.getChunkNo() == null ? 0 : hit.getChunkNo(),
                            hit.getContent(),
                            hit.getScore()));
        }
        return candidates;
    }

    /**
     * 文件名补充召回：先按名称模糊查出候选文件（最多 20 条），再剔除不可访问文件。
     *
     * <p>补充项 score 固定 0，排在分块命中之后；无分块时取抽取文本前 300 字。
     */
    private List<Candidate> recallByFileName(String keyword, Set<Long> accessibleFileIds) {
        List<DocFile> named =
                fileMapper.selectList(
                        new LambdaQueryWrapper<DocFile>()
                                .select(DocFile::getId, DocFile::getDirId, DocFile::getName)
                                .like(DocFile::getName, keyword)
                                .orderByDesc(DocFile::getUpdateTime)
                                .last("LIMIT " + FILE_NAME_LIMIT));
        List<Candidate> candidates = new ArrayList<>();
        if (named == null) {
            return candidates;
        }
        for (DocFile file : named) {
            if (file == null || file.getId() == null || !accessibleFileIds.contains(file.getId())) {
                continue;
            }
            DocChunk first = firstChunk(file.getId());
            if (first != null) {
                candidates.add(
                        new Candidate(
                                file.getId(),
                                first.getChunkNo() == null ? 0 : first.getChunkNo(),
                                first.getContent(),
                                0d));
                continue;
            }
            String text = extractedTextPreview(file.getId());
            if (StringUtils.isNotBlank(text)) {
                candidates.add(new Candidate(file.getId(), 0, text, 0d));
            }
        }
        return candidates;
    }

    /** 文件最小 chunk_no 的分块（无分块返回 null）。 */
    private DocChunk firstChunk(Long fileId) {
        return chunkMapper.selectOne(
                new LambdaQueryWrapper<DocChunk>()
                        .eq(DocChunk::getFileId, fileId)
                        .orderByAsc(DocChunk::getChunkNo)
                        .last("LIMIT 1"));
    }

    /** 抽取文本前 300 字（无抽取结果返回 null）。 */
    private String extractedTextPreview(Long fileId) {
        DocFileText text =
                fileTextMapper.selectOne(
                        new LambdaQueryWrapper<DocFileText>()
                                .eq(DocFileText::getFileId, fileId)
                                .last("LIMIT 1"));
        if (text == null || StringUtils.isBlank(text.getContent())) {
            return null;
        }
        String content = text.getContent().trim();
        return content.length() <= TEXT_PREVIEW_LENGTH
                ? content
                : content.substring(0, TEXT_PREVIEW_LENGTH);
    }

    /** 每个文件只保留第一条（候选已按 score 倒序），累计到 limit。 */
    private List<Candidate> pickOnePerFile(List<Candidate> candidates, int limit) {
        Map<Long, Candidate> byFile = new LinkedHashMap<>();
        for (Candidate candidate : candidates) {
            if (candidate == null || candidate.fileId() == null) {
                continue;
            }
            byFile.putIfAbsent(candidate.fileId(), candidate);
        }
        List<Candidate> selected = new ArrayList<>();
        for (Candidate candidate : byFile.values()) {
            if (selected.size() >= limit) {
                break;
            }
            selected.add(candidate);
        }
        return selected;
    }

    // ------------------------------------------------------------------
    // 目录与片段
    // ------------------------------------------------------------------

    /** 批量加载命中文件（selectBatchIds 一次查询）。 */
    private Map<Long, DocFile> loadFiles(List<Candidate> selected) {
        Set<Long> fileIds = new LinkedHashSet<>();
        for (Candidate candidate : selected) {
            fileIds.add(candidate.fileId());
        }
        Map<Long, DocFile> files = new LinkedHashMap<>();
        if (fileIds.isEmpty()) {
            return files;
        }
        List<DocFile> loaded = fileMapper.selectBatchIds(fileIds);
        if (loaded == null) {
            return files;
        }
        for (DocFile file : loaded) {
            if (file != null && file.getId() != null) {
                files.put(file.getId(), file);
            }
        }
        return files;
    }

    /**
     * 构造目录视图：目录 ID → 目录实体（用于权限复核）与 目录 ID → 目录名路径。
     *
     * <p>路径上的全部目录一次性 IN 查询，避免逐条反查。
     */
    private DirectoryView loadDirectoryView(Collection<DocFile> files) {
        Map<Long, DocDirectory> dirsById = new HashMap<>();
        Map<Long, String> paths = new HashMap<>();
        Set<Long> dirIds = new LinkedHashSet<>();
        for (DocFile file : files) {
            if (file != null && file.getDirId() != null) {
                dirIds.add(file.getDirId());
            }
        }
        if (dirIds.isEmpty()) {
            return new DirectoryView(dirsById, paths);
        }
        List<DocDirectory> dirs =
                dirMapper.selectList(
                        new LambdaQueryWrapper<DocDirectory>().in(DocDirectory::getId, dirIds));
        if (dirs == null || dirs.isEmpty()) {
            return new DirectoryView(dirsById, paths);
        }
        // 先收集所有路径上的目录 ID，再一次性加载名称
        Set<Long> allIds = new LinkedHashSet<>(dirIds);
        for (DocDirectory dir : dirs) {
            if (dir != null) {
                allIds.addAll(parsePathIds(dir.getPath()));
            }
        }
        List<DocDirectory> all = allIds.isEmpty() ? List.of() : dirMapper.selectBatchIds(allIds);
        Map<Long, String> nameById = new HashMap<>();
        if (all != null) {
            for (DocDirectory dir : all) {
                if (dir != null && dir.getId() != null) {
                    nameById.put(dir.getId(), dir.getName());
                    if (dirIds.contains(dir.getId())) {
                        dirsById.put(dir.getId(), dir);
                    }
                }
            }
        }
        for (Long dirId : dirIds) {
            DocDirectory dir = dirsById.get(dirId);
            if (dir != null) {
                paths.put(dirId, joinPath(dir, nameById));
            }
        }
        return new DirectoryView(dirsById, paths);
    }

    /** 按路径顺序拼接目录名；缺失名称忽略。 */
    private String joinPath(DocDirectory dir, Map<Long, String> nameById) {
        List<String> names = new ArrayList<>();
        for (Long id : parsePathIds(dir.getPath())) {
            String name = nameById.get(id);
            if (StringUtils.isNotBlank(name)) {
                names.add(name);
            }
        }
        if (names.isEmpty() && StringUtils.isNotBlank(dir.getName())) {
            names.add(dir.getName());
        }
        return String.join(PATH_SEPARATOR, names);
    }

    /** 解析 {@code /1/2/3/} 形式的目录路径，非法片段忽略。 */
    private List<Long> parsePathIds(String path) {
        List<Long> ids = new ArrayList<>();
        if (StringUtils.isBlank(path)) {
            return ids;
        }
        for (String token : path.split("/")) {
            if (StringUtils.isBlank(token)) {
                continue;
            }
            try {
                ids.add(Long.valueOf(token.trim()));
            } catch (NumberFormatException e) {
                log.debug("目录路径存在非法片段，已忽略：{}", token);
            }
        }
        return ids;
    }

    /**
     * 生成片段：空白压成单空格，定位关键词首次出现（忽略大小写）向前 60 字、总长约 260 字，
     * 两端超出时补「…」；未命中取前 260 字。
     */
    private String snippet(String content, String keyword) {
        String normalized = content == null ? "" : content.replaceAll("\\s+", " ").trim();
        if (normalized.isEmpty()) {
            return "";
        }
        int index = indexOfIgnoreCase(normalized, keyword);
        if (index < 0) {
            return cutTail(normalized, 0, Math.min(normalized.length(), SNIPPET_LENGTH));
        }
        int start = Math.max(0, index - SNIPPET_LEAD);
        int end = Math.min(normalized.length(), start + SNIPPET_LENGTH);
        if (end - start < SNIPPET_LENGTH) {
            start = Math.max(0, end - SNIPPET_LENGTH);
        }
        return cutTail(normalized, start, end);
    }

    /** 截取 [start, end) 并按需补省略号。 */
    private String cutTail(String text, int start, int end) {
        String piece = text.substring(start, end);
        StringBuilder builder = new StringBuilder(piece);
        if (start > 0) {
            builder.insert(0, ELLIPSIS);
        }
        if (end < text.length()) {
            builder.append(ELLIPSIS);
        }
        return builder.toString();
    }

    /** 忽略大小写查找首次出现位置，未命中返回 -1。 */
    private int indexOfIgnoreCase(String text, String keyword) {
        if (StringUtils.isBlank(keyword)) {
            return -1;
        }
        return text.toLowerCase(Locale.ROOT).indexOf(keyword.toLowerCase(Locale.ROOT));
    }

    // ------------------------------------------------------------------
    // 参数与配置
    // ------------------------------------------------------------------

    /** limit 夹在 1 ~ maxSearchLimit；非正值取默认值。 */
    private int resolveLimit(int limit) {
        DocProperties.Rag rag = rag();
        int max = Math.max(1, rag.getMaxSearchLimit());
        int value = limit > 0 ? limit : Math.max(1, rag.getSearchDefaultLimit());
        return Math.min(value, max);
    }

    /** 候选召回条数：取配置值，非法配置回退 300。 */
    private int resolveCandidateLimit() {
        DocProperties.Rag rag = rag();
        int configured = rag.getSearchCandidateLimit();
        return configured > 0 ? configured : 300;
    }

    /** 库类型解析：空表示不过滤，非法值抛参数错误。 */
    private LibraryType parseLibraryType(String raw) {
        if (StringUtils.isBlank(raw)) {
            return null;
        }
        LibraryType type = LibraryType.from(raw);
        if (type == null) {
            throw BusinessException.of(ErrorCode.VALID_ILLEGAL_ARGUMENT, "非法的库类型：" + raw);
        }
        return type;
    }

    private DocProperties.Rag rag() {
        return docProperties.getRag() == null ? new DocProperties.Rag() : docProperties.getRag();
    }

    /** 候选片段（不含授权信息，目录实体在返回阶段统一加载）。 */
    private record Candidate(Long fileId, int chunkNo, String content, double score) {}

    /** 目录视图：目录实体（权限复核）与目录名路径（展示）。 */
    private record DirectoryView(Map<Long, DocDirectory> dirs, Map<Long, String> paths) {}
}
