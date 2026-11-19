package com.ceams.module.project.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ceams.common.exception.BusinessException;
import com.ceams.common.exception.ErrorCode;
import com.ceams.common.response.PageResult;
import com.ceams.module.hr.support.SimpleXlsxWriter;
import com.ceams.module.project.converter.SettlementProofConverter;
import com.ceams.module.project.dto.settlementproof.SettlementProofCreateRequest;
import com.ceams.module.project.dto.settlementproof.SettlementProofUpdateRequest;
import com.ceams.module.project.entity.SettlementProof;
import com.ceams.module.project.entity.Subcontractor;
import com.ceams.module.project.entity.SubcontractorContract;
import com.ceams.module.project.mapper.SettlementProofMapper;
import com.ceams.module.project.mapper.SubcontractorContractMapper;
import com.ceams.module.project.mapper.SubcontractorMapper;
import com.ceams.module.project.service.SettlementProofService;
import com.ceams.module.project.util.PageUtil;
import com.ceams.module.project.vo.SettlementProofVO;
import java.io.OutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 结算凭证服务实现。 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SettlementProofServiceImpl implements SettlementProofService {

    /** 导出列顺序。 */
    private static final List<String> EXPORT_HEADERS =
            List.of(
                    "凭证名称",
                    "凭证编号",
                    "合同名称",
                    "分包商",
                    "凭证日期",
                    "结算金额",
                    "附件文件名",
                    "描述",
                    "创建时间");

    private final SettlementProofMapper settlementProofMapper;
    private final SubcontractorContractMapper subcontractorContractMapper;
    private final SubcontractorMapper subcontractorMapper;
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
        List<SettlementProofVO> records = settlementProofConverter.toVOList(result.getRecords());
        fillNames(records);
        return new PageResult<>(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    public SettlementProofVO detail(Long id) {
        SettlementProofVO vo = settlementProofConverter.toVO(requireProof(id));
        fillNames(List.of(vo));
        return vo;
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
        proof.setFilePath(request.getFilePath());
        proof.setFileName(request.getFileName());
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
        if (StringUtils.isNotBlank(request.getFilePath())) {
            proof.setFilePath(request.getFilePath());
        }
        if (StringUtils.isNotBlank(request.getFileName())) {
            proof.setFileName(request.getFileName());
        }
        settlementProofMapper.updateById(proof);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        requireProof(id);
        settlementProofMapper.deleteById(id);
    }

    @Override
    public void export(OutputStream out, Long contractId) {
        LambdaQueryWrapper<SettlementProof> wrapper = new LambdaQueryWrapper<>();
        if (contractId != null) {
            wrapper.eq(SettlementProof::getContractId, contractId);
        }
        wrapper.orderByDesc(SettlementProof::getCreateTime);
        List<SettlementProof> proofs = settlementProofMapper.selectList(wrapper);
        Set<Long> contractIds =
                proofs.stream()
                        .map(SettlementProof::getContractId)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());
        Set<Long> subcontractorIds =
                proofs.stream()
                        .map(SettlementProof::getSubcontractorId)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());
        Map<Long, String> contractNames = loadContractNames(contractIds);
        Map<Long, String> subcontractorNames = loadSubcontractorNames(subcontractorIds);
        List<List<Object>> rows = new ArrayList<>(proofs.size());
        for (SettlementProof proof : proofs) {
            List<Object> row = new ArrayList<>(EXPORT_HEADERS.size());
            row.add(proof.getProofName());
            row.add(proof.getProofNumber());
            row.add(nameOrId(contractNames.get(proof.getContractId()), proof.getContractId()));
            row.add(
                    nameOrId(
                            subcontractorNames.get(proof.getSubcontractorId()),
                            proof.getSubcontractorId()));
            row.add(dateText(proof.getProofDate()));
            row.add(proof.getSettlementAmount());
            row.add(proof.getFileName());
            row.add(proof.getDescription());
            row.add(proof.getCreateTime() == null ? null : proof.getCreateTime().toString());
            rows.add(row);
        }
        SimpleXlsxWriter.write(out, "结算凭证", EXPORT_HEADERS, rows);
        log.info("结算凭证导出完成：合同 {}，条数 {}", contractId, rows.size());
    }

    /**
     * 批量填充合同名称与分包商名称（展示用）。
     *
     * <p>先收集本批记录中的非空 ID，再各用一次批量查询换取名称，避免逐条查询造成 N+1；
     * 查不到（含记录已删除）时保持名称字段为 null。
     */
    private void fillNames(List<SettlementProofVO> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        Set<Long> contractIds =
                records.stream()
                        .map(SettlementProofVO::getContractId)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());
        Set<Long> subcontractorIds =
                records.stream()
                        .map(SettlementProofVO::getSubcontractorId)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());
        Map<Long, String> contractNames = loadContractNames(contractIds);
        Map<Long, String> subcontractorNames = loadSubcontractorNames(subcontractorIds);
        for (SettlementProofVO vo : records) {
            if (vo.getContractId() != null) {
                vo.setContractName(contractNames.get(vo.getContractId()));
            }
            if (vo.getSubcontractorId() != null) {
                vo.setSubcontractorName(subcontractorNames.get(vo.getSubcontractorId()));
            }
        }
    }

    private Map<Long, String> loadContractNames(Collection<Long> ids) {
        Map<Long, String> names = new HashMap<>();
        if (ids == null || ids.isEmpty()) {
            return names;
        }
        for (SubcontractorContract contract : subcontractorContractMapper.selectBatchIds(ids)) {
            names.put(contract.getId(), contract.getContractName());
        }
        return names;
    }

    private Map<Long, String> loadSubcontractorNames(Collection<Long> ids) {
        Map<Long, String> names = new HashMap<>();
        if (ids == null || ids.isEmpty()) {
            return names;
        }
        for (Subcontractor subcontractor : subcontractorMapper.selectBatchIds(ids)) {
            names.put(subcontractor.getId(), subcontractor.getName());
        }
        return names;
    }

    /** 名称缺失时回退写 ID 文本。 */
    private String nameOrId(String name, Long id) {
        return StringUtils.isNotBlank(name) ? name : idText(id);
    }

    /** ID 按字符串写出，避免 Excel 数值单元格丢失雪花 ID 精度。 */
    private String idText(Long id) {
        return id == null ? null : String.valueOf(id);
    }

    /** 日期写出为 yyyy-MM-dd 文本。 */
    private String dateText(LocalDateTime date) {
        return date == null ? null : date.toLocalDate().toString();
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
        LambdaQueryWrapper<SettlementProof> wrapper =
                new LambdaQueryWrapper<SettlementProof>()
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
