package com.ceams.module.integration.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ceams.module.integration.entity.IntegrationWecomMessage;
import org.apache.ibatis.annotations.Mapper;

/** 企业微信应用消息发件箱 Mapper（P4 使用）。 */
@Mapper
public interface IntegrationWecomMessageMapper extends BaseMapper<IntegrationWecomMessage> {}
