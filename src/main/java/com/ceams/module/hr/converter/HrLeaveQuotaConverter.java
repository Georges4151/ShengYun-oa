package com.ceams.module.hr.converter;

import com.ceams.module.hr.entity.HrLeaveQuota;
import com.ceams.module.hr.vo.LeaveQuotaVO;
import java.util.List;
import org.mapstruct.Mapper;

/** 假期额度实体 <-> VO 转换。 */
@Mapper(componentModel = "spring")
public interface HrLeaveQuotaConverter {

    default LeaveQuotaVO toVO(HrLeaveQuota quota) {
        if (quota == null) {
            return null;
        }
        LeaveQuotaVO vo = new LeaveQuotaVO();
        vo.setId(quota.getId());
        vo.setEmployeeId(quota.getEmployeeId());
        vo.setYear(quota.getYear());
        vo.setLeaveType(quota.getLeaveType());
        vo.setTotalDays(quota.getTotalDays());
        vo.setUsedDays(quota.getUsedDays());
        vo.setRemainingDays(quota.getRemainingDays());
        vo.setExpireDate(quota.getExpireDate());
        return vo;
    }

    List<LeaveQuotaVO> toVOList(List<HrLeaveQuota> quotas);
}
