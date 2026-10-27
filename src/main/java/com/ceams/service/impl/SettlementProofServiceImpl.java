package com.ceams.service.impl;

import com.ceams.dao.SettlementProofMapper;
import com.ceams.dao.SubcontractorContractMapper;
import com.ceams.model.entity.SettlementProof;
import com.ceams.model.entity.SubcontractorContract;
import com.ceams.service.SettlementProofService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SettlementProofServiceImpl implements SettlementProofService {

    @Autowired
    private SettlementProofMapper settlementProofMapper;

    @Autowired
    private SubcontractorContractMapper contractMapper;

    @Override
    public SettlementProof getById(Integer id) {
        return settlementProofMapper.selectById(id);
    }

    @Override
    public SettlementProof getByContractId(Integer contractId) {
        return settlementProofMapper.selectByContractId(contractId);
    }

    @Override
    public List<SettlementProof> getBySubcontractorId(Integer subcontractorId) {
        return settlementProofMapper.selectBySubcontractorId(subcontractorId);
    }

    @Override
    public List<SettlementProof> getAll() {
        return settlementProofMapper.selectAll();
    }

    @Override
    @Transactional
    public boolean add(SettlementProof proof) {
        return settlementProofMapper.insert(proof) > 0;
    }

    @Override
    @Transactional
    public boolean update(SettlementProof proof) {
        return settlementProofMapper.update(proof) > 0;
    }

    @Override
    @Transactional
    public boolean delete(Integer id) {
        return settlementProofMapper.deleteById(id) > 0;
    }

    @Override
    public boolean hasSettlementProof(Integer contractId) {
        return settlementProofMapper.existsByContractId(contractId);
    }

    @Override
    @Transactional
    public boolean importSettlementProof(Integer contractId, Integer subcontractorId, String proofName,
            String proofNumber, java.util.Date proofDate,
            java.math.BigDecimal settlementAmount, String filePath,
            String fileName, String description, Integer createdBy) {
        SubcontractorContract contract = contractMapper.selectById(contractId);
        if (contract == null) {
            throw new RuntimeException("合同不存在");
        }

        SettlementProof existingProof = settlementProofMapper.selectByContractId(contractId);
        if (existingProof != null) {
            existingProof.setProofName(proofName);
            existingProof.setProofNumber(proofNumber);
            existingProof.setProofDate(proofDate);
            existingProof.setSettlementAmount(settlementAmount);
            existingProof.setFilePath(filePath);
            existingProof.setFileName(fileName);
            existingProof.setDescription(description);
            return settlementProofMapper.update(existingProof) > 0;
        }

        SettlementProof proof = new SettlementProof();
        proof.setContractId(contractId);
        proof.setSubcontractorId(subcontractorId);
        proof.setProofName(proofName);
        proof.setProofNumber(proofNumber);
        proof.setProofDate(proofDate);
        proof.setSettlementAmount(settlementAmount);
        proof.setFilePath(filePath);
        proof.setFileName(fileName);
        proof.setDescription(description);
        proof.setCreatedBy(createdBy);

        return settlementProofMapper.insert(proof) > 0;
    }
}
