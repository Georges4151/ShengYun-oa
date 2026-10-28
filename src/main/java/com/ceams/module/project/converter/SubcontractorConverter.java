package com.ceams.module.project.converter;

import com.ceams.module.project.entity.Subcontractor;
import com.ceams.common.mask.SensitiveMaskUtil;
import com.ceams.module.project.vo.SubcontractorVO;
import org.mapstruct.Mapper;

import java.util.List;

/**
 * 分包商实体 <-> VO 转换（敏感字段脱敏）。
 */
@Mapper(componentModel = "spring")
public interface SubcontractorConverter {

    default SubcontractorVO toVO(Subcontractor subcontractor) {
        if (subcontractor == null) {
            return null;
        }
        SubcontractorVO vo = new SubcontractorVO();
        vo.setId(subcontractor.getId());
        vo.setName(subcontractor.getName());
        vo.setContactPerson(subcontractor.getContactPerson());
        vo.setPhone(SensitiveMaskUtil.maskPhone(subcontractor.getPhone()));
        vo.setAddress(subcontractor.getAddress());
        vo.setQualificationLevel(subcontractor.getQualificationLevel());
        vo.setCreditRating(subcontractor.getCreditRating());
        vo.setBusinessScope(subcontractor.getBusinessScope());
        vo.setTaxIdentificationNumber(SensitiveMaskUtil.maskTaxNumber(subcontractor.getTaxIdentificationNumber()));
        vo.setBankAccount(SensitiveMaskUtil.maskBankAccount(subcontractor.getBankAccount()));
        vo.setDescription(subcontractor.getDescription());
        vo.setCreateTime(subcontractor.getCreateTime());
        return vo;
    }

    List<SubcontractorVO> toVOList(List<Subcontractor> subcontractors);
}