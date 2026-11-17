package com.ceams.module.iam.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ceams.common.exception.BusinessException;
import com.ceams.common.exception.ErrorCode;
import com.ceams.common.response.PageResult;
import com.ceams.common.security.SecurityUtils;
import com.ceams.module.iam.dto.dept.DeptCreateRequest;
import com.ceams.module.iam.dto.dept.DeptUpdateRequest;
import com.ceams.module.iam.entity.SysDept;
import com.ceams.module.iam.entity.SysUser;
import com.ceams.module.iam.mapper.DeptMapper;
import com.ceams.module.iam.mapper.UserMapper;
import com.ceams.module.iam.service.DeptService;
import com.ceams.module.iam.service.PermissionService;
import com.ceams.module.iam.util.PageUtil;
import com.ceams.module.iam.vo.DeptVO;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 部门服务实现：组织树查询与最小维护（不级联删除）。 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeptServiceImpl implements DeptService {

    /** 部门查看权限码（V20 种子）。 */
    private static final String PERMISSION_VIEW = "iam:dept:view";

    /** 部门管理权限码（V20 种子）。 */
    private static final String PERMISSION_MANAGE = "iam:dept:manage";

    private final DeptMapper deptMapper;
    private final UserMapper userMapper;
    private final PermissionService permissionService;

    @Override
    public List<DeptVO> tree() {
        requirePermission(PERMISSION_VIEW);
        List<SysDept> depts =
                deptMapper.selectList(
                        new LambdaQueryWrapper<SysDept>()
                                .orderByAsc(SysDept::getSortOrder)
                                .orderByAsc(SysDept::getId));
        Map<Long, String> leaderNames = loadLeaderNames(depts);
        Map<Long, DeptVO> voMap = new HashMap<>();
        for (SysDept dept : depts) {
            voMap.put(dept.getId(), toVO(dept, leaderNames));
        }
        List<DeptVO> roots = new ArrayList<>();
        for (SysDept dept : depts) {
            DeptVO vo = voMap.get(dept.getId());
            DeptVO parent = dept.getParentId() == null ? null : voMap.get(dept.getParentId());
            if (parent != null && parent != vo) {
                parent.getChildren().add(vo);
            } else {
                // 根部门，或上级已被删除（降级为根，避免节点丢失）
                roots.add(vo);
            }
        }
        return roots;
    }

    @Override
    public PageResult<DeptVO> page(long page, long size, String keyword) {
        requirePermission(PERMISSION_VIEW);
        Page<SysDept> p = PageUtil.resolve(page, size);
        LambdaQueryWrapper<SysDept> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.isNotBlank(keyword)) {
            wrapper.like(SysDept::getName, keyword.trim());
        }
        wrapper.orderByAsc(SysDept::getSortOrder).orderByAsc(SysDept::getId);
        IPage<SysDept> result = deptMapper.selectPage(p, wrapper);
        List<SysDept> records = result.getRecords();
        List<DeptVO> vos = toVOList(records);
        return new PageResult<>(vos, result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String create(DeptCreateRequest request) {
        requirePermission(PERMISSION_MANAGE);
        String name = StringUtils.trimToNull(request.getName());
        if (name == null) {
            throw BusinessException.of(ErrorCode.VALID_REQUIRED_FIELD, "部门名称不能为空");
        }
        Long parentId = request.getParentId();
        validateParent(parentId, null);
        if (request.getLeaderId() != null) {
            validateLeader(request.getLeaderId());
        }
        ensureNameUnique(name, parentId, null);

        SysDept dept = new SysDept();
        dept.setName(name);
        dept.setParentId(parentId);
        dept.setLeaderId(request.getLeaderId());
        dept.setSortOrder(request.getSortOrder() == null ? 0 : request.getSortOrder());
        deptMapper.insert(dept);
        log.info("新建部门：id={}，name={}，parentId={}", dept.getId(), name, parentId);
        return String.valueOf(dept.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, DeptUpdateRequest request) {
        requirePermission(PERMISSION_MANAGE);
        SysDept dept = requireDept(id);

        String name = dept.getName();
        if (request.getName() != null) {
            name = StringUtils.trimToNull(request.getName());
            if (name == null) {
                throw BusinessException.of(ErrorCode.VALID_REQUIRED_FIELD, "部门名称不能为空");
            }
        }
        // parentId 为空表示不修改；0 表示移动到根部门
        Long parentId = dept.getParentId();
        if (request.getParentId() != null) {
            parentId = request.getParentId() == 0L ? null : request.getParentId();
            validateParent(parentId, id);
        }
        if (request.getLeaderId() != null) {
            validateLeader(request.getLeaderId());
        }
        ensureNameUnique(name, parentId, id);

        dept.setName(name);
        dept.setParentId(parentId);
        if (request.getLeaderId() != null) {
            dept.setLeaderId(request.getLeaderId());
        }
        if (request.getSortOrder() != null) {
            dept.setSortOrder(request.getSortOrder());
        }
        deptMapper.updateById(dept);
        log.info("修改部门：id={}，name={}，parentId={}", dept.getId(), name, parentId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        requirePermission(PERMISSION_MANAGE);
        SysDept dept = requireDept(id);
        long children =
                deptMapper.selectCount(
                        new LambdaQueryWrapper<SysDept>().eq(SysDept::getParentId, id));
        if (children > 0) {
            throw BusinessException.of(
                    ErrorCode.BIZ_REFERENCED, "该部门下存在子部门，请先删除或迁移子部门后再删除");
        }
        long users =
                userMapper.selectCount(new LambdaQueryWrapper<SysUser>().eq(SysUser::getDeptId, id));
        if (users > 0) {
            throw BusinessException.of(
                    ErrorCode.BIZ_REFERENCED, "该部门下存在用户，请先调整用户所属部门后再删除");
        }
        deptMapper.deleteById(id);
        log.info("删除部门：id={}，name={}", dept.getId(), dept.getName());
    }

    // ------------------------------------------------------------------
    // 内部工具
    // ------------------------------------------------------------------

    /** 权限校验：管理员直接放行，其余用户需命中对应权限码（权限码由 V20 种子授予 SYS_ADMIN）。 */
    private void requirePermission(String permissionCode) {
        Long current = SecurityUtils.getCurrentUserId();
        if (current == null) {
            throw BusinessException.of(ErrorCode.AUTH_ACCESS_DENIED, "未登录不可操作部门");
        }
        if (isAdmin()) {
            return;
        }
        List<String> codes = permissionService.currentUserPermissionCodes();
        if (codes == null || !codes.contains(permissionCode)) {
            throw BusinessException.of(ErrorCode.AUTH_ACCESS_DENIED, "缺少权限：" + permissionCode);
        }
    }

    /** 是否系统管理员（ROLE_SYS_ADMIN）；与 BPM 权限守卫保持一致的角色判定口径。 */
    private boolean isAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_SYS_ADMIN"::equals);
    }

    private SysDept requireDept(Long id) {
        SysDept dept = deptMapper.selectById(id);
        if (dept == null) {
            throw BusinessException.of(ErrorCode.BIZ_NOT_FOUND, "部门不存在");
        }
        return dept;
    }

    /**
     * 校验上级部门：必须存在，且不能成环（不能挂到自己或自己的子部门下）。
     *
     * @param parentId 目标上级部门 ID（null 表示根部门）
     * @param selfId 当前部门 ID（新建时为 null）
     */
    private void validateParent(Long parentId, Long selfId) {
        if (parentId == null) {
            return;
        }
        if (selfId != null && parentId.equals(selfId)) {
            throw BusinessException.of(ErrorCode.VALID_ILLEGAL_ARGUMENT, "上级部门不能是自己");
        }
        SysDept parent = deptMapper.selectById(parentId);
        if (parent == null) {
            throw BusinessException.of(ErrorCode.BIZ_NOT_FOUND, "上级部门不存在");
        }
        if (selfId == null) {
            return;
        }
        // 沿新上级向上回溯，若遇到自己说明目标在其子树内，拒绝形成环
        Set<Long> visited = new HashSet<>();
        Long cursor = parentId;
        while (cursor != null && visited.add(cursor)) {
            SysDept node = deptMapper.selectById(cursor);
            if (node == null) {
                break;
            }
            if (node.getId().equals(selfId)) {
                throw BusinessException.of(
                        ErrorCode.VALID_ILLEGAL_ARGUMENT, "不能移动到自己的子部门下");
            }
            cursor = node.getParentId();
        }
    }

    private void validateLeader(Long leaderId) {
        SysUser leader = userMapper.selectById(leaderId);
        if (leader == null) {
            throw BusinessException.of(ErrorCode.BIZ_NOT_FOUND, "部门主管用户不存在");
        }
    }

    /** 同级部门名称唯一性校验。 */
    private void ensureNameUnique(String name, Long parentId, Long excludeId) {
        LambdaQueryWrapper<SysDept> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysDept::getName, name);
        if (parentId == null) {
            wrapper.isNull(SysDept::getParentId);
        } else {
            wrapper.eq(SysDept::getParentId, parentId);
        }
        if (excludeId != null) {
            wrapper.ne(SysDept::getId, excludeId);
        }
        if (deptMapper.selectCount(wrapper) > 0) {
            throw BusinessException.of(
                    ErrorCode.BIZ_DUPLICATE_CODE, "同级下已存在同名部门：" + name);
        }
    }

    private List<DeptVO> toVOList(List<SysDept> depts) {
        if (depts == null || depts.isEmpty()) {
            return List.of();
        }
        Map<Long, String> leaderNames = loadLeaderNames(depts);
        return depts.stream().map(dept -> toVO(dept, leaderNames)).toList();
    }

    private DeptVO toVO(SysDept dept, Map<Long, String> leaderNames) {
        DeptVO vo = new DeptVO();
        vo.setId(dept.getId());
        vo.setName(dept.getName());
        vo.setParentId(dept.getParentId());
        vo.setLeaderId(dept.getLeaderId());
        vo.setLeaderName(dept.getLeaderId() == null ? null : leaderNames.get(dept.getLeaderId()));
        vo.setSortOrder(dept.getSortOrder());
        vo.setCreateTime(dept.getCreateTime());
        return vo;
    }

    /** 批量查询部门主管姓名（一次 IN 查询，避免 N+1）。 */
    private Map<Long, String> loadLeaderNames(List<SysDept> depts) {
        Set<Long> leaderIds =
                depts.stream()
                        .map(SysDept::getLeaderId)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());
        if (leaderIds.isEmpty()) {
            return Map.of();
        }
        List<SysUser> users = userMapper.selectBatchIds(leaderIds);
        if (users == null || users.isEmpty()) {
            return Map.of();
        }
        return users.stream()
                .filter(user -> user.getId() != null)
                .collect(
                        Collectors.toMap(
                                SysUser::getId,
                                user -> user.getName() == null ? "" : user.getName(),
                                (a, b) -> a));
    }
}
