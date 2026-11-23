package com.ceams.module.bpm.support;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ceams.module.bpm.enums.BpmAssigneeSource;
import com.ceams.module.bpm.enums.BpmBizType;
import com.ceams.module.iam.entity.SysDept;
import com.ceams.module.iam.entity.SysRole;
import com.ceams.module.iam.entity.SysUser;
import com.ceams.module.iam.entity.UserRole;
import com.ceams.module.iam.enums.UserStatus;
import com.ceams.module.iam.mapper.DeptMapper;
import com.ceams.module.iam.mapper.RoleMapper;
import com.ceams.module.iam.mapper.UserMapper;
import com.ceams.module.iam.mapper.UserRoleMapper;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 审批人解析器。
 *
 * <p>支持指定用户、指定角色、部门主管、发起人直属上级四种来源；解析结果同时用于
 * 写 bpm_assignee_snapshot 与冗余到 bpm_task。解析为空时由上层落 PENDING_ASSIGNEE，
 * 不允许生成无人待办。
 *
 * <p>组织口径（V50 起）：直属上级统一取 {@code hr_employee.leader_id}（经
 * {@link BpmLeaderProvider} 按业务单据员工或发起人员工档案解析），{@code sys_user.leader_id}
 * 已废弃不再读取。部门主管仍优先取发起人 {@code sys_user.dept_id} 对应
 * {@code sys_dept.leader_id}，为空时回退直属上级。任一环节为空均记录告警。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BpmAssigneeResolver {

    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final UserRoleMapper userRoleMapper;
    private final DeptMapper deptMapper;
    private final List<BpmLeaderProvider> leaderProviders;

    /**
     * 解析节点审批人。
     *
     * @param node 节点定义
     * @param initiatorId 发起人 ID
     * @param bizType 业务类型（可为空）
     * @param bizId 业务单据 ID（可为空）
     * @return 去重后的审批人列表（可能为空）
     */
    public List<BpmResolvedAssignee> resolve(
            BpmSchemaNode node, Long initiatorId, BpmBizType bizType, Long bizId) {
        List<BpmResolvedAssignee> result = new ArrayList<>();
        if (node == null || node.getAssignees() == null) {
            return result;
        }
        Set<String> seen = new LinkedHashSet<>();
        SysUser initiator = null;
        boolean initiatorLoaded = false;
        for (BpmAssigneeSourceConfig source : node.getAssignees()) {
            if (source == null || source.getType() == null) {
                continue;
            }
            switch (source.getType()) {
                case USER:
                    addIfAbsent(result, seen, parseUserId(source.getValue()), BpmAssigneeSource.USER);
                    break;
                case ROLE:
                    resolveRole(source.getValue(), result, seen);
                    break;
                case DEPT_LEADER:
                    if (!initiatorLoaded) {
                        initiator = loadInitiator(initiatorId);
                        initiatorLoaded = true;
                    }
                    resolveDeptLeader(node, initiator, initiatorId, bizType, bizId, result, seen);
                    break;
                case SUPERIOR:
                    resolveSuperior(node, initiatorId, bizType, bizId, result, seen);
                    break;
                default:
                    log.warn("审批人来源暂不支持：节点={}，来源={}", node.getNodeKey(), source.getType());
            }
        }
        return result;
    }

    /**
     * 解析节点审批人（无业务单据上下文）。
     *
     * @deprecated 无法按业务员工解析直属上级，请改用
     *     {@link #resolve(BpmSchemaNode, Long, BpmBizType, Long)}。
     */
    @Deprecated
    public List<BpmResolvedAssignee> resolve(BpmSchemaNode node, Long initiatorId) {
        return resolve(node, initiatorId, null, null);
    }

    /** 指定用户：直接使用配置中的用户 ID。 */
    private Long parseUserId(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            log.warn("审批人解析忽略非法用户ID：value={}", value);
            return null;
        }
    }

    /** 指定角色：按角色编码查询启用用户，保持角色内用户顺序稳定。 */
    private void resolveRole(String roleCode, List<BpmResolvedAssignee> result, Set<String> seen) {
        if (!StringUtils.hasText(roleCode)) {
            return;
        }
        SysRole role =
                roleMapper.selectOne(
                        new LambdaQueryWrapper<SysRole>().eq(SysRole::getCode, roleCode.trim()));
        if (role == null) {
            log.warn("审批人解析为空：角色编码={} 不存在", roleCode);
            return;
        }
        List<UserRole> userRoles =
                userRoleMapper.selectList(
                        new LambdaQueryWrapper<UserRole>().eq(UserRole::getRoleId, role.getId()));
        if (userRoles.isEmpty()) {
            log.warn("审批人解析为空：角色编码={} 未绑定用户", roleCode);
            return;
        }
        List<Long> userIds = userRoles.stream().map(UserRole::getUserId).distinct().toList();
        List<SysUser> users =
                userMapper.selectList(
                        new LambdaQueryWrapper<SysUser>()
                                .in(SysUser::getId, userIds)
                                .eq(SysUser::getStatus, UserStatus.ACTIVE)
                                .orderByAsc(SysUser::getId));
        if (users.isEmpty()) {
            log.warn("审批人解析为空：角色编码={} 下无启用用户", roleCode);
        }
        for (SysUser user : users) {
            addIfAbsent(result, seen, user.getId(), BpmAssigneeSource.ROLE);
        }
    }

    /** 部门主管：优先取发起人部门对应的 sys_dept.leader_id，为空回退员工档案直属上级。 */
    private void resolveDeptLeader(
            BpmSchemaNode node,
            SysUser initiator,
            Long initiatorId,
            BpmBizType bizType,
            Long bizId,
            List<BpmResolvedAssignee> result,
            Set<String> seen) {
        Long leaderId = null;
        if (initiator != null && initiator.getDeptId() != null) {
            SysDept dept = deptMapper.selectById(initiator.getDeptId());
            if (dept != null) {
                leaderId = dept.getLeaderId();
            }
        }
        if (leaderId == null) {
            leaderId = resolveLeaderUserId(initiatorId, bizType, bizId);
        }
        if (leaderId == null) {
            log.warn(
                    "审批人解析为空：节点={}，来源=DEPT_LEADER，原因=部门未配置主管且员工档案未维护直属上级，任务将落 PENDING_ASSIGNEE",
                    node.getNodeKey());
            return;
        }
        addIfAbsent(result, seen, leaderId, BpmAssigneeSource.DEPT_LEADER);
    }

    /** 直属上级：由员工档案 hr_employee.leader_id 解析（业务员工优先，回退发起人）。 */
    private void resolveSuperior(
            BpmSchemaNode node,
            Long initiatorId,
            BpmBizType bizType,
            Long bizId,
            List<BpmResolvedAssignee> result,
            Set<String> seen) {
        Long leaderId = resolveLeaderUserId(initiatorId, bizType, bizId);
        if (leaderId == null) {
            log.warn(
                    "审批人解析为空：节点={}，来源=SUPERIOR，原因=发起人及业务员工均未维护直属上级，任务将落 PENDING_ASSIGNEE",
                    node.getNodeKey());
            return;
        }
        addIfAbsent(result, seen, leaderId, BpmAssigneeSource.SUPERIOR);
    }

    /** 依次询问各扩展：先按业务单据员工解析，再回退发起人员工档案。 */
    private Long resolveLeaderUserId(Long initiatorId, BpmBizType bizType, Long bizId) {
        for (BpmLeaderProvider p : leaderProviders) {
            Long id = bizType == null || bizId == null ? null : p.resolveLeaderUserId(bizType, bizId);
            if (id != null) {
                return id;
            }
            id = p.resolveLeaderUserIdByUser(initiatorId);
            if (id != null) {
                return id;
            }
        }
        return null;
    }

    private SysUser loadInitiator(Long initiatorId) {
        return initiatorId == null ? null : userMapper.selectById(initiatorId);
    }

    private void addIfAbsent(
            List<BpmResolvedAssignee> result,
            Set<String> seen,
            Long assigneeId,
            BpmAssigneeSource sourceType) {
        if (assigneeId == null) {
            return;
        }
        // 同一审批人即使来自多种配置来源也只生成一条待办
        String key = String.valueOf(assigneeId);
        if (!seen.add(key)) {
            return;
        }
        BpmResolvedAssignee resolved = new BpmResolvedAssignee();
        resolved.setAssigneeId(assigneeId);
        resolved.setSourceType(sourceType);
        result.add(resolved);
    }
}
