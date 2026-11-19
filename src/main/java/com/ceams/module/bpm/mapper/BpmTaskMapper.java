package com.ceams.module.bpm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ceams.module.bpm.entity.BpmTask;
import org.apache.ibatis.annotations.Mapper;

/** 审批任务 Mapper。 */
@Mapper
public interface BpmTaskMapper extends BaseMapper<BpmTask> {}
