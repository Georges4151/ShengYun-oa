package com.ceams.module.integration.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ceams.module.integration.entity.IntegrationWecomSync;
import org.apache.ibatis.annotations.Mapper;

/** 企业微信组织同步批次 Mapper。 */
@Mapper
public interface IntegrationWecomSyncMapper extends BaseMapper<IntegrationWecomSync> {}
