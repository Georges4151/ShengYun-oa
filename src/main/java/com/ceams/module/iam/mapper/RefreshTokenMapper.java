package com.ceams.module.iam.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ceams.module.iam.entity.SysRefreshToken;
import org.apache.ibatis.annotations.Mapper;

/**
 * 刷新令牌 Mapper。
 */
@Mapper
public interface RefreshTokenMapper extends BaseMapper<SysRefreshToken> {
}