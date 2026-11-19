package com.ceams.module.seal.converter;

import com.ceams.module.seal.entity.ElectronicSealApplication;
import com.ceams.module.seal.vo.ElectronicSealApplicationVO;
import java.util.List;
import org.mapstruct.Mapper;

/** 用印申请实体 <-> VO 转换。 */
@Mapper(componentModel = "spring")
public interface ElectronicSealApplicationConverter {

    default ElectronicSealApplicationVO toVO(ElectronicSealApplication entity) {
        if (entity == null) {
            return null;
        }
        ElectronicSealApplicationVO vo = new ElectronicSealApplicationVO();
        vo.setId(entity.getId());
        vo.setSealId(entity.getSealId());
        vo.setSealName(entity.getSealName());
        vo.setApplicantId(entity.getApplicantId());
        vo.setApplicantName(entity.getApplicantName());
        vo.setPurpose(entity.getPurpose());
        vo.setStatus(entity.getStatus());
        vo.setProjectId(entity.getProjectId());
        vo.setProjectName(entity.getProjectName());
        vo.setContractName(entity.getContractName());
        vo.setContractPath(entity.getContractPath());
        vo.setStampPosition(entity.getStampPosition());
        vo.setApproverId(entity.getApproverId());
        vo.setApproverName(entity.getApproverName());
        vo.setApprovalComment(entity.getApprovalComment());
        vo.setApplyTime(entity.getApplyTime());
        vo.setApprovalTime(entity.getApprovalTime());
        vo.setCreateTime(entity.getCreateTime());
        return vo;
    }

    List<ElectronicSealApplicationVO> toVOList(List<ElectronicSealApplication> entities);
}
