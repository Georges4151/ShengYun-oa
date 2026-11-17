package com.ceams.module.integration.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ceams.module.iam.entity.SysDept;
import com.ceams.module.iam.entity.SysUser;
import com.ceams.module.iam.enums.UserStatus;
import com.ceams.module.iam.mapper.DeptMapper;
import com.ceams.module.iam.mapper.UserMapper;
import com.ceams.module.integration.client.WeComDept;
import com.ceams.module.integration.client.WeComUser;
import com.ceams.module.integration.config.WeComAppProperties;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

/** 企业微信组织同步执行器单元测试：部门两轮回填、成员新增/手机号绑定/多命中、离职禁用。 */
@ExtendWith(MockitoExtension.class)
class WeComSyncRunnerTest {

    @Mock private UserMapper userMapper;

    @Mock private DeptMapper deptMapper;

    @Mock private PasswordEncoder passwordEncoder;

    private WeComSyncRunner runner;

    @BeforeEach
    void setUp() {
        runner =
                spy(
                        new WeComSyncRunner(
                                userMapper, deptMapper, passwordEncoder, new WeComAppProperties()));
    }

    @Test
    void shouldBackfillDepartmentParentsInSecondPass() {
        stubLoads(List.of(), List.of(), List.of());
        stubDeptInsertFrom(100L);
        List<WeComDept> depts =
                List.of(
                        new WeComDept(1L, "总部", 1L, 1),
                        new WeComDept(2L, "研发部", 1L, 10),
                        new WeComDept(3L, "后端组", 2L, 20),
                        new WeComDept(4L, "平台小组", 3L, 30));

        WeComSyncRunner.SyncResult result = runner.run(depts, Map.of());

        assertThat(result.total()).isEqualTo(4);
        assertThat(result.success()).isEqualTo(4);
        assertThat(result.failed()).isZero();
        ArgumentCaptor<SysDept> captor = ArgumentCaptor.forClass(SysDept.class);
        verify(deptMapper, times(2)).updateById(captor.capture());
        Map<Long, Long> parents =
                captor.getAllValues().stream()
                        .filter(dept -> dept.getParentId() != null)
                        .collect(
                                Collectors.toMap(
                                        SysDept::getId, SysDept::getParentId, (a, b) -> a));
        // 上级为根部门（id=1）时父级置空，故只有 3、4 两级产生回填
        assertThat(parents).containsEntry(102L, 101L).containsEntry(103L, 102L);
    }

    @Test
    void shouldCreateNewUserWhenWecomUseridNotFound() {
        stubLoads(List.of(), List.of(), List.of());
        stubDeptInsertFrom(200L);
        stubUserInsert(300L);
        when(passwordEncoder.encode(anyString())).thenReturn("ENCODED");
        List<WeComDept> depts = List.of(new WeComDept(10L, "研发部", 1L, 1));
        Map<Long, List<WeComUser>> users =
                Map.of(
                        10L,
                        List.of(
                                new WeComUser(
                                        "wx001", "张三", "13800000000", "z@x.com", 1, List.of(10L))));

        WeComSyncRunner.SyncResult result = runner.run(depts, users);

        assertThat(result.total()).isEqualTo(2);
        assertThat(result.success()).isEqualTo(2);
        assertThat(result.failed()).isZero();
        ArgumentCaptor<SysUser> captor = ArgumentCaptor.forClass(SysUser.class);
        verify(userMapper).insert(captor.capture());
        SysUser created = captor.getValue();
        assertThat(created.getUsername()).isEqualTo("wx001");
        assertThat(created.getWecomUserid()).isEqualTo("wx001");
        assertThat(created.getPasswordHash()).isEqualTo("ENCODED");
        assertThat(created.getName()).isEqualTo("张三");
        assertThat(created.getPhone()).isEqualTo("13800000000");
        assertThat(created.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(created.getDeptId()).isEqualTo(200L);
    }

    @Test
    void shouldBindLocalUserByUniquePhone() {
        SysUser local = user(400L, "local01", "13800000000", UserStatus.ACTIVE, null);
        stubLoads(List.of(), List.of(), List.of(local));
        Map<Long, List<WeComUser>> users =
                Map.of(
                        99L,
                        List.of(
                                new WeComUser(
                                        "wx002", "李四", "13800000000", null, 1, List.of(99L))));

        WeComSyncRunner.SyncResult result = runner.run(List.of(), users);

        assertThat(result.total()).isEqualTo(1);
        assertThat(result.success()).isEqualTo(1);
        assertThat(result.failed()).isZero();
        ArgumentCaptor<SysUser> captor = ArgumentCaptor.forClass(SysUser.class);
        verify(userMapper).updateById(captor.capture());
        SysUser updated = captor.getValue();
        assertThat(updated.getId()).isEqualTo(400L);
        assertThat(updated.getWecomUserid()).isEqualTo("wx002");
        assertThat(updated.getName()).isEqualTo("李四");
        assertThat(updated.getStatus()).isEqualTo(UserStatus.ACTIVE);
        verify(userMapper, never()).insert(any(SysUser.class));
    }

    @Test
    void shouldSkipAndCountFailureWhenPhoneMatchesMultipleUsers() {
        SysUser first = user(500L, "localA", "13800000000", UserStatus.ACTIVE, null);
        SysUser second = user(501L, "localB", "13800000000", UserStatus.ACTIVE, null);
        stubLoads(List.of(), List.of(), List.of(first, second));
        Map<Long, List<WeComUser>> users =
                Map.of(
                        99L,
                        List.of(
                                new WeComUser(
                                        "wx003", "王五", "13800000000", null, 1, List.of(99L))));

        WeComSyncRunner.SyncResult result = runner.run(List.of(), users);

        assertThat(result.total()).isEqualTo(1);
        assertThat(result.success()).isZero();
        assertThat(result.failed()).isEqualTo(1);
        verify(userMapper, never()).updateById(any(SysUser.class));
        verify(userMapper, never()).insert(any(SysUser.class));
    }

    @Test
    void shouldDisableMappedUserMissingFromCurrentBatch() {
        SysUser mapped = user(600L, "wx-away", "13900000000", UserStatus.ACTIVE, "wx-away");
        stubLoads(List.of(), List.of(mapped), List.of());
        stubUserInsert(700L);
        when(passwordEncoder.encode(anyString())).thenReturn("ENCODED");
        Map<Long, List<WeComUser>> users =
                Map.of(
                        88L,
                        List.of(new WeComUser("wx-keep", "赵六", null, null, 1, List.of(88L))));

        WeComSyncRunner.SyncResult result = runner.run(List.of(), users);

        assertThat(result.total()).isEqualTo(1);
        assertThat(result.success()).isEqualTo(1);
        ArgumentCaptor<SysUser> captor = ArgumentCaptor.forClass(SysUser.class);
        verify(userMapper).updateById(captor.capture());
        SysUser disabled = captor.getValue();
        assertThat(disabled.getId()).isEqualTo(600L);
        assertThat(disabled.getStatus()).isEqualTo(UserStatus.DISABLED);
    }

    @Test
    void shouldNeverDisableLocalUserWithoutWecomUserid() {
        SysUser local = user(800L, "local-only", "13700000000", UserStatus.ACTIVE, null);
        stubLoads(List.of(), List.of(), List.of(local));
        stubUserInsert(900L);
        when(passwordEncoder.encode(anyString())).thenReturn("ENCODED");
        Map<Long, List<WeComUser>> users =
                Map.of(88L, List.of(new WeComUser("wx-new", "钱七", null, null, 1, List.of(88L))));

        WeComSyncRunner.SyncResult result = runner.run(List.of(), users);

        assertThat(result.total()).isEqualTo(1);
        assertThat(result.success()).isEqualTo(1);
        verify(userMapper, never()).updateById(any(SysUser.class));
    }

    private void stubLoads(
            List<SysDept> mappedDepts, List<SysUser> mappedUsers, List<SysUser> unboundUsers) {
        doReturn(mappedDepts).when(runner).loadMappedDepts();
        doReturn(mappedUsers).when(runner).loadMappedUsers();
        doReturn(unboundUsers).when(runner).loadUnboundUsers();
    }

    private void stubDeptInsertFrom(long startId) {
        AtomicLong sequence = new AtomicLong(startId);
        when(deptMapper.insert(any(SysDept.class)))
                .thenAnswer(
                        invocation -> {
                            SysDept dept = invocation.getArgument(0);
                            dept.setId(sequence.getAndIncrement());
                            return 1;
                        });
    }

    private void stubUserInsert(long id) {
        when(userMapper.insert(any(SysUser.class)))
                .thenAnswer(
                        invocation -> {
                            SysUser user = invocation.getArgument(0);
                            user.setId(id);
                            return 1;
                        });
    }

    private SysUser user(
            Long id, String username, String phone, UserStatus status, String wecomUserid) {
        SysUser user = new SysUser();
        user.setId(id);
        user.setUsername(username);
        user.setName(username);
        user.setPhone(phone);
        user.setStatus(status);
        user.setWecomUserid(wecomUserid);
        return user;
    }
}
