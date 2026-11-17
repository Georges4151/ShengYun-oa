package com.ceams.module.hr.enums;

/** 月度薪资核算状态（hr_salary_calc.status）。 */
public enum PayrollStatus {
    /** 草稿：可重算、可手工调整。 */
    DRAFT,
    /** 已确认：禁止直接修改，仅允许更正流程。 */
    CONFIRMED,
    /** 已锁定：月结后状态，核算重算直接跳过。 */
    LOCKED
}
