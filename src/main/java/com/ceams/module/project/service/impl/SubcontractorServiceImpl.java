package com.ceams.module.project.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ceams.common.exception.BusinessException;
import com.ceams.common.exception.ErrorCode;
import com.ceams.common.response.PageResult;
import com.ceams.module.project.converter.SubcontractorConverter;
import com.ceams.module.project.dto.subcontractor.SubcontractorCreateRequest;
import com.ceams.module.project.dto.subcontractor.SubcontractorUpdateRequest;
import com.ceams.module.project.entity.Subcontractor;
import com.ceams.module.project.entity.SubcontractorContract;
import com.ceams.module.project.mapper.SubcontractorContractMapper;
import com.ceams.module.project.mapper.SubcontractorMapper;
import com.ceams.module.project.service.SubcontractorService;
import com.ceams.module.project.util.PageUtil;
import com.ceams.module.project.vo.IdResponse;
import com.ceams.module.project.vo.SubcontractorVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 分包商服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SubcontractorServiceImpl implements SubcontractorService {

    private final SubcontractorMapper subcontractorMapper;
    private final SubcontractorContractMapper subcontractorContractMapper;
    private final SubcontractorConverter subcontractorConverter;

    @Override
    public PageResult<SubcontractorVO> page(long page, long size, String keyword) {
        Page<Subcontractor> p = PageUtil.resolve(page, size);
        LambdaQueryWrapper<Subcontractor> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.isNotBlank(keyword)) {
            wrapper.and(w -> w.like(Subcontractor::getName, keyword)
                    .or().like(Subcontractor::getContactPerson, keyword));
        }
        wrapper.orderByDesc(Subcontractor::getCreateTime);
        IPage<Subcontractor> result = subcontractorMapper.selectPage(p, wrapper);
        return new PageResult<>(subcontractorConverter.toVOList(result.getRecords()),
                result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    public SubcontractorVO detail(Long id) {
        return subcontractorConverter.toVO(requireSubcontractor(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public IdResponse create(SubcontractorCreateRequest request) {
        Subcontractor subcontractor = new Subcontractor();
        subcontractor.setName(request.getName());
        subcontractor.setContactPerson(request.getContactPerson());
        subcontractor.setPhone(request.getPhone());
        subcontractor.setAddress(request.getAddress());
        subcontractor.setQualificationLevel(request.getQualificationLevel());
        subcontractor.setCreditRating(request.getCreditRating());
        subcontractor.setBusinessScope(request.getBusinessScope());
        subcontractor.setTaxIdentificationNumber(request.getTaxIdentificationNumber());
        subcontractor.setBankAccount(request.getBankAccount());
        subcontractor.setDescription(request.getDescription());
        subcontractorMapper.insert(subcontractor);
        return IdResponse.of(subcontractor.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, SubcontractorUpdateRequest request) {
        Subcontractor subcontractor = requireSubcontractor(id);
        if (StringUtils.isNotBlank(request.getName())) {
            subcontractor.setName(request.getName());
        }
        if (request.getContactPerson() != null) {
            subcontractor.setContactPerson(request.getContactPerson());
        }
        if (request.getPhone() != null) {
            subcontractor.setPhone(request.getPhone());
        }
        if (request.getAddress() != null) {
            subcontractor.setAddress(request.getAddress());
        }
        if (request.getQualificationLevel() != null) {
            subcontractor.setQualificationLevel(request.getQualificationLevel());
        }
        if (request.getCreditRating() != null) {
            subcontractor.setCreditRating(request.getCreditRating());
        }
        if (request.getBusinessScope() != null) {
            subcontractor.setBusinessScope(request.getBusinessScope());
        }
        if (request.getTaxIdentificationNumber() != null) {
            subcontractor.setTaxIdentificationNumber(request.getTaxIdentificationNumber());
        }
        if (request.getBankAccount() != null) {
            subcontractor.setBankAccount(request.getBankAccount());
        }
        if (request.getDescription() != null) {
            subcontractor.setDescription(request.getDescription());
        }
        subcontractorMapper.updateById(subcontractor);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        requireSubcontractor(id);
        Long contractRefs = subcontractorContractMapper.selectCount(new LambdaQueryWrapper<SubcontractorContract>()
                .eq(SubcontractorContract::getSubcontractorId, id));
        if (contractRefs != null && contractRefs > 0) {
            throw BusinessException.of(ErrorCode.BIZ_REFERENCED, "分包商存在关联分包合同，无法删除");
        }
        subcontractorMapper.deleteById(id);
    }

    private Subcontractor requireSubcontractor(Long id) {
        Subcontractor subcontractor = subcontractorMapper.selectById(id);
        if (subcontractor == null) {
            throw BusinessException.of(ErrorCode.BIZ_NOT_FOUND, "分包商不存在");
        }
        return subcontractor;
    }
}