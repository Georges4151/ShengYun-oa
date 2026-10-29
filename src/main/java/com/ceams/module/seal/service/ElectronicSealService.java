package com.ceams.module.seal.service;

import com.ceams.common.response.PageResult;
import com.ceams.module.seal.dto.ElectronicSealCreateRequest;
import com.ceams.module.seal.dto.ElectronicSealUpdateRequest;
import com.ceams.module.seal.enums.SealStatus;
import com.ceams.module.seal.enums.StorageType;
import com.ceams.module.seal.vo.ElectronicSealVO;

/** 电子印章服务。 */
public interface ElectronicSealService {

    PageResult<ElectronicSealVO> page(
            long page, long size, String name, SealStatus status, StorageType storageType);

    ElectronicSealVO detail(Long id);

    String create(ElectronicSealCreateRequest request);

    void update(Long id, ElectronicSealUpdateRequest request);

    void delete(Long id);
}
