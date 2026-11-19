package com.ceams.module.hr.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ceams.common.exception.BusinessException;
import com.ceams.common.exception.ErrorCode;
import com.ceams.module.hr.converter.HrLeaveQuotaConverter;
import com.ceams.module.hr.entity.HrLeaveQuota;
import com.ceams.module.hr.enums.LeaveType;
import com.ceams.module.hr.mapper.HrLeaveQuotaMapper;
import com.ceams.module.hr.service.HrLeaveQuotaService;
import com.ceams.module.hr.vo.LeaveQuotaVO;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 假期额度服务实现。
 *
 * <p>扣减使用带 {@code remaining_days >= days} 条件的原子 UPDATE，
 * 由数据库保证并发下不出现负库存。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HrLeaveQuotaServiceImpl implements HrLeaveQuotaService {

    /** 初始化默认额度：年假 5 天。 */
    private static final BigDecimal DEFAULT_ANNUAL_DAYS = new BigDecimal("5.0");

    /** 初始化默认额度：病假 10 天。 */
    private static final BigDecimal DEFAULT_SICK_DAYS = new BigDecimal("10.0");

    /** 初始化默认额度：事假 5 天。 */
    private static final BigDecimal DEFAULT_PERSONAL_DAYS = new BigDecimal("5.0");

    private final HrLeaveQuotaMapper quotaMapper;
    private final HrLeaveQuotaConverter quotaConverter;

    @Override
    public List<LeaveQuotaVO> list(Long employeeId, Integer year) {
        int targetYear = year == null ? LocalDate.now().getYear() : year;
        LambdaQueryWrapper<HrLeaveQuota> wrapper = new LambdaQueryWrapper<>();
        if (employeeId != null) {
            wrapper.eq(HrLeaveQuota::getEmployeeId, employeeId);
        }
        wrapper.eq(HrLeaveQuota::getYear, targetYear).orderByAsc(HrLeaveQuota::getLeaveType);
        return quotaConverter.toVOList(quotaMapper.selectList(wrapper));
    }

    @Override
    @Transactional
    public void adjust(Long id, BigDecimal totalDays) {
        HrLeaveQuota quota = quotaMapper.selectById(id);
        if (quota == null) {
            throw BusinessException.of(ErrorCode.BIZ_NOT_FOUND, "假期额度不存在");
        }
        BigDecimal total = totalDays == null ? BigDecimal.ZERO : totalDays;
        BigDecimal used = quota.getUsedDays() == null ? BigDecimal.ZERO : quota.getUsedDays();
        if (used.compareTo(total) > 0) {
            throw BusinessException.of(
                    ErrorCode.BIZ_STATE_CONFLICT, "已使用天数大于调整后的总额度，无法调整");
        }
        quota.setTotalDays(total);
        quota.setRemainingDays(total.subtract(used));
        quotaMapper.updateById(quota);
        log.info("调整假期额度 {}：总额度 {}，剩余 {}", quota.getId(), total, quota.getRemainingDays());
    }

    @Override
    @Transactional
    public void init(Long employeeId, Integer year) {
        int targetYear = year == null ? LocalDate.now().getYear() : year;
        Map<LeaveType, BigDecimal> defaults = new LinkedHashMap<>();
        defaults.put(LeaveType.ANNUAL, DEFAULT_ANNUAL_DAYS);
        defaults.put(LeaveType.SICK, DEFAULT_SICK_DAYS);
        defaults.put(LeaveType.PERSONAL, DEFAULT_PERSONAL_DAYS);
        LocalDate expireDate = LocalDate.of(targetYear, 12, 31);
        defaults.forEach(
                (leaveType, totalDays) -> {
                    Long exists =
                            quotaMapper.selectCount(
                                    new LambdaQueryWrapper<HrLeaveQuota>()
                                            .eq(HrLeaveQuota::getEmployeeId, employeeId)
                                            .eq(HrLeaveQuota::getYear, targetYear)
                                            .eq(HrLeaveQuota::getLeaveType, leaveType));
                    if (exists != null && exists > 0) {
                        log.info(
                                "员工 {} {} 年 {} 额度已存在，跳过初始化",
                                employeeId,
                                targetYear,
                                leaveType);
                        return;
                    }
                    HrLeaveQuota quota = new HrLeaveQuota();
                    quota.setEmployeeId(employeeId);
                    quota.setYear(targetYear);
                    quota.setLeaveType(leaveType);
                    quota.setTotalDays(totalDays);
                    quota.setUsedDays(BigDecimal.ZERO);
                    quota.setRemainingDays(totalDays);
                    quota.setExpireDate(expireDate);
                    quotaMapper.insert(quota);
                });
    }

    @Override
    @Transactional
    public void deduct(Long employeeId, Integer year, LeaveType leaveType, BigDecimal days) {
        if (days == null || days.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        int targetYear = year == null ? LocalDate.now().getYear() : year;
        int affected = quotaMapper.deduct(employeeId, targetYear, leaveType.name(), days);
        if (affected == 0) {
            throw BusinessException.of(
                    ErrorCode.BIZ_STATE_CONFLICT, "假期额度不足，无法完成扣减（" + leaveType + "）");
        }
        log.info("员工 {} {} 年 {} 扣减 {} 天", employeeId, targetYear, leaveType, days);
    }
}
