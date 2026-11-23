package com.ceams.module.hr.converter;

import com.ceams.module.hr.entity.HrJobLevel;
import com.ceams.module.hr.vo.JobLevelVO;
import java.util.List;
import org.mapstruct.Mapper;

/** 职级实体 <-> VO 转换。 */
@Mapper(componentModel = "spring")
public interface HrJobLevelConverter {

    default JobLevelVO toVO(HrJobLevel jobLevel) {
        if (jobLevel == null) {
            return null;
        }
        JobLevelVO vo = new JobLevelVO();
        vo.setId(jobLevel.getId());
        vo.setCode(jobLevel.getCode());
        vo.setName(jobLevel.getName());
        vo.setJobFamily(jobLevel.getJobFamily());
        vo.setRankNo(jobLevel.getRankNo());
        vo.setSortOrder(jobLevel.getSortOrder());
        vo.setStatus(jobLevel.getStatus());
        vo.setRemark(jobLevel.getRemark());
        vo.setCreateTime(jobLevel.getCreateTime());
        return vo;
    }

    List<JobLevelVO> toVOList(List<HrJobLevel> jobLevels);
}
