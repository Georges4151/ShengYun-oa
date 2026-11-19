package com.ceams.module.hr.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ceams.module.hr.entity.HrEmployee;
import org.apache.ibatis.annotations.Mapper;

/** 员工档案 Mapper。 */
@Mapper
public interface HrEmployeeMapper extends BaseMapper<HrEmployee> {}
