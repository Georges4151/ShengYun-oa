package com.ceams.module.hr.converter;

import com.ceams.module.hr.entity.HrContract;
import com.ceams.module.hr.vo.ContractVO;
import java.util.List;
import org.mapstruct.Mapper;

/** 劳动合同实体 <-> VO 转换。 */
@Mapper(componentModel = "spring")
public interface HrContractConverter {

    default ContractVO toVO(HrContract contract) {
        if (contract == null) {
            return null;
        }
        ContractVO vo = new ContractVO();
        vo.setId(contract.getId());
        vo.setEmployeeId(contract.getEmployeeId());
        vo.setContractNo(contract.getContractNo());
        vo.setContractType(contract.getContractType());
        vo.setStartDate(contract.getStartDate());
        vo.setEndDate(contract.getEndDate());
        vo.setSignDate(contract.getSignDate());
        vo.setFileId(contract.getFileId());
        vo.setFilePath(contract.getFilePath());
        vo.setFileName(contract.getFileName());
        vo.setStatus(contract.getStatus());
        vo.setRenewalRemindDate(contract.getRenewalRemindDate());
        vo.setRemark(contract.getRemark());
        vo.setCreateTime(contract.getCreateTime());
        return vo;
    }

    List<ContractVO> toVOList(List<HrContract> contracts);
}
