package com.ceams.module.hr.vo;

import com.ceams.module.hr.enums.EmployeeStatus;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

/**
 * 人事组织合并树节点：部门 → 子部门 → 员工（部门内按直属上级递归嵌套）。
 *
 * <p>同一结构承载三类节点，由 {@link NodeType} 区分；部门节点只填部门字段（含人数），
 * 员工节点只填员工字段，未分配部门虚拟节点仅作为 dept_id 为空员工的容器。
 */
@Data
public class OrgTreeNodeVO {

    /** 节点类型。 */
    public enum NodeType {
        /** 部门节点。 */
        DEPT,
        /** 员工节点。 */
        EMPLOYEE,
        /** 未分配部门虚拟根节点。 */
        UNASSIGNED
    }

    /** 部门/员工真实 ID；未分配部门虚拟节点固定为字符串 {@code UNASSIGNED}。 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Object id;

    /** 节点类型。 */
    private NodeType nodeType;

    /** 部门名称 / 员工姓名 / 未分配部门名称。 */
    private String name;

    /** 上级部门 ID（部门节点，根部门为空）。 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long parentId;

    /** 所属部门 ID（员工节点）。 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long deptId;

    /** 直属上级员工 ID（员工节点）。 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long leaderId;

    /** 员工工号（员工节点）。 */
    private String employeeNo;

    /** 员工状态（员工节点）。 */
    private EmployeeStatus status;

    /** 职级 ID（员工节点）。 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long levelId;

    /** 职级名称（员工节点）。 */
    private String levelName;

    /** 职级编码（员工节点）。 */
    private String levelCode;

    /** 岗位名称（员工节点）。 */
    private String positionName;

    /** 部门节点：含所有子部门且状态非 TERMINATED 的在职人数。 */
    private Integer headcount;

    /** 部门节点：含所有子部门、含离职的总人数。 */
    private Integer totalHeadcount;

    /** 是否存在可展开的子节点（懒加载时 children 为空但该标记正确）。 */
    private Boolean hasChildren;

    /** 部门节点：部门负责人姓名；员工节点：直属上级姓名。 */
    private String leaderName;

    /** 员工节点：跨部门上级所在部门名称（仅跨部门时填充）。 */
    private String leaderDeptName;

    /** 员工节点：直属上级是否跨部门。 */
    private Boolean crossDeptLeader;

    /** 子节点（叶子节点为空数组）。 */
    private List<OrgTreeNodeVO> children = new ArrayList<>();
}
