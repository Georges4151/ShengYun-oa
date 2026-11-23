package com.ceams.module.project.service;

import com.ceams.common.response.PageResult;
import com.ceams.module.project.dto.subcontractorcontract.SubcontractorContractCreateRequest;
import com.ceams.module.project.dto.subcontractorcontract.SubcontractorContractUpdateRequest;
import com.ceams.module.project.vo.SubcontractorContractVO;
import java.io.OutputStream;

/** 分包合同服务。 */
public interface SubcontractorContractService {

    PageResult<SubcontractorContractVO> page(
            long page, long size, Long subcontractorId, Long projectId);

    SubcontractorContractVO detail(Long id);

    String create(SubcontractorContractCreateRequest request);

    void update(Long id, SubcontractorContractUpdateRequest request);

    void delete(Long id);

    void export(OutputStream out, Long subcontractorId, Long projectId);

    /**
     * 提交分包合同审批。
     *
     * @param id 合同 ID
     * @return BPM 流程实例 ID（字符串，避免前端 Long 精度丢失）
     */
    String submitApproval(Long id);
}
