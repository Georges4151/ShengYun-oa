package com.ceams.module.doc.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** 分片上传初始化请求。 */
@Data
public class UploadInitRequest {

    /** 目标目录 ID。 */
    @NotNull(message = "目录 ID 不能为空")
    private Long dirId;

    /** 文件名（含扩展名，服务端只保留基础名）。 */
    @NotBlank(message = "文件名不能为空")
    private String fileName;

    /** 文件字节数。 */
    @NotNull(message = "文件大小不能为空")
    private Long size;

    /** 整文件 SHA-256（64 位 hex，可空；为空则不走秒传、完成时也不校验校验和）。 */
    private String checksum;
}
