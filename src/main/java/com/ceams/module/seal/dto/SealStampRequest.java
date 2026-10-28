package com.ceams.module.seal.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 盖章请求（HMAC 高危接口，JSON body）。
 */
@Data
public class SealStampRequest {

    /** 印章 ID。 */
    @NotNull(message = "印章ID不能为空")
    private Long sealId;

    /** 用印申请 ID。 */
    @NotNull(message = "用印申请ID不能为空")
    private Long applicationId;

    /** 源 PDF 的 base64。 */
    @NotBlank(message = "源PDF的base64不能为空")
    private String sourcePdfBase64;

    /** 印章图片 PNG/JPEG 的 base64。 */
    @NotBlank(message = "印章图片的base64不能为空")
    private String sealImageBase64;

    /** 盖章位置（可选，格式 x,y 如 100,200）。 */
    private String stampPosition;
}
