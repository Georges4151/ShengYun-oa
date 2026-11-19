package com.ceams.module.hr.converter;

import com.ceams.module.hr.entity.HrCheckinSupplement;
import com.ceams.module.hr.vo.CheckinSupplementVO;
import java.util.List;
import org.mapstruct.Mapper;

/** 补卡申请单实体 <-> VO 转换。 */
@Mapper(componentModel = "spring")
public interface HrCheckinSupplementConverter {

    default CheckinSupplementVO toVO(HrCheckinSupplement supplement) {
        if (supplement == null) {
            return null;
        }
        CheckinSupplementVO vo = new CheckinSupplementVO();
        vo.setId(supplement.getId());
        vo.setEmployeeId(supplement.getEmployeeId());
        vo.setCheckinDate(supplement.getCheckinDate());
        vo.setCheckinTime(supplement.getCheckinTime());
        vo.setCheckinType(supplement.getCheckinType());
        vo.setReason(supplement.getReason());
        vo.setApprovalStatus(supplement.getApprovalStatus());
        vo.setBpmInstanceId(supplement.getBpmInstanceId());
        vo.setCreateTime(supplement.getCreateTime());
        return vo;
    }

    List<CheckinSupplementVO> toVOList(List<HrCheckinSupplement> supplements);
}
