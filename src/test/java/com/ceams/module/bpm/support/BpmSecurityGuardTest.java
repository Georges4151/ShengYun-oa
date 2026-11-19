package com.ceams.module.bpm.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.ceams.module.bpm.enums.BpmDataScope;
import com.ceams.module.iam.entity.SysRole;
import com.ceams.module.iam.entity.SysUser;
import com.ceams.module.iam.entity.UserRole;
import com.ceams.module.iam.enums.RoleDataScope;
import com.ceams.module.iam.mapper.RoleMapper;
import com.ceams.module.iam.mapper.UserMapper;
import com.ceams.module.iam.mapper.UserRoleMapper;
import com.ceams.module.iam.service.PermissionService;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/** 数据范围单元测试：角色 data_scope 的 ALL/DEPT/SELF、多角色取最宽与非法值回退。 */
@ExtendWith(MockitoExtension.class)
class BpmSecurityGuardTest {

    private static final Long CURRENT_USER_ID = 10L;

    @Mock private PermissionService permissionService;
    @Mock private UserMapper userMapper;
    @Mock private UserRoleMapper userRoleMapper;
    @Mock private RoleMapper roleMapper;
    @Mock private BpmOrgResolver orgResolver;

    private BpmSecurityGuard guard;

    @BeforeEach
    void setUp() {
        guard =
                new BpmSecurityGuard(
                        permissionService, userMapper, userRoleMapper, roleMapper, orgResolver);
        authenticate("ROLE_USER");
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void 角色范围ALL时可见全部且不限制用户() {
        stubRoles(RoleDataScope.ALL);

        assertThat(guard.resolveDataScope()).isEqualTo(BpmDataScope.ALL);
        assertThat(guard.visibleUserIds()).isNull();
    }

    @Test
    void 角色范围DEPT时可见同部门及子部门用户() {
        stubRoles(RoleDataScope.DEPT);
        when(userMapper.selectById(CURRENT_USER_ID)).thenReturn(user(CURRENT_USER_ID, 3L));
        when(orgResolver.deptUserIds(3L)).thenReturn(List.of(CURRENT_USER_ID, 11L, 12L));

        assertThat(guard.resolveDataScope()).isEqualTo(BpmDataScope.DEPT);
        assertThat(guard.visibleUserIds()).containsExactlyInAnyOrder(10L, 11L, 12L);
    }

    @Test
    void 角色范围SELF时即使有部门也仅本人可见() {
        stubRoles(RoleDataScope.SELF);
        // 用户即使维护了部门，SELF 角色也不会触发部门范围查询
        lenient().when(userMapper.selectById(CURRENT_USER_ID)).thenReturn(user(CURRENT_USER_ID, 3L));

        assertThat(guard.resolveDataScope()).isEqualTo(BpmDataScope.SELF);
        assertThat(guard.visibleUserIds()).containsExactly(CURRENT_USER_ID);
        verify(orgResolver, never()).deptUserIds(anyLong());
    }

    @Test
    void 多角色时取最宽范围() {
        stubRoles(RoleDataScope.SELF, RoleDataScope.DEPT);

        assertThat(guard.resolveDataScope()).isEqualTo(BpmDataScope.DEPT);
    }

    @Test
    void 多角色含ALL时取ALL() {
        stubRoles(RoleDataScope.DEPT, RoleDataScope.ALL);

        assertThat(guard.resolveDataScope()).isEqualTo(BpmDataScope.ALL);
    }

    @Test
    void 无角色时回退SELF() {
        when(userRoleMapper.selectList(any(Wrapper.class))).thenReturn(List.of());

        assertThat(guard.resolveDataScope()).isEqualTo(BpmDataScope.SELF);
        assertThat(guard.visibleUserIds()).containsExactly(CURRENT_USER_ID);
    }

    @Test
    void 角色范围为空值或非法值时回退SELF() {
        stubRoles((RoleDataScope) null);

        assertThat(guard.resolveDataScope()).isEqualTo(BpmDataScope.SELF);
        assertThat(guard.visibleUserIds()).containsExactly(CURRENT_USER_ID);
    }

    @Test
    void 部门数据异常时当前用户仍可见() {
        stubRoles(RoleDataScope.DEPT);
        when(userMapper.selectById(CURRENT_USER_ID)).thenReturn(user(CURRENT_USER_ID, 3L));
        when(orgResolver.deptUserIds(3L)).thenReturn(List.of(11L));

        assertThat(guard.visibleUserIds()).containsExactlyInAnyOrder(11L, CURRENT_USER_ID);
    }

    // ------------------------------------------------------------------
    // 夹具
    // ------------------------------------------------------------------

    /** 为当前用户装配一个「用户-角色」关联，并返回对应 data_scope 的角色。 */
    private void stubRoles(RoleDataScope... scopes) {
        UserRole link = new UserRole();
        link.setUserId(CURRENT_USER_ID);
        link.setRoleId(7L);
        when(userRoleMapper.selectList(any(Wrapper.class))).thenReturn(List.of(link));
        List<SysRole> roles = new java.util.ArrayList<>();
        for (int i = 0; i < scopes.length; i++) {
            SysRole role = new SysRole();
            role.setId(7L + i);
            role.setDataScope(scopes[i]);
            roles.add(role);
        }
        when(roleMapper.selectBatchIds(any())).thenReturn(roles);
    }

    private void authenticate(String... roles) {
        List<SimpleGrantedAuthority> authorities =
                Arrays.stream(roles).map(SimpleGrantedAuthority::new).toList();
        SecurityContextHolder.getContext()
                .setAuthentication(
                        new UsernamePasswordAuthenticationToken(
                                CURRENT_USER_ID, "n/a", authorities));
    }

    private SysUser user(Long id, Long deptId) {
        SysUser user = new SysUser();
        user.setId(id);
        user.setDeptId(deptId);
        return user;
    }
}
