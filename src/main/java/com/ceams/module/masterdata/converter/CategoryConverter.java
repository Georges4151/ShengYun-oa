package com.ceams.module.masterdata.converter;

import com.ceams.module.masterdata.entity.Category;
import com.ceams.module.masterdata.vo.CategoryVO;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/** 分类实体 <-> VO 转换。 */
@Mapper(componentModel = "spring")
public interface CategoryConverter {

    @Mapping(target = "sort", source = "sortOrder")
    @Mapping(target = "children", ignore = true)
    CategoryVO toVO(Category category);

    List<CategoryVO> toVOList(List<Category> categories);
}
