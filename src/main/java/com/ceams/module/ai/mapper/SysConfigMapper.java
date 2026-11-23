package com.ceams.module.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ceams.module.ai.entity.SysConfig;
import org.apache.ibatis.annotations.Mapper;

/** 系统配置 Mapper（AI 模块仅使用 ai.enabled 一项）。 */
@Mapper
public interface SysConfigMapper extends BaseMapper<SysConfig> {}
