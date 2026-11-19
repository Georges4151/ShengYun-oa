package com.ceams.module.hr.enums;

/** 工资条阅读状态（hr_salary_slip.read_status）。 */
public enum SalarySlipReadStatus {
    /** 待阅读：发布后未回写阅读时间。 */
    PENDING,
    /** 已阅读：本人已查看并回写阅读时间。 */
    READ
}
