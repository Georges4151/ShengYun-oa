package com.ceams.module.masterdata.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ceams.module.masterdata.entity.Contact;
import org.apache.ibatis.annotations.Mapper;

/**
 * 往来单位 Mapper。
 */
@Mapper
public interface ContactMapper extends BaseMapper<Contact> {
}