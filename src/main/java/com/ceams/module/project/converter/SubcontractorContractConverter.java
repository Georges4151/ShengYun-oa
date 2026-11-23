package com.ceams.module.project.converter;

import com.ceams.module.project.entity.SubcontractorContract;
import com.ceams.module.project.vo.SubcontractorContractVO;
import java.util.List;
import org.mapstruct.Mapper;

/** 分包合同实体 <-> VO 转换。 */
@Mapper(componentModel = "spring")
public interface SubcontractorContractConverter {

    default SubcontractorContractVO toVO(SubcontractorContract contract) {
        if (contract == null) {
            return null;
        }
        SubcontractorContractVO vo = new SubcontractorContractVO();
        vo.setId(contract.getId());
        vo.setSubcontractorId(contract.getSubcontractorId());
        vo.setProjectId(contract.getProjectId());
        vo.setContractName(contract.getContractName());
        vo.setContractNumber(contract.getContractNumber());
        vo.setContractAmount(contract.getContractAmount());
        vo.setStartDate(
                contract.getStartDate() == null ? null : contract.getStartDate().toLocalDate());
        vo.setEndDate(contract.getEndDate() == null ? null : contract.getEndDate().toLocalDate());
        vo.setStatus(contract.getStatus());
        vo.setApprovalStatus(contract.getApprovalStatus());
        vo.setBpmInstanceId(contract.getBpmInstanceId());
        vo.setDescription(contract.getDescription());
        vo.setFilePath(contract.getFilePath());
        vo.setFileName(contract.getFileName());
        vo.setPaidAmount(contract.getPaidAmount());
        vo.setCreateTime(contract.getCreateTime());
        return vo;
    }

    List<SubcontractorContractVO> toVOList(List<SubcontractorContract> contracts);
}
