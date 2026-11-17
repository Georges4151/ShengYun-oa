package com.ceams.module.bpm.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.ceams.module.iam.entity.SysDept;
import com.ceams.module.iam.entity.SysUser;
import com.ceams.module.iam.mapper.DeptMapper;
import com.ceams.module.iam.mapper.UserMapper;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** 组织关系解析单元测试：部门子树递归、部门用户与部门名集合。 */
@ExtendWith(MockitoExtension.class)
class BpmOrgResolverTest {

    @Mock private DeptMapper deptMapper;
    @Mock private UserMapper userMapper;

    private BpmOrgResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new BpmOrgResolver(deptMapper, userMapper);
    }

    @Test
    void 部门子树包含自身与全部后代() {
        when(deptMapper.selectList(any(Wrapper.class)))
                .thenReturn(List.of(dept(1L, null), dept(2L, 1L), dept(3L, 1L), dept(4L, 2L)));

        assertThat(resolver.deptSubtreeIds(1L)).containsExactlyInAnyOrder(1L, 2L, 3L, 4L);
        assertThat(resolver.deptSubtreeIds(2L)).containsExactlyInAnyOrder(2L, 4L);
    }

    @Test
    void 未配置子部门时子树仅含自身() {
        when(deptMapper.selectList(any(Wrapper.class)))
                .thenReturn(List.of(dept(1L, null), dept(2L, 1L)));

        assertThat(resolver.deptSubtreeIds(2L)).containsExactly(2L);
    }

    @Test
    void 部门为空时子树与用户为空() {
        assertThat(resolver.deptSubtreeIds(null)).isEmpty();
        assertThat(resolver.deptUserIds(null)).isEmpty();
    }

    @Test
    void 部门用户集合包含子树内全部用户() {
        when(deptMapper.selectList(any(Wrapper.class)))
                .thenReturn(List.of(dept(1L, null), dept(2L, 1L)));
        when(userMapper.selectList(any(Wrapper.class)))
                .thenReturn(List.of(user(11L, 1L), user(12L, 2L)));

        assertThat(resolver.deptUserIds(1L)).containsExactly(11L, 12L);
    }

    @Test
    void 部门名集合按部门ID解析() {
        when(deptMapper.selectList(any(Wrapper.class)))
                .thenReturn(List.of(dept(1L, null, "销售部"), dept(2L, 1L, "华东组")));

        assertThat(resolver.deptNames(Set.of(1L, 2L))).containsExactlyInAnyOrder("销售部", "华东组");
    }

    // ------------------------------------------------------------------
    // 夹具
    // ------------------------------------------------------------------

    private SysDept dept(Long id, Long parentId) {
        return dept(id, parentId, "部门" + id);
    }

    private SysDept dept(Long id, Long parentId, String name) {
        SysDept dept = new SysDept();
        dept.setId(id);
        dept.setParentId(parentId);
        dept.setName(name);
        return dept;
    }

    private SysUser user(Long id, Long deptId) {
        SysUser user = new SysUser();
        user.setId(id);
        user.setDeptId(deptId);
        return user;
    }
}
