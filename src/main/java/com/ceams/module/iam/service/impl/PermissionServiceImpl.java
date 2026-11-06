package com.ceams.module.iam.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ceams.common.exception.BusinessException;
import com.ceams.common.exception.ErrorCode;
import com.ceams.common.security.SecurityUtils;
import com.ceams.module.iam.converter.PermissionConverter;
import com.ceams.module.iam.dto.permission.PermissionCreateRequest;
import com.ceams.module.iam.dto.permission.PermissionUpdateRequest;
import com.ceams.module.iam.entity.RolePermission;
import com.ceams.module.iam.entity.SysPermission;
import com.ceams.module.iam.entity.UserRole;
import com.ceams.module.iam.mapper.PermissionMapper;
import com.ceams.module.iam.mapper.RolePermissionMapper;
import com.ceams.module.iam.mapper.UserRoleMapper;
import com.ceams.module.iam.service.PermissionService;
import com.ceams.module.iam.vo.PermissionVO;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 权限服务实现。 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionServiceImpl implements PermissionService {

    private final PermissionMapper permissionMapper;
    private final UserRoleMapper userRoleMapper;
    private final RolePermissionMapper rolePermissionMapper;
    private final PermissionConverter permissionConverter;

    @Override
    public List<PermissionVO> tree() {
        List<SysPermission> permissions =
                permissionMapper.selectList(
                        new LambdaQueryWrapper<SysPermission>()
                                .orderByAsc(SysPermission::getSortOrder));
        List<PermissionVO> vos = permissionConverter.toVOList(permissions);

        Map<Long, PermissionVO> idMap = new HashMap<>();
        for (PermissionVO vo : vos) {
            idMap.put(vo.getId(), vo);
        }
        List<PermissionVO> roots = new ArrayList<>();
        for (PermissionVO vo : vos) {
            Long parentId = vo.getParentId();
            PermissionVO parent = (parentId != null && parentId != 0L) ? idMap.get(parentId) : null;
            if (parent != null && parent != vo) {
                parent.getChildren().add(vo);
            } else {
                roots.add(vo);
            }
        }
        sortTree(roots);
        return roots;
    }

    @Override
    public List<String> currentUserPermissionCodes() {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null || userId == 0L) {
            return List.of();
        }
        List<Long> roleIds =
                userRoleMapper
                        .selectList(
                                new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, userId))
                        .stream()
                        .map(UserRole::getRoleId)
                        .distinct()
                        .toList();
        if (roleIds.isEmpty()) {
            return List.of();
        }
        List<Long> permissionIds =
                rolePermissionMapper
                        .selectList(
                                new LambdaQueryWrapper<RolePermission>()
                                        .in(RolePermission::getRoleId, roleIds))
                        .stream()
                        .map(RolePermission::getPermissionId)
                        .distinct()
                        .toList();
        if (permissionIds.isEmpty()) {
            return List.of();
        }
        Set<String> codes = new LinkedHashSet<>();
        for (SysPermission permission : permissionMapper.selectBatchIds(permissionIds)) {
            codes.add(permission.getCode());
        }
        return new ArrayList<>(codes).stream().filter(Objects::nonNull).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String create(PermissionCreateRequest request) {
        ensureCodeUnique(request.getCode(), null);
        if (request.getParentId() != null) {
            validateParentChain(null, request.getParentId());
        }
        SysPermission permission = permissionConverter.toEntity(request);
        permissionMapper.insert(permission);
        return String.valueOf(permission.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, PermissionUpdateRequest request) {
        SysPermission permission = requirePermission(id);
        ensureCodeUnique(request.getCode(), id);
        if (request.getParentId() != null) {
            validateParentChain(id, request.getParentId());
        }
        permissionConverter.updateEntity(request, permission);
        permissionMapper.updateById(permission);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        requirePermission(id);
        Long children =
                permissionMapper.selectCount(
                        new LambdaQueryWrapper<SysPermission>().eq(SysPermission::getParentId, id));
        if (children != null && children > 0) {
            throw BusinessException.of(ErrorCode.BIZ_STATE_CONFLICT, "存在子权限，请先删除子权限");
        }
        // 关联表为物理删除，需与权限删除保持同一事务
        rolePermissionMapper.delete(
                new LambdaQueryWrapper<RolePermission>().eq(RolePermission::getPermissionId, id));
        permissionMapper.deleteById(id);
    }

    /** 校验权限编码全局唯一（excludeId 用于更新时排除自身）。 */
    private void ensureCodeUnique(String code, Long excludeId) {
        LambdaQueryWrapper<SysPermission> wrapper =
                new LambdaQueryWrapper<SysPermission>().eq(SysPermission::getCode, code);
        if (excludeId != null) {
            wrapper.ne(SysPermission::getId, excludeId);
        }
        Long count = permissionMapper.selectCount(wrapper);
        if (count != null && count > 0) {
            throw BusinessException.of(ErrorCode.BIZ_DUPLICATE_CODE, "权限编码已存在");
        }
    }

    /** 沿父链上溯：父权限必须存在，且更新时不得指向自身或后代，避免形成环。 */
    private void validateParentChain(Long id, Long parentId) {
        Set<Long> visited = new HashSet<>();
        Long cursor = parentId;
        while (cursor != null && cursor != 0L) {
            if (cursor.equals(id) || !visited.add(cursor)) {
                throw BusinessException.of(ErrorCode.BIZ_STATE_CONFLICT, "父权限不能是自身或其子权限，禁止形成循环引用");
            }
            SysPermission parent = permissionMapper.selectById(cursor);
            if (parent == null) {
                throw BusinessException.of(ErrorCode.BIZ_NOT_FOUND, "父权限不存在");
            }
            cursor = parent.getParentId();
        }
    }

    private SysPermission requirePermission(Long id) {
        SysPermission permission = permissionMapper.selectById(id);
        if (permission == null) {
            throw BusinessException.of(ErrorCode.BIZ_NOT_FOUND, "权限不存在");
        }
        return permission;
    }

    private void sortTree(List<PermissionVO> nodes) {
        if (nodes == null) {
            return;
        }
        nodes.sort(
                Comparator.comparing(
                        PermissionVO::getSort, Comparator.nullsLast(Comparator.naturalOrder())));
        for (PermissionVO node : nodes) {
            sortTree(node.getChildren());
        }
    }
}
