package com.ceams.module.doc.service;

import com.ceams.module.doc.dto.UploadInitRequest;
import com.ceams.module.doc.vo.DocFileVO;
import com.ceams.module.doc.vo.UploadInitVO;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

/** 文档分片上传服务（初始化 / 上传分片 / 合并落库）。 */
public interface DocUploadService {

    /**
     * 初始化上传：校验目录与文件限制，命中 SHA-256 秒传时直接建元数据。
     *
     * @param request 目录、文件名、大小、可选 checksum
     * @return 上传批次信息（秒传命中时 {@code instant=true} 且返回 fileId）
     */
    UploadInitVO init(UploadInitRequest request);

    /**
     * 保存单个分片。
     *
     * @param uploadId 上传批次 ID
     * @param index 分片序号（从 0 起）
     * @param file 分片内容
     * @return 已上传分片序号（升序）
     */
    List<Integer> saveChunk(String uploadId, int index, MultipartFile file);

    /**
     * 合并分片并落库（含 SHA-256 校验、v1 版本与审计）。
     *
     * @param uploadId 上传批次 ID
     * @return 新文件元数据
     */
    DocFileVO complete(String uploadId);
}
