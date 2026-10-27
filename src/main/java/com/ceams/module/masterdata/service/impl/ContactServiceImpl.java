package com.ceams.module.masterdata.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ceams.common.exception.BusinessException;
import com.ceams.common.exception.ErrorCode;
import com.ceams.common.response.PageResult;
import com.ceams.module.masterdata.converter.ContactConverter;
import com.ceams.module.masterdata.dto.contact.ContactCreateRequest;
import com.ceams.module.masterdata.dto.contact.ContactUpdateRequest;
import com.ceams.module.masterdata.entity.Contact;
import com.ceams.module.masterdata.entity.EquipmentLease;
import com.ceams.module.masterdata.entity.Material;
import com.ceams.module.masterdata.enums.ContactType;
import com.ceams.module.masterdata.mapper.ContactMapper;
import com.ceams.module.masterdata.mapper.EquipmentLeaseMapper;
import com.ceams.module.masterdata.mapper.MaterialMapper;
import com.ceams.module.masterdata.service.ContactService;
import com.ceams.module.masterdata.util.PageUtil;
import com.ceams.module.masterdata.vo.ContactVO;
import com.ceams.module.masterdata.vo.IdResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 往来单位服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContactServiceImpl implements ContactService {

    private final ContactMapper contactMapper;
    private final MaterialMapper materialMapper;
    private final EquipmentLeaseMapper equipmentLeaseMapper;
    private final ContactConverter contactConverter;

    @Override
    public PageResult<ContactVO> page(long page, long size, String keyword, ContactType type) {
        Page<Contact> p = PageUtil.resolve(page, size);
        LambdaQueryWrapper<Contact> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.isNotBlank(keyword)) {
            wrapper.and(w -> w.like(Contact::getName, keyword)
                    .or().like(Contact::getContactPerson, keyword)
                    .or().like(Contact::getPhone, keyword));
        }
        if (type != null) {
            wrapper.eq(Contact::getType, type);
        }
        wrapper.orderByDesc(Contact::getCreateTime);
        IPage<Contact> result = contactMapper.selectPage(p, wrapper);
        return new PageResult<>(contactConverter.toVOList(result.getRecords()),
                result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    public ContactVO detail(Long id) {
        return contactConverter.toVO(requireContact(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public IdResponse create(ContactCreateRequest request) {
        Contact contact = new Contact();
        contact.setName(request.getName());
        contact.setType(request.getType());
        contact.setContactPerson(request.getContactPerson());
        contact.setPhone(request.getPhone());
        contact.setEmail(request.getEmail());
        contact.setAddress(request.getAddress());
        contact.setTaxIdentificationNumber(request.getTaxIdentificationNumber());
        contact.setBankAccount(request.getBankAccount());
        contactMapper.insert(contact);
        return IdResponse.of(contact.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, ContactUpdateRequest request) {
        Contact contact = requireContact(id);
        if (StringUtils.isNotBlank(request.getName())) {
            contact.setName(request.getName());
        }
        if (request.getType() != null) {
            contact.setType(request.getType());
        }
        if (request.getContactPerson() != null) {
            contact.setContactPerson(request.getContactPerson());
        }
        if (request.getPhone() != null) {
            contact.setPhone(request.getPhone());
        }
        if (request.getEmail() != null) {
            contact.setEmail(request.getEmail());
        }
        if (request.getAddress() != null) {
            contact.setAddress(request.getAddress());
        }
        if (request.getTaxIdentificationNumber() != null) {
            contact.setTaxIdentificationNumber(request.getTaxIdentificationNumber());
        }
        if (request.getBankAccount() != null) {
            contact.setBankAccount(request.getBankAccount());
        }
        contactMapper.updateById(contact);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        requireContact(id);
        Long materialRefs = materialMapper.selectCount(new LambdaQueryWrapper<Material>()
                .eq(Material::getSupplierId, id));
        if (materialRefs != null && materialRefs > 0) {
            throw BusinessException.of(ErrorCode.BIZ_REFERENCED, "往来单位已被材料关联，无法删除");
        }
        Long leaseRefs = equipmentLeaseMapper.selectCount(new LambdaQueryWrapper<EquipmentLease>()
                .eq(EquipmentLease::getLessorId, id));
        if (leaseRefs != null && leaseRefs > 0) {
            throw BusinessException.of(ErrorCode.BIZ_REFERENCED, "往来单位已被设备租赁关联，无法删除");
        }
        contactMapper.deleteById(id);
    }

    private Contact requireContact(Long id) {
        Contact contact = contactMapper.selectById(id);
        if (contact == null) {
            throw BusinessException.of(ErrorCode.BIZ_NOT_FOUND, "往来单位不存在");
        }
        return contact;
    }
}