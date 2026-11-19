package com.ceams.module.masterdata.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ceams.common.exception.BusinessException;
import com.ceams.common.exception.ErrorCode;
import com.ceams.common.response.PageResult;
import com.ceams.module.masterdata.converter.CategoryConverter;
import com.ceams.module.masterdata.dto.category.CategoryCreateRequest;
import com.ceams.module.masterdata.dto.category.CategoryUpdateRequest;
import com.ceams.module.masterdata.entity.Category;
import com.ceams.module.masterdata.entity.Material;
import com.ceams.module.masterdata.enums.CategoryType;
import com.ceams.module.masterdata.mapper.CategoryMapper;
import com.ceams.module.masterdata.mapper.MaterialMapper;
import com.ceams.module.masterdata.service.CategoryService;
import com.ceams.module.masterdata.util.PageUtil;
import com.ceams.module.masterdata.vo.CategoryVO;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 分类服务实现。 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryMapper categoryMapper;
    private final MaterialMapper materialMapper;
    private final CategoryConverter categoryConverter;

    @Override
    public List<CategoryVO> tree(CategoryType type) {
        LambdaQueryWrapper<Category> wrapper = new LambdaQueryWrapper<>();
        if (type != null) {
            wrapper.eq(Category::getType, type);
        }
        wrapper.orderByAsc(Category::getSortOrder).orderByAsc(Category::getId);
        List<Category> categories = categoryMapper.selectList(wrapper);
        return buildTree(categoryConverter.toVOList(categories));
    }

    @Override
    public PageResult<CategoryVO> page(long page, long size, CategoryType type, Long parentId) {
        Page<Category> p = PageUtil.resolve(page, size);
        LambdaQueryWrapper<Category> wrapper = new LambdaQueryWrapper<>();
        if (type != null) {
            wrapper.eq(Category::getType, type);
        }
        if (parentId != null) {
            wrapper.eq(Category::getParentId, parentId);
        }
        wrapper.orderByAsc(Category::getSortOrder).orderByAsc(Category::getId);
        IPage<Category> result = categoryMapper.selectPage(p, wrapper);
        return new PageResult<>(
                categoryConverter.toVOList(result.getRecords()),
                result.getTotal(),
                result.getCurrent(),
                result.getSize());
    }

    @Override
    public CategoryVO detail(Long id) {
        return categoryConverter.toVO(requireCategory(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String create(CategoryCreateRequest request) {
        Long parentId = request.getParentId();
        if (parentId != null) {
            Category parent = categoryMapper.selectById(parentId);
            if (parent == null) {
                throw BusinessException.of(ErrorCode.BIZ_NOT_FOUND, "父分类不存在");
            }
            if (parent.getType() != request.getType()) {
                throw BusinessException.of(ErrorCode.BIZ_UNPROCESSABLE, "父分类类型不一致");
            }
        }
        Category category = new Category();
        category.setName(request.getName());
        category.setType(request.getType());
        category.setCode(request.getCode());
        category.setParentId(parentId);
        category.setSortOrder(request.getSort() == null ? 0 : request.getSort());
        category.setEnabled(Boolean.TRUE);
        categoryMapper.insert(category);
        return String.valueOf(category.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, CategoryUpdateRequest request) {
        Category category = requireCategory(id);
        if (StringUtils.isNotBlank(request.getName())) {
            category.setName(request.getName());
        }
        if (request.getCode() != null) {
            category.setCode(request.getCode());
        }
        boolean clearParent = request.getParentId() == null;
        if (request.getParentId() != null) {
            Long parentId = request.getParentId();
            if (parentId.equals(id)) {
                throw BusinessException.of(ErrorCode.BIZ_STATE_CONFLICT, "父分类不能是自身");
            }
            assertNotDescendant(id, parentId);
            Category parent = categoryMapper.selectById(parentId);
            if (parent == null) {
                throw BusinessException.of(ErrorCode.BIZ_NOT_FOUND, "父分类不存在");
            }
            if (parent.getType() != category.getType()) {
                throw BusinessException.of(ErrorCode.BIZ_UNPROCESSABLE, "父分类类型不一致");
            }
            category.setParentId(parentId);
        } else {
            category.setParentId(null);
        }
        if (request.getSort() != null) {
            category.setSortOrder(request.getSort());
        }
        if (request.getEnabled() != null) {
            category.setEnabled(request.getEnabled());
        }
        categoryMapper.updateById(category);
        if (clearParent) {
            categoryMapper.update(
                    null,
                    new LambdaUpdateWrapper<Category>()
                            .eq(Category::getId, id)
                            .set(Category::getParentId, null));
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        requireCategory(id);
        Long children =
                categoryMapper.selectCount(
                        new LambdaQueryWrapper<Category>().eq(Category::getParentId, id));
        if (children != null && children > 0) {
            throw BusinessException.of(ErrorCode.BIZ_REFERENCED, "存在子分类，无法删除");
        }
        Long materialRefs =
                materialMapper.selectCount(
                        new LambdaQueryWrapper<Material>().eq(Material::getCategoryId, id));
        if (materialRefs != null && materialRefs > 0) {
            throw BusinessException.of(ErrorCode.BIZ_REFERENCED, "分类已被材料引用，无法删除");
        }
        categoryMapper.deleteById(id);
    }

    private Category requireCategory(Long id) {
        Category category = categoryMapper.selectById(id);
        if (category == null) {
            throw BusinessException.of(ErrorCode.BIZ_NOT_FOUND, "分类不存在");
        }
        return category;
    }

    private void assertNotDescendant(Long id, Long parentId) {
        Long current = parentId;
        while (current != null) {
            if (current.equals(id)) {
                throw BusinessException.of(ErrorCode.BIZ_STATE_CONFLICT, "父分类不能是自身的子孙分类");
            }
            Category node = categoryMapper.selectById(current);
            if (node == null) {
                break;
            }
            current = node.getParentId();
        }
    }

    private List<CategoryVO> buildTree(List<CategoryVO> all) {
        Map<Long, CategoryVO> idMap = new HashMap<>();
        for (CategoryVO vo : all) {
            idMap.put(vo.getId(), vo);
        }
        List<CategoryVO> roots = new ArrayList<>();
        for (CategoryVO vo : all) {
            Long parentId = vo.getParentId();
            if (parentId == null || !idMap.containsKey(parentId)) {
                roots.add(vo);
            } else {
                CategoryVO parent = idMap.get(parentId);
                if (parent.getChildren() == null) {
                    parent.setChildren(new ArrayList<>());
                }
                parent.getChildren().add(vo);
            }
        }
        return roots;
    }
}
