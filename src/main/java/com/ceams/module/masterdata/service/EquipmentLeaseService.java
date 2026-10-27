package com.ceams.module.masterdata.service;

import com.ceams.common.response.PageResult;
import com.ceams.module.masterdata.dto.lease.EquipmentLeaseCreateRequest;
import com.ceams.module.masterdata.dto.lease.EquipmentLeaseUpdateRequest;
import com.ceams.module.masterdata.vo.EquipmentLeaseVO;
import com.ceams.module.masterdata.vo.IdResponse;

/**
 * 设备租赁服务。
 */
public interface EquipmentLeaseService {

    PageResult<EquipmentLeaseVO> page(long page, long size, Long equipmentId, Long projectId);

    EquipmentLeaseVO detail(Long id);

    IdResponse create(EquipmentLeaseCreateRequest request);

    void update(Long id, EquipmentLeaseUpdateRequest request);

    void delete(Long id);
}