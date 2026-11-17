package com.ceams.module.hr.converter;

import com.ceams.module.hr.entity.HrCheckin;
import com.ceams.module.hr.vo.CheckinVO;
import java.util.List;
import org.mapstruct.Mapper;

/** 打卡记录实体 <-> VO 转换。 */
@Mapper(componentModel = "spring")
public interface HrCheckinConverter {

    default CheckinVO toVO(HrCheckin checkin) {
        if (checkin == null) {
            return null;
        }
        CheckinVO vo = new CheckinVO();
        vo.setId(checkin.getId());
        vo.setEmployeeId(checkin.getEmployeeId());
        vo.setCheckinDate(checkin.getCheckinDate());
        vo.setCheckinTime(checkin.getCheckinTime());
        vo.setCheckinType(checkin.getCheckinType());
        vo.setLatitude(checkin.getLatitude());
        vo.setLongitude(checkin.getLongitude());
        vo.setWifiSsid(checkin.getWifiSsid());
        vo.setWifiBssid(checkin.getWifiBssid());
        vo.setAddress(checkin.getAddress());
        vo.setSource(checkin.getSource());
        vo.setStatus(checkin.getStatus());
        vo.setRemark(checkin.getRemark());
        return vo;
    }

    List<CheckinVO> toVOList(List<HrCheckin> checkins);
}
