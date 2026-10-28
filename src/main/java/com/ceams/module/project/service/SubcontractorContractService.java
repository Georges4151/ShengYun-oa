package com.ceams.module.project.service;

import com.ceams.common.response.PageResult;
import com.ceams.module.project.dto.subcontractorcontract.SubcontractorContractCreateRequest;
import com.ceams.module.project.dto.subcontractorcontract.SubcontractorContractUpdateRequest;
import com.ceams.module.project.vo.SubcontractorContractVO;

/**
 * 分包合同服务。
 */
public interface SubcontractorContractService {

    PageResult<SubcontractorContractVO> page(long page, long size, Long subcontractorId, Long projectId);

    SubcontractorContractVO detail(Long id);

    String create(SubcontractorContractCreateRequest request);

    void update(Long id, SubcontractorContractUpdateRequest request);

    void delete(Long id);
}