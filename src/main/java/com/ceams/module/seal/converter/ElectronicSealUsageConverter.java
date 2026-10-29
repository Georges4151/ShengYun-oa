package com.ceams.module.seal.converter;

import com.ceams.module.seal.entity.ElectronicSealUsage;
import com.ceams.module.seal.vo.ElectronicSealUsageVO;
import java.util.List;
import org.mapstruct.Mapper;

/** 印章使用审计实体 <-> VO 转换。 */
@Mapper(componentModel = "spring")
public interface ElectronicSealUsageConverter {

    default ElectronicSealUsageVO toVO(ElectronicSealUsage entity) {
        if (entity == null) {
            return null;
        }
        ElectronicSealUsageVO vo = new ElectronicSealUsageVO();
        vo.setId(entity.getId());
        vo.setSealId(entity.getSealId());
        vo.setSealName(entity.getSealName());
        vo.setUserId(entity.getUserId());
        vo.setUserName(entity.getUserName());
        vo.setPurpose(entity.getPurpose());
        vo.setDocumentType(entity.getDocumentType());
        vo.setDocumentPath(entity.getDocumentPath());
        vo.setUsageTime(entity.getUsageTime());
        vo.setIpAddress(entity.getIpAddress());
        vo.setCreateTime(entity.getCreateTime());
        return vo;
    }

    List<ElectronicSealUsageVO> toVOList(List<ElectronicSealUsage> entities);
}
