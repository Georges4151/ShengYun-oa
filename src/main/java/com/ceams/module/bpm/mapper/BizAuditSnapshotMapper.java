package com.ceams.module.bpm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ceams.module.bpm.entity.BizAuditSnapshot;
import org.apache.ibatis.annotations.Mapper;

/** 业务变更审计快照 Mapper。 */
@Mapper
public interface BizAuditSnapshotMapper extends BaseMapper<BizAuditSnapshot> {}
