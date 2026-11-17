package com.ceams.module.integration.support;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.ceams.module.iam.entity.SysDept;
import com.ceams.module.iam.entity.SysUser;
import com.ceams.module.iam.enums.UserStatus;
import com.ceams.module.iam.mapper.DeptMapper;
import com.ceams.module.iam.mapper.UserMapper;
import com.ceams.module.integration.client.WeComDept;
import com.ceams.module.integration.client.WeComUser;
import com.ceams.module.integration.config.WeComAppProperties;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 企业微信组织同步执行器（纯本地逻辑，不发起 HTTP）。
 *
 * <p>口径：
 *
 * <ul>
 *   <li>部门先按 {@code wecom_dept_id} upsert，第一轮建立 {@code wecomDeptId → sysDeptId}
 *       映射，第二轮统一回填 {@code parent_id}；企业微信根部门（id=1）或上级未映射时置空。
 *   <li>成员按 {@code wecom_userid} 去重后 upsert：已绑定按账号更新；未绑定先按手机号在
 *       「未绑定且未删除」的本地账号中唯一匹配并回填，多命中不猜测直接跳过并计失败。
 *   <li>本批未出现的已绑定成员置为 DISABLED；{@code wecom_userid IS NULL} 的本地账号绝不禁用。
 *   <li>{@code leader_id} 不同步。
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WeComSyncRunner {

    private final UserMapper userMapper;
    private final DeptMapper deptMapper;
    private final PasswordEncoder passwordEncoder;
    @SuppressWarnings("unused")
    private final WeComAppProperties properties;

    /**
     * 执行一次本地组织同步。
     *
     * @param depts 企业微信部门列表
     * @param usersByDept 部门 ID → 直属成员列表
     * @return 同步统计（total = 部门数 + 去重后成员数）
     */
    public SyncResult run(List<WeComDept> depts, Map<Long, List<WeComUser>> usersByDept) {
        List<WeComDept> deptList =
                depts == null
                        ? List.of()
                        : depts.stream().filter(Objects::nonNull).toList();
        List<WeComUser> members = dedupeMembers(usersByDept);
        int total = deptList.size() + members.size();
        int success = 0;
        int failed = 0;

        Map<String, SysDept> mappedDepts = indexDepts(loadMappedDepts());
        Map<String, Long> deptMapping = new HashMap<>();
        Map<String, SysDept> syncedDepts = new LinkedHashMap<>();

        // 第一轮：upsert 部门基础信息并建立映射
        for (WeComDept dept : deptList) {
            try {
                if (dept.id() == null) {
                    throw new IllegalArgumentException("部门 ID 为空");
                }
                SysDept sysDept = upsertDeptBasic(dept, mappedDepts);
                deptMapping.put(String.valueOf(dept.id()), sysDept.getId());
                syncedDepts.put(String.valueOf(dept.id()), sysDept);
            } catch (Exception e) {
                failed++;
                log.warn("同步企业微信部门失败：wecomDeptId={}，原因={}", dept.id(), e.getMessage());
            }
        }

        // 第二轮：统一回填上级部门（新增部门此时也能拿到映射）
        for (WeComDept dept : deptList) {
            SysDept sysDept = syncedDepts.get(String.valueOf(dept.id()));
            if (sysDept == null) {
                continue;
            }
            try {
                applyParent(dept, sysDept, deptMapping);
                success++;
            } catch (Exception e) {
                failed++;
                log.warn("回填部门上级失败：wecomDeptId={}，原因={}", dept.id(), e.getMessage());
            }
        }

        List<SysUser> mappedUsers = loadMappedUsers();
        Map<String, SysUser> mappedByWecomid = indexUsers(mappedUsers);
        Map<String, List<SysUser>> phoneIndex = indexPhones(loadUnboundUsers());
        Set<String> presentUserids = new HashSet<>();

        for (WeComUser user : members) {
            presentUserids.add(user.userid().trim());
            try {
                upsertUser(user, mappedByWecomid, phoneIndex, deptMapping);
                success++;
            } catch (Exception e) {
                failed++;
                log.warn("同步企业微信成员失败：userid={}，原因={}", user.userid(), e.getMessage());
            }
        }

        disableMissingUsers(mappedUsers, presentUserids);

        log.info("企业微信组织同步完成：total={}，success={}，failed={}", total, success, failed);
        return new SyncResult(total, success, failed);
    }

    // ------------------------------------------------------------------
    // 部门
    // ------------------------------------------------------------------

    /** 加载已绑定企业微信的部门，供 spy 单测替换。 */
    List<SysDept> loadMappedDepts() {
        return deptMapper.selectList(
                new LambdaQueryWrapper<SysDept>().isNotNull(SysDept::getWecomDeptId));
    }

    private SysDept upsertDeptBasic(WeComDept dept, Map<String, SysDept> mappedDepts) {
        String wecomDeptId = String.valueOf(dept.id());
        String name =
                StringUtils.hasText(dept.name()) ? dept.name().trim() : "企业微信部门-" + wecomDeptId;
        Integer sortOrder = dept.order() == null ? 0 : dept.order();
        SysDept existing = mappedDepts.get(wecomDeptId);
        if (existing == null) {
            SysDept created = new SysDept();
            created.setWecomDeptId(wecomDeptId);
            created.setName(name);
            created.setSortOrder(sortOrder);
            deptMapper.insert(created);
            return created;
        }
        SysDept update = new SysDept();
        update.setId(existing.getId());
        update.setName(name);
        update.setSortOrder(sortOrder);
        deptMapper.updateById(update);
        existing.setName(name);
        existing.setSortOrder(sortOrder);
        return existing;
    }

    private void applyParent(WeComDept dept, SysDept sysDept, Map<String, Long> deptMapping) {
        Long parentWecomId = dept.parentId();
        Long desiredParentId =
                parentWecomId == null || parentWecomId <= 1L
                        ? null
                        : deptMapping.get(String.valueOf(parentWecomId));
        if (Objects.equals(sysDept.getParentId(), desiredParentId)) {
            return;
        }
        if (desiredParentId == null) {
            // updateById 会忽略 null 字段，清空上级必须使用显式 set
            deptMapper.update(
                    null,
                    new LambdaUpdateWrapper<SysDept>()
                            .eq(SysDept::getId, sysDept.getId())
                            .set(SysDept::getParentId, null));
        } else {
            SysDept update = new SysDept();
            update.setId(sysDept.getId());
            update.setParentId(desiredParentId);
            deptMapper.updateById(update);
        }
        sysDept.setParentId(desiredParentId);
    }

    // ------------------------------------------------------------------
    // 成员
    // ------------------------------------------------------------------

    /** 加载已绑定企业微信的成员，供 spy 单测替换。 */
    List<SysUser> loadMappedUsers() {
        return userMapper.selectList(
                new LambdaQueryWrapper<SysUser>().isNotNull(SysUser::getWecomUserid));
    }

    /** 加载未绑定企业微信的本地账号（手机号兜底匹配候选），供 spy 单测替换。 */
    List<SysUser> loadUnboundUsers() {
        return userMapper.selectList(
                new LambdaQueryWrapper<SysUser>().isNull(SysUser::getWecomUserid));
    }

    private void upsertUser(
            WeComUser user,
            Map<String, SysUser> mappedByWecomid,
            Map<String, List<SysUser>> phoneIndex,
            Map<String, Long> deptMapping) {
        String wecomUserid = user.userid().trim();
        Long deptId = resolveDeptId(user, deptMapping);
        UserStatus status = mapStatus(user.status());
        SysUser mapped = mappedByWecomid.get(wecomUserid);
        if (mapped != null) {
            SysUser update = new SysUser();
            update.setId(mapped.getId());
            fillUserFields(update, user, deptId, status);
            if (StringUtils.hasText(user.mobile())) {
                // 通过实体 updateById 写入，交由 SensitiveTypeHandler 自动加密
                update.setPhone(user.mobile().trim());
            }
            userMapper.updateById(update);
            return;
        }

        String phone = normalizePhone(user.mobile());
        if (phone != null) {
            List<SysUser> candidates = phoneIndex.get(phone);
            if (candidates != null && candidates.size() > 1) {
                throw new IllegalStateException(
                        "手机号 " + maskPhone(phone) + " 匹配到多个本地账号，已跳过");
            }
            if (candidates != null && candidates.size() == 1) {
                SysUser target = candidates.get(0);
                SysUser update = new SysUser();
                update.setId(target.getId());
                update.setWecomUserid(wecomUserid);
                fillUserFields(update, user, deptId, status);
                userMapper.updateById(update);
                phoneIndex.remove(phone);
                return;
            }
        }

        SysUser created = new SysUser();
        created.setUsername(wecomUserid);
        created.setWecomUserid(wecomUserid);
        // 占位随机密码：同步账号不可用密码登录，需走企业微信扫码（P3）
        created.setPasswordHash(passwordEncoder.encode(UUID.randomUUID().toString()));
        created.setName(
                StringUtils.hasText(user.name()) ? user.name().trim() : wecomUserid);
        created.setEmail(StringUtils.hasText(user.email()) ? user.email().trim() : null);
        created.setPhone(StringUtils.hasText(user.mobile()) ? user.mobile().trim() : null);
        created.setStatus(status);
        created.setDeptId(deptId);
        userMapper.insert(created);
    }

    private void fillUserFields(
            SysUser target, WeComUser user, Long deptId, UserStatus status) {
        if (StringUtils.hasText(user.name())) {
            target.setName(user.name().trim());
        }
        if (StringUtils.hasText(user.email())) {
            target.setEmail(user.email().trim());
        }
        if (deptId != null) {
            target.setDeptId(deptId);
        }
        target.setStatus(status);
    }

    private void disableMissingUsers(List<SysUser> mappedUsers, Set<String> presentUserids) {
        for (SysUser user : mappedUsers) {
            String wecomUserid = user.getWecomUserid();
            if (!StringUtils.hasText(wecomUserid)) {
                continue;
            }
            if (presentUserids.contains(wecomUserid.trim())
                    || user.getStatus() == UserStatus.DISABLED) {
                continue;
            }
            try {
                SysUser update = new SysUser();
                update.setId(user.getId());
                update.setStatus(UserStatus.DISABLED);
                userMapper.updateById(update);
                log.info(
                        "企业微信成员已不在本批组织内，本地账号已禁用：userId={}，wecomUserid={}",
                        user.getId(),
                        wecomUserid);
            } catch (Exception e) {
                log.warn(
                        "禁用离职成员本地账号失败：userId={}，wecomUserid={}，原因={}",
                        user.getId(),
                        wecomUserid,
                        e.getMessage());
            }
        }
    }

    private Long resolveDeptId(WeComUser user, Map<String, Long> deptMapping) {
        if (user.departmentIds() == null) {
            return null;
        }
        for (Long wecomDeptId : user.departmentIds()) {
            if (wecomDeptId == null) {
                continue;
            }
            Long sysDeptId = deptMapping.get(String.valueOf(wecomDeptId));
            if (sysDeptId != null) {
                return sysDeptId;
            }
        }
        return null;
    }

    private UserStatus mapStatus(Integer wecomStatus) {
        return wecomStatus != null && wecomStatus == 1 ? UserStatus.ACTIVE : UserStatus.DISABLED;
    }

    private List<WeComUser> dedupeMembers(Map<Long, List<WeComUser>> usersByDept) {
        Map<String, WeComUser> unique = new LinkedHashMap<>();
        if (usersByDept != null) {
            for (List<WeComUser> users : usersByDept.values()) {
                if (users == null) {
                    continue;
                }
                for (WeComUser user : users) {
                    if (user == null || !StringUtils.hasText(user.userid())) {
                        continue;
                    }
                    unique.putIfAbsent(user.userid().trim(), user);
                }
            }
        }
        return new ArrayList<>(unique.values());
    }

    private Map<String, SysDept> indexDepts(List<SysDept> depts) {
        Map<String, SysDept> index = new LinkedHashMap<>();
        if (depts == null) {
            return index;
        }
        for (SysDept dept : depts) {
            if (dept != null && StringUtils.hasText(dept.getWecomDeptId())) {
                index.put(dept.getWecomDeptId().trim(), dept);
            }
        }
        return index;
    }

    private Map<String, SysUser> indexUsers(List<SysUser> users) {
        Map<String, SysUser> index = new LinkedHashMap<>();
        if (users == null) {
            return index;
        }
        for (SysUser user : users) {
            if (user != null && StringUtils.hasText(user.getWecomUserid())) {
                index.put(user.getWecomUserid().trim(), user);
            }
        }
        return index;
    }

    private Map<String, List<SysUser>> indexPhones(List<SysUser> users) {
        Map<String, List<SysUser>> index = new HashMap<>();
        if (users == null) {
            return index;
        }
        for (SysUser user : users) {
            String phone = normalizePhone(user.getPhone());
            if (phone != null) {
                index.computeIfAbsent(phone, key -> new ArrayList<>()).add(user);
            }
        }
        return index;
    }

    private String normalizePhone(String phone) {
        if (!StringUtils.hasText(phone)) {
            return null;
        }
        return phone.trim();
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return "***";
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    /**
     * 同步结果统计。
     *
     * @param total 总对象数（部门 + 去重后成员）
     * @param success 成功数
     * @param failed 失败数
     */
    public record SyncResult(int total, int success, int failed) {}
}
