package com.ceams.module.hr.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ceams.common.exception.BusinessException;
import com.ceams.common.exception.ErrorCode;
import com.ceams.common.response.PageResult;
import com.ceams.module.bpm.dto.InstanceStartRequest;
import com.ceams.module.bpm.enums.BpmBizType;
import com.ceams.module.bpm.service.BpmInstanceService;
import com.ceams.module.hr.converter.HrCheckinConverter;
import com.ceams.module.hr.converter.HrCheckinSupplementConverter;
import com.ceams.module.hr.dto.CheckinClockRequest;
import com.ceams.module.hr.dto.CheckinSupplementCreateRequest;
import com.ceams.module.hr.entity.HrCheckin;
import com.ceams.module.hr.entity.HrCheckinSupplement;
import com.ceams.module.hr.entity.HrEmployee;
import com.ceams.module.hr.entity.HrSchedule;
import com.ceams.module.hr.enums.CheckinSource;
import com.ceams.module.hr.enums.CheckinStatus;
import com.ceams.module.hr.enums.CheckinType;
import com.ceams.module.hr.enums.HrApprovalStatus;
import com.ceams.module.hr.mapper.HrCheckinMapper;
import com.ceams.module.hr.mapper.HrCheckinSupplementMapper;
import com.ceams.module.hr.mapper.HrEmployeeMapper;
import com.ceams.module.hr.mapper.HrScheduleMapper;
import com.ceams.module.hr.service.HrCheckinService;
import com.ceams.module.hr.service.HrEmployeeService;
import com.ceams.module.hr.support.HrCheckinProperties;
import com.ceams.module.hr.util.HrPageUtil;
import com.ceams.module.hr.vo.AttendanceSummaryVO;
import com.ceams.module.hr.vo.CheckinSupplementVO;
import com.ceams.module.hr.vo.CheckinVO;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 打卡与补卡服务实现。
 *
 * <p>迟到/早退判定口径：以上班时间 + 宽限分钟为界，晚于判 LATE；以下班时间 - 宽限分钟为界，
 * 早于判 EARLY；其余为 NORMAL。本期不校验 WiFi 与地理位置，仅留痕。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HrCheckinServiceImpl implements HrCheckinService {

    /** 补卡流程模板编码。 */
    private static final String SUPPLEMENT_TEMPLATE = BpmBizType.HR_CHECKIN_SUPPLEMENT.name();

    private final HrCheckinMapper checkinMapper;
    private final HrCheckinSupplementMapper supplementMapper;
    private final HrScheduleMapper scheduleMapper;
    private final HrEmployeeService employeeService;
    private final HrCheckinConverter checkinConverter;
    private final HrCheckinSupplementConverter supplementConverter;
    private final HrCheckinProperties checkinProperties;
    private final BpmInstanceService bpmInstanceService;
    private final HrEmployeeMapper employeeMapper;

    @Override
    @Transactional
    public CheckinVO clock(CheckinClockRequest request) {
        Long employeeId = employeeService.currentEmployeeId();
        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();
        HrSchedule schedule = findSchedule(employeeId, today);
        if (schedule == null && checkinProperties.isRequireSchedule()) {
            throw BusinessException.of(ErrorCode.BIZ_STATE_CONFLICT, "今日无排班，不允许打卡");
        }
        Long duplicated =
                checkinMapper.selectCount(
                        new LambdaQueryWrapper<HrCheckin>()
                                .eq(HrCheckin::getEmployeeId, employeeId)
                                .eq(HrCheckin::getCheckinDate, today)
                                .eq(HrCheckin::getCheckinType, request.getCheckinType()));
        if (duplicated != null && duplicated > 0) {
            throw BusinessException.of(
                    ErrorCode.BIZ_STATE_CONFLICT,
                    "今日已存在" + label(request.getCheckinType()) + "打卡记录，请勿重复打卡");
        }
        LocalTime shiftStart =
                schedule != null && schedule.getStartTime() != null
                        ? schedule.getStartTime()
                        : checkinProperties.getDefaultStart();
        LocalTime shiftEnd =
                schedule != null && schedule.getEndTime() != null
                        ? schedule.getEndTime()
                        : checkinProperties.getDefaultEnd();
        CheckinStatus status = judge(request.getCheckinType(), today, now, shiftStart, shiftEnd);

        HrCheckin checkin = new HrCheckin();
        checkin.setEmployeeId(employeeId);
        checkin.setCheckinDate(today);
        checkin.setCheckinTime(now);
        checkin.setCheckinType(request.getCheckinType());
        checkin.setLatitude(request.getLatitude());
        checkin.setLongitude(request.getLongitude());
        checkin.setWifiSsid(request.getWifiSsid());
        checkin.setWifiBssid(request.getWifiBssid());
        checkin.setAddress(request.getAddress());
        checkin.setSource(
                request.getSource() == null ? CheckinSource.MINI : request.getSource());
        checkin.setStatus(status);
        checkinMapper.insert(checkin);
        log.info(
                "员工 {} {} 打卡成功，时间 {}，状态 {}",
                employeeId,
                request.getCheckinType(),
                now,
                status);
        return checkinConverter.toVO(checkin);
    }

    @Override
    public List<CheckinVO> my(String month) {
        Long employeeId = employeeService.currentEmployeeId();
        YearMonth yearMonth = parseMonth(month);
        List<HrCheckin> checkins =
                checkinMapper.selectList(
                        new LambdaQueryWrapper<HrCheckin>()
                                .eq(HrCheckin::getEmployeeId, employeeId)
                                .ge(HrCheckin::getCheckinDate, yearMonth.atDay(1))
                                .le(HrCheckin::getCheckinDate, yearMonth.atEndOfMonth())
                                .orderByAsc(HrCheckin::getCheckinDate)
                                .orderByAsc(HrCheckin::getCheckinTime));
        return checkinConverter.toVOList(checkins);
    }

    @Override
    public PageResult<CheckinVO> page(
            long page,
            long size,
            Long employeeId,
            LocalDate startDate,
            LocalDate endDate,
            CheckinStatus status) {
        Page<HrCheckin> p = HrPageUtil.resolve(page, size);
        LambdaQueryWrapper<HrCheckin> wrapper = new LambdaQueryWrapper<>();
        if (employeeId != null) {
            wrapper.eq(HrCheckin::getEmployeeId, employeeId);
        }
        if (startDate != null) {
            wrapper.ge(HrCheckin::getCheckinDate, startDate);
        }
        if (endDate != null) {
            wrapper.le(HrCheckin::getCheckinDate, endDate);
        }
        if (status != null) {
            wrapper.eq(HrCheckin::getStatus, status);
        }
        wrapper.orderByDesc(HrCheckin::getCheckinDate).orderByDesc(HrCheckin::getCheckinTime);
        IPage<HrCheckin> result = checkinMapper.selectPage(p, wrapper);
        return new PageResult<>(
                checkinConverter.toVOList(result.getRecords()),
                result.getTotal(),
                result.getCurrent(),
                result.getSize());
    }

    @Override
    @Transactional
    public String createSupplement(CheckinSupplementCreateRequest request) {
        Long employeeId = employeeService.currentEmployeeId();
        HrCheckinSupplement supplement = new HrCheckinSupplement();
        supplement.setEmployeeId(employeeId);
        supplement.setCheckinDate(request.getCheckinDate());
        supplement.setCheckinTime(request.getCheckinTime());
        supplement.setCheckinType(request.getCheckinType());
        supplement.setReason(request.getReason());
        supplement.setApprovalStatus(HrApprovalStatus.PENDING);
        supplementMapper.insert(supplement);

        InstanceStartRequest start = new InstanceStartRequest();
        start.setTemplateCode(SUPPLEMENT_TEMPLATE);
        start.setBizType(SUPPLEMENT_TEMPLATE);
        start.setBizId(supplement.getId());
        Map<String, String> variables = new LinkedHashMap<>();
        variables.put("employeeId", String.valueOf(employeeId));
        variables.put("checkinDate", String.valueOf(request.getCheckinDate()));
        variables.put("checkinType", request.getCheckinType().name());
        start.setVariables(variables);
        String instanceId = bpmInstanceService.start(start);

        supplement.setBpmInstanceId(parseInstanceId(instanceId));
        supplementMapper.updateById(supplement);
        log.info("员工 {} 提交补卡申请 {}，BPM 实例 {}", employeeId, supplement.getId(), instanceId);
        return String.valueOf(supplement.getId());
    }

    @Override
    public List<CheckinSupplementVO> mySupplements() {
        Long employeeId = employeeService.currentEmployeeId();
        List<HrCheckinSupplement> supplements =
                supplementMapper.selectList(
                        new LambdaQueryWrapper<HrCheckinSupplement>()
                                .eq(HrCheckinSupplement::getEmployeeId, employeeId)
                                .orderByDesc(HrCheckinSupplement::getCreateTime));
        return supplementConverter.toVOList(supplements);
    }

    @Override
    public AttendanceSummaryVO summary(
            LocalDate start, LocalDate end, Long deptId, Long employeeId) {
        AttendanceSummaryVO summary = new AttendanceSummaryVO();

        // 部门 → 员工集合：部门下无员工时无需再查打卡表，直接返回全 0
        Set<Long> scope = null;
        if (deptId != null) {
            List<HrEmployee> employees =
                    employeeMapper.selectList(
                            new LambdaQueryWrapper<HrEmployee>()
                                    .eq(HrEmployee::getDeptId, deptId));
            scope =
                    employees.stream()
                            .map(HrEmployee::getId)
                            .filter(Objects::nonNull)
                            .collect(Collectors.toSet());
            if (scope.isEmpty()) {
                return summary;
            }
            if (employeeId != null && !scope.contains(employeeId)) {
                return summary;
            }
        }

        LambdaQueryWrapper<HrCheckin> checkinWrapper = new LambdaQueryWrapper<>();
        if (employeeId != null) {
            checkinWrapper.eq(HrCheckin::getEmployeeId, employeeId);
        } else if (scope != null) {
            checkinWrapper.in(HrCheckin::getEmployeeId, scope);
        }
        if (start != null) {
            checkinWrapper.ge(HrCheckin::getCheckinDate, start);
        }
        if (end != null) {
            checkinWrapper.le(HrCheckin::getCheckinDate, end);
        }
        List<HrCheckin> checkins = checkinMapper.selectList(checkinWrapper);

        Set<Long> employeeIds = new HashSet<>();
        for (HrCheckin checkin : checkins) {
            if (checkin.getEmployeeId() != null) {
                employeeIds.add(checkin.getEmployeeId());
            }
            CheckinStatus status = checkin.getStatus();
            if (status == null) {
                continue;
            }
            switch (status) {
                case NORMAL -> summary.setNormalCount(summary.getNormalCount() + 1);
                case LATE -> summary.setLateCount(summary.getLateCount() + 1);
                case EARLY -> summary.setEarlyCount(summary.getEarlyCount() + 1);
                case ABSENT -> summary.setAbsentCount(summary.getAbsentCount() + 1);
                default -> {
                    // 枚举已穷举，保留分支防御未来扩展
                }
            }
        }
        summary.setEmployeeCount(employeeIds.size());

        LambdaQueryWrapper<HrCheckinSupplement> supplementWrapper =
                new LambdaQueryWrapper<HrCheckinSupplement>()
                        .eq(
                                HrCheckinSupplement::getApprovalStatus,
                                HrApprovalStatus.APPROVED);
        if (employeeId != null) {
            supplementWrapper.eq(HrCheckinSupplement::getEmployeeId, employeeId);
        } else if (scope != null) {
            supplementWrapper.in(HrCheckinSupplement::getEmployeeId, scope);
        }
        if (start != null) {
            supplementWrapper.ge(HrCheckinSupplement::getCheckinDate, start);
        }
        if (end != null) {
            supplementWrapper.le(HrCheckinSupplement::getCheckinDate, end);
        }
        summary.setSupplementCount(supplementMapper.selectList(supplementWrapper).size());
        return summary;
    }

    /**
     * 迟到/早退判定。
     *
     * @param type 打卡类型
     * @param date 打卡日期
     * @param now 打卡时间
     * @param shiftStart 上班时间
     * @param shiftEnd 下班时间
     */
    private CheckinStatus judge(
            CheckinType type,
            LocalDate date,
            LocalDateTime now,
            LocalTime shiftStart,
            LocalTime shiftEnd) {
        int grace = Math.max(checkinProperties.getGraceMinutes(), 0);
        if (type == CheckinType.IN) {
            LocalDateTime latest = LocalDateTime.of(date, shiftStart).plusMinutes(grace);
            return now.isAfter(latest) ? CheckinStatus.LATE : CheckinStatus.NORMAL;
        }
        LocalDateTime earliest = LocalDateTime.of(date, shiftEnd).minusMinutes(grace);
        return now.isBefore(earliest) ? CheckinStatus.EARLY : CheckinStatus.NORMAL;
    }

    private HrSchedule findSchedule(Long employeeId, LocalDate workDate) {
        return scheduleMapper.selectOne(
                new LambdaQueryWrapper<HrSchedule>()
                        .eq(HrSchedule::getEmployeeId, employeeId)
                        .eq(HrSchedule::getWorkDate, workDate)
                        .last("LIMIT 1"));
    }

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

    private Long parseInstanceId(String instanceId) {
        if (StringUtils.isBlank(instanceId)) {
            return null;
        }
        try {
            return Long.valueOf(instanceId.trim());
        } catch (NumberFormatException e) {
            log.warn("BPM 实例 ID 非数字，无法回写补卡申请：{}", instanceId);
            return null;
        }
    }

    private String label(CheckinType type) {
        return type == CheckinType.IN ? "上班" : "下班";
    }
}
