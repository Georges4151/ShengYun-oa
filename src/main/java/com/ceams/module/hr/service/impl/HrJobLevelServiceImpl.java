package com.ceams.module.hr.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ceams.common.exception.BusinessException;
import com.ceams.common.exception.ErrorCode;
import com.ceams.module.hr.converter.HrJobLevelConverter;
import com.ceams.module.hr.dto.JobLevelCreateRequest;
import com.ceams.module.hr.dto.JobLevelUpdateRequest;
import com.ceams.module.hr.entity.HrJobLevel;
import com.ceams.module.hr.mapper.HrJobLevelMapper;
import com.ceams.module.hr.service.HrJobLevelService;
import com.ceams.module.hr.vo.JobLevelVO;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 职级服务实现。 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HrJobLevelServiceImpl implements HrJobLevelService {

    /** 默认职级状态。 */
    private static final String STATUS_ENABLED = "ENABLED";

    private final HrJobLevelMapper jobLevelMapper;
    private final HrJobLevelConverter jobLevelConverter;

    @Override
    public List<JobLevelVO> list() {
        List<HrJobLevel> jobLevels =
                jobLevelMapper.selectList(
                        new LambdaQueryWrapper<HrJobLevel>()
                                .orderByAsc(HrJobLevel::getSortOrder)
                                .orderByAsc(HrJobLevel::getCode));
        return jobLevelConverter.toVOList(jobLevels);
    }

    @Override
    public List<JobLevelVO> enabledList() {
        List<HrJobLevel> jobLevels =
                jobLevelMapper.selectList(
                        new LambdaQueryWrapper<HrJobLevel>()
                                .eq(HrJobLevel::getStatus, STATUS_ENABLED)
                                .orderByAsc(HrJobLevel::getSortOrder)
                                .orderByAsc(HrJobLevel::getCode));
        return jobLevelConverter.toVOList(jobLevels);
    }

    @Override
    @Transactional
    public String create(JobLevelCreateRequest request) {
        Long count =
                jobLevelMapper.selectCount(
                        new LambdaQueryWrapper<HrJobLevel>()
                                .eq(HrJobLevel::getCode, request.getCode().trim()));
        if (count != null && count > 0) {
            throw BusinessException.of(
                    ErrorCode.BIZ_DUPLICATE_CODE, "职级编码已存在：" + request.getCode().trim());
        }
        HrJobLevel jobLevel = new HrJobLevel();
        jobLevel.setCode(request.getCode().trim());
        jobLevel.setName(request.getName().trim());
        jobLevel.setJobFamily(request.getJobFamily());
        jobLevel.setRankNo(request.getRankNo());
        jobLevel.setSortOrder(request.getSortOrder() == null ? 0 : request.getSortOrder());
        jobLevel.setStatus(
                StringUtils.isBlank(request.getStatus())
                        ? STATUS_ENABLED
                        : request.getStatus().trim());
        jobLevel.setRemark(request.getRemark());
        jobLevelMapper.insert(jobLevel);
        log.info("新建职级 {}（编码 {}）", jobLevel.getId(), jobLevel.getCode());
        return String.valueOf(jobLevel.getId());
    }

    @Override
    @Transactional
    public void update(Long id, JobLevelUpdateRequest request) {
        HrJobLevel jobLevel = jobLevelMapper.selectById(id);
        if (jobLevel == null) {
            throw BusinessException.of(ErrorCode.BIZ_NOT_FOUND, "职级不存在");
        }
        if (StringUtils.isNotBlank(request.getCode())) {
            String code = request.getCode().trim();
            Long count =
                    jobLevelMapper.selectCount(
                            new LambdaQueryWrapper<HrJobLevel>()
                                    .eq(HrJobLevel::getCode, code)
                                    .ne(HrJobLevel::getId, id));
            if (count != null && count > 0) {
                throw BusinessException.of(ErrorCode.BIZ_DUPLICATE_CODE, "职级编码已存在：" + code);
            }
            jobLevel.setCode(code);
        }
        if (request.getName() != null) {
            jobLevel.setName(request.getName().trim());
        }
        if (request.getJobFamily() != null) {
            jobLevel.setJobFamily(request.getJobFamily());
        }
        if (request.getRankNo() != null) {
            jobLevel.setRankNo(request.getRankNo());
        }
        if (request.getSortOrder() != null) {
            jobLevel.setSortOrder(request.getSortOrder());
        }
        if (StringUtils.isNotBlank(request.getStatus())) {
            jobLevel.setStatus(request.getStatus().trim());
        }
        if (request.getRemark() != null) {
            jobLevel.setRemark(request.getRemark());
        }
        jobLevelMapper.updateById(jobLevel);
        log.info("更新职级 {}", jobLevel.getId());
    }
}
