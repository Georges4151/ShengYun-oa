package com.ceams.module.bpm.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
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
    @Mock private BpmLeaderProvider leaderProvider;

    private BpmAssigneeResolver resolver;

    @BeforeEach
    void setUp() {
        resolver =
                new BpmAssigneeResolver(
                        userMapper,
                        roleMapper,
                        userRoleMapper,
                        deptMapper,
                        List.of(leaderProvider));
        // Mockito 对 Long 返回值默认 0，需显式置空表示"解析不到"
        lenient().when(leaderProvider.resolveLeaderUserId(any(), any())).thenReturn(null);
        lenient().when(leaderProvider.resolveLeaderUserIdByUser(any())).thenReturn(null);
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
        SysDept dept = new SysDept();
        dept.setId(3L);
        dept.setLeaderId(null);
        when(userMapper.selectById(10L)).thenReturn(initiator);
        when(deptMapper.selectById(3L)).thenReturn(dept);
        when(leaderProvider.resolveLeaderUserIdByUser(10L)).thenReturn(6L);

        List<BpmResolvedAssignee> result =
                resolver.resolve(node(BpmAssigneeSource.DEPT_LEADER, null), 10L);

        assertThat(result).extracting(BpmResolvedAssignee::getAssigneeId).containsExactly(6L);
    }

    @Test
    void 部门与直属上级都为空时解析为空() {
        SysUser initiator = user(10L);
        initiator.setDeptId(3L);
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
    void 直属上级按发起人员工档案解析() {
        when(leaderProvider.resolveLeaderUserIdByUser(10L)).thenReturn(7L);

        List<BpmResolvedAssignee> result =
                resolver.resolve(node(BpmAssigneeSource.SUPERIOR, null), 10L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getAssigneeId()).isEqualTo(7L);
        assertThat(result.get(0).getSourceType()).isEqualTo(BpmAssigneeSource.SUPERIOR);
        // 直属上级不再读取 sys_user，无需加载发起人
        verify(userMapper, never()).selectById(anyLong());
    }

    @Test
    void 业务员工直属上级优先于发起人() {
        when(leaderProvider.resolveLeaderUserId(BpmBizType.HR_LEAVE, 123L)).thenReturn(8L);

        List<BpmResolvedAssignee> result =
                resolver.resolve(node(BpmAssigneeSource.SUPERIOR, null), 10L, BpmBizType.HR_LEAVE, 123L);

        assertThat(result).extracting(BpmResolvedAssignee::getAssigneeId).containsExactly(8L);
        verify(leaderProvider, never()).resolveLeaderUserIdByUser(any());
    }

    @Test
    void 发起人未维护直属上级时解析为空() {
        List<BpmResolvedAssignee> result =
                resolver.resolve(node(BpmAssigneeSource.SUPERIOR, null), 10L);

        assertThat(result).isEmpty();
    }

    @Test
    void HR单据解析业务员工上级时不读发起人档案() {
        when(leaderProvider.resolveLeaderUserId(BpmBizType.HR_PROMOTION, 500L)).thenReturn(9L);

        List<BpmResolvedAssignee> result =
                resolver.resolve(node(BpmAssigneeSource.SUPERIOR, null), 10L, BpmBizType.HR_PROMOTION, 500L);

        assertThat(result).extracting(BpmResolvedAssignee::getAssigneeId).containsExactly(9L);
        // 直属上级已由 hr_employee.leader_id 解析，不再读取 sys_user
        verify(userMapper, never()).selectById(anyLong());
        verify(leaderProvider, never()).resolveLeaderUserIdByUser(any());
    }

    @Test
    void 非HR单据回退发起人员工上级() {
        // EXPENSE 无对应员工档案解析实现，业务侧返回空后回退发起人
        when(leaderProvider.resolveLeaderUserIdByUser(10L)).thenReturn(8L);

        List<BpmResolvedAssignee> result =
                resolver.resolve(node(BpmAssigneeSource.SUPERIOR, null), 10L, BpmBizType.EXPENSE, 501L);

        assertThat(result).extracting(BpmResolvedAssignee::getAssigneeId).containsExactly(8L);
        verify(leaderProvider).resolveLeaderUserId(BpmBizType.EXPENSE, 501L);
    }

    @Test
    void 业务单据为空时回退发起人员工上级() {
        when(leaderProvider.resolveLeaderUserIdByUser(10L)).thenReturn(8L);

        List<BpmResolvedAssignee> result =
                resolver.resolve(node(BpmAssigneeSource.SUPERIOR, null), 10L, BpmBizType.HR_LEAVE, null);

        assertThat(result).extracting(BpmResolvedAssignee::getAssigneeId).containsExactly(8L);
        // bizId 为空时不应按业务单据解析
        verify(leaderProvider, never()).resolveLeaderUserId(any(), any());
    }

    @Test
    void 业务员工与发起人均无上级时解析为空() {
        List<BpmResolvedAssignee> result =
                resolver.resolve(node(BpmAssigneeSource.SUPERIOR, null), 10L, BpmBizType.HR_LEAVE, 123L);

        assertThat(result).isEmpty();
        verify(leaderProvider).resolveLeaderUserId(BpmBizType.HR_LEAVE, 123L);
        verify(leaderProvider).resolveLeaderUserIdByUser(10L);
    }

    @Test
    void 直属上级解析不读取SysUser已废弃字段() {
        // sys_user.leader_id 已废弃：即使存在历史值也不应参与解析
        SysUser legacyUser = user(10L);
        legacyUser.setLeaderId(99L);
        lenient().when(userMapper.selectById(10L)).thenReturn(legacyUser);

        List<BpmResolvedAssignee> result =
                resolver.resolve(node(BpmAssigneeSource.SUPERIOR, null), 10L);

        assertThat(result).isEmpty();
        verify(userMapper, never()).selectById(anyLong());
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
