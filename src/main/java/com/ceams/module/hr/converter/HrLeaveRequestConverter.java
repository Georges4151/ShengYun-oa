package com.ceams.module.hr.converter;

import com.ceams.module.hr.entity.HrLeaveRequest;
import com.ceams.module.hr.vo.LeaveRequestVO;
import java.util.List;
import org.mapstruct.Mapper;

/** 请假申请单实体 <-> VO 转换。 */
@Mapper(componentModel = "spring")
public interface HrLeaveRequestConverter {

    default LeaveRequestVO toVO(HrLeaveRequest request) {
        if (request == null) {
            return null;
        }
        LeaveRequestVO vo = new LeaveRequestVO();
        vo.setId(request.getId());
        vo.setEmployeeId(request.getEmployeeId());
        vo.setLeaveType(request.getLeaveType());
        vo.setStartDate(request.getStartDate());
        vo.setEndDate(request.getEndDate());
        vo.setDays(request.getDays());
        vo.setReason(request.getReason());
        vo.setApprovalStatus(request.getApprovalStatus());
        vo.setBpmInstanceId(request.getBpmInstanceId());
        vo.setCreateTime(request.getCreateTime());
        return vo;
    }

    List<LeaveRequestVO> toVOList(List<HrLeaveRequest> requests);
}
