package com.ceams.module.hr.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ceams.module.bpm.service.BpmInstanceService;
import com.ceams.module.hr.converter.HrCheckinConverter;
import com.ceams.module.hr.converter.HrCheckinSupplementConverter;
import com.ceams.module.hr.entity.HrCheckin;
import com.ceams.module.hr.entity.HrCheckinSupplement;
import com.ceams.module.hr.entity.HrEmployee;
import com.ceams.module.hr.enums.CheckinStatus;
import com.ceams.module.hr.mapper.HrCheckinMapper;
import com.ceams.module.hr.mapper.HrCheckinSupplementMapper;
import com.ceams.module.hr.mapper.HrEmployeeMapper;
import com.ceams.module.hr.mapper.HrScheduleMapper;
import com.ceams.module.hr.service.HrEmployeeService;
import com.ceams.module.hr.support.HrCheckinProperties;
import com.ceams.module.hr.vo.AttendanceSummaryVO;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** 考勤汇总单元测试：状态计数、补卡通过计数、部门解析与空集合短路。 */
@ExtendWith(MockitoExtension.class)
class HrCheckinSummaryTest {

    private static final LocalDate START = LocalDate.of(2026, 11, 1);
    private static final LocalDate END = LocalDate.of(2026, 11, 30);

    @Mock private HrCheckinMapper checkinMapper;
    @Mock private HrCheckinSupplementMapper supplementMapper;
    @Mock private HrScheduleMapper scheduleMapper;
    @Mock private HrEmployeeService employeeService;
    @Mock private BpmInstanceService bpmInstanceService;
    @Mock private HrEmployeeMapper employeeMapper;

    private HrCheckinServiceImpl checkinService;

    @BeforeEach
    void setUp() {
        checkinService =
                new HrCheckinServiceImpl(
                        checkinMapper,
                        supplementMapper,
                        scheduleMapper,
                        employeeService,
                        Mappers.getMapper(HrCheckinConverter.class),
                        Mappers.getMapper(HrCheckinSupplementConverter.class),
                        new HrCheckinProperties(),
                        bpmInstanceService,
                        employeeMapper);
    }

    @Test
    void shouldCountStatusesAndApprovedSupplements() {
        when(checkinMapper.selectList(any()))
                .thenReturn(
                        List.of(
                                checkin(1L, CheckinStatus.NORMAL),
                                checkin(1L, CheckinStatus.LATE),
                                checkin(2L, CheckinStatus.EARLY),
                                checkin(2L, CheckinStatus.ABSENT)));
        when(supplementMapper.selectList(any()))
                .thenReturn(List.of(supplement(1L), supplement(2L)));

        AttendanceSummaryVO summary = checkinService.summary(START, END, null, null);

        assertThat(summary.getEmployeeCount()).isEqualTo(2L);
        assertThat(summary.getNormalCount()).isEqualTo(1L);
        assertThat(summary.getLateCount()).isEqualTo(1L);
        assertThat(summary.getEarlyCount()).isEqualTo(1L);
        assertThat(summary.getAbsentCount()).isEqualTo(1L);
        assertThat(summary.getSupplementCount()).isEqualTo(2L);
    }

    @Test
    void shouldResolveDeptToEmployeeScope() {
        when(employeeMapper.selectList(any())).thenReturn(List.of(employee(7L, 3L)));
        when(checkinMapper.selectList(any()))
                .thenReturn(List.of(checkin(7L, CheckinStatus.NORMAL)));
        when(supplementMapper.selectList(any())).thenReturn(List.of());

        AttendanceSummaryVO summary = checkinService.summary(START, END, 3L, null);

        assertThat(summary.getEmployeeCount()).isEqualTo(1L);
        assertThat(summary.getNormalCount()).isEqualTo(1L);
    }

    @Test
    void shouldReturnZeroWhenDeptHasNoEmployee() {
        when(employeeMapper.selectList(any())).thenReturn(List.of());

        AttendanceSummaryVO summary = checkinService.summary(START, END, 3L, null);

        assertThat(summary.getEmployeeCount()).isZero();
        assertThat(summary.getNormalCount()).isZero();
        assertThat(summary.getSupplementCount()).isZero();
        // 部门下无员工时不应再查打卡 / 补卡表
        verify(checkinMapper, never()).selectList(any());
        verify(supplementMapper, never()).selectList(any());
    }

    @Test
    void shouldReturnZeroWhenEmployeeNotInDeptScope() {
        when(employeeMapper.selectList(any())).thenReturn(List.of(employee(7L, 3L)));

        AttendanceSummaryVO summary = checkinService.summary(START, END, 3L, 99L);

        assertThat(summary.getEmployeeCount()).isZero();
        verify(checkinMapper, never()).selectList(any());
    }

    private HrCheckin checkin(Long employeeId, CheckinStatus status) {
        HrCheckin checkin = new HrCheckin();
        checkin.setEmployeeId(employeeId);
        checkin.setStatus(status);
        return checkin;
    }

    private HrCheckinSupplement supplement(Long employeeId) {
        HrCheckinSupplement supplement = new HrCheckinSupplement();
        supplement.setEmployeeId(employeeId);
        return supplement;
    }

    private HrEmployee employee(Long id, Long deptId) {
        HrEmployee employee = new HrEmployee();
        employee.setId(id);
        employee.setDeptId(deptId);
        return employee;
    }
}
