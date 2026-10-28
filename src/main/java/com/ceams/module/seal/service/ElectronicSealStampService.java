package com.ceams.module.seal.service;

import com.ceams.common.response.PageResult;
import com.ceams.module.seal.vo.DownloadUrlVO;
import com.ceams.module.seal.vo.ElectronicSealStampVO;
import jakarta.servlet.http.HttpServletRequest;

/**
 * 用印盖章记录服务。
 */
public interface ElectronicSealStampService {

    PageResult<ElectronicSealStampVO> page(long page, long size, Long sealId, Long projectId);

    ElectronicSealStampVO detail(Long id);

    /** 生成短期有效、带签名的下载 URL。 */
    DownloadUrlVO downloadUrl(Long id);

    /** 校验下载链接签名后返回盖章 PDF 字节，并写操作留痕（文件不存在抛 BIZ_NOT_FOUND）。 */
    byte[] download(Long id, Long expires, String sign, HttpServletRequest request);
}
