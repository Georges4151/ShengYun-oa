package com.ceams.module.hr.service;

import com.ceams.common.response.PageResult;
import com.ceams.module.hr.dto.EmployeeChangeRequest;
import com.ceams.module.hr.dto.EmployeeCreateRequest;
import com.ceams.module.hr.dto.EmployeeUpdateRequest;
import com.ceams.module.hr.enums.EmployeeStatus;
import com.ceams.module.hr.vo.EmployeeChangeVO;
import com.ceams.module.hr.vo.EmployeeTreeVO;
import com.ceams.module.hr.vo.EmployeeVO;
import com.ceams.module.hr.vo.HeadcountDeptVO;
import com.ceams.module.hr.vo.OrgTreeNodeVO;
import java.util.List;

/**
 * 员工档案与入转调离服务。
 *
 * <p>入转调离的审批链路由 BPM 承载，本服务只负责业务单据落库、发起流程与回写实例 ID；
 * 终态由 {@code support} 包内对应回调派生。
 */
public interface HrEmployeeService {

    /**
     * 员工分页（支持部门、状态、姓名/工号关键字）。
     *
     * <p>{@code deptId} 语义为「含所有子部门」：先递归收集子孙部门，再以 IN 过滤；
     * 部门不存在时返回空页。
     */
    PageResult<EmployeeVO> page(
            long page, long size, Long deptId, EmployeeStatus status, String keyword);

    /** 员工详情（敏感字段脱敏）。 */
    EmployeeVO detail(Long id);

    /** 当前登录账号绑定的员工档案；未绑定抛 BIZ_NOT_FOUND。 */
    EmployeeVO currentEmployee();

    /** 当前登录账号绑定的员工 ID；未绑定抛 BIZ_NOT_FOUND。 */
    Long currentEmployeeId();

    /** 新建员工档案，返回新增 ID。 */
    String create(EmployeeCreateRequest request);

    /** 修改员工档案（字段为 null 表示不修改）。 */
    void update(Long id, EmployeeUpdateRequest request);

    /** 该员工的入转调离申请记录（按创建时间倒序）。 */
    List<EmployeeChangeVO> listChanges(Long employeeId);

    /** 在职直接下属（用于离职前下属转交选择）。 */
    List<EmployeeVO> subordinates(Long employeeId);

    /** 发起转正申请，返回申请单 ID。 */
    String regular(Long employeeId, EmployeeChangeRequest request);

    /** 发起调岗申请，返回申请单 ID。 */
    String transfer(Long employeeId, EmployeeChangeRequest request);

    /** 发起离职申请，返回申请单 ID。 */
    String resign(Long employeeId, EmployeeChangeRequest request);

    /** 发起晋升申请（审批通过后立即生效），返回申请单 ID。 */
    String promotion(Long employeeId, EmployeeChangeRequest request);

    /**
     * 员工组织树（按直属上级关系组装）。
     *
     * @param deptId 部门 ID，非空时只取该部门
     * @param status 员工状态，非空时只取该状态
     * @return 根节点列表（无直属上级或上级不在结果集内）
     */
    List<EmployeeTreeVO> tree(Long deptId, EmployeeStatus status);

    /**
     * 人事组织合并树：部门 → 子部门 → 员工（部门内按直属上级递归嵌套）。
     *
     * @param status 员工状态，非空时只保留该状态员工（保留其部门祖先链）
     * @param keyword 员工姓名/工号或部门名称关键字
     * @param deptId 部门 ID，非空时只返回该部门子树（含祖先根链）
     * @param includeResigned 未指定 status 时是否包含已离职员工（默认排除）
     * @return 根节点列表；未分配部门虚拟节点固定最后
     */
    List<OrgTreeNodeVO> orgTree(
            EmployeeStatus status, String keyword, Long deptId, boolean includeResigned);

    /**
     * 人事组织合并树根节点（懒加载首屏）：组装口径与全量树一致，仅裁剪 children。
     *
     * <p>每个返回节点的 {@code children} 为空数组，是否可展开由 {@code hasChildren} 表达；
     * 未分配部门虚拟节点规则不变（存在未分配员工且通过过滤时返回，固定最后）。
     *
     * @param status 员工状态，非空时只保留该状态员工（保留其部门祖先链）
     * @param keyword 员工姓名/工号或部门名称关键字
     * @param deptId 部门 ID，非空时只返回该部门子树（含祖先根链）
     * @param includeResigned 未指定 status 时是否包含已离职员工（默认排除）
     * @return 根节点列表，children 已裁剪
     */
    List<OrgTreeNodeVO> orgTreeRoots(
            EmployeeStatus status, String keyword, Long deptId, boolean includeResigned);

    /**
     * 人事组织合并树某节点的直接子节点（懒加载展开）：复用全量组装结果定位节点后裁剪 children。
     *
     * @param nodeType 节点类型（DEPT / UNASSIGNED / EMPLOYEE，大小写不敏感）；非法或为空时返回空列表
     * @param nodeId 部门 ID 或员工 ID（UNASSIGNED 忽略）
     * @param status 员工状态，非空时只保留该状态员工
     * @param keyword 员工姓名/工号或部门名称关键字
     * @param deptId 部门 ID，非空时只返回该部门子树（含祖先根链）
     * @param includeResigned 未指定 status 时是否包含已离职员工（默认排除）
     * @return 直接子节点列表，children 已裁剪；节点不存在时返回空列表
     */
    List<OrgTreeNodeVO> orgTreeChildren(
            String nodeType,
            Long nodeId,
            EmployeeStatus status,
            String keyword,
            Long deptId,
            boolean includeResigned);

    /**
     * 按部门统计员工人数（只读聚合，无事务）。
     *
     * <p>口径：统计全部未删除员工，按 {@code deptId} 分组；total 为部门全部人数，
     * probation / active / terminated 分别对应 PROBATION / ACTIVE / TERMINATED，
     * LEAVE 计入 total 但不单列；未归属部门的员工不产生统计行。结果按 deptId 升序。
     *
     * @param deptId 部门 ID，非空时只统计该部门
     * @return 部门人数列表，无数据时返回空列表
     */
    List<HeadcountDeptVO> headcountByDept(Long deptId);
}
