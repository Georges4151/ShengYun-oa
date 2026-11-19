package com.ceams.module.bpm.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.ceams.module.bpm.enums.BpmAssigneeSource;
import com.ceams.module.iam.entity.SysDept;
import com.ceams.module.iam.entity.SysRole;
import com.ceams.module.iam.entity.SysUser;
import com.ceams.module.iam.entity.UserRole;
import com.ceams.module.iam.enums.UserStatus;
import com.ceams.module.iam.mapper.DeptMapper;
import com.ceams.module.iam.mapper.RoleMapper;
import com.ceams.module.iam.mapper.UserMapper;
import com.ceams.module.iam.mapper.UserRoleMapper;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** 审批人解析器单元测试：指定用户、指定角色、部门主管、直属上级与空解析。 */
@ExtendWith(MockitoExtension.class)
class BpmAssigneeResolverTest {

    @Mock private UserMapper userMapper;
    @Mock private RoleMapper roleMapper;
    @Mock private UserRoleMapper userRoleMapper;
    @Mock private DeptMapper deptMapper;

    private BpmAssigneeResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new BpmAssigneeResolver(userMapper, roleMapper, userRoleMapper, deptMapper);
    }

    @Test
    void 指定用户直接解析为用户ID() {
        List<BpmResolvedAssignee> result = resolver.resolve(node(BpmAssigneeSource.USER, "20"), 10L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getAssigneeId()).isEqualTo(20L);
        assertThat(result.get(0).getSourceType()).isEqualTo(BpmAssigneeSource.USER);
        verify(userMapper, never()).selectList(any(Wrapper.class));
    }

    @Test
    void 指定角色解析为角色下启用用户() {
        SysRole role = new SysRole();
        role.setId(7L);
        role.setCode("FINANCE");
        when(roleMapper.selectOne(any(Wrapper.class))).thenReturn(role);
        when(userRoleMapper.selectList(any(Wrapper.class)))
                .thenReturn(List.of(userRole(21L, 7L), userRole(22L, 7L), userRole(21L, 7L)));
        when(userMapper.selectList(any(Wrapper.class))).thenReturn(List.of(user(21L), user(22L)));

        List<BpmResolvedAssignee> result = resolver.resolve(node(BpmAssigneeSource.ROLE, "FINANCE"), 10L);

        assertThat(result).extracting(BpmResolvedAssignee::getAssigneeId).containsExactly(21L, 22L);
        assertThat(result).allMatch(a -> a.getSourceType() == BpmAssigneeSource.ROLE);
    }

    @Test
    void 指定角色不存在时解析为空() {
        when(roleMapper.selectOne(any(Wrapper.class))).thenReturn(null);

        List<BpmResolvedAssignee> result = resolver.resolve(node(BpmAssigneeSource.ROLE, "NOPE"), 10L);

        assertThat(result).isEmpty();
    }

    @Test
    void 部门主管优先取发起人所属部门的部门主管() {
        SysUser initiator = user(10L);
        initiator.setDeptId(3L);
        initiator.setLeaderId(6L);
        SysDept dept = new SysDept();
        dept.setId(3L);
        dept.setLeaderId(5L);
        when(userMapper.selectById(10L)).thenReturn(initiator);
        when(deptMapper.selectById(3L)).thenReturn(dept);

        List<BpmResolvedAssignee> result =
                resolver.resolve(node(BpmAssigneeSource.DEPT_LEADER, null), 10L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getAssigneeId()).isEqualTo(5L);
        assertThat(result.get(0).getSourceType()).isEqualTo(BpmAssigneeSource.DEPT_LEADER);
    }

    @Test
    void 部门主管为空时回退发起人直属上级() {
        SysUser initiator = user(10L);
        initiator.setDeptId(3L);
        initiator.setLeaderId(6L);
        SysDept dept = new SysDept();
        dept.setId(3L);
        dept.setLeaderId(null);
        when(userMapper.selectById(10L)).thenReturn(initiator);
        when(deptMapper.selectById(3L)).thenReturn(dept);

        List<BpmResolvedAssignee> result =
                resolver.resolve(node(BpmAssigneeSource.DEPT_LEADER, null), 10L);

        assertThat(result).extracting(BpmResolvedAssignee::getAssigneeId).containsExactly(6L);
    }

    @Test
    void 部门与直属上级都为空时解析为空() {
        SysUser initiator = user(10L);
        initiator.setDeptId(3L);
        initiator.setLeaderId(null);
        when(userMapper.selectById(10L)).thenReturn(initiator);
        when(deptMapper.selectById(3L)).thenReturn(null);

        List<BpmResolvedAssignee> result =
                resolver.resolve(node(BpmAssigneeSource.DEPT_LEADER, null), 10L);

        assertThat(result).isEmpty();
    }

    @Test
    void 发起人不存在时部门主管解析为空() {
        when(userMapper.selectById(10L)).thenReturn(null);

        List<BpmResolvedAssignee> result =
                resolver.resolve(node(BpmAssigneeSource.DEPT_LEADER, null), 10L);

        assertThat(result).isEmpty();
        verify(deptMapper, never()).selectById(anyLong());
    }

    @Test
    void 直属上级取发起人leaderId() {
        SysUser initiator = user(10L);
        initiator.setLeaderId(7L);
        when(userMapper.selectById(10L)).thenReturn(initiator);

        List<BpmResolvedAssignee> result =
                resolver.resolve(node(BpmAssigneeSource.SUPERIOR, null), 10L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getAssigneeId()).isEqualTo(7L);
        assertThat(result.get(0).getSourceType()).isEqualTo(BpmAssigneeSource.SUPERIOR);
    }

    @Test
    void 发起人未维护直属上级时解析为空() {
        SysUser initiator = user(10L);
        when(userMapper.selectById(10L)).thenReturn(initiator);

        List<BpmResolvedAssignee> result =
                resolver.resolve(node(BpmAssigneeSource.SUPERIOR, null), 10L);

        assertThat(result).isEmpty();
    }

    @Test
    void 多来源解析同一审批人时去重() {
        SysRole role = new SysRole();
        role.setId(7L);
        role.setCode("FINANCE");
        when(roleMapper.selectOne(any(Wrapper.class))).thenReturn(role);
        when(userRoleMapper.selectList(any(Wrapper.class)))
                .thenReturn(List.of(userRole(20L, 7L)));
        when(userMapper.selectList(any(Wrapper.class))).thenReturn(List.of(user(20L)));
        BpmSchemaNode node = new BpmSchemaNode();
        node.setNodeKey("multi");
        BpmAssigneeSourceConfig userSource = new BpmAssigneeSourceConfig();
        userSource.setType(BpmAssigneeSource.USER);
        userSource.setValue("20");
        BpmAssigneeSourceConfig roleSource = new BpmAssigneeSourceConfig();
        roleSource.setType(BpmAssigneeSource.ROLE);
        roleSource.setValue("FINANCE");
        node.setAssignees(List.of(userSource, roleSource));

        List<BpmResolvedAssignee> result = resolver.resolve(node, 10L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getAssigneeId()).isEqualTo(20L);
    }

    // ------------------------------------------------------------------
    // 夹具
    // ------------------------------------------------------------------

    private BpmSchemaNode node(BpmAssigneeSource type, String value) {
        BpmAssigneeSourceConfig source = new BpmAssigneeSourceConfig();
        source.setType(type);
        source.setValue(value);
        BpmSchemaNode node = new BpmSchemaNode();
        node.setNodeKey("node");
        node.setAssignees(List.of(source));
        return node;
    }

    private UserRole userRole(Long userId, Long roleId) {
        UserRole userRole = new UserRole();
        userRole.setUserId(userId);
        userRole.setRoleId(roleId);
        return userRole;
    }

    private SysUser user(Long id) {
        SysUser user = new SysUser();
        user.setId(id);
        user.setStatus(UserStatus.ACTIVE);
        return user;
    }
}
