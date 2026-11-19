package com.ceams.module.masterdata.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ceams.common.exception.BusinessException;
import com.ceams.common.exception.ErrorCode;
import com.ceams.common.response.PageResult;
import com.ceams.module.masterdata.converter.MaterialConverter;
import com.ceams.module.masterdata.dto.material.MaterialCreateRequest;
import com.ceams.module.masterdata.dto.material.MaterialUpdateRequest;
import com.ceams.module.masterdata.entity.Category;
import com.ceams.module.masterdata.entity.Contact;
import com.ceams.module.masterdata.entity.Material;
import com.ceams.module.masterdata.mapper.CategoryMapper;
import com.ceams.module.masterdata.mapper.ContactMapper;
import com.ceams.module.masterdata.mapper.MaterialMapper;
import com.ceams.module.masterdata.service.MaterialService;
import com.ceams.module.masterdata.util.PageUtil;
import com.ceams.module.masterdata.vo.MaterialVO;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 材料服务实现。 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MaterialServiceImpl implements MaterialService {

    private final MaterialMapper materialMapper;
    private final MaterialConverter materialConverter;
    private final ContactMapper contactMapper;
    private final CategoryMapper categoryMapper;

    @Override
    public PageResult<MaterialVO> page(long page, long size, String keyword, Long categoryId) {
        Page<Material> p = PageUtil.resolve(page, size);
        LambdaQueryWrapper<Material> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.isNotBlank(keyword)) {
            wrapper.and(
                    w ->
                            w.like(Material::getMaterialName, keyword)
                                    .or()
                                    .like(Material::getMaterialCode, keyword)
                                    .or()
                                    .like(Material::getSpecification, keyword));
        }
        if (categoryId != null) {
            wrapper.eq(Material::getCategoryId, categoryId);
        }
        wrapper.orderByDesc(Material::getCreateTime);
        IPage<Material> result = materialMapper.selectPage(p, wrapper);
        List<MaterialVO> records = materialConverter.toVOList(result.getRecords());
        // 展示场景补充供应商 / 分类名称，避免前端展示雪花 ID
        fillNames(records);
        return new PageResult<>(
                records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    public MaterialVO detail(Long id) {
        MaterialVO vo = materialConverter.toVO(requireMaterial(id));
        fillNames(List.of(vo));
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String create(MaterialCreateRequest request) {
        Long count =
                materialMapper.selectCount(
                        new LambdaQueryWrapper<Material>()
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
        try {
            materialMapper.insert(material);
        } catch (DuplicateKeyException e) {
            throw BusinessException.of(ErrorCode.BIZ_DUPLICATE_CODE, "材料编码已存在");
        }
        return String.valueOf(material.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, MaterialUpdateRequest request) {
        Material material = requireMaterial(id);
        if (StringUtils.isNotBlank(request.getMaterialName())) {
            material.setMaterialName(request.getMaterialName());
        }
        if (StringUtils.isNotBlank(request.getUnit())) {
            material.setUnit(request.getUnit());
        }
        boolean clearSupplier = request.getSupplierId() == null;
        boolean clearCategory = request.getCategoryId() == null;
        boolean clearSpecification = request.getSpecification() == null;
        boolean clearPrice = request.getCurrentPrice() == null;
        material.setSpecification(request.getSpecification());
        material.setCurrentPrice(request.getCurrentPrice());
        if (!clearSupplier) {
            material.setSupplierId(request.getSupplierId());
        } else {
            material.setSupplierId(null);
        }
        if (!clearCategory) {
            material.setCategoryId(request.getCategoryId());
        } else {
            material.setCategoryId(null);
        }
        materialMapper.updateById(material);
        if (clearSupplier || clearCategory || clearSpecification || clearPrice) {
            LambdaUpdateWrapper<Material> clearWrapper = new LambdaUpdateWrapper<>();
            clearWrapper.eq(Material::getId, id);
            if (clearSupplier) {
                clearWrapper.set(Material::getSupplierId, null);
            }
            if (clearCategory) {
                clearWrapper.set(Material::getCategoryId, null);
            }
            if (clearSpecification) {
                clearWrapper.set(Material::getSpecification, null);
            }
            if (clearPrice) {
                clearWrapper.set(Material::getCurrentPrice, null);
            }
            materialMapper.update(null, clearWrapper);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        requireMaterial(id);
        materialMapper.deleteById(id);
    }

    /**
     * 批量填充供应商名称与分类名称（展示用）。
     *
     * <p>先收集本批记录中的非空 ID，再各用一次批量查询换取名称，避免逐条查询造成 N+1；
     * 查不到（含已逻辑删除）时保持名称字段为 null。
     */
    private void fillNames(List<MaterialVO> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        Set<Long> supplierIds =
                records.stream()
                        .map(MaterialVO::getSupplierId)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());
        Set<Long> categoryIds =
                records.stream()
                        .map(MaterialVO::getCategoryId)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());

        Map<Long, String> supplierNameMap = new HashMap<>();
        if (!supplierIds.isEmpty()) {
            for (Contact contact : contactMapper.selectBatchIds(supplierIds)) {
                supplierNameMap.put(contact.getId(), contact.getName());
            }
        }
        Map<Long, String> categoryNameMap = new HashMap<>();
        if (!categoryIds.isEmpty()) {
            for (Category category : categoryMapper.selectBatchIds(categoryIds)) {
                categoryNameMap.put(category.getId(), category.getName());
            }
        }

        for (MaterialVO vo : records) {
            if (vo.getSupplierId() != null) {
                vo.setSupplierName(supplierNameMap.get(vo.getSupplierId()));
            }
            if (vo.getCategoryId() != null) {
                vo.setCategoryName(categoryNameMap.get(vo.getCategoryId()));
            }
        }
    }

    private Material requireMaterial(Long id) {
        Material material = materialMapper.selectById(id);
        if (material == null) {
            throw BusinessException.of(ErrorCode.BIZ_NOT_FOUND, "材料不存在");
        }
        return material;
    }
}
