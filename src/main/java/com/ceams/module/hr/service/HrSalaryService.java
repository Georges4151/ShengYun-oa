package com.ceams.module.hr.service;

import com.ceams.common.response.PageResult;
import com.ceams.module.hr.dto.SalaryCreateRequest;
import com.ceams.module.hr.enums.SalaryStatus;
import com.ceams.module.hr.vo.SalaryVO;

/**
 * 薪资台账服务。
 *
 * <p>台账只允许「首次建档」在此完成；后续任何金额变化必须走薪资变动申请（BPM 审批），
 * 审批通过后由 {@code HrSalaryBpmCallback} 生成新的 ACTIVE 记录。
 */
public interface HrSalaryService {

    /** 首次建立薪资台账；同一员工已存在 ACTIVE 时抛 BIZ_STATE_CONFLICT。 */
    String createSalary(SalaryCreateRequest request);

    /** 当前登录员工本人的生效薪资；无员工档案或无生效薪资均抛 BIZ_NOT_FOUND。 */
    SalaryVO mySalary();

    /** 薪资台账分页（支持员工、状态过滤）。 */
    PageResult<SalaryVO> page(long page, long size, Long employeeId, SalaryStatus status);
}
