package com.ceams.module.seal.service;

import com.ceams.common.response.PageResult;
import com.ceams.module.seal.vo.ElectronicSealUsageVO;
import java.time.LocalDateTime;
import java.util.Map;

/** 印章使用审计服务。 */
public interface ElectronicSealUsageService {

    PageResult<ElectronicSealUsageVO> page(long page, long size, Long sealId, Long userId);

    ElectronicSealUsageVO detail(Long id);

    /** 期间用印汇总（只读）：次数与用印人分布。 */
    UsageSummary summary(LocalDateTime start, LocalDateTime end, Long sealId, Long userId);

    /** 用印汇总结果。 */
    record UsageSummary(long totalCount, Map<String, Long> userDistribution) {}
}
