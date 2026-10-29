package com.ceams.module.seal.converter;

import com.ceams.module.seal.entity.ElectronicSealStamp;
import com.ceams.module.seal.vo.ElectronicSealStampVO;
import java.util.List;
import org.mapstruct.Mapper;

/** 用印盖章记录实体 <-> VO 转换。 */
@Mapper(componentModel = "spring")
public interface ElectronicSealStampConverter {

    default ElectronicSealStampVO toVO(ElectronicSealStamp entity) {
        if (entity == null) {
            return null;
        }
        ElectronicSealStampVO vo = new ElectronicSealStampVO();
        vo.setId(entity.getId());
        vo.setSealId(entity.getSealId());
        vo.setSealName(entity.getSealName());
        vo.setProjectId(entity.getProjectId());
        vo.setProjectName(entity.getProjectName());
        vo.setContractName(entity.getContractName());
        vo.setContractPath(entity.getContractPath());
        vo.setStampPosition(entity.getStampPosition());
        vo.setStampImage(entity.getStampImage());
        vo.setStampTime(entity.getStampTime());
        vo.setUserId(entity.getUserId());
        vo.setUserName(entity.getUserName());
        vo.setIpAddress(entity.getIpAddress());
        vo.setCreateTime(entity.getCreateTime());
        return vo;
    }

    List<ElectronicSealStampVO> toVOList(List<ElectronicSealStamp> entities);
}
