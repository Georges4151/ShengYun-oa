package com.ceams.module.hr.vo;

import lombok.Data;

/** 考勤汇总（只读聚合视图）。 */
@Data
public class AttendanceSummaryVO {

    /** 区间内有打卡记录的员工数。 */
    private long employeeCount;

    /** 正常次数（NORMAL）。 */
    private long normalCount;

    /** 迟到次数（LATE）。 */
    private long lateCount;

    /** 早退次数（EARLY）。 */
    private long earlyCount;

    /** 缺卡次数（ABSENT）。 */
    private long absentCount;

    /** 区间内审批通过的补卡条数。 */
    private long supplementCount;
}
