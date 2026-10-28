package com.ceams.module.project.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ceams.common.exception.BusinessException;
import com.ceams.common.exception.ErrorCode;
import com.ceams.common.response.PageResult;
import com.ceams.module.project.converter.SettlementProofConverter;
import com.ceams.module.project.dto.settlementproof.SettlementProofCreateRequest;
import com.ceams.module.project.dto.settlementproof.SettlementProofUpdateRequest;
import com.ceams.module.project.entity.SettlementProof;
import com.ceams.module.project.entity.SubcontractorContract;
import com.ceams.module.project.mapper.SettlementProofMapper;
import com.ceams.module.project.mapper.SubcontractorContractMapper;
import com.ceams.module.project.service.SettlementProofService;
import com.ceams.module.project.util.PageUtil;
import com.ceams.module.project.vo.SettlementProofVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 结算凭证服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SettlementProofServiceImpl implements SettlementProofService {

    private final SettlementProofMapper settlementProofMapper;
    private final SubcontractorContractMapper subcontractorContractMapper;
    private final SettlementProofConverter settlementProofConverter;

    @Override
    public PageResult<SettlementProofVO> page(long page, long size, Long contractId) {
        Page<SettlementProof> p = PageUtil.resolve(page, size);
        LambdaQueryWrapper<SettlementProof> wrapper = new LambdaQueryWrapper<>();
        if (contractId != null) {
            wrapper.eq(SettlementProof::getContractId, contractId);
        }
        wrapper.orderByDesc(SettlementProof::getCreateTime);
        IPage<SettlementProof> result = settlementProofMapper.selectPage(p, wrapper);
        return new PageResult<>(settlementProofConverter.toVOList(result.getRecords()),
                result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    public SettlementProofVO detail(Long id) {
        return settlementProofConverter.toVO(requireProof(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String create(SettlementProofCreateRequest request) {
        requireContract(request.getContractId());
        checkNumberUnique(request.getProofNumber(), null);
        SettlementProof proof = new SettlementProof();
        proof.setContractId(request.getContractId());
        proof.setSubcontractorId(request.getSubcontractorId());
        proof.setProofName(request.getProofName());
        proof.setProofNumber(request.getProofNumber());
        proof.setProofDate(toDateTime(request.getProofDate()));
        proof.setSettlementAmount(request.getSettlementAmount());
        proof.setDescription(request.getDescription());
        settlementProofMapper.insert(proof);
        return String.valueOf(proof.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, SettlementProofUpdateRequest request) {
        SettlementProof proof = requireProof(id);
        if (StringUtils.isNotBlank(request.getProofName())) {
            proof.setProofName(request.getProofName());
        }
        if (StringUtils.isNotBlank(request.getProofNumber())) {
            checkNumberUnique(request.getProofNumber(), id);
            proof.setProofNumber(request.getProofNumber());
        }
        if (request.getProofDate() != null) {
            proof.setProofDate(toDateTime(request.getProofDate()));
        }
        if (request.getSettlementAmount() != null) {
            proof.setSettlementAmount(request.getSettlementAmount());
        }
        if (request.getDescription() != null) {
            proof.setDescription(request.getDescription());
        }
        settlementProofMapper.updateById(proof);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        requireProof(id);
        settlementProofMapper.deleteById(id);
    }

    private SettlementProof requireProof(Long id) {
        SettlementProof proof = settlementProofMapper.selectById(id);
        if (proof == null) {
            throw BusinessException.of(ErrorCode.BIZ_NOT_FOUND, "结算凭证不存在");
        }
        return proof;
    }

    private void requireContract(Long contractId) {
        SubcontractorContract contract = subcontractorContractMapper.selectById(contractId);
        if (contract == null) {
            throw BusinessException.of(ErrorCode.BIZ_NOT_FOUND, "分包合同不存在");
        }
    }

    private void checkNumberUnique(String proofNumber, Long excludeId) {
        LambdaQueryWrapper<SettlementProof> wrapper = new LambdaQueryWrapper<SettlementProof>()
                .eq(SettlementProof::getProofNumber, proofNumber);
        if (excludeId != null) {
            wrapper.ne(SettlementProof::getId, excludeId);
        }
        Long count = settlementProofMapper.selectCount(wrapper);
        if (count != null && count > 0) {
            throw BusinessException.of(ErrorCode.BIZ_DUPLICATE_CODE, "凭证编号已存在");
        }
    }

    private LocalDateTime toDateTime(LocalDate date) {
        return date == null ? null : date.atStartOfDay();
    }
}