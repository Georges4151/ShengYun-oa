package com.ceams.module.hr.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.ceams.common.exception.BusinessException;
import com.ceams.common.exception.ErrorCode;
import com.ceams.module.bpm.service.BpmInstanceService;
import com.ceams.module.hr.converter.HrCheckinConverter;
import com.ceams.module.hr.converter.HrCheckinSupplementConverter;
import com.ceams.module.hr.dto.CheckinClockRequest;
import com.ceams.module.hr.entity.HrCheckin;
import com.ceams.module.hr.entity.HrSchedule;
import com.ceams.module.hr.enums.CheckinStatus;
import com.ceams.module.hr.enums.CheckinType;
import com.ceams.module.hr.mapper.HrCheckinMapper;
import com.ceams.module.hr.mapper.HrCheckinSupplementMapper;
import com.ceams.module.hr.mapper.HrEmployeeMapper;
import com.ceams.module.hr.mapper.HrScheduleMapper;
import com.ceams.module.hr.service.HrEmployeeService;
import com.ceams.module.hr.support.HrCheckinProperties;
import com.ceams.module.hr.vo.CheckinVO;
import java.time.LocalDate;
import java.time.LocalTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** 打卡服务单元测试：迟到/早退/正常判定与当天重复打卡冲突。 */
@ExtendWith(MockitoExtension.class)
class HrCheckinServiceImplTest {

    private static final Long EMPLOYEE_ID = 501L;

    @Mock private HrCheckinMapper checkinMapper;
    @Mock private HrCheckinSupplementMapper supplementMapper;
    @Mock private HrScheduleMapper scheduleMapper;
    @Mock private HrEmployeeService employeeService;
    @Mock private BpmInstanceService bpmInstanceService;
    @Mock private HrEmployeeMapper employeeMapper;

    private HrCheckinProperties properties;
    private HrCheckinServiceImpl checkinService;

    @BeforeEach
    void setUp() {
        properties = new HrCheckinProperties();
        properties.setGraceMinutes(0);
        checkinService =
                new HrCheckinServiceImpl(
                        checkinMapper,
                        supplementMapper,
                        scheduleMapper,
                        employeeService,
                        Mappers.getMapper(HrCheckinConverter.class),
                        Mappers.getMapper(HrCheckinSupplementConverter.class),
                        properties,
                        bpmInstanceService,
                        employeeMapper);
    }

    @Test
    void clockInAfterShiftStartShouldBeLate() {
        mockEmployeeAndSchedule(LocalTime.MIDNIGHT, LocalTime.MIDNIGHT);
        when(checkinMapper.selectCount(any(Wrapper.class))).thenReturn(0L);

        CheckinVO vo = checkinService.clock(clockRequest(CheckinType.IN));

        assertThat(vo.getStatus()).isEqualTo(CheckinStatus.LATE);
        assertThat(vo.getCheckinType()).isEqualTo(CheckinType.IN);
        assertThat(vo.getCheckinDate()).isEqualTo(LocalDate.now());
    }

    @Test
    void clockOutBeforeShiftEndShouldBeEarly() {
        mockEmployeeAndSchedule(LocalTime.MIDNIGHT, LocalTime.of(23, 59));
        when(checkinMapper.selectCount(any(Wrapper.class))).thenReturn(0L);

        CheckinVO vo = checkinService.clock(clockRequest(CheckinType.OUT));

        assertThat(vo.getStatus()).isEqualTo(CheckinStatus.EARLY);
    }

    @Test
    void clockInWithinGraceShouldBeNormal() {
        mockEmployeeAndSchedule(LocalTime.of(23, 59), LocalTime.MIDNIGHT);
        when(checkinMapper.selectCount(any(Wrapper.class))).thenReturn(0L);

        CheckinVO vo = checkinService.clock(clockRequest(CheckinType.IN));

        assertThat(vo.getStatus()).isEqualTo(CheckinStatus.NORMAL);
    }

    @Test
    void duplicateClockInSameDayShouldConflict() {
        when(employeeService.currentEmployeeId()).thenReturn(EMPLOYEE_ID);
        when(checkinMapper.selectCount(any(Wrapper.class))).thenReturn(1L);

        assertThatThrownBy(() -> checkinService.clock(clockRequest(CheckinType.IN)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.BIZ_STATE_CONFLICT);
    }

    private void mockEmployeeAndSchedule(LocalTime start, LocalTime end) {
        when(employeeService.currentEmployeeId()).thenReturn(EMPLOYEE_ID);
        HrSchedule schedule = new HrSchedule();
        schedule.setEmployeeId(EMPLOYEE_ID);
        schedule.setWorkDate(LocalDate.now());
        schedule.setStartTime(start);
        schedule.setEndTime(end);
        when(scheduleMapper.selectOne(any(Wrapper.class))).thenReturn(schedule);
    }

    private CheckinClockRequest clockRequest(CheckinType type) {
        CheckinClockRequest request = new CheckinClockRequest();
        request.setCheckinType(type);
        return request;
    }
}
