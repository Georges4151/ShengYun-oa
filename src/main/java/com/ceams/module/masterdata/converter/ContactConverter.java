package com.ceams.module.masterdata.converter;

import com.ceams.module.masterdata.entity.Contact;
import com.ceams.common.mask.SensitiveMaskUtil;
import com.ceams.module.masterdata.vo.ContactVO;
import org.mapstruct.Mapper;

import java.util.List;

/**
 * 往来单位实体 <-> VO 转换（敏感字段脱敏）。
 */
@Mapper(componentModel = "spring")
public interface ContactConverter {

    default ContactVO toVO(Contact contact) {
        if (contact == null) {
            return null;
        }
        ContactVO vo = new ContactVO();
        vo.setId(contact.getId());
        vo.setName(contact.getName());
        vo.setType(contact.getType());
        vo.setContactPerson(contact.getContactPerson());
        vo.setPhone(SensitiveMaskUtil.maskPhone(contact.getPhone()));
        vo.setEmail(contact.getEmail());
        vo.setAddress(contact.getAddress());
        vo.setTaxIdentificationNumber(SensitiveMaskUtil.maskTaxNumber(contact.getTaxIdentificationNumber()));
        vo.setBankAccount(SensitiveMaskUtil.maskBankAccount(contact.getBankAccount()));
        vo.setCreateTime(contact.getCreateTime());
        return vo;
    }

    List<ContactVO> toVOList(List<Contact> contacts);
}