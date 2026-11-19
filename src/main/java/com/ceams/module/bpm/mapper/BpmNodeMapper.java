package com.ceams.module.bpm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ceams.module.bpm.entity.BpmNode;
import org.apache.ibatis.annotations.Mapper;

/** 流程节点实例 Mapper。 */
@Mapper
public interface BpmNodeMapper extends BaseMapper<BpmNode> {}
