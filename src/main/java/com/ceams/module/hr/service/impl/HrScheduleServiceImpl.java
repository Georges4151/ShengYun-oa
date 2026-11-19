package com.ceams.module.hr.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ceams.common.exception.BusinessException;
import com.ceams.common.exception.ErrorCode;
import com.ceams.module.hr.converter.HrScheduleConverter;
import com.ceams.module.hr.dto.ScheduleBatchRequest;
import com.ceams.module.hr.dto.ScheduleCreateRequest;
import com.ceams.module.hr.dto.ScheduleUpdateRequest;
import com.ceams.module.hr.entity.HrEmployee;
import com.ceams.module.hr.entity.HrSchedule;
import com.ceams.module.hr.enums.ScheduleType;
import com.ceams.module.hr.mapper.HrEmployeeMapper;
import com.ceams.module.hr.mapper.HrScheduleMapper;
import com.ceams.module.hr.service.HrScheduleService;
import com.ceams.module.hr.vo.ScheduleVO;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 排班服务实现。 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HrScheduleServiceImpl implements HrScheduleService {

    /** 单次批量排班允许的最大日期跨度（天）。 */
    private static final int MAX_BATCH_DAYS = 366;

    private final HrScheduleMapper scheduleMapper;
    private final HrEmployeeMapper employeeMapper;
    private final HrScheduleConverter scheduleConverter;

    @Override
    public List<ScheduleVO> list(Long employeeId, String month) {
        YearMonth yearMonth = parseMonth(month);
        LambdaQueryWrapper<HrSchedule> wrapper = new LambdaQueryWrapper<>();
        if (employeeId != null) {
            wrapper.eq(HrSchedule::getEmployeeId, employeeId);
        }
        wrapper.ge(HrSchedule::getWorkDate, yearMonth.atDay(1))
                .le(HrSchedule::getWorkDate, yearMonth.atEndOfMonth())
                .orderByAsc(HrSchedule::getWorkDate);
        return scheduleConverter.toVOList(scheduleMapper.selectList(wrapper));
    }

    @Override
    @Transactional
    public String create(ScheduleCreateRequest request) {
        requireEmployee(request.getEmployeeId());
        HrSchedule exists = findSchedule(request.getEmployeeId(), request.getWorkDate());
        if (exists != null) {
            throw BusinessException.of(
                    ErrorCode.BIZ_DUPLICATE_CODE, "该员工当天已存在排班，请改为修改");
        }
        HrSchedule schedule = new HrSchedule();
        schedule.setEmployeeId(request.getEmployeeId());
        schedule.setWorkDate(request.getWorkDate());
        schedule.setShiftCode(request.getShiftCode());
        schedule.setStartTime(request.getStartTime());
        schedule.setEndTime(request.getEndTime());
        schedule.setScheduleType(
                request.getScheduleType() == null ? ScheduleType.FIXED : request.getScheduleType());
        schedule.setRemark(request.getRemark());
        scheduleMapper.insert(schedule);
        log.info("新建排班 {}（员工 {}，日期 {}）", schedule.getId(), schedule.getEmployeeId(), schedule.getWorkDate());
        return String.valueOf(schedule.getId());
    }

    @Override
    @Transactional
    public void update(Long id, ScheduleUpdateRequest request) {
        HrSchedule schedule = scheduleMapper.selectById(id);
        if (schedule == null) {
            throw BusinessException.of(ErrorCode.BIZ_NOT_FOUND, "排班不存在");
        }
        if (request.getShiftCode() != null) {
            schedule.setShiftCode(request.getShiftCode());
        }
        if (request.getStartTime() != null) {
            schedule.setStartTime(request.getStartTime());
        }
        if (request.getEndTime() != null) {
            schedule.setEndTime(request.getEndTime());
        }
        if (request.getScheduleType() != null) {
            schedule.setScheduleType(request.getScheduleType());
        }
        if (request.getRemark() != null) {
            schedule.setRemark(request.getRemark());
        }
        scheduleMapper.updateById(schedule);
        log.info("更新排班 {}", schedule.getId());
    }

    @Override
    @Transactional
    public int batch(ScheduleBatchRequest request) {
        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw BusinessException.of(ErrorCode.VALID_ILLEGAL_ARGUMENT, "结束日期不能早于开始日期");
        }
        long span = request.getEndDate().toEpochDay() - request.getStartDate().toEpochDay() + 1;
        if (span > MAX_BATCH_DAYS) {
            throw BusinessException.of(
                    ErrorCode.VALID_ILLEGAL_ARGUMENT, "单次批量排班区间不能超过 " + MAX_BATCH_DAYS + " 天");
        }
        int handled = 0;
        for (Long employeeId : request.getEmployeeIds()) {
            requireEmployee(employeeId);
            for (LocalDate date = request.getStartDate();
                    !date.isAfter(request.getEndDate());
                    date = date.plusDays(1)) {
                HrSchedule schedule = findSchedule(employeeId, date);
                if (schedule == null) {
                    schedule = new HrSchedule();
                    schedule.setEmployeeId(employeeId);
                    schedule.setWorkDate(date);
                    schedule.setShiftCode(request.getShiftCode());
                    schedule.setStartTime(request.getStartTime());
                    schedule.setEndTime(request.getEndTime());
                    schedule.setScheduleType(ScheduleType.FIXED);
                    scheduleMapper.insert(schedule);
                } else {
                    if (StringUtils.isNotBlank(request.getShiftCode())) {
                        schedule.setShiftCode(request.getShiftCode());
                    }
                    if (request.getStartTime() != null) {
                        schedule.setStartTime(request.getStartTime());
                    }
                    if (request.getEndTime() != null) {
                        schedule.setEndTime(request.getEndTime());
                    }
                    scheduleMapper.updateById(schedule);
                }
                handled++;
            }
        }
        log.info("批量排班完成，共处理 {} 条", handled);
        return handled;
    }

    private HrSchedule findSchedule(Long employeeId, LocalDate workDate) {
        return scheduleMapper.selectOne(
                new LambdaQueryWrapper<HrSchedule>()
                        .eq(HrSchedule::getEmployeeId, employeeId)
                        .eq(HrSchedule::getWorkDate, workDate)
                        .last("LIMIT 1"));
    }

    private HrEmployee requireEmployee(Long employeeId) {
        HrEmployee employee = employeeId == null ? null : employeeMapper.selectById(employeeId);
        if (employee == null) {
            throw BusinessException.of(ErrorCode.BIZ_NOT_FOUND, "员工档案不存在");
        }
        return employee;
    }

    /** 解析 yyyy-MM，空值取当前月。 */
    private YearMonth parseMonth(String month) {
        if (StringUtils.isBlank(month)) {
            return YearMonth.now();
        }
        try {
            return YearMonth.parse(month.trim());
        } catch (DateTimeParseException e) {
            throw BusinessException.of(
                    ErrorCode.VALID_ILLEGAL_ARGUMENT, "月份格式应为 yyyy-MM：" + month);
        }
    }
}
