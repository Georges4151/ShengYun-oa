package com.ceams.module.hr.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ceams.common.exception.BusinessException;
import com.ceams.common.exception.ErrorCode;
import com.ceams.module.hr.converter.HrLeaveQuotaConverter;
import com.ceams.module.hr.entity.HrLeaveQuota;
import com.ceams.module.hr.enums.LeaveType;
import com.ceams.module.hr.mapper.HrLeaveQuotaMapper;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** 假期额度单元测试：额度调整重算与扣减额度不足拦截。 */
@ExtendWith(MockitoExtension.class)
class HrLeaveQuotaServiceImplTest {

    @Mock private HrLeaveQuotaMapper quotaMapper;

    private HrLeaveQuotaServiceImpl quotaService;

    @BeforeEach
    void setUp() {
        quotaService =
                new HrLeaveQuotaServiceImpl(
                        quotaMapper, Mappers.getMapper(HrLeaveQuotaConverter.class));
    }

    @Test
    void adjustShouldRecomputeRemainingDays() {
        HrLeaveQuota quota = quota(1L, new BigDecimal("5.0"), new BigDecimal("2.0"));
        when(quotaMapper.selectById(1L)).thenReturn(quota);

        quotaService.adjust(1L, new BigDecimal("8.0"));

        ArgumentCaptor<HrLeaveQuota> captor = ArgumentCaptor.forClass(HrLeaveQuota.class);
        verify(quotaMapper).updateById(captor.capture());
        assertThat(captor.getValue().getTotalDays()).isEqualByComparingTo("8.0");
        assertThat(captor.getValue().getRemainingDays()).isEqualByComparingTo("6.0");
    }

    @Test
    void adjustShouldRejectWhenUsedGreaterThanTotal() {
        HrLeaveQuota quota = quota(2L, new BigDecimal("5.0"), new BigDecimal("4.0"));
        when(quotaMapper.selectById(2L)).thenReturn(quota);

        assertThatThrownBy(() -> quotaService.adjust(2L, new BigDecimal("3.0")))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.BIZ_STATE_CONFLICT);
        verify(quotaMapper, never()).updateById(org.mockito.ArgumentMatchers.any(HrLeaveQuota.class));
    }

    @Test
    void deductShouldThrowWhenQuotaInsufficient() {
        when(quotaMapper.deduct(3L, 2026, LeaveType.ANNUAL.name(), new BigDecimal("2.0")))
                .thenReturn(0);

        assertThatThrownBy(
                        () -> quotaService.deduct(3L, 2026, LeaveType.ANNUAL, new BigDecimal("2.0")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("假期额度不足");
    }

    @Test
    void deductShouldPassWhenQuotaEnough() {
        when(quotaMapper.deduct(4L, 2026, LeaveType.SICK.name(), new BigDecimal("1.5")))
                .thenReturn(1);

        quotaService.deduct(4L, 2026, LeaveType.SICK, new BigDecimal("1.5"));

        verify(quotaMapper).deduct(4L, 2026, LeaveType.SICK.name(), new BigDecimal("1.5"));
    }

    private HrLeaveQuota quota(Long id, BigDecimal total, BigDecimal used) {
        HrLeaveQuota quota = new HrLeaveQuota();
        quota.setId(id);
        quota.setEmployeeId(100L);
        quota.setYear(2026);
        quota.setLeaveType(LeaveType.ANNUAL);
        quota.setTotalDays(total);
        quota.setUsedDays(used);
        quota.setRemainingDays(total.subtract(used));
        return quota;
    }
}
