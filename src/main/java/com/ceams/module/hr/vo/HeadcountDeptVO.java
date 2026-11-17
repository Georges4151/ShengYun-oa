package com.ceams.module.hr.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

/** 部门在职人数统计（只读聚合视图）。 */
@Data
public class HeadcountDeptVO {

    /** 部门 ID。 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long deptId;

    /** 部门名称；部门不存在时回落空串。 */
    private String deptName;

    /** 该部门全部未删除员工数（含试用、在职、休假、离职）。 */
    private long totalCount;

    /** 试用期人数（PROBATION）。 */
    private long probationCount;

    /** 在职人数（ACTIVE）。 */
    private long activeCount;

    /** 离职人数（TERMINATED）。 */
    private long terminatedCount;
}
