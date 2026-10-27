package com.ceams.module.masterdata.service;

import com.ceams.common.response.PageResult;
import com.ceams.module.masterdata.dto.equipment.EquipmentCreateRequest;
import com.ceams.module.masterdata.dto.equipment.EquipmentUpdateRequest;
import com.ceams.module.masterdata.enums.EquipmentStatus;
import com.ceams.module.masterdata.vo.EquipmentVO;
import com.ceams.module.masterdata.vo.IdResponse;

/**
 * 设备台账服务。
 */
public interface EquipmentService {

    PageResult<EquipmentVO> page(long page, long size, String keyword, EquipmentStatus status);

    EquipmentVO detail(Long id);

    IdResponse create(EquipmentCreateRequest request);

    void update(Long id, EquipmentUpdateRequest request);

    void delete(Long id);
}