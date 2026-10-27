package com.ceams.module.masterdata.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ceams.common.exception.BusinessException;
import com.ceams.common.exception.ErrorCode;
import com.ceams.common.response.PageResult;
import com.ceams.module.masterdata.converter.MaterialConverter;
import com.ceams.module.masterdata.dto.material.MaterialCreateRequest;
import com.ceams.module.masterdata.dto.material.MaterialUpdateRequest;
import com.ceams.module.masterdata.entity.Material;
import com.ceams.module.masterdata.mapper.MaterialMapper;
import com.ceams.module.masterdata.service.MaterialService;
import com.ceams.module.masterdata.util.PageUtil;
import com.ceams.module.masterdata.vo.IdResponse;
import com.ceams.module.masterdata.vo.MaterialVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 材料服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MaterialServiceImpl implements MaterialService {

    private final MaterialMapper materialMapper;
    private final MaterialConverter materialConverter;

    @Override
    public PageResult<MaterialVO> page(long page, long size, String keyword, Long categoryId) {
        Page<Material> p = PageUtil.resolve(page, size);
        LambdaQueryWrapper<Material> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.isNotBlank(keyword)) {
            wrapper.and(w -> w.like(Material::getMaterialName, keyword)
                    .or().like(Material::getMaterialCode, keyword)
                    .or().like(Material::getSpecification, keyword));
        }
        if (categoryId != null) {
            wrapper.eq(Material::getCategoryId, categoryId);
        }
        wrapper.orderByDesc(Material::getCreateTime);
        IPage<Material> result = materialMapper.selectPage(p, wrapper);
        return new PageResult<>(materialConverter.toVOList(result.getRecords()),
                result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    public MaterialVO detail(Long id) {
        return materialConverter.toVO(requireMaterial(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public IdResponse create(MaterialCreateRequest request) {
        Long count = materialMapper.selectCount(new LambdaQueryWrapper<Material>()
                .eq(Material::getMaterialCode, request.getMaterialCode()));
        if (count != null && count > 0) {
            throw BusinessException.of(ErrorCode.BIZ_DUPLICATE_CODE, "材料编码已存在");
        }
        Material material = new Material();
        material.setMaterialCode(request.getMaterialCode());
        material.setMaterialName(request.getMaterialName());
        material.setSpecification(request.getSpecification());
        material.setUnit(request.getUnit());
        material.setCurrentPrice(request.getCurrentPrice());
        material.setSupplierId(request.getSupplierId());
        material.setCategoryId(request.getCategoryId());
        materialMapper.insert(material);
        return IdResponse.of(material.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, MaterialUpdateRequest request) {
        Material material = requireMaterial(id);
        if (StringUtils.isNotBlank(request.getMaterialName())) {
            material.setMaterialName(request.getMaterialName());
        }
        if (request.getSpecification() != null) {
            material.setSpecification(request.getSpecification());
        }
        if (StringUtils.isNotBlank(request.getUnit())) {
            material.setUnit(request.getUnit());
        }
        if (request.getCurrentPrice() != null) {
            material.setCurrentPrice(request.getCurrentPrice());
        }
        if (request.getSupplierId() != null) {
            material.setSupplierId(request.getSupplierId());
        }
        if (request.getCategoryId() != null) {
            material.setCategoryId(request.getCategoryId());
        }
        materialMapper.updateById(material);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        requireMaterial(id);
        materialMapper.deleteById(id);
    }

    private Material requireMaterial(Long id) {
        Material material = materialMapper.selectById(id);
        if (material == null) {
            throw BusinessException.of(ErrorCode.BIZ_NOT_FOUND, "材料不存在");
        }
        return material;
    }
}