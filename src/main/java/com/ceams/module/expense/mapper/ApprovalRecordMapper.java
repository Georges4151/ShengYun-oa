package com.ceams.module.expense.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ceams.module.expense.entity.ApprovalRecord;
import org.apache.ibatis.annotations.Mapper;

/** 审批记录 Mapper。 */
@Mapper
public interface ApprovalRecordMapper extends BaseMapper<ApprovalRecord> {}
