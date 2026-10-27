package com.ceams.service;

import com.ceams.model.entity.SettlementProof;

import java.util.List;

public interface SettlementProofService {

    SettlementProof getById(Integer id);

    SettlementProof getByContractId(Integer contractId);

    List<SettlementProof> getBySubcontractorId(Integer subcontractorId);

    List<SettlementProof> getAll();

    boolean add(SettlementProof proof);

    boolean update(SettlementProof proof);

    boolean delete(Integer id);

    boolean hasSettlementProof(Integer contractId);

    boolean importSettlementProof(Integer contractId, Integer subcontractorId, String proofName,
            String proofNumber, java.util.Date proofDate,
            java.math.BigDecimal settlementAmount, String filePath,
            String fileName, String description, Integer createdBy);
}
