package com.ceams.module.hr.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ceams.common.exception.BusinessException;
import com.ceams.common.exception.ErrorCode;
import com.ceams.module.hr.converter.HrPositionConverter;
import com.ceams.module.hr.dto.PositionCreateRequest;
import com.ceams.module.hr.dto.PositionUpdateRequest;
import com.ceams.module.hr.entity.HrPosition;
import com.ceams.module.hr.mapper.HrPositionMapper;
import com.ceams.module.hr.service.HrPositionService;
import com.ceams.module.hr.vo.PositionVO;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 岗位服务实现。 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HrPositionServiceImpl implements HrPositionService {

    /** 默认岗位状态。 */
    private static final String STATUS_ENABLED = "ENABLED";

    private final HrPositionMapper positionMapper;
    private final HrPositionConverter positionConverter;

    @Override
    public List<PositionVO> list() {
        List<HrPosition> positions =
                positionMapper.selectList(
                        new LambdaQueryWrapper<HrPosition>()
                                .orderByAsc(HrPosition::getSortOrder)
                                .orderByAsc(HrPosition::getCode));
        return positionConverter.toVOList(positions);
    }

    @Override
    @Transactional
    public String create(PositionCreateRequest request) {
        Long count =
                positionMapper.selectCount(
                        new LambdaQueryWrapper<HrPosition>()
                                .eq(HrPosition::getCode, request.getCode().trim()));
        if (count != null && count > 0) {
            throw BusinessException.of(
                    ErrorCode.BIZ_DUPLICATE_CODE, "岗位编码已存在：" + request.getCode().trim());
        }
        HrPosition position = new HrPosition();
        position.setCode(request.getCode().trim());
        position.setName(request.getName().trim());
        position.setLevel(request.getLevel());
        position.setJobFamily(request.getJobFamily());
        position.setSortOrder(request.getSortOrder() == null ? 0 : request.getSortOrder());
        position.setStatus(
                StringUtils.isBlank(request.getStatus())
                        ? STATUS_ENABLED
                        : request.getStatus().trim());
        position.setRemark(request.getRemark());
        positionMapper.insert(position);
        log.info("新建岗位 {}（编码 {}）", position.getId(), position.getCode());
        return String.valueOf(position.getId());
    }

    @Override
    @Transactional
    public void update(Long id, PositionUpdateRequest request) {
        HrPosition position = positionMapper.selectById(id);
        if (position == null) {
            throw BusinessException.of(ErrorCode.BIZ_NOT_FOUND, "岗位不存在");
        }
        if (request.getName() != null) {
            position.setName(request.getName().trim());
        }
        if (request.getLevel() != null) {
            position.setLevel(request.getLevel());
        }
        if (request.getJobFamily() != null) {
            position.setJobFamily(request.getJobFamily());
        }
        if (request.getSortOrder() != null) {
            position.setSortOrder(request.getSortOrder());
        }
        if (StringUtils.isNotBlank(request.getStatus())) {
            position.setStatus(request.getStatus().trim());
        }
        if (request.getRemark() != null) {
            position.setRemark(request.getRemark());
        }
        positionMapper.updateById(position);
        log.info("更新岗位 {}", position.getId());
    }
}
