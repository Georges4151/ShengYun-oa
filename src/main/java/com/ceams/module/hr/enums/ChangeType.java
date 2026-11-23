package com.ceams.module.hr.enums;

import com.ceams.module.bpm.enums.BpmBizType;

/** 入转调离申请类型（hr_employee_change.change_type）。 */
public enum ChangeType {
    /** 转正。 */
    REGULAR(BpmBizType.HR_REGULAR),
    /** 调岗。 */
    TRANSFER(BpmBizType.HR_TRANSFER),
    /** 离职。 */
    RESIGN(BpmBizType.HR_RESIGN),
    /** 晋升。 */
    PROMOTION(BpmBizType.HR_PROMOTION),
    /** 上级离职自动转交（系统写入，不发起 BPM）。 */
    LEADER_TRANSFER(null);

    private final BpmBizType bpmBizType;

    ChangeType(BpmBizType bpmBizType) {
        this.bpmBizType = bpmBizType;
    }

    /** 对应的 BPM 业务类型（同时作为模板编码）。 */
    public BpmBizType bpmBizType() {
        return bpmBizType;
    }
}
