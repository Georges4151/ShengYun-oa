package com.ceams.module.bpm.support;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ceams.module.bpm.entity.BpmInstance;
import com.ceams.module.bpm.entity.BpmVariable;
import com.ceams.module.bpm.mapper.BpmVariableMapper;
import com.ceams.module.iam.entity.SysRole;
import com.ceams.module.iam.entity.SysUser;
import com.ceams.module.iam.entity.UserRole;
import com.ceams.module.iam.mapper.RoleMapper;
import com.ceams.module.iam.mapper.UserMapper;
import com.ceams.module.iam.mapper.UserRoleMapper;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 条件求值上下文构造器。
 *
 * <p>从 bpm_variable 读取发起时提交的表单变量，从 IAM 读取发起人角色与部门子树。
 * 任何组织数据缺失都只产生空集合，由 {@link BpmConditionEvaluator} 降级为不命中并告警，
 * 不影响流程主链路。
 */
@Component
@RequiredArgsConstructor
public class BpmConditionContextBuilder {

    private final BpmVariableMapper variableMapper;
    private final UserMapper userMapper;
    private final UserRoleMapper userRoleMapper;
    private final RoleMapper roleMapper;
    private final BpmOrgResolver orgResolver;

    /** 构造实例的条件求值上下文；实例为空时返回空上下文。 */
    public BpmConditionContext build(BpmInstance instance) {
        BpmConditionContext context = new BpmConditionContext();
        if (instance == null || instance.getId() == null) {
            return context;
        }
        context.setVariables(loadVariables(instance.getId()));
        Long initiatorId = instance.getInitiatorId();
        if (initiatorId == null) {
            return context;
        }
        SysUser initiator = userMapper.selectById(initiatorId);
        if (initiator == null) {
            return context;
        }
        context.setInitiatorDeptId(initiator.getDeptId());
        context.setRoles(loadRoles(initiatorId));
        if (initiator.getDeptId() != null) {
            Set<Long> deptIds = orgResolver.deptSubtreeIds(initiator.getDeptId());
            context.setInitiatorDeptIds(deptIds);
            context.setInitiatorDeptNames(orgResolver.deptNames(deptIds));
        }
        return context;
    }

    private Map<String, String> loadVariables(Long instanceId) {
        List<BpmVariable> variables =
                variableMapper.selectList(
                        new LambdaQueryWrapper<BpmVariable>()
                                .eq(BpmVariable::getInstanceId, instanceId));
        Map<String, String> result = new LinkedHashMap<>();
        for (BpmVariable variable : variables) {
            if (variable != null && variable.getVarKey() != null) {
                result.put(variable.getVarKey(), Objects.toString(variable.getVarValue(), ""));
            }
        }
        return result;
    }

    private Set<String> loadRoles(Long userId) {
        List<UserRole> links =
                userRoleMapper.selectList(
                        new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, userId));
        if (links.isEmpty()) {
            return Set.of();
        }
        List<Long> roleIds =
                links.stream()
                        .map(UserRole::getRoleId)
                        .filter(Objects::nonNull)
                        .distinct()
                        .toList();
        if (roleIds.isEmpty()) {
            return Set.of();
        }
        List<SysRole> roles = roleMapper.selectBatchIds(roleIds);
        Set<String> codes = new LinkedHashSet<>();
        for (SysRole role : roles) {
            if (role != null && role.getCode() != null) {
                codes.add(role.getCode().trim().toUpperCase(Locale.ROOT));
            }
        }
        return codes;
    }
}
