package com.ceams.module.hr.converter;

import com.ceams.module.hr.entity.HrSchedule;
import com.ceams.module.hr.vo.ScheduleVO;
import java.util.List;
import org.mapstruct.Mapper;

/** 排班实体 <-> VO 转换。 */
@Mapper(componentModel = "spring")
public interface HrScheduleConverter {

    default ScheduleVO toVO(HrSchedule schedule) {
        if (schedule == null) {
            return null;
        }
        ScheduleVO vo = new ScheduleVO();
        vo.setId(schedule.getId());
        vo.setEmployeeId(schedule.getEmployeeId());
        vo.setWorkDate(schedule.getWorkDate());
        vo.setShiftCode(schedule.getShiftCode());
        vo.setStartTime(schedule.getStartTime());
        vo.setEndTime(schedule.getEndTime());
        vo.setScheduleType(schedule.getScheduleType());
        vo.setRemark(schedule.getRemark());
        return vo;
    }

    List<ScheduleVO> toVOList(List<HrSchedule> schedules);
}
