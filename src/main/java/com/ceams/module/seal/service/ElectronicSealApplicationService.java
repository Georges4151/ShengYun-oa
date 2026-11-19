package com.ceams.module.seal.service;

import com.ceams.common.response.PageResult;
import com.ceams.module.seal.dto.ElectronicSealApplicationCreateRequest;
import com.ceams.module.seal.dto.ElectronicSealApplicationUpdateRequest;
import com.ceams.module.seal.enums.ApplicationStatus;
import com.ceams.module.seal.vo.ElectronicSealApplicationVO;

/** 用印申请服务。 */
public interface ElectronicSealApplicationService {

    PageResult<ElectronicSealApplicationVO> page(
            long page, long size, Long sealId, ApplicationStatus status, Long applicantId);

    ElectronicSealApplicationVO detail(Long id);

    String create(ElectronicSealApplicationCreateRequest request);

    void update(Long id, ElectronicSealApplicationUpdateRequest request);

    void cancel(Long id);

    void delete(Long id);
}
