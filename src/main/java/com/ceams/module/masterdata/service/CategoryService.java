package com.ceams.module.masterdata.service;

import com.ceams.common.response.PageResult;
import com.ceams.module.masterdata.dto.category.CategoryCreateRequest;
import com.ceams.module.masterdata.dto.category.CategoryUpdateRequest;
import com.ceams.module.masterdata.enums.CategoryType;
import com.ceams.module.masterdata.vo.CategoryVO;
import com.ceams.module.masterdata.vo.IdResponse;

import java.util.List;

/**
 * 分类服务。
 */
public interface CategoryService {

    List<CategoryVO> tree(CategoryType type);

    PageResult<CategoryVO> page(long page, long size, CategoryType type, Long parentId);

    CategoryVO detail(Long id);

    IdResponse create(CategoryCreateRequest request);

    void update(Long id, CategoryUpdateRequest request);

    void delete(Long id);
}