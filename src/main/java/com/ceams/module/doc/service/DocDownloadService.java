package com.ceams.module.doc.service;

import jakarta.servlet.http.HttpServletResponse;

/** 文档下载与预览服务（一次性 token + 浏览器直出）。 */
public interface DocDownloadService {

    /** 签发一次性下载 token（要求 DOWNLOAD 或 PREVIEW 权）。 */
    String issueToken(Long fileId);

    /** 一次性下载：token 校验通过后流式输出并累加下载次数。 */
    void download(Long fileId, String token, HttpServletResponse response);

    /** 浏览器内联预览（图片 / PDF / 文本；Office 与压缩包拒绝）。 */
    void preview(Long fileId, HttpServletResponse response);
}
