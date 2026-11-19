package com.ceams.module.doc.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ceams.module.doc.dto.ChunkSearchHit;
import com.ceams.module.doc.entity.DocChunk;
import java.util.Collection;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 文档分块 Mapper（含全文 / 模糊候选召回）。
 *
 * <p>{@code fileIds} 必须是调用方已完成权限收窄的文件 ID 集合，Mapper 只做召回，不做任何授权判断。
 */
@Mapper
public interface DocChunkMapper extends BaseMapper<DocChunk> {

    /**
     * ngram 全文检索候选（按相关度倒序）。
     *
     * @param q 检索词（方法内全部参数化）
     * @param fileIds 已收窄的可访问文件 ID 集合，不得为空集合
     * @param limit 返回上限
     * @return 候选分块（仍需服务层做防御性权限复核）
     */
    List<ChunkSearchHit> searchFulltext(
            @Param("q") String q, @Param("fileIds") Collection<Long> fileIds, @Param("limit") int limit);

    /**
     * LIKE 兜底召回（短词 / 全文索引不可用时，score 固定 0）。
     *
     * @param q 检索词（方法内全部参数化）
     * @param fileIds 已收窄的可访问文件 ID 集合，不得为空集合
     * @param limit 返回上限
     * @return 候选分块（仍需服务层做防御性权限复核）
     */
    List<ChunkSearchHit> searchLike(
            @Param("q") String q, @Param("fileIds") Collection<Long> fileIds, @Param("limit") int limit);
}
