package com.ceams.module.seal.service;

import com.ceams.module.seal.dto.SealStampRequest;
import com.ceams.module.seal.vo.SealStampResultVO;

/**
 * PDF 盖章服务（HMAC 高危接口）。
 */
public interface SealStampService {

    SealStampResultVO stamp(SealStampRequest request);
}
