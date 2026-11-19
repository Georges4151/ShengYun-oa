package com.ceams.module.bpm.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.ceams.module.bpm.entity.BpmInstance;
import com.ceams.module.bpm.entity.BpmVariable;
import com.ceams.module.bpm.mapper.BpmVariableMapper;
import com.ceams.module.iam.entity.SysRole;
import com.ceams.module.iam.entity.SysUser;
import com.ceams.module.iam.entity.UserRole;
import com.ceams.module.iam.mapper.RoleMapper;
import com.ceams.module.iam.mapper.UserMapper;
import com.ceams.module.iam.mapper.UserRoleMapper;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** 条件上下文构造单元测试：变量、角色、部门子树的装配与缺失降级。 */
@ExtendWith(MockitoExtension.class)
class BpmConditionContextBuilderTest {

    @Mock private BpmVariableMapper variableMapper;
    @Mock private UserMapper userMapper;
    @Mock private UserRoleMapper userRoleMapper;
    @Mock private RoleMapper roleMapper;
    @Mock private BpmOrgResolver orgResolver;

    private BpmConditionContextBuilder builder;

    @BeforeEach
    void setUp() {
        builder =
                new BpmConditionContextBuilder(
                        variableMapper, userMapper, userRoleMapper, roleMapper, orgResolver);
    }

    @Test
    void 构造变量角色与部门上下文() {
        BpmInstance instance = instance(1L, 10L);
        when(variableMapper.selectList(any(Wrapper.class)))
                .thenReturn(List.of(variable("amount", "5000"), variable("level", "高")));
        SysUser initiator = new SysUser();
        initiator.setId(10L);
        initiator.setDeptId(3L);
        when(userMapper.selectById(10L)).thenReturn(initiator);
        UserRole link = new UserRole();
        link.setUserId(10L);
        link.setRoleId(7L);
        when(userRoleMapper.selectList(any(Wrapper.class))).thenReturn(List.of(link));
        SysRole role = new SysRole();
        role.setId(7L);
        role.setCode("finance");
        when(roleMapper.selectBatchIds(any())).thenReturn(List.of(role));
        when(orgResolver.deptSubtreeIds(3L)).thenReturn(Set.of(3L, 4L));
        when(orgResolver.deptNames(Set.of(3L, 4L))).thenReturn(Set.of("销售部", "华东组"));

        BpmConditionContext context = builder.build(instance);

        assertThat(context.getVariables()).containsEntry("amount", "5000").containsEntry("level", "高");
        assertThat(context.getRoles()).containsExactly("FINANCE");
        assertThat(context.getInitiatorDeptId()).isEqualTo(3L);
        assertThat(context.getInitiatorDeptIds()).containsExactlyInAnyOrder(3L, 4L);
        assertThat(context.getInitiatorDeptNames()).containsExactlyInAnyOrder("销售部", "华东组");
    }

    @Test
    void 发起人不存在时保留变量但组织为空() {
        BpmInstance instance = instance(1L, 10L);
        when(variableMapper.selectList(any(Wrapper.class)))
                .thenReturn(List.of(variable("amount", "5000")));
        when(userMapper.selectById(10L)).thenReturn(null);

        BpmConditionContext context = builder.build(instance);

        assertThat(context.getVariables()).containsEntry("amount", "5000");
        assertThat(context.getRoles()).isEmpty();
        assertThat(context.getInitiatorDeptIds()).isEmpty();
        assertThat(context.getInitiatorDeptNames()).isEmpty();
    }

    // ------------------------------------------------------------------
    // 夹具
    // ------------------------------------------------------------------

    private BpmInstance instance(Long id, Long initiatorId) {
        BpmInstance instance = new BpmInstance();
        instance.setId(id);
        instance.setInitiatorId(initiatorId);
        return instance;
    }

    private BpmVariable variable(String key, String value) {
        BpmVariable variable = new BpmVariable();
        variable.setVarKey(key);
        variable.setVarValue(value);
        return variable;
    }
}
