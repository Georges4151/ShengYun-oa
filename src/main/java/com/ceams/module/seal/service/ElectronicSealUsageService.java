package com.ceams.module.seal.service;

import com.ceams.common.response.PageResult;
import com.ceams.module.seal.vo.ElectronicSealUsageVO;

/**
 * 印章使用审计服务。
 */
public interface ElectronicSealUsageService {

    PageResult<ElectronicSealUsageVO> page(long page, long size, Long sealId, Long userId);

    ElectronicSealUsageVO detail(Long id);
}
