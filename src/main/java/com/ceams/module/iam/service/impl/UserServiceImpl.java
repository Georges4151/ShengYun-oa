package com.ceams.module.iam.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ceams.common.exception.BusinessException;
import com.ceams.common.exception.ErrorCode;
import com.ceams.common.response.PageResult;
import com.ceams.module.iam.converter.RoleConverter;
import com.ceams.module.iam.converter.UserConverter;
import com.ceams.module.iam.dto.user.ResetPasswordRequest;
import com.ceams.module.iam.dto.user.UserCreateRequest;
import com.ceams.module.iam.dto.user.UserUpdateRequest;
import com.ceams.module.iam.entity.SysRole;
import com.ceams.module.iam.entity.SysUser;
import com.ceams.module.iam.entity.UserRole;
import com.ceams.module.iam.enums.RoleStatus;
import com.ceams.module.iam.enums.UserStatus;
import com.ceams.module.iam.mapper.RoleMapper;
import com.ceams.module.iam.mapper.UserMapper;
import com.ceams.module.iam.mapper.UserRoleMapper;
import com.ceams.module.iam.service.UserService;
import com.ceams.module.iam.util.PageUtil;
import com.ceams.module.iam.vo.RoleVO;
import com.ceams.module.iam.vo.UserVO;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 用户服务实现。 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private static final String DEFAULT_PASSWORD = "123456";

    private final UserMapper userMapper;
    private final UserRoleMapper userRoleMapper;
    private final RoleMapper roleMapper;
    private final UserConverter userConverter;
    private final RoleConverter roleConverter;
    private final PasswordEncoder passwordEncoder;

    @Override
    public PageResult<UserVO> page(long page, long size, String keyword, String status) {
        UserStatus userStatus = parseEnum(UserStatus.class, status);
        Page<SysUser> p = PageUtil.resolve(page, size);
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.isNotBlank(keyword)) {
            wrapper.and(
                    w ->
                            w.like(SysUser::getUsername, keyword)
                                    .or()
                                    .like(SysUser::getName, keyword)
                                    .or()
                                    .like(SysUser::getEmail, keyword));
        }
        wrapper.eq(userStatus != null, SysUser::getStatus, userStatus);
        wrapper.orderByDesc(SysUser::getCreateTime);

        IPage<SysUser> result = userMapper.selectPage(p, wrapper);
        List<SysUser> records = result.getRecords();
        List<UserVO> vos = userConverter.toVOList(records);
        fillRoles(vos);
        return new PageResult<>(vos, result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    public UserVO detail(Long id) {
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw BusinessException.of(ErrorCode.BIZ_NOT_FOUND, "用户不存在");
        }
        UserVO vo = userConverter.toVO(user);
        vo.setRoles(listRolesByUser(id));
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String create(UserCreateRequest request) {
        Long count =
                userMapper.selectCount(
                        new LambdaQueryWrapper<SysUser>()
                                .eq(SysUser::getUsername, request.getUsername()));
        if (count != null && count > 0) {
            throw BusinessException.of(ErrorCode.BIZ_DUPLICATE_CODE, "用户名已存在");
        }

        SysUser user = new SysUser();
        user.setUsername(request.getUsername());
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setStatus(request.getStatus() == null ? UserStatus.ACTIVE : request.getStatus());
        user.setDeptId(request.getDeptId());
        user.setLeaderId(request.getLeaderId());
        String rawPassword =
                StringUtils.isBlank(request.getPassword())
                        ? DEFAULT_PASSWORD
                        : request.getPassword();
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        userMapper.insert(user);

        saveUserRoles(user.getId(), request.getRoleIds());
        return String.valueOf(user.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, UserUpdateRequest request) {
        SysUser user = requireUser(id);
        if (StringUtils.isNotBlank(request.getName())) {
            user.setName(request.getName());
        }
        if (request.getEmail() != null) {
            user.setEmail(request.getEmail());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }
        if (request.getStatus() != null) {
            user.setStatus(request.getStatus());
        }
        if (request.getDeptId() != null) {
            user.setDeptId(request.getDeptId());
        }
        if (request.getLeaderId() != null) {
            user.setLeaderId(request.getLeaderId());
        }
        userMapper.updateById(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        requireUser(id);
        userMapper.deleteById(id);
        userRoleMapper.delete(new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignRoles(Long id, List<Long> roleIds) {
        requireUser(id);
        List<Long> distinct = distinct(roleIds);
        validateRolesExist(distinct);
        userRoleMapper.delete(new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, id));
        if (!distinct.isEmpty()) {
            List<UserRole> links =
                    distinct.stream()
                            .map(
                                    rid -> {
                                        UserRole link = new UserRole();
                                        link.setUserId(id);
                                        link.setRoleId(rid);
                                        return link;
                                    })
                            .toList();
            links.forEach(userRoleMapper::insert);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(Long id, ResetPasswordRequest request) {
        SysUser user = requireUser(id);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        userMapper.updateById(user);
    }

    @Override
    public List<RoleVO> listRolesByUser(Long userId) {
        List<UserRole> links =
                userRoleMapper.selectList(
                        new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, userId));
        if (links.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> roleIds = links.stream().map(UserRole::getRoleId).distinct().toList();
        List<SysRole> roles = roleMapper.selectBatchIds(roleIds);
        return roleConverter.toVOList(roles);
    }

    @Override
    public List<Long> listActiveUserIdsByRoleCodes(Collection<String> roleCodes) {
        if (roleCodes == null || roleCodes.isEmpty()) {
            return List.of();
        }
        Set<String> codes =
                roleCodes.stream()
                        .filter(code -> StringUtils.isNotBlank(code))
                        .map(String::trim)
                        .collect(Collectors.toSet());
        if (codes.isEmpty()) {
            return List.of();
        }
        List<SysRole> roles =
                roleMapper.selectList(
                        new LambdaQueryWrapper<SysRole>()
                                .in(SysRole::getCode, codes)
                                .eq(SysRole::getStatus, RoleStatus.ACTIVE));
        if (roles.isEmpty()) {
            return List.of();
        }
        List<Long> roleIds =
                roles.stream().map(SysRole::getId).filter(Objects::nonNull).distinct().toList();
        if (roleIds.isEmpty()) {
            return List.of();
        }
        List<UserRole> links =
                userRoleMapper.selectList(
                        new LambdaQueryWrapper<UserRole>().in(UserRole::getRoleId, roleIds));
        if (links.isEmpty()) {
            return List.of();
        }
        Set<Long> userIds =
                links.stream()
                        .map(UserRole::getUserId)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());
        if (userIds.isEmpty()) {
            return List.of();
        }
        List<SysUser> users =
                userMapper.selectList(
                        new LambdaQueryWrapper<SysUser>()
                                .in(SysUser::getId, userIds)
                                .eq(SysUser::getStatus, UserStatus.ACTIVE));
        return users.stream().map(SysUser::getId).filter(Objects::nonNull).distinct().toList();
    }

    private void fillRoles(List<UserVO> vos) {
        if (vos.isEmpty()) {
            return;
        }
        List<Long> userIds =
                vos.stream().map(UserVO::getId).filter(Objects::nonNull).distinct().toList();
        if (userIds.isEmpty()) {
            return;
        }
        // 一次查询所有关联
        List<UserRole> links =
                userRoleMapper.selectList(
                        new LambdaQueryWrapper<UserRole>().in(UserRole::getUserId, userIds));
        if (links.isEmpty()) {
            return;
        }
        List<Long> roleIds = links.stream().map(UserRole::getRoleId).distinct().toList();
        List<SysRole> roles = roleMapper.selectBatchIds(roleIds);
        Map<Long, SysRole> roleMap =
                roles.stream().collect(Collectors.toMap(SysRole::getId, r -> r, (a, b) -> a));
        Map<Long, List<RoleVO>> userRoleMap = new HashMap<>();
        for (UserRole link : links) {
            SysRole role = roleMap.get(link.getRoleId());
            if (role == null) {
                continue;
            }
            userRoleMap
                    .computeIfAbsent(link.getUserId(), k -> new ArrayList<>())
                    .add(roleConverter.toVO(role));
        }
        for (UserVO vo : vos) {
            vo.setRoles(userRoleMap.getOrDefault(vo.getId(), Collections.emptyList()));
        }
    }

    private SysUser requireUser(Long id) {
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw BusinessException.of(ErrorCode.BIZ_NOT_FOUND, "用户不存在");
        }
        return user;
    }

    private void saveUserRoles(Long userId, List<Long> roleIds) {
        List<Long> distinct = distinct(roleIds);
        validateRolesExist(distinct);
        for (Long rid : distinct) {
            UserRole link = new UserRole();
            link.setUserId(userId);
            link.setRoleId(rid);
            userRoleMapper.insert(link);
        }
    }

    private void validateRolesExist(List<Long> roleIds) {
        if (roleIds.isEmpty()) {
            return;
        }
        Set<Long> existing =
                roleMapper.selectBatchIds(roleIds).stream()
                        .map(SysRole::getId)
                        .collect(Collectors.toSet());
        if (existing.size() != roleIds.size()) {
            throw BusinessException.of(ErrorCode.BIZ_NOT_FOUND, "存在无效的角色");
        }
    }

    private List<Long> distinct(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return new ArrayList<>();
        }
        return ids.stream().filter(Objects::nonNull).distinct().collect(Collectors.toList());
    }

    private <E extends Enum<E>> E parseEnum(Class<E> type, String value) {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        try {
            return Enum.valueOf(type, value.trim());
        } catch (IllegalArgumentException ex) {
            throw BusinessException.of(ErrorCode.VALID_ILLEGAL_ARGUMENT, "非法的枚举值: " + value);
        }
    }
}
