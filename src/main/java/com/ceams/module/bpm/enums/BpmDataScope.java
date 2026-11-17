package com.ceams.module.bpm.enums;

/** 数据范围（现有 sys_role 尚未落地 data_scope，暂按用户部门归属判定）。 */
public enum BpmDataScope {
    /** 仅本人。 */
    SELF,
    /** 本部门及子部门（按 sys_dept.parent_id 递归；无部门时回退 SELF）。 */
    DEPT,
    /** 全部。 */
    ALL
}
