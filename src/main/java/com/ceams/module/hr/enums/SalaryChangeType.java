package com.ceams.module.hr.enums;

import com.ceams.module.bpm.enums.BpmBizType;

/** 薪资变动类型（hr_salary_change_apply.change_type）。 */
public enum SalaryChangeType {
    /** 薪资调整（含首次建档）。 */
    ADJUST(BpmBizType.HR_SALARY_CHANGE),
    /** 晋升调薪。 */
    PROMOTION(BpmBizType.HR_SALARY_CHANGE),
    /** 数据更正。 */
    CORRECTION(BpmBizType.HR_SALARY_CORRECTION);

    private final BpmBizType bpmBizType;

    SalaryChangeType(BpmBizType bpmBizType) {
        this.bpmBizType = bpmBizType;
    }

    /** 对应的 BPM 业务类型（同时作为模板编码）。 */
    public BpmBizType bpmBizType() {
        return bpmBizType;
    }
}
