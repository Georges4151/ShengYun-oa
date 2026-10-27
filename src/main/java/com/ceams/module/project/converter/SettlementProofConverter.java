package com.ceams.module.project.converter;

import com.ceams.module.project.entity.SettlementProof;
import com.ceams.module.project.vo.SettlementProofVO;
import org.mapstruct.Mapper;

import java.util.List;

/**
 * 结算凭证实体 <-> VO 转换。
 */
@Mapper(componentModel = "spring")
public interface SettlementProofConverter {

    default SettlementProofVO toVO(SettlementProof proof) {
        if (proof == null) {
            return null;
        }
        SettlementProofVO vo = new SettlementProofVO();
        vo.setId(proof.getId());
        vo.setContractId(proof.getContractId());
        vo.setSubcontractorId(proof.getSubcontractorId());
        vo.setProofName(proof.getProofName());
        vo.setProofNumber(proof.getProofNumber());
        vo.setProofDate(proof.getProofDate() == null ? null : proof.getProofDate().toLocalDate());
        vo.setSettlementAmount(proof.getSettlementAmount());
        vo.setFilePath(proof.getFilePath());
        vo.setFileName(proof.getFileName());
        vo.setDescription(proof.getDescription());
        vo.setCreateTime(proof.getCreateTime());
        return vo;
    }

    List<SettlementProofVO> toVOList(List<SettlementProof> proofs);
}