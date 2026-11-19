package com.ceams.module.masterdata.service;

import com.ceams.common.response.PageResult;
import com.ceams.module.masterdata.dto.material.MaterialCreateRequest;
import com.ceams.module.masterdata.dto.material.MaterialUpdateRequest;
import com.ceams.module.masterdata.vo.MaterialVO;

/** 材料服务。 */
public interface MaterialService {

    PageResult<MaterialVO> page(long page, long size, String keyword, Long categoryId);

    MaterialVO detail(Long id);

    String create(MaterialCreateRequest request);

    void update(Long id, MaterialUpdateRequest request);

    void delete(Long id);
}
