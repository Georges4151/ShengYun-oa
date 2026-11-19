package com.ceams.module.bpm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ceams.module.bpm.entity.BpmInstance;
import org.apache.ibatis.annotations.Mapper;

/** 流程实例 Mapper。 */
@Mapper
public interface BpmInstanceMapper extends BaseMapper<BpmInstance> {}
