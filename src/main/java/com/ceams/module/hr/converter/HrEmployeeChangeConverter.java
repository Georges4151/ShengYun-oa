package com.ceams.module.hr.converter;

import com.ceams.module.hr.entity.HrEmployeeChange;
import com.ceams.module.hr.vo.EmployeeChangeVO;
import java.util.List;
import org.mapstruct.Mapper;

/** 入转调离申请单实体 <-> VO 转换。 */
@Mapper(componentModel = "spring")
public interface HrEmployeeChangeConverter {

    default EmployeeChangeVO toVO(HrEmployeeChange change) {
        if (change == null) {
            return null;
        }
        EmployeeChangeVO vo = new EmployeeChangeVO();
        vo.setId(change.getId());
        vo.setEmployeeId(change.getEmployeeId());
        vo.setChangeType(change.getChangeType());
        vo.setTargetDeptId(change.getTargetDeptId());
        vo.setTargetPositionId(change.getTargetPositionId());
        vo.setTargetLevelId(change.getTargetLevelId());
        vo.setResignLeaderId(change.getResignLeaderId());
        vo.setEffectiveDate(change.getEffectiveDate());
        vo.setReason(change.getReason());
        vo.setApprovalStatus(change.getApprovalStatus());
        vo.setBpmInstanceId(change.getBpmInstanceId());
        vo.setRemark(change.getRemark());
        vo.setCreateTime(change.getCreateTime());
        return vo;
    }

    List<EmployeeChangeVO> toVOList(List<HrEmployeeChange> changes);
}
