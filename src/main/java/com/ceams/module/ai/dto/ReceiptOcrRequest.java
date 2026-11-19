package com.ceams.module.ai.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** 票据图片识别请求。 */
@Data
public class ReceiptOcrRequest {

    /**
     * 票据文件路径：支持纯文件名（如 {@code 1752560000000000001.png}）， 也支持上传接口返回的 URL（如 {@code
     * /api/v1/files/1752560000000000001.png}）。
     */
    @NotBlank(message = "文件路径不能为空")
    private String filePath;
}
