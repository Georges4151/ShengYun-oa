package com.ceams.module.project.service;

import com.ceams.common.response.PageResult;
import com.ceams.module.project.dto.subcontractor.SubcontractorCreateRequest;
import com.ceams.module.project.dto.subcontractor.SubcontractorUpdateRequest;
import com.ceams.module.project.vo.IdResponse;
import com.ceams.module.project.vo.SubcontractorVO;

/**
 * 分包商服务。
 */
public interface SubcontractorService {

    PageResult<SubcontractorVO> page(long page, long size, String keyword);

    SubcontractorVO detail(Long id);

    IdResponse create(SubcontractorCreateRequest request);

    void update(Long id, SubcontractorUpdateRequest request);

    void delete(Long id);
}