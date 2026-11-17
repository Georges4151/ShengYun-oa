package com.ceams.module.hr.service;

import com.ceams.common.response.PageResult;
import com.ceams.module.hr.dto.EmployeeChangeRequest;
import com.ceams.module.hr.dto.EmployeeCreateRequest;
import com.ceams.module.hr.dto.EmployeeUpdateRequest;
import com.ceams.module.hr.enums.EmployeeStatus;
import com.ceams.module.hr.vo.EmployeeChangeVO;
import com.ceams.module.hr.vo.EmployeeVO;
import com.ceams.module.hr.vo.HeadcountDeptVO;
import java.util.List;

/**
 * 员工档案与入转调离服务。
 *
 * <p>入转调离的审批链路由 BPM 承载，本服务只负责业务单据落库、发起流程与回写实例 ID；
 * 终态由 {@code support} 包内对应回调派生。
 */
public interface HrEmployeeService {

    /** 员工分页（支持部门、状态、姓名/工号关键字）。 */
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

    /** 发起转正申请，返回申请单 ID。 */
    String regular(Long employeeId, EmployeeChangeRequest request);

    /** 发起调岗申请，返回申请单 ID。 */
    String transfer(Long employeeId, EmployeeChangeRequest request);

    /** 发起离职申请，返回申请单 ID。 */
    String resign(Long employeeId, EmployeeChangeRequest request);

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
