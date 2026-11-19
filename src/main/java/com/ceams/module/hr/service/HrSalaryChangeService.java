package com.ceams.module.hr.service;

import com.ceams.common.response.PageResult;
import com.ceams.module.hr.dto.SalaryChangeApplyRequest;
import com.ceams.module.hr.enums.SalaryChangeStatus;
import com.ceams.module.hr.enums.SalaryChangeType;
import com.ceams.module.hr.vo.SalaryChangeVO;

/**
 * 薪资变动申请服务。
 *
 * <p>统一走「落库申请单 → 发起 BPM → 回写实例 ID」三步；台账变更只发生在审批通过后的回调中。
 */
public interface HrSalaryChangeService {

    /** 发起薪资变动申请（提交即启动 BPM），返回申请单 ID。 */
    String applyChange(SalaryChangeApplyRequest request);

    /** 薪资变动申请分页（支持员工、变动类型、状态过滤）。 */
    PageResult<SalaryChangeVO> pageChanges(
            long page,
            long size,
            Long employeeId,
            SalaryChangeType changeType,
            SalaryChangeStatus status);

    /** 本人发起的薪资变动申请分页。 */
    PageResult<SalaryChangeVO> myChanges(long page, long size);
}
