package com.ceams.module.iam.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ceams.common.exception.BusinessException;
import com.ceams.common.exception.ErrorCode;
import com.ceams.common.response.PageResult;
import com.ceams.module.iam.converter.RoleConverter;
import com.ceams.module.iam.dto.role.RoleCreateRequest;
import com.ceams.module.iam.dto.role.RoleUpdateRequest;
import com.ceams.module.iam.entity.RolePermission;
import com.ceams.module.iam.entity.SysRole;
import com.ceams.module.iam.entity.UserRole;
import com.ceams.module.iam.enums.RoleStatus;
import com.ceams.module.iam.mapper.RoleMapper;
import com.ceams.module.iam.mapper.RolePermissionMapper;
import com.ceams.module.iam.mapper.UserRoleMapper;
import com.ceams.module.iam.service.RoleService;
import com.ceams.module.iam.util.PageUtil;
import com.ceams.module.iam.vo.RoleVO;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 角色服务实现。 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

    private final RoleMapper roleMapper;
    private final RolePermissionMapper rolePermissionMapper;
    private final UserRoleMapper userRoleMapper;
    private final RoleConverter roleConverter;

    @Override
    public PageResult<RoleVO> page(long page, long size, String keyword) {
        Page<SysRole> p = PageUtil.resolve(page, size);
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.isNotBlank(keyword)) {
            wrapper.and(
                    w -> w.like(SysRole::getCode, keyword).or().like(SysRole::getName, keyword));
        }
        wrapper.orderByDesc(SysRole::getCreateTime);
        IPage<SysRole> result = roleMapper.selectPage(p, wrapper);
        return new PageResult<>(
                roleConverter.toVOList(result.getRecords()),
                result.getTotal(),
                result.getCurrent(),
                result.getSize());
    }

    @Override
    public RoleVO detail(Long id) {
        return roleConverter.toVO(requireRole(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String create(RoleCreateRequest request) {
        Long count =
                roleMapper.selectCount(
                        new LambdaQueryWrapper<SysRole>().eq(SysRole::getCode, request.getCode()));
        if (count != null && count > 0) {
            throw BusinessException.of(ErrorCode.BIZ_DUPLICATE_CODE, "角色编码已存在");
        }
        SysRole role = new SysRole();
        role.setCode(request.getCode());
        role.setName(request.getName());
        role.setDescription(request.getDescription());
        role.setStatus(RoleStatus.ACTIVE);
        roleMapper.insert(role);
        return String.valueOf(role.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, RoleUpdateRequest request) {
        SysRole role = requireRole(id);
        if (StringUtils.isNotBlank(request.getName())) {
            role.setName(request.getName());
        }
        if (request.getDescription() != null) {
            role.setDescription(request.getDescription());
        }
        if (request.getStatus() != null) {
            role.setStatus(request.getStatus());
        }
        roleMapper.updateById(role);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        requireRole(id);
        Long refs =
                userRoleMapper.selectCount(
                        new LambdaQueryWrapper<UserRole>().eq(UserRole::getRoleId, id));
        if (refs != null && refs > 0) {
            throw BusinessException.of(ErrorCode.BIZ_REFERENCED, "该角色已分配给用户，无法删除");
        }
        rolePermissionMapper.delete(
                new LambdaQueryWrapper<RolePermission>().eq(RolePermission::getRoleId, id));
        roleMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignPermissions(Long id, List<Long> permissionIds) {
        requireRole(id);
        List<Long> distinct =
                permissionIds == null
                        ? new ArrayList<>()
                        : permissionIds.stream().filter(Objects::nonNull).distinct().toList();
        rolePermissionMapper.delete(
                new LambdaQueryWrapper<RolePermission>().eq(RolePermission::getRoleId, id));
        for (Long pid : distinct) {
            RolePermission link = new RolePermission();
            link.setRoleId(id);
            link.setPermissionId(pid);
            rolePermissionMapper.insert(link);
        }
    }

    private SysRole requireRole(Long id) {
        SysRole role = roleMapper.selectById(id);
        if (role == null) {
            throw BusinessException.of(ErrorCode.BIZ_NOT_FOUND, "角色不存在");
        }
        return role;
    }
}
