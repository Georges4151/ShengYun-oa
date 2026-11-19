package com.ceams.module.iam.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ceams.common.exception.BusinessException;
import com.ceams.common.exception.ErrorCode;
import com.ceams.common.response.PageResult;
import com.ceams.module.iam.converter.OperationLogConverter;
import com.ceams.module.iam.entity.OperationLog;
import com.ceams.module.iam.enums.OperationType;
import com.ceams.module.iam.mapper.OperationLogMapper;
import com.ceams.module.iam.service.OperationLogService;
import com.ceams.module.iam.util.PageUtil;
import com.ceams.module.iam.vo.OperationLogVO;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

/** 操作日志服务实现。 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OperationLogServiceImpl implements OperationLogService {

    private final OperationLogMapper operationLogMapper;
    private final OperationLogConverter operationLogConverter;

    @Override
    public PageResult<OperationLogVO> page(
            long page,
            long size,
            String userId,
            String operationType,
            String startTime,
            String endTime) {
        Page<OperationLog> p = PageUtil.resolve(page, size);
        LambdaQueryWrapper<OperationLog> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.isNotBlank(userId)) {
            wrapper.eq(OperationLog::getUserId, parseLong(userId));
        }
        if (StringUtils.isNotBlank(operationType)) {
            wrapper.eq(OperationLog::getOperationType, parseOperationType(operationType));
        }
        if (StringUtils.isNotBlank(startTime)) {
            wrapper.ge(OperationLog::getCreateTime, parseDateTime(startTime, "startTime"));
        }
        if (StringUtils.isNotBlank(endTime)) {
            wrapper.le(OperationLog::getCreateTime, parseDateTime(endTime, "endTime"));
        }
        wrapper.orderByDesc(OperationLog::getCreateTime);

        IPage<OperationLog> result = operationLogMapper.selectPage(p, wrapper);
        return new PageResult<>(
                operationLogConverter.toVOList(result.getRecords()),
                result.getTotal(),
                result.getCurrent(),
                result.getSize());
    }

    private Long parseLong(String value) {
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException ex) {
            throw BusinessException.of(ErrorCode.VALID_ILLEGAL_ARGUMENT, "非法的 userId: " + value);
        }
    }

    private OperationType parseOperationType(String value) {
        try {
            return OperationType.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw BusinessException.of(ErrorCode.VALID_ILLEGAL_ARGUMENT, "非法的操作类型: " + value);
        }
    }

    private LocalDateTime parseDateTime(String value, String field) {
        try {
            return LocalDateTime.parse(value.trim());
        } catch (DateTimeParseException ex) {
            throw BusinessException.of(
                    ErrorCode.VALID_ILLEGAL_ARGUMENT, "非法的 " + field + " 时间: " + value);
        }
    }
}
