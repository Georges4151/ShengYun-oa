package com.ceams.module.bpm.support;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ceams.module.iam.entity.SysDept;
import com.ceams.module.iam.entity.SysUser;
import com.ceams.module.iam.mapper.DeptMapper;
import com.ceams.module.iam.mapper.UserMapper;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 组织关系解析器（部门子树与部门用户）。
 *
 * <p>本期组织树规模较小，采用“一次加载全部部门后在内存 BFS 收集子树”的方式，
 * 避免递归 SQL 与 N+1 查询；后续组织规模变大时可替换为独立查询实现，调用方不变。
 */
@Component
@RequiredArgsConstructor
public class BpmOrgResolver {

    private final DeptMapper deptMapper;
    private final UserMapper userMapper;

    /** 部门子树 ID 集合（含自身）；deptId 为空返回空集。 */
    public Set<Long> deptSubtreeIds(Long deptId) {
        if (deptId == null) {
            return Set.of();
        }
        List<SysDept> all = deptMapper.selectList(new LambdaQueryWrapper<SysDept>());
        Map<Long, List<Long>> childrenByParent = new HashMap<>();
        for (SysDept dept : all) {
            if (dept == null || dept.getId() == null || dept.getParentId() == null) {
                continue;
            }
            childrenByParent
                    .computeIfAbsent(dept.getParentId(), k -> new ArrayList<>())
                    .add(dept.getId());
        }
        Set<Long> result = new LinkedHashSet<>();
        result.add(deptId);
        Deque<Long> queue = new ArrayDeque<>();
        queue.add(deptId);
        while (!queue.isEmpty()) {
            Long current = queue.poll();
            for (Long child : childrenByParent.getOrDefault(current, List.of())) {
                if (result.add(child)) {
                    queue.add(child);
                }
            }
        }
        return result;
    }

    /** 部门子树内全部用户 ID（按 ID 升序去重，不区分用户状态）；deptId 为空返回空集。 */
    public List<Long> deptUserIds(Long deptId) {
        Set<Long> deptIds = deptSubtreeIds(deptId);
        if (deptIds.isEmpty()) {
            return List.of();
        }
        return userMapper
                .selectList(
                        new LambdaQueryWrapper<SysUser>().in(SysUser::getDeptId, deptIds))
                .stream()
                .map(SysUser::getId)
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .toList();
    }

    /** 部门 ID 集合对应的部门名称集合（用于部门条件按名称匹配）。 */
    public Set<String> deptNames(Set<Long> deptIds) {
        if (deptIds == null || deptIds.isEmpty()) {
            return Set.of();
        }
        List<SysDept> depts =
                deptMapper.selectList(
                        new LambdaQueryWrapper<SysDept>().in(SysDept::getId, deptIds));
        Set<String> names = new LinkedHashSet<>();
        for (SysDept dept : depts) {
            if (dept != null && dept.getName() != null) {
                names.add(dept.getName());
            }
        }
        return names;
    }
}
