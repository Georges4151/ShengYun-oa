package com.ceams.module.doc.vo;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

/** 分片上传初始化结果。 */
@Data
public class UploadInitVO {

    /** 上传批次 ID（秒传命中时为空）。 */
    private String uploadId;

    /** 分片大小（字节）。 */
    private Integer chunkSize;

    /** 已上传分片序号（断点续传用，升序）。 */
    private List<Integer> uploaded = new ArrayList<>();

    /** 是否秒传命中（true 时 fileId 有效、无需再传分片）。 */
    private boolean instant;

    /** 秒传命中时的新文件 ID。 */
    private Long fileId;

    /** 规范化后的文件名。 */
    private String fileName;
}
