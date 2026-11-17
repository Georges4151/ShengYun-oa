package com.ceams.module.project.converter;

import com.ceams.module.project.entity.ChangeRequest;
import com.ceams.module.project.vo.ChangeRequestVO;
import java.util.List;
import org.mapstruct.Mapper;

/** 变更请求实体 <-> VO 转换。 */
@Mapper(componentModel = "spring")
public interface ChangeRequestConverter {

    default ChangeRequestVO toVO(ChangeRequest request) {
        if (request == null) {
            return null;
        }
        ChangeRequestVO vo = new ChangeRequestVO();
        vo.setId(request.getId());
        vo.setProjectId(request.getProjectId());
        vo.setRequestType(request.getRequestType());
        vo.setDescription(request.getDescription());
        vo.setEstimatedCost(request.getEstimatedCost());
        vo.setStatus(request.getStatus());
        vo.setBpmInstanceId(request.getBpmInstanceId());
        vo.setRequestedBy(request.getRequestedBy());
        vo.setApprovedBy(request.getApprovedBy());
        vo.setApprovedAt(request.getApprovedAt());
        vo.setApprovalComment(request.getApprovalComment());
        vo.setCreateTime(request.getCreateTime());
        return vo;
    }

    List<ChangeRequestVO> toVOList(List<ChangeRequest> requests);
}
