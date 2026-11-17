package com.ceams.module.doc.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.ceams.module.bpm.support.BpmSecurityGuard;
import com.ceams.module.doc.entity.DocDirectory;
import com.ceams.module.doc.enums.DocAction;
import com.ceams.module.doc.enums.LibraryType;
import com.ceams.module.doc.mapper.DocDirPermissionMapper;
import com.ceams.module.doc.mapper.DocFilePermissionMapper;
import com.ceams.module.iam.entity.SysDept;
import com.ceams.module.iam.entity.SysUser;
import com.ceams.module.iam.mapper.DeptMapper;
import com.ceams.module.iam.mapper.UserMapper;
import com.ceams.module.iam.mapper.UserRoleMapper;
import com.ceams.module.project.entity.Project;
import com.ceams.module.project.mapper.ProjectMapper;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** 文档数据权限解析单元测试：库默认规则与管理员全权。 */
@ExtendWith(MockitoExtension.class)
class DocAccessResolverTest {

    private static final Long USER_ID = 100L;

    private static final Long OTHER_USER_ID = 200L;

    private static final Long PROJECT_ID = 900L;

    /** 部门树：10 → 20 → 30，另有兄弟部门 21。 */
    private static final Long DEPT_ROOT = 10L;

    private static final Long DEPT_CHILD = 20L;

    private static final Long DEPT_GRAND_CHILD = 30L;

    private static final Long DEPT_SIBLING = 21L;

    @Mock private DeptMapper deptMapper;

    @Mock private DocDirPermissionMapper dirPermissionMapper;

    @Mock private DocFilePermissionMapper filePermissionMapper;

    @Mock private BpmSecurityGuard securityGuard;

    @Mock private UserMapper userMapper;

    @Mock private UserRoleMapper userRoleMapper;

    @Mock private ProjectMapper projectMapper;

    private DocAccessResolver resolver;

    @BeforeEach
    void setUp() {
        resolver =
                new DocAccessResolver(
                        deptMapper,
                        dirPermissionMapper,
                        filePermissionMapper,
                        securityGuard,
                        userMapper,
                        userRoleMapper,
                        projectMapper);
    }

    @Test
    void personalLibraryShouldBeVisibleOnlyToOwner() {
        DocDirectory dir = dir(LibraryType.PERSONAL, USER_ID, null, null);

        assertThat(resolver.resolve(USER_ID, dir, null))
                .containsExactlyInAnyOrder(
                        DocAction.PREVIEW, DocAction.DOWNLOAD, DocAction.EDIT, DocAction.DELETE);
        assertThat(resolver.resolve(OTHER_USER_ID, dir, null)).isEmpty();
    }

    @Test
    void deptLibraryShouldBeVisibleToSameDept() {
        when(userMapper.selectById(USER_ID)).thenReturn(user(DEPT_CHILD));
        when(deptMapper.selectList(any())).thenReturn(deptTree());

        DocDirectory dir = dir(LibraryType.DEPT, null, DEPT_CHILD, null);

        assertThat(resolver.resolve(USER_ID, dir, null)).hasSize(4);
    }

    @Test
    void deptLibraryShouldBeVisibleToChildDept() {
        // 用户在高一级部门（祖先链 30 → 20 → 10），部门库归属根部门 10
        when(userMapper.selectById(USER_ID)).thenReturn(user(DEPT_GRAND_CHILD));
        when(deptMapper.selectList(any())).thenReturn(deptTree());

        DocDirectory dir = dir(LibraryType.DEPT, null, DEPT_ROOT, null);

        assertThat(resolver.resolve(USER_ID, dir, null)).hasSize(4);
    }

    @Test
    void deptLibraryShouldNotBeVisibleToSiblingDept() {
        when(userMapper.selectById(USER_ID)).thenReturn(user(DEPT_CHILD));
        when(deptMapper.selectList(any())).thenReturn(deptTree());

        DocDirectory dir = dir(LibraryType.DEPT, null, DEPT_SIBLING, null);

        assertThat(resolver.resolve(USER_ID, dir, null)).isEmpty();
    }

    @Test
    void projectLibraryShouldBeVisibleToManager() {
        when(projectMapper.selectById(PROJECT_ID)).thenReturn(project(PROJECT_ID, USER_ID));

        DocDirectory dir = dir(LibraryType.PROJECT, null, null, PROJECT_ID);

        assertThat(resolver.resolve(USER_ID, dir, null)).hasSize(4);
        assertThat(resolver.resolve(OTHER_USER_ID, dir, null)).isEmpty();
    }

    @Test
    void publicLibraryShouldBeVisibleToEveryone() {
        DocDirectory dir = dir(LibraryType.PUBLIC, null, null, null);

        assertThat(resolver.resolve(USER_ID, dir, null)).hasSize(4);
        assertThat(resolver.resolve(OTHER_USER_ID, dir, null)).hasSize(4);
    }

    @Test
    void adminShouldGetAllActionsEvenIfLibraryNotMatched() {
        when(securityGuard.hasPermission(anyString())).thenReturn(true);

        // 个人库属于他人，但管理员（doc:permission:manage）仍全权
        DocDirectory dir = dir(LibraryType.PERSONAL, OTHER_USER_ID, null, null);

        assertThat(resolver.resolve(USER_ID, dir, null))
                .containsExactlyInAnyOrder(
                        DocAction.PREVIEW, DocAction.DOWNLOAD, DocAction.EDIT, DocAction.DELETE);
        assertThat(resolver.isFullAccess()).isTrue();
    }

    // ------------------------------------------------------------------
    // 测试夹具
    // ------------------------------------------------------------------

    private DocDirectory dir(
            LibraryType type, Long ownerUserId, Long ownerDeptId, Long projectId) {
        DocDirectory dir = new DocDirectory();
        dir.setId(500L);
        dir.setName("测试目录");
        dir.setLibraryType(type);
        dir.setOwnerUserId(ownerUserId);
        dir.setOwnerDeptId(ownerDeptId);
        dir.setProjectId(projectId);
        dir.setPath("/500/");
        return dir;
    }

    private SysUser user(Long deptId) {
        SysUser user = new SysUser();
        user.setId(USER_ID);
        user.setName("测试用户");
        user.setDeptId(deptId);
        return user;
    }

    private Project project(Long projectId, Long managerId) {
        Project project = new Project();
        project.setId(projectId);
        project.setProjectName("测试项目");
        project.setManagerId(managerId);
        return project;
    }

    private List<SysDept> deptTree() {
        return List.of(
                dept(DEPT_ROOT, null),
                dept(DEPT_CHILD, DEPT_ROOT),
                dept(DEPT_GRAND_CHILD, DEPT_CHILD),
                dept(DEPT_SIBLING, DEPT_ROOT));
    }

    private SysDept dept(Long id, Long parentId) {
        SysDept dept = new SysDept();
        dept.setId(id);
        dept.setName("部门" + id);
        dept.setParentId(parentId);
        return dept;
    }
}
