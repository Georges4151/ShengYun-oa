package com.ceams.module.seal.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

/** 短期签名下载 URL 负载。 */
@Data
@AllArgsConstructor
public class DownloadUrlVO {

    /** 带签名、短期有效的下载 URL（相对路径）。 */
    private String url;

    /** URL 过期时间（epoch 毫秒）。 */
    private long expiresAt;
}
