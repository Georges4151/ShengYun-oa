package com.ceams.module.hr.converter;

import com.ceams.common.mask.SensitiveMaskUtil;
import com.ceams.module.hr.entity.HrEmployee;
import com.ceams.module.hr.vo.EmployeeVO;
import java.util.List;
import org.mapstruct.Mapper;

/** 员工档案实体 <-> VO 转换（敏感字段统一脱敏，密文不出接口）。 */
@Mapper(componentModel = "spring")
public interface HrEmployeeConverter {

    default EmployeeVO toVO(HrEmployee employee) {
        if (employee == null) {
            return null;
        }
        EmployeeVO vo = new EmployeeVO();
        vo.setId(employee.getId());
        vo.setUserId(employee.getUserId());
        vo.setEmployeeNo(employee.getEmployeeNo());
        vo.setName(employee.getName());
        vo.setGender(employee.getGender());
        vo.setBirthDate(employee.getBirthDate());
        vo.setIdCard(SensitiveMaskUtil.maskIdCard(employee.getIdCardEnc()));
        vo.setPhone(SensitiveMaskUtil.maskPhone(employee.getPhoneEnc()));
        vo.setEmail(SensitiveMaskUtil.maskEmail(employee.getEmail()));
        vo.setDeptId(employee.getDeptId());
        vo.setPositionId(employee.getPositionId());
        vo.setLeaderId(employee.getLeaderId());
        vo.setLevelId(employee.getLevelId());
        vo.setEmploymentType(employee.getEmploymentType());
        vo.setHireDate(employee.getHireDate());
        vo.setProbationEndDate(employee.getProbationEndDate());
        vo.setRegularDate(employee.getRegularDate());
        vo.setLeaveDate(employee.getLeaveDate());
        vo.setStatus(employee.getStatus());
        vo.setEmergencyContact(employee.getEmergencyContact());
        vo.setEmergencyPhone(SensitiveMaskUtil.maskPhone(employee.getEmergencyPhoneEnc()));
        vo.setIdCardFileId(employee.getIdCardFileId());
        vo.setRemark(employee.getRemark());
        vo.setCreateTime(employee.getCreateTime());
        return vo;
    }

    List<EmployeeVO> toVOList(List<HrEmployee> employees);
}
