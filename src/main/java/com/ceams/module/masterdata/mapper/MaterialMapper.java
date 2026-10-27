package com.ceams.module.masterdata.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ceams.module.masterdata.entity.Material;
import org.apache.ibatis.annotations.Mapper;

/**
 * 材料 Mapper。
 */
@Mapper
public interface MaterialMapper extends BaseMapper<Material> {
}