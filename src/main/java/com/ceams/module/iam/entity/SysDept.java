package com.ceams.module.iam.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ceams.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 部门（sys_dept，最小组织树）。
 *
 * <p>本期仅承载组织关系：上级部门（parent_id）用于计算“同部门及子部门”，
 * 部门主管（leader_id）用于 BPM 的 DEPT_LEADER 审批人解析。不建物理外键，
 * 仅逻辑引用 sys_dept.id / sys_user.id。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_dept")
public class SysDept extends BaseEntity {

    /** 部门名称。 */
    private String name;

    /** 上级部门 ID（根部门为空）。 */
    private Long parentId;

    /** 部门主管用户 ID。 */
    private Long leaderId;

    /** 同级排序号。 */
    private Integer sortOrder;

    /** 企业微信部门 ID（唯一；未同步部门为空，由组织同步回填）。 */
    private String wecomDeptId;
}
