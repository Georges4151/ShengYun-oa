package com.ceams.module.seal.converter;

import com.ceams.module.seal.entity.ElectronicSeal;
import com.ceams.module.seal.vo.ElectronicSealVO;
import java.util.List;
import org.mapstruct.Mapper;

/** 电子印章实体 <-> VO 转换。 */
@Mapper(componentModel = "spring")
public interface ElectronicSealConverter {

    default ElectronicSealVO toVO(ElectronicSeal entity) {
        if (entity == null) {
            return null;
        }
        ElectronicSealVO vo = new ElectronicSealVO();
        vo.setId(entity.getId());
        vo.setSealName(entity.getSealName());
        vo.setSealType(entity.getSealType());
        vo.setSealCode(entity.getSealCode());
        vo.setDescription(entity.getDescription());
        vo.setStatus(entity.getStatus());
        vo.setSealImage(entity.getSealImage());
        vo.setStorageType(entity.getStorageType());
        vo.setSealFileId(entity.getSealFileId());
        vo.setUsbDriveId(entity.getUsbDriveId());
        vo.setPasswordProtected(entity.getPasswordProtected());
        vo.setLastUsedTime(entity.getLastUsedTime());
        vo.setUsageCount(entity.getUsageCount());
        vo.setCreateTime(entity.getCreateTime());
        vo.setUpdateTime(entity.getUpdateTime());
        return vo;
    }

    List<ElectronicSealVO> toVOList(List<ElectronicSeal> entities);
}
