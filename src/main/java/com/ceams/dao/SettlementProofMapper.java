package com.ceams.dao;

import com.ceams.model.entity.SettlementProof;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SettlementProofMapper {

    SettlementProof selectById(Integer id);

    SettlementProof selectByContractId(Integer contractId);

    List<SettlementProof> selectBySubcontractorId(Integer subcontractorId);

    List<SettlementProof> selectAll();

    int insert(SettlementProof proof);

    int update(SettlementProof proof);

    int deleteById(Integer id);

    int deleteByContractId(Integer contractId);

    boolean existsByContractId(Integer contractId);
}
