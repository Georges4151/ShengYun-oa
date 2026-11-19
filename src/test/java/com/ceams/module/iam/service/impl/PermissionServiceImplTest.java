package com.ceams.module.iam.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ceams.common.cache.BusinessCache;
import com.ceams.common.exception.BusinessException;
import com.ceams.common.exception.ErrorCode;
import com.ceams.module.iam.converter.PermissionConverter;
import com.ceams.module.iam.dto.permission.PermissionCreateRequest;
import com.ceams.module.iam.dto.permission.PermissionUpdateRequest;
import com.ceams.module.iam.entity.SysPermission;
import com.ceams.module.iam.enums.PermissionType;
import com.ceams.module.iam.mapper.PermissionMapper;
import com.ceams.module.iam.mapper.RolePermissionMapper;
import com.ceams.module.iam.mapper.UserRoleMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** {@link PermissionServiceImpl} 权限维护逻辑单元测试。 */
@ExtendWith(MockitoExtension.class)
class PermissionServiceImplTest {

    @Mock private PermissionMapper permissionMapper;
    @Mock private UserRoleMapper userRoleMapper;
    @Mock private RolePermissionMapper rolePermissionMapper;
    @Mock private PermissionConverter permissionConverter;
    @Mock private BusinessCache businessCache;

    @InjectMocks private PermissionServiceImpl service;

    @Test
    void 创建权限_编码重复_抛出业务异常() {
        PermissionCreateRequest request = createRequest("project:view");
        when(permissionMapper.selectCount(any())).thenReturn(1L);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.create(request));

        assertEquals(ErrorCode.BIZ_DUPLICATE_CODE, ex.getErrorCode());
        verify(permissionMapper, never()).insert(any(SysPermission.class));
    }

    @Test
    void 创建权限_父权限不存在_抛出业务异常() {
        PermissionCreateRequest request = createRequest("project:demo");
        request.setParentId(999L);
        when(permissionMapper.selectCount(any())).thenReturn(0L);
        when(permissionMapper.selectById(999L)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.create(request));

        assertEquals(ErrorCode.BIZ_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void 创建权限_成功返回新权限ID() {
        PermissionCreateRequest request = createRequest("project:view");
        SysPermission entity = new SysPermission();
        when(permissionMapper.selectCount(any())).thenReturn(0L);
        when(permissionConverter.toEntity(request)).thenReturn(entity);
        doAnswer(
                        invocation -> {
                            SysPermission inserted = invocation.getArgument(0);
                            inserted.setId(200L);
                            return 1;
                        })
                .when(permissionMapper)
                .insert(any(SysPermission.class));

        String id = service.create(request);

        assertEquals("200", id);
    }

    @Test
    void 更新权限_父权限为自身_抛出状态冲突() {
        SysPermission permission = menu(100L, null);
        when(permissionMapper.selectById(100L)).thenReturn(permission);
        when(permissionMapper.selectCount(any())).thenReturn(0L);
        PermissionUpdateRequest request = updateRequest("project:view");
        request.setParentId(100L);

        BusinessException ex =
                assertThrows(BusinessException.class, () -> service.update(100L, request));

        assertEquals(ErrorCode.BIZ_STATE_CONFLICT, ex.getErrorCode());
        verify(permissionMapper, never()).updateById(any(SysPermission.class));
    }

    @Test
    void 更新权限_父权限为后代_形成环被拒() {
        SysPermission permission = menu(100L, null);
        SysPermission child = menu(101L, 100L);
        when(permissionMapper.selectById(100L)).thenReturn(permission);
        when(permissionMapper.selectCount(any())).thenReturn(0L);
        when(permissionMapper.selectById(101L)).thenReturn(child);
        PermissionUpdateRequest request = updateRequest("project:view");
        request.setParentId(101L);

        BusinessException ex =
                assertThrows(BusinessException.class, () -> service.update(100L, request));

        assertEquals(ErrorCode.BIZ_STATE_CONFLICT, ex.getErrorCode());
        verify(permissionMapper, never()).updateById(any(SysPermission.class));
    }

    @Test
    void 更新权限_成功并委托转换器更新字段() {
        SysPermission permission = menu(100L, null);
        when(permissionMapper.selectById(100L)).thenReturn(permission);
        when(permissionMapper.selectCount(any())).thenReturn(0L);
        PermissionUpdateRequest request = updateRequest("project:view:new");

        service.update(100L, request);

        verify(permissionConverter).updateEntity(request, permission);
        verify(permissionMapper).updateById(permission);
    }

    @Test
    void 删除权限_存在子权限_抛出状态冲突() {
        when(permissionMapper.selectById(100L)).thenReturn(menu(100L, null));
        when(permissionMapper.selectCount(any())).thenReturn(1L);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.delete(100L));

        assertEquals(ErrorCode.BIZ_STATE_CONFLICT, ex.getErrorCode());
        assertEquals("存在子权限，请先删除子权限", ex.getMessage());
        verify(rolePermissionMapper, never()).delete(any());
        verify(permissionMapper, never()).deleteById(any());
    }

    @Test
    void 删除权限_成功并清理角色关联() {
        when(permissionMapper.selectById(100L)).thenReturn(menu(100L, null));
        when(permissionMapper.selectCount(any())).thenReturn(0L);

        service.delete(100L);

        verify(rolePermissionMapper).delete(any());
        verify(permissionMapper).deleteById(100L);
    }

    private PermissionCreateRequest createRequest(String code) {
        PermissionCreateRequest request = new PermissionCreateRequest();
        request.setCode(code);
        request.setName("测试权限");
        request.setType(PermissionType.BUTTON);
        return request;
    }

    private PermissionUpdateRequest updateRequest(String code) {
        PermissionUpdateRequest request = new PermissionUpdateRequest();
        request.setCode(code);
        request.setName("测试权限");
        request.setType(PermissionType.BUTTON);
        return request;
    }

    private SysPermission menu(Long id, Long parentId) {
        SysPermission permission = new SysPermission();
        permission.setId(id);
        permission.setCode("project");
        permission.setName("项目管理");
        permission.setType(PermissionType.MENU);
        permission.setParentId(parentId);
        return permission;
    }
}
