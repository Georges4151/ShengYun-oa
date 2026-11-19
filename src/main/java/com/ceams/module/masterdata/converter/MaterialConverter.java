package com.ceams.module.masterdata.converter;

import com.ceams.module.masterdata.entity.Material;
import com.ceams.module.masterdata.vo.MaterialVO;
import java.util.List;
import org.mapstruct.Mapper;

/** 材料实体 <-> VO 转换。 */
@Mapper(componentModel = "spring")
public interface MaterialConverter {

    MaterialVO toVO(Material material);

    List<MaterialVO> toVOList(List<Material> materials);
}
