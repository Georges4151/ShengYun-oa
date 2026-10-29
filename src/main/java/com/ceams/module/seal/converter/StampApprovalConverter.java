package com.ceams.module.seal.converter;

import com.ceams.module.seal.entity.StampApproval;
import com.ceams.module.seal.vo.StampApprovalVO;
import java.util.List;
import org.mapstruct.Mapper;

/** 用印审批实体 <-> VO 转换。 */
@Mapper(componentModel = "spring")
public interface StampApprovalConverter {

    default StampApprovalVO toVO(StampApproval entity) {
        if (entity == null) {
            return null;
        }
        StampApprovalVO vo = new StampApprovalVO();
        vo.setId(entity.getId());
        vo.setStampId(entity.getStampId());
        vo.setSealId(entity.getSealId());
        vo.setSealName(entity.getSealName());
        vo.setSealCode(entity.getSealCode());
        vo.setSealPermission(entity.getSealPermission());
        vo.setProjectId(entity.getProjectId());
        vo.setProjectName(entity.getProjectName());
        vo.setContractName(entity.getContractName());
        vo.setContractPath(entity.getContractPath());
        vo.setOriginalContractPath(entity.getOriginalContractPath());
        vo.setStampPositions(entity.getStampPositions());
        vo.setApplicantId(entity.getApplicantId());
        vo.setApplicantName(entity.getApplicantName());
        vo.setApproverId(entity.getApproverId());
        vo.setApproverName(entity.getApproverName());
        vo.setStatus(entity.getStatus());
        vo.setRejectReason(entity.getRejectReason());
        vo.setApplyTime(entity.getApplyTime());
        vo.setApprovalTime(entity.getApprovalTime());
        vo.setCreateTime(entity.getCreateTime());
        return vo;
    }

    List<StampApprovalVO> toVOList(List<StampApproval> entities);
}
