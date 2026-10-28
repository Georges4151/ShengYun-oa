package com.ceams.module.seal.service;

import com.ceams.common.response.PageResult;
import com.ceams.module.seal.vo.ElectronicSealStampVO;

/**
 * 用印盖章记录服务。
 */
public interface ElectronicSealStampService {

    PageResult<ElectronicSealStampVO> page(long page, long size, Long sealId, Long projectId);

    ElectronicSealStampVO detail(Long id);

    /** 下载盖章 PDF 字节（文件不存在抛 BIZ_NOT_FOUND）。 */
    byte[] download(Long id);
}
