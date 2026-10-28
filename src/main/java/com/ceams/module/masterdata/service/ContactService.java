package com.ceams.module.masterdata.service;

import com.ceams.common.response.PageResult;
import com.ceams.module.masterdata.dto.contact.ContactCreateRequest;
import com.ceams.module.masterdata.dto.contact.ContactUpdateRequest;
import com.ceams.module.masterdata.enums.ContactType;
import com.ceams.module.masterdata.vo.ContactVO;

/**
 * 往来单位服务。
 */
public interface ContactService {

    PageResult<ContactVO> page(long page, long size, String keyword, ContactType type);

    ContactVO detail(Long id);

    String create(ContactCreateRequest request);

    void update(Long id, ContactUpdateRequest request);

    void delete(Long id);
}