package com.ceams.module.iam.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ceams.common.security.SecurityUtils;
import com.ceams.module.iam.converter.PermissionConverter;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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
