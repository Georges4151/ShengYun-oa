package com.ceams.module.hr.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ceams.common.exception.BusinessException;
import com.ceams.common.exception.ErrorCode;
import com.ceams.common.response.PageResult;
import com.ceams.common.security.SecurityUtils;
import com.ceams.module.bpm.dto.InstanceStartRequest;
import com.ceams.module.bpm.enums.BpmBizType;
import com.ceams.module.bpm.service.BpmInstanceService;
import com.ceams.module.hr.converter.HrEmployeeChangeConverter;
import com.ceams.module.hr.converter.HrEmployeeConverter;
import com.ceams.module.hr.dto.EmployeeChangeRequest;
import com.ceams.module.hr.dto.EmployeeCreateRequest;
import com.ceams.module.hr.dto.EmployeeUpdateRequest;
import com.ceams.module.hr.entity.HrEmployee;
import com.ceams.module.hr.entity.HrEmployeeChange;
import com.ceams.module.hr.entity.HrJobLevel;
import com.ceams.module.hr.entity.HrPosition;
import com.ceams.module.hr.enums.ChangeType;
import com.ceams.module.hr.enums.EmployeeStatus;
import com.ceams.module.hr.enums.HrApprovalStatus;
import com.ceams.module.hr.mapper.HrEmployeeChangeMapper;
import com.ceams.module.hr.mapper.HrEmployeeMapper;
import com.ceams.module.hr.mapper.HrJobLevelMapper;
import com.ceams.module.hr.mapper.HrPositionMapper;
import com.ceams.module.hr.service.HrEmployeeService;
import com.ceams.module.hr.util.HrPageUtil;
import com.ceams.module.hr.vo.EmployeeChangeVO;
import com.ceams.module.hr.vo.EmployeeTreeVO;
import com.ceams.module.hr.vo.EmployeeVO;
import com.ceams.module.hr.vo.HeadcountDeptVO;
import com.ceams.module.hr.vo.OrgTreeNodeVO;
import com.ceams.module.iam.entity.SysDept;
import com.ceams.module.iam.entity.SysUser;
import com.ceams.module.iam.mapper.DeptMapper;
import com.ceams.module.iam.mapper.UserMapper;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 员工档案与入转调离服务实现。
 *
 * <p>入转调离统一走「落库申请单 → 发起 BPM → 回写实例 ID」三步；审批结论由
 * {@code support} 包内回调派生，本服务不直接修改员工状态。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HrEmployeeServiceImpl implements HrEmployeeService {

    private final HrEmployeeMapper employeeMapper;
    private final HrEmployeeChangeMapper changeMapper;
    private final HrJobLevelMapper jobLevelMapper;
    private final HrPositionMapper positionMapper;
    private final HrEmployeeConverter employeeConverter;
    private final HrEmployeeChangeConverter changeConverter;
    private final BpmInstanceService bpmInstanceService;
    private final DeptMapper deptMapper;
    private final UserMapper userMapper;

    /** 未分配部门虚拟节点 ID（前端按字符串识别）。 */
    private static final String UNASSIGNED_ID = "UNASSIGNED";

    /** 未分配部门虚拟节点名称。 */
    private static final String UNASSIGNED_NAME = "未分配部门";

    @Override
    public PageResult<EmployeeVO> page(
            long page, long size, Long deptId, EmployeeStatus status, String keyword) {
        Page<HrEmployee> p = HrPageUtil.resolve(page, size);
        LambdaQueryWrapper<HrEmployee> wrapper = new LambdaQueryWrapper<>();
        if (deptId != null) {
            Set<Long> deptScope = resolveDeptScope(deptId);
            if (deptScope.isEmpty()) {
                // 部门不存在（或已删除）时无任何可归属员工
                return new PageResult<>(List.of(), 0L, p.getCurrent(), p.getSize());
            }
            wrapper.in(HrEmployee::getDeptId, deptScope);
        }
        if (status != null) {
            wrapper.eq(HrEmployee::getStatus, status);
        }
        if (StringUtils.isNotBlank(keyword)) {
            String kw = keyword.trim();
            wrapper.and(
                    w -> w.like(HrEmployee::getName, kw).or().like(HrEmployee::getEmployeeNo, kw));
        }
        wrapper.orderByDesc(HrEmployee::getCreateTime);
        IPage<HrEmployee> result = employeeMapper.selectPage(p, wrapper);
        List<EmployeeVO> records = employeeConverter.toVOList(result.getRecords());
        fillNames(records);
        return new PageResult<>(
                records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    public EmployeeVO detail(Long id) {
        EmployeeVO vo = employeeConverter.toVO(requireEmployee(id));
        fillNames(List.of(vo));
        return vo;
    }

    @Override
    public EmployeeVO currentEmployee() {
        return employeeConverter.toVO(requireCurrentEmployee());
    }

    @Override
    public Long currentEmployeeId() {
        return requireCurrentEmployee().getId();
    }

    @Override
    @Transactional
    public String create(EmployeeCreateRequest request) {
        validateUnique(request.getEmployeeNo(), request.getUserId(), null);
        HrEmployee employee = new HrEmployee();
        employee.setEmployeeNo(request.getEmployeeNo().trim());
        employee.setName(request.getName().trim());
        employee.setUserId(normalizeUserId(request.getUserId()));
        employee.setGender(request.getGender());
        employee.setBirthDate(request.getBirthDate());
        employee.setIdCardEnc(request.getIdCard());
        employee.setPhoneEnc(request.getPhone());
        employee.setEmail(request.getEmail());
        employee.setDeptId(request.getDeptId());
        employee.setPositionId(request.getPositionId());
        Long leaderId = normalizeId(request.getLeaderId());
        if (leaderId != null) {
            validateLeader(null, leaderId);
        }
        employee.setLeaderId(leaderId);
        Long levelId = normalizeId(request.getLevelId());
        if (levelId != null && jobLevelMapper.selectById(levelId) == null) {
            throw BusinessException.of(ErrorCode.BIZ_NOT_FOUND, "职级不存在");
        }
        employee.setLevelId(levelId);
        employee.setEmploymentType(request.getEmploymentType());
        employee.setHireDate(request.getHireDate());
        employee.setProbationEndDate(request.getProbationEndDate());
        employee.setEmergencyContact(request.getEmergencyContact());
        employee.setEmergencyPhoneEnc(request.getEmergencyPhone());
        employee.setIdCardFileId(request.getIdCardFileId());
        employee.setRemark(request.getRemark());
        employee.setStatus(
                request.getStatus() == null ? EmployeeStatus.PROBATION : request.getStatus());
        employeeMapper.insert(employee);
        log.info("新建员工档案 {}（工号 {}）", employee.getId(), employee.getEmployeeNo());
        return String.valueOf(employee.getId());
    }

    @Override
    @Transactional
    public void update(Long id, EmployeeUpdateRequest request) {
        HrEmployee employee = requireEmployee(id);
        Long userId = request.getUserId() == null ? employee.getUserId() : normalizeUserId(request.getUserId());
        String employeeNo =
                StringUtils.isBlank(request.getEmployeeNo())
                        ? employee.getEmployeeNo()
                        : request.getEmployeeNo().trim();
        boolean userIdChanged = !Objects.equals(userId, employee.getUserId());
        boolean noChanged = !employeeNo.equals(employee.getEmployeeNo());
        boolean clearLeader = false;
        if (userIdChanged || noChanged) {
            validateUnique(noChanged ? employeeNo : null, userIdChanged ? userId : null, id);
        }
        employee.setEmployeeNo(employeeNo);
        employee.setUserId(userId);
        if (request.getName() != null) {
            employee.setName(request.getName().trim());
        }
        if (request.getGender() != null) {
            employee.setGender(request.getGender());
        }
        if (request.getBirthDate() != null) {
            employee.setBirthDate(request.getBirthDate());
        }
        if (StringUtils.isNotBlank(request.getIdCard())) {
            employee.setIdCardEnc(request.getIdCard());
        }
        if (StringUtils.isNotBlank(request.getPhone())) {
            employee.setPhoneEnc(request.getPhone());
        }
        if (request.getEmail() != null) {
            employee.setEmail(request.getEmail());
        }
        if (request.getDeptId() != null) {
            employee.setDeptId(request.getDeptId());
        }
        if (request.getPositionId() != null) {
            employee.setPositionId(request.getPositionId());
        }
        if (request.getLeaderId() != null) {
            Long leaderId = normalizeId(request.getLeaderId());
            if (leaderId != null) {
                validateLeader(id, leaderId);
                employee.setLeaderId(leaderId);
            } else {
                // updateById 默认忽略 null，清空上级需显式 set null
                clearLeader = true;
            }
        }
        if (request.getEmploymentType() != null) {
            employee.setEmploymentType(request.getEmploymentType());
        }
        if (request.getHireDate() != null) {
            employee.setHireDate(request.getHireDate());
        }
        if (request.getProbationEndDate() != null) {
            employee.setProbationEndDate(request.getProbationEndDate());
        }
        if (request.getRegularDate() != null) {
            employee.setRegularDate(request.getRegularDate());
        }
        if (request.getLeaveDate() != null) {
            employee.setLeaveDate(request.getLeaveDate());
        }
        if (request.getStatus() != null) {
            employee.setStatus(request.getStatus());
        }
        if (request.getEmergencyContact() != null) {
            employee.setEmergencyContact(request.getEmergencyContact());
        }
        if (StringUtils.isNotBlank(request.getEmergencyPhone())) {
            employee.setEmergencyPhoneEnc(request.getEmergencyPhone());
        }
        if (request.getIdCardFileId() != null) {
            employee.setIdCardFileId(request.getIdCardFileId());
        }
        if (request.getRemark() != null) {
            employee.setRemark(request.getRemark());
        }
        employeeMapper.updateById(employee);
        if (clearLeader) {
            employeeMapper.update(
                    null,
                    new LambdaUpdateWrapper<HrEmployee>()
                            .eq(HrEmployee::getId, id)
                            .set(HrEmployee::getLeaderId, null));
        }
        log.info("更新员工档案 {}（工号 {}）", employee.getId(), employee.getEmployeeNo());
    }

    @Override
    public List<EmployeeChangeVO> listChanges(Long employeeId) {
        requireEmployee(employeeId);
        List<HrEmployeeChange> changes =
                changeMapper.selectList(
                        new LambdaQueryWrapper<HrEmployeeChange>()
                                .eq(HrEmployeeChange::getEmployeeId, employeeId)
                                .orderByDesc(HrEmployeeChange::getCreateTime));
        List<EmployeeChangeVO> vos = changeConverter.toVOList(changes);
        fillTargetLevelNames(vos);
        fillResignLeaderNames(vos);
        return vos;
    }

    @Override
    public List<EmployeeVO> subordinates(Long employeeId) {
        requireEmployee(employeeId);
        List<EmployeeVO> vos = employeeConverter.toVOList(activeSubordinates(employeeId));
        fillNames(vos);
        return vos;
    }

    @Override
    @Transactional
    public String regular(Long employeeId, EmployeeChangeRequest request) {
        return apply(employeeId, ChangeType.REGULAR, request);
    }

    @Override
    @Transactional
    public String transfer(Long employeeId, EmployeeChangeRequest request) {
        return apply(employeeId, ChangeType.TRANSFER, request);
    }

    @Override
    @Transactional
    public String resign(Long employeeId, EmployeeChangeRequest request) {
        return apply(employeeId, ChangeType.RESIGN, request);
    }

    @Override
    @Transactional
    public String promotion(Long employeeId, EmployeeChangeRequest request) {
        return apply(employeeId, ChangeType.PROMOTION, request);
    }

    @Override
    public List<EmployeeTreeVO> tree(Long deptId, EmployeeStatus status) {
        LambdaQueryWrapper<HrEmployee> wrapper = new LambdaQueryWrapper<>();
        if (deptId != null) {
            wrapper.eq(HrEmployee::getDeptId, deptId);
        }
        if (status != null) {
            wrapper.eq(HrEmployee::getStatus, status);
        }
        wrapper.orderByAsc(HrEmployee::getDeptId).orderByAsc(HrEmployee::getEmployeeNo);
        List<HrEmployee> employees = employeeMapper.selectList(wrapper);
        if (employees.isEmpty()) {
            return List.of();
        }
        return buildTree(employees);
    }

    @Override
    public List<OrgTreeNodeVO> orgTree(
            EmployeeStatus status, String keyword, Long deptId, boolean includeResigned) {
        return buildOrgTree(status, keyword, deptId, includeResigned);
    }

    @Override
    public List<OrgTreeNodeVO> orgTreeRoots(
            EmployeeStatus status, String keyword, Long deptId, boolean includeResigned) {
        List<OrgTreeNodeVO> roots = buildOrgTree(status, keyword, deptId, includeResigned);
        // 懒加载首屏只回根节点，展开能力由 hasChildren 表达
        roots.forEach(this::detachChildren);
        return roots;
    }

    @Override
    public List<OrgTreeNodeVO> orgTreeChildren(
            String nodeType,
            Long nodeId,
            EmployeeStatus status,
            String keyword,
            Long deptId,
            boolean includeResigned) {
        OrgTreeNodeVO.NodeType type = parseNodeType(nodeType);
        if (type == null || (type != OrgTreeNodeVO.NodeType.UNASSIGNED && nodeId == null)) {
            return List.of();
        }
        OrgTreeNodeVO target =
                findNode(buildOrgTree(status, keyword, deptId, includeResigned), type, nodeId);
        if (target == null) {
            return List.of();
        }
        List<OrgTreeNodeVO> children = target.getChildren();
        children.forEach(this::detachChildren);
        return children;
    }

    /**
     * 全量组装组织合并树（部门 + 员工 + 未分配虚拟根），懒加载接口复用同一结果。
     *
     * <p>员工 / 部门 / 职级 / 岗位各一次批量查询，名称回填全部在内存完成，禁止逐行查询。
     */
    private List<OrgTreeNodeVO> buildOrgTree(
            EmployeeStatus status, String keyword, Long deptId, boolean includeResigned) {
        List<SysDept> depts = new ArrayList<>(deptMapper.selectList(new LambdaQueryWrapper<>()));
        depts.sort(deptComparator());
        List<HrEmployee> employees = employeeMapper.selectList(new LambdaQueryWrapper<>());
        if (depts.isEmpty() && employees.isEmpty()) {
            return List.of();
        }

        Map<Long, SysDept> deptById = new HashMap<>();
        for (SysDept dept : depts) {
            if (dept.getId() != null) {
                deptById.put(dept.getId(), dept);
            }
        }
        if (deptId != null && !deptById.containsKey(deptId)) {
            return List.of();
        }

        Map<Long, HrEmployee> employeeById = new HashMap<>();
        // 部门负责人存的是用户 ID，通过员工档案的 userId 批量映射姓名
        Map<Long, String> employeeNameByUserId = new HashMap<>();
        Map<Long, List<HrEmployee>> membersByDept = new HashMap<>();
        List<HrEmployee> unassignedMembers = new ArrayList<>();
        Map<Long, List<HrEmployee>> subordinates = new HashMap<>();
        for (HrEmployee employee : employees) {
            employeeById.put(employee.getId(), employee);
            if (employee.getUserId() != null && employee.getName() != null) {
                employeeNameByUserId.putIfAbsent(employee.getUserId(), employee.getName());
            }
            if (employee.getDeptId() != null && deptById.containsKey(employee.getDeptId())) {
                membersByDept
                        .computeIfAbsent(employee.getDeptId(), key -> new ArrayList<>())
                        .add(employee);
            } else {
                unassignedMembers.add(employee);
            }
            if (employee.getLeaderId() != null) {
                subordinates
                        .computeIfAbsent(employee.getLeaderId(), key -> new ArrayList<>())
                        .add(employee);
            }
        }

        Map<Long, List<SysDept>> childDepts = groupChildDepts(depts);
        Map<Long, HrJobLevel> levelById = loadJobLevels(employees);
        Map<Long, String> positionNameById = loadPositionNames(employees);
        Map<Long, Integer> levelRanks = new HashMap<>();
        for (HrJobLevel level : levelById.values()) {
            levelRanks.put(level.getId(), level.getRankNo());
        }

        OrgTreeScope scope =
                resolveOrgTreeScope(
                        depts,
                        employees,
                        deptById,
                        childDepts,
                        employeeById,
                        status,
                        keyword,
                        deptId,
                        includeResigned);
        OrgTreeContext context =
                new OrgTreeContext(
                        deptById,
                        employeeById,
                        levelById,
                        levelRanks,
                        positionNameById,
                        membersByDept,
                        unassignedMembers,
                        subordinates,
                        scope.visibleEmployeeIds());

        // headcount：在职口径（排除 TERMINATED）；totalHeadcount：含离职的总人数
        Map<Long, Integer> headcounts = computeHeadcounts(depts, childDepts, employees, true);
        Map<Long, Integer> totalHeadcounts = computeHeadcounts(depts, childDepts, employees, false);

        // 部门负责人存的是用户 ID：优先档案姓名，回退 sys_user.name；去重后一次批量查询
        Map<Long, String> deptLeaderNames = loadDeptLeaderNames(depts, employeeNameByUserId);

        Map<Long, OrgTreeNodeVO> deptNodes = new LinkedHashMap<>();
        for (SysDept dept : depts) {
            if (scope.visibleDeptIds().contains(dept.getId())) {
                deptNodes.put(
                        dept.getId(),
                        toDeptNode(dept, deptLeaderNames, headcounts, totalHeadcounts));
            }
        }
        List<OrgTreeNodeVO> roots = new ArrayList<>();
        for (SysDept dept : depts) {
            OrgTreeNodeVO node = deptNodes.get(dept.getId());
            if (node == null) {
                continue;
            }
            OrgTreeNodeVO parent =
                    dept.getParentId() == null ? null : deptNodes.get(dept.getParentId());
            if (parent != null) {
                parent.getChildren().add(node);
            } else {
                roots.add(node);
            }
        }
        // 子部门优先，员工根节点追加在其后
        for (SysDept dept : depts) {
            OrgTreeNodeVO node = deptNodes.get(dept.getId());
            if (node != null) {
                node.getChildren().addAll(buildEmployeeRoots(dept.getId(), context));
            }
        }
        List<OrgTreeNodeVO> unassignedRoots = buildEmployeeRoots(null, context);
        if (!unassignedRoots.isEmpty()) {
            roots.add(toUnassignedNode(unassignedRoots));
        }
        roots.forEach(this::fillHasChildren);
        return roots;
    }

    /** 自底向上回填 hasChildren：子部门 / 下属员工 / 未分配员工均视为可展开。 */
    private void fillHasChildren(OrgTreeNodeVO node) {
        for (OrgTreeNodeVO child : node.getChildren()) {
            fillHasChildren(child);
        }
        node.setHasChildren(!node.getChildren().isEmpty());
    }

    /** 裁剪 children：保留 hasChildren 等字段，children 置为空数组（懒加载契约）。 */
    private void detachChildren(OrgTreeNodeVO node) {
        node.setChildren(new ArrayList<>());
    }

    /** 在已组装的内存树中按类型 + ID 定位节点（广度优先，避免深树递归过深）。 */
    private OrgTreeNodeVO findNode(
            List<OrgTreeNodeVO> roots, OrgTreeNodeVO.NodeType type, Long nodeId) {
        Deque<OrgTreeNodeVO> pending = new ArrayDeque<>(roots);
        while (!pending.isEmpty()) {
            OrgTreeNodeVO node = pending.poll();
            if (node.getNodeType() == type
                    && (type == OrgTreeNodeVO.NodeType.UNASSIGNED
                            || nodeId.equals(node.getId()))) {
                return node;
            }
            pending.addAll(node.getChildren());
        }
        return null;
    }

    /** 节点类型解析：忽略大小写与首尾空白，非法值返回 null（调用方转为空列表）。 */
    private OrgTreeNodeVO.NodeType parseNodeType(String nodeType) {
        if (StringUtils.isBlank(nodeType)) {
            return null;
        }
        try {
            return OrgTreeNodeVO.NodeType.valueOf(nodeType.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @Override
    public List<HeadcountDeptVO> headcountByDept(Long deptId) {
        LambdaQueryWrapper<HrEmployee> wrapper = new LambdaQueryWrapper<>();
        if (deptId != null) {
            wrapper.eq(HrEmployee::getDeptId, deptId);
        }
        wrapper.orderByAsc(HrEmployee::getDeptId);
        List<HrEmployee> employees = employeeMapper.selectList(wrapper);

        // 按部门在 Java 内分组累加，避免 SQL group by 与枚举映射耦合
        Map<Long, HeadcountDeptVO> grouped = new LinkedHashMap<>();
        for (HrEmployee employee : employees) {
            Long key = employee.getDeptId();
            if (key == null) {
                // 未归属部门的员工无法归属到任一部门行
                continue;
            }
            HeadcountDeptVO vo =
                    grouped.computeIfAbsent(
                            key,
                            id -> {
                                HeadcountDeptVO created = new HeadcountDeptVO();
                                created.setDeptId(id);
                                created.setDeptName("");
                                return created;
                            });
            vo.setTotalCount(vo.getTotalCount() + 1);
            EmployeeStatus status = employee.getStatus();
            if (status == EmployeeStatus.PROBATION) {
                vo.setProbationCount(vo.getProbationCount() + 1);
            } else if (status == EmployeeStatus.ACTIVE) {
                vo.setActiveCount(vo.getActiveCount() + 1);
            } else if (status == EmployeeStatus.TERMINATED) {
                vo.setTerminatedCount(vo.getTerminatedCount() + 1);
            }
            // LEAVE 计入 total，不单列
        }
        if (grouped.isEmpty()) {
            return List.of();
        }

        Map<Long, String> deptNames = loadDeptNames(grouped.keySet());
        List<HeadcountDeptVO> result = new ArrayList<>(grouped.values());
        for (HeadcountDeptVO vo : result) {
            vo.setDeptName(deptNames.getOrDefault(vo.getDeptId(), ""));
        }
        result.sort(Comparator.comparing(HeadcountDeptVO::getDeptId));
        return result;
    }

    /** 批量查部门名称；部门不存在或名称为空由调用方回落空串。 */
    private Map<Long, String> loadDeptNames(Set<Long> deptIds) {
        if (deptIds == null || deptIds.isEmpty()) {
            return Map.of();
        }
        return deptMapper.selectBatchIds(deptIds).stream()
                .filter(dept -> dept.getId() != null && StringUtils.isNotBlank(dept.getName()))
                .collect(Collectors.toMap(SysDept::getId, SysDept::getName, (a, b) -> a));
    }

    /**
     * 批量解析部门负责人姓名：优先取绑定该用户 ID 的员工档案姓名，缺失时回退 {@code sys_user.name}。
     *
     * <p>负责人用户 ID 去重后一次 IN 查询，禁止逐部门查询。
     */
    private Map<Long, String> loadDeptLeaderNames(
            List<SysDept> depts, Map<Long, String> employeeNameByUserId) {
        Set<Long> leaderUserIds =
                depts.stream()
                        .map(SysDept::getLeaderId)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toCollection(LinkedHashSet::new));
        if (leaderUserIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, String> userNameById = new HashMap<>();
        for (SysUser user : userMapper.selectBatchIds(leaderUserIds)) {
            if (user.getId() != null && StringUtils.isNotBlank(user.getName())) {
                userNameById.putIfAbsent(user.getId(), user.getName());
            }
        }
        Map<Long, String> leaderNames = new HashMap<>();
        for (Long userId : leaderUserIds) {
            String name = employeeNameByUserId.get(userId);
            if (StringUtils.isBlank(name)) {
                name = userNameById.get(userId);
            }
            if (name != null) {
                leaderNames.put(userId, name);
            }
        }
        return leaderNames;
    }

    /**
     * 统一的入转调离发起逻辑：校验员工状态与重复申请 → 落库 → 发起 BPM → 回写实例 ID。
     */
    private String apply(Long employeeId, ChangeType type, EmployeeChangeRequest request) {
        HrEmployee employee = requireEmployee(employeeId);
        if (employee.getStatus() == EmployeeStatus.TERMINATED) {
            throw BusinessException.of(
                    ErrorCode.BIZ_STATE_CONFLICT,
                    "员工已离职，不可发起" + label(type) + "申请");
        }
        if (type == ChangeType.TRANSFER
                && request.getTargetDeptId() == null
                && request.getTargetPositionId() == null) {
            throw BusinessException.of(
                    ErrorCode.VALID_ILLEGAL_ARGUMENT, "调岗申请需指定目标部门或目标岗位");
        }
        if (type == ChangeType.PROMOTION) {
            if (request.getTargetLevelId() == null) {
                throw BusinessException.of(
                        ErrorCode.VALID_ILLEGAL_ARGUMENT, "晋升申请需指定目标职级");
            }
            if (jobLevelMapper.selectById(request.getTargetLevelId()) == null) {
                throw BusinessException.of(ErrorCode.BIZ_NOT_FOUND, "目标职级不存在");
            }
        }
        if (type == ChangeType.RESIGN) {
            List<HrEmployee> subordinates = activeSubordinates(employeeId);
            if (!subordinates.isEmpty()) {
                Long resignLeaderId = request.getResignLeaderId();
                if (resignLeaderId == null) {
                    throw BusinessException.of(
                            ErrorCode.VALID_ILLEGAL_ARGUMENT,
                            "该员工名下还有 " + subordinates.size() + " 名在职下属，离职申请必须指定下属转交上级");
                }
                validateResignLeader(employeeId, resignLeaderId, subordinates);
            }
        }
        Long pending =
                changeMapper.selectCount(
                        new LambdaQueryWrapper<HrEmployeeChange>()
                                .eq(HrEmployeeChange::getEmployeeId, employeeId)
                                .eq(HrEmployeeChange::getChangeType, type)
                                .eq(HrEmployeeChange::getApprovalStatus, HrApprovalStatus.PENDING));
        if (pending != null && pending > 0) {
            throw BusinessException.of(
                    ErrorCode.BIZ_STATE_CONFLICT,
                    "该员工存在审批中的" + label(type) + "申请，请勿重复提交");
        }

        HrEmployeeChange change = new HrEmployeeChange();
        change.setEmployeeId(employeeId);
        change.setChangeType(type);
        change.setTargetDeptId(request.getTargetDeptId());
        change.setTargetPositionId(request.getTargetPositionId());
        change.setTargetLevelId(request.getTargetLevelId());
        change.setResignLeaderId(
                type == ChangeType.RESIGN ? request.getResignLeaderId() : null);
        change.setEffectiveDate(request.getEffectiveDate());
        change.setReason(request.getReason());
        change.setRemark(request.getRemark());
        change.setApprovalStatus(HrApprovalStatus.PENDING);
        changeMapper.insert(change);

        BpmBizType bizType = type.bpmBizType();
        InstanceStartRequest start = new InstanceStartRequest();
        start.setTemplateCode(bizType.name());
        start.setBizType(bizType.name());
        start.setBizId(change.getId());
        start.setVariables(buildChangeVariables(employeeId, type, request));
        String instanceId = bpmInstanceService.start(start);

        change.setBpmInstanceId(parseInstanceId(instanceId));
        changeMapper.updateById(change);
        log.info(
                "员工 {} 发起{}申请，申请单 {}，BPM 实例 {}",
                employeeId,
                label(type),
                change.getId(),
                instanceId);
        return String.valueOf(change.getId());
    }

    /** 组装流程变量（BPM 条件与审批人解析均从此取值）。 */
    private Map<String, String> buildChangeVariables(
            Long employeeId, ChangeType type, EmployeeChangeRequest request) {
        Map<String, String> variables = new LinkedHashMap<>();
        variables.put("employeeId", String.valueOf(employeeId));
        variables.put("changeType", type.name());
        if (request.getTargetDeptId() != null) {
            variables.put("targetDeptId", String.valueOf(request.getTargetDeptId()));
        }
        if (request.getTargetPositionId() != null) {
            variables.put("targetPositionId", String.valueOf(request.getTargetPositionId()));
        }
        if (request.getTargetLevelId() != null) {
            variables.put("targetLevelId", String.valueOf(request.getTargetLevelId()));
        }
        if (request.getResignLeaderId() != null) {
            variables.put("resignLeaderId", String.valueOf(request.getResignLeaderId()));
        }
        if (request.getEffectiveDate() != null) {
            variables.put("effectiveDate", request.getEffectiveDate().toString());
        }
        return variables;
    }

    /** 员工工号 / 账号绑定唯一性校验（excludeId 用于更新场景排除自身）。 */
    private void validateUnique(String employeeNo, Long userId, Long excludeId) {
        if (StringUtils.isNotBlank(employeeNo)) {
            Long count =
                    employeeMapper.selectCount(
                            new LambdaQueryWrapper<HrEmployee>()
                                    .eq(HrEmployee::getEmployeeNo, employeeNo.trim())
                                    .ne(excludeId != null, HrEmployee::getId, excludeId));
            if (count != null && count > 0) {
                throw BusinessException.of(
                        ErrorCode.BIZ_DUPLICATE_CODE, "员工工号已存在：" + employeeNo.trim());
            }
        }
        if (userId != null && userId > 0) {
            Long count =
                    employeeMapper.selectCount(
                            new LambdaQueryWrapper<HrEmployee>()
                                    .eq(HrEmployee::getUserId, userId)
                                    .ne(excludeId != null, HrEmployee::getId, excludeId));
            if (count != null && count > 0) {
                throw BusinessException.of(
                        ErrorCode.BIZ_DUPLICATE_CODE, "该账号已绑定其他员工档案：" + userId);
            }
        }
    }

    /** userId 传 0 视为解绑。 */
    private Long normalizeUserId(Long userId) {
        return userId == null || userId <= 0 ? null : userId;
    }

    /** 直属上级 / 职级等关联 ID：null 或 0 视为空。 */
    private Long normalizeId(Long id) {
        return id == null || id <= 0 ? null : id;
    }

    /**
     * 校验直属上级：存在、在职、已绑定账号、非本人，且不形成循环引用。
     *
     * @param employeeId 待设置上级的员工 ID（新建时为 null）
     * @param leaderId 直属上级员工 ID
     */
    private void validateLeader(Long employeeId, Long leaderId) {
        HrEmployee leader = employeeMapper.selectById(leaderId);
        if (leader == null) {
            throw BusinessException.of(ErrorCode.BIZ_NOT_FOUND, "直属上级员工不存在");
        }
        if (leader.getStatus() == EmployeeStatus.TERMINATED) {
            throw BusinessException.of(ErrorCode.BIZ_STATE_CONFLICT, "直属上级已离职，不可指定");
        }
        if (leader.getUserId() == null) {
            throw BusinessException.of(ErrorCode.BIZ_STATE_CONFLICT, "直属上级必须绑定系统账号");
        }
        if (employeeId != null && leaderId.equals(employeeId)) {
            throw BusinessException.of(ErrorCode.BIZ_STATE_CONFLICT, "直属上级不能是本人");
        }
        Set<Long> visited = new HashSet<>();
        Long current = leaderId;
        int depth = 0;
        while (current != null) {
            if (employeeId != null && employeeId.equals(current)) {
                throw BusinessException.of(ErrorCode.BIZ_STATE_CONFLICT, "直属上级存在循环引用");
            }
            if (!visited.add(current)) {
                throw BusinessException.of(ErrorCode.BIZ_STATE_CONFLICT, "直属上级存在循环引用");
            }
            if (++depth > 64) {
                throw BusinessException.of(
                        ErrorCode.BIZ_STATE_CONFLICT, "直属上级层级过深或存在循环引用");
            }
            HrEmployee node = employeeMapper.selectById(current);
            current = node == null ? null : node.getLeaderId();
        }
    }

    /** 批量回填直属上级与职级名称/编码（各一次批量查询，禁止逐行查询）。 */
    private void fillNames(List<EmployeeVO> vos) {
        if (vos == null || vos.isEmpty()) {
            return;
        }
        Set<Long> leaderIds = new HashSet<>();
        Set<Long> levelIds = new HashSet<>();
        for (EmployeeVO vo : vos) {
            if (vo.getLeaderId() != null) {
                leaderIds.add(vo.getLeaderId());
            }
            if (vo.getLevelId() != null) {
                levelIds.add(vo.getLevelId());
            }
        }
        Map<Long, HrEmployee> leaders = new HashMap<>();
        if (!leaderIds.isEmpty()) {
            for (HrEmployee leader : employeeMapper.selectBatchIds(leaderIds)) {
                leaders.put(leader.getId(), leader);
            }
        }
        Map<Long, HrJobLevel> levels = new HashMap<>();
        if (!levelIds.isEmpty()) {
            for (HrJobLevel level : jobLevelMapper.selectBatchIds(levelIds)) {
                levels.put(level.getId(), level);
            }
        }
        for (EmployeeVO vo : vos) {
            HrEmployee leader = vo.getLeaderId() == null ? null : leaders.get(vo.getLeaderId());
            if (leader != null) {
                vo.setLeaderName(leader.getName());
                vo.setLeaderCode(leader.getEmployeeNo());
            }
            HrJobLevel level = vo.getLevelId() == null ? null : levels.get(vo.getLevelId());
            if (level != null) {
                vo.setLevelName(level.getName());
                vo.setLevelCode(level.getCode());
            }
        }
    }

    /** 批量回填离职下属转交上级姓名（一次批量查询）。 */
    private void fillResignLeaderNames(List<EmployeeChangeVO> vos) {
        if (vos == null || vos.isEmpty()) {
            return;
        }
        Set<Long> leaderIds = new HashSet<>();
        for (EmployeeChangeVO vo : vos) {
            if (vo.getResignLeaderId() != null) {
                leaderIds.add(vo.getResignLeaderId());
            }
        }
        if (leaderIds.isEmpty()) {
            return;
        }
        Map<Long, String> names = new HashMap<>();
        for (HrEmployee leader : employeeMapper.selectBatchIds(leaderIds)) {
            names.put(leader.getId(), leader.getName());
        }
        for (EmployeeChangeVO vo : vos) {
            if (vo.getResignLeaderId() != null) {
                vo.setResignLeaderName(names.get(vo.getResignLeaderId()));
            }
        }
    }

    /** 在职直接下属（排除已离职，按工号排序）。 */
    private List<HrEmployee> activeSubordinates(Long employeeId) {
        return employeeMapper.selectList(
                new LambdaQueryWrapper<HrEmployee>()
                        .eq(HrEmployee::getLeaderId, employeeId)
                        .ne(HrEmployee::getStatus, EmployeeStatus.TERMINATED)
                        .orderByAsc(HrEmployee::getEmployeeNo));
    }

    /** 离职下属转交上级校验：存在、在职、绑定账号、非本人、非待转交下属，且不形成环。 */
    private void validateResignLeader(
            Long employeeId, Long resignLeaderId, List<HrEmployee> subordinates) {
        if (resignLeaderId.equals(employeeId)) {
            throw BusinessException.of(
                    ErrorCode.BIZ_STATE_CONFLICT, "下属转交上级不能是离职员工本人");
        }
        HrEmployee target = employeeMapper.selectById(resignLeaderId);
        if (target == null) {
            throw BusinessException.of(ErrorCode.BIZ_NOT_FOUND, "下属转交上级员工不存在");
        }
        if (target.getStatus() == EmployeeStatus.TERMINATED) {
            throw BusinessException.of(
                    ErrorCode.BIZ_STATE_CONFLICT, "下属转交上级已离职，不可指定");
        }
        if (target.getUserId() == null) {
            throw BusinessException.of(
                    ErrorCode.BIZ_STATE_CONFLICT, "下属转交上级必须绑定系统账号");
        }
        for (HrEmployee subordinate : subordinates) {
            if (subordinate.getId().equals(resignLeaderId)) {
                throw BusinessException.of(
                        ErrorCode.BIZ_STATE_CONFLICT, "下属转交上级不能是待转交的下属");
            }
            validateLeader(subordinate.getId(), resignLeaderId);
        }
    }

    /** 批量回填晋升申请的目标职级名称（一次批量查询）。 */
    private void fillTargetLevelNames(List<EmployeeChangeVO> vos) {
        if (vos == null || vos.isEmpty()) {
            return;
        }
        Set<Long> levelIds = new HashSet<>();
        for (EmployeeChangeVO vo : vos) {
            if (vo.getTargetLevelId() != null) {
                levelIds.add(vo.getTargetLevelId());
            }
        }
        if (levelIds.isEmpty()) {
            return;
        }
        Map<Long, String> levelNames = new HashMap<>();
        for (HrJobLevel level : jobLevelMapper.selectBatchIds(levelIds)) {
            levelNames.put(level.getId(), level.getName());
        }
        for (EmployeeChangeVO vo : vos) {
            if (vo.getTargetLevelId() != null) {
                vo.setTargetLevelName(levelNames.get(vo.getTargetLevelId()));
            }
        }
    }

    /** 按直属上级关系组装组织树；根 = 无上级或上级不在结果集中。 */
    private List<EmployeeTreeVO> buildTree(List<HrEmployee> employees) {
        Map<Long, EmployeeTreeVO> nodes = new LinkedHashMap<>();
        Map<Long, Long> leaders = new HashMap<>();
        List<Long> order = new ArrayList<>(employees.size());
        for (HrEmployee employee : employees) {
            EmployeeTreeVO node = new EmployeeTreeVO();
            node.setId(employee.getId());
            node.setEmployeeNo(employee.getEmployeeNo());
            node.setName(employee.getName());
            node.setDeptId(employee.getDeptId());
            node.setPositionId(employee.getPositionId());
            node.setLevelId(employee.getLevelId());
            node.setLeaderId(employee.getLeaderId());
            node.setStatus(employee.getStatus());
            nodes.put(employee.getId(), node);
            leaders.put(employee.getId(), employee.getLeaderId());
            order.add(employee.getId());
        }
        Map<Long, Long> cycleAnchors = detectCycleAnchors(order, leaders, nodes.keySet());
        List<EmployeeTreeVO> roots = new ArrayList<>();
        for (Long id : order) {
            EmployeeTreeVO node = nodes.get(id);
            Long leaderId = leaders.get(id);
            if (leaderId == null || !nodes.containsKey(leaderId)) {
                roots.add(node);
                continue;
            }
            Long anchor = cycleAnchors.get(id);
            if (anchor != null && anchor.equals(id)) {
                // 环上第一个节点作为根，跳过回边
                roots.add(node);
                continue;
            }
            nodes.get(leaderId).getChildren().add(node);
        }
        return roots;
    }

    /**
     * 识别上级链中的环：环上节点映射到该环在查询顺序中最靠前的节点（根），
     * 其余环节点仍挂到各自上级；不在环上的节点映射为 null。
     */
    private Map<Long, Long> detectCycleAnchors(
            List<Long> order, Map<Long, Long> leaders, Set<Long> ids) {
        Map<Long, Long> anchorOf = new HashMap<>();
        Map<Long, Integer> index = new HashMap<>();
        for (int i = 0; i < order.size(); i++) {
            index.put(order.get(i), i);
        }
        for (Long start : order) {
            if (anchorOf.containsKey(start)) {
                continue;
            }
            List<Long> path = new ArrayList<>();
            Map<Long, Integer> position = new HashMap<>();
            Long current = start;
            while (current != null && ids.contains(current) && !anchorOf.containsKey(current)) {
                Integer pos = position.get(current);
                if (pos != null) {
                    Long anchor = path.get(pos);
                    for (int i = pos; i < path.size(); i++) {
                        Long member = path.get(i);
                        if (index.get(member) < index.get(anchor)) {
                            anchor = member;
                        }
                    }
                    for (int i = pos; i < path.size(); i++) {
                        anchorOf.put(path.get(i), anchor);
                    }
                    break;
                }
                position.put(current, path.size());
                path.add(current);
                current = leaders.get(current);
            }
            for (Long node : path) {
                anchorOf.putIfAbsent(node, null);
            }
        }
        return anchorOf;
    }

    // ------------------------------------------------------------------
    // 组织合并树组装
    // ------------------------------------------------------------------

    /** 同级部门排序：sort_order 升序（空值最后），再按 ID 升序。 */
    private Comparator<SysDept> deptComparator() {
        return Comparator.comparing(
                        SysDept::getSortOrder, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(SysDept::getId, Comparator.nullsLast(Comparator.naturalOrder()));
    }

    /** 部门自身 + 全部子孙部门 ID；部门不存在时返回空集合。 */
    private Set<Long> resolveDeptScope(Long deptId) {
        List<SysDept> depts = deptMapper.selectList(new LambdaQueryWrapper<>());
        Map<Long, SysDept> deptById = new HashMap<>();
        for (SysDept dept : depts) {
            if (dept.getId() != null) {
                deptById.put(dept.getId(), dept);
            }
        }
        if (!deptById.containsKey(deptId)) {
            return Set.of();
        }
        return collectDeptSubtree(deptId, groupChildDepts(depts));
    }

    /** 批量加载职级（一次 IN 查询）。 */
    private Map<Long, HrJobLevel> loadJobLevels(List<HrEmployee> employees) {
        Set<Long> levelIds = new HashSet<>();
        for (HrEmployee employee : employees) {
            if (employee.getLevelId() != null) {
                levelIds.add(employee.getLevelId());
            }
        }
        if (levelIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, HrJobLevel> levels = new HashMap<>();
        for (HrJobLevel level : jobLevelMapper.selectBatchIds(levelIds)) {
            levels.put(level.getId(), level);
        }
        return levels;
    }

    /** 批量加载岗位名称（一次 IN 查询，禁止逐行查询）。 */
    private Map<Long, String> loadPositionNames(List<HrEmployee> employees) {
        Set<Long> positionIds = new HashSet<>();
        for (HrEmployee employee : employees) {
            if (employee.getPositionId() != null) {
                positionIds.add(employee.getPositionId());
            }
        }
        if (positionIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, String> names = new HashMap<>();
        for (HrPosition position : positionMapper.selectBatchIds(positionIds)) {
            if (position.getId() != null && position.getName() != null) {
                names.put(position.getId(), position.getName());
            }
        }
        return names;
    }

    /** 按 parent_id 分组子部门；入参已按 sort_order, id 排序，分组后同级顺序保持一致。 */
    private Map<Long, List<SysDept>> groupChildDepts(List<SysDept> depts) {
        Map<Long, List<SysDept>> childDepts = new HashMap<>();
        for (SysDept dept : depts) {
            if (dept.getParentId() != null) {
                childDepts.computeIfAbsent(dept.getParentId(), key -> new ArrayList<>()).add(dept);
            }
        }
        return childDepts;
    }

    /** 部门子树 ID（含自身）；带访问标记，部门层级成环时不会死循环。 */
    private Set<Long> collectDeptSubtree(Long rootId, Map<Long, List<SysDept>> childDepts) {
        Set<Long> ids = new LinkedHashSet<>();
        Deque<Long> pending = new ArrayDeque<>();
        pending.add(rootId);
        while (!pending.isEmpty()) {
            Long current = pending.poll();
            if (!ids.add(current)) {
                continue;
            }
            for (SysDept child : childDepts.getOrDefault(current, List.of())) {
                pending.add(child.getId());
            }
        }
        return ids;
    }

    /** 部门祖先链（含自身，根在前）；deptId 为空或已无上级时自然终止。 */
    private Set<Long> collectDeptChain(Long deptId, Map<Long, SysDept> deptById) {
        Set<Long> chain = new LinkedHashSet<>();
        Long cursor = deptId;
        while (cursor != null && chain.add(cursor)) {
            SysDept dept = deptById.get(cursor);
            cursor = dept == null ? null : dept.getParentId();
        }
        return chain;
    }

    /**
     * 各部门人数（含所有子部门）；{@code excludeTerminated} 为 true 时为在职口径，
     * 为 false 时为含离职的总人数。
     */
    private Map<Long, Integer> computeHeadcounts(
            List<SysDept> depts,
            Map<Long, List<SysDept>> childDepts,
            List<HrEmployee> employees,
            boolean excludeTerminated) {
        Map<Long, Integer> own = new HashMap<>();
        for (HrEmployee employee : employees) {
            if (employee.getDeptId() == null) {
                continue;
            }
            if (excludeTerminated && employee.getStatus() == EmployeeStatus.TERMINATED) {
                continue;
            }
            own.merge(employee.getDeptId(), 1, Integer::sum);
        }
        Map<Long, Integer> totals = new HashMap<>();
        for (SysDept dept : depts) {
            accumulateHeadcount(dept.getId(), childDepts, own, totals, new HashSet<>());
        }
        return totals;
    }

    private int accumulateHeadcount(
            Long deptId,
            Map<Long, List<SysDept>> childDepts,
            Map<Long, Integer> own,
            Map<Long, Integer> totals,
            Set<Long> visiting) {
        Integer cached = totals.get(deptId);
        if (cached != null) {
            return cached;
        }
        if (!visiting.add(deptId)) {
            // 部门层级成环：只计本部门，避免无限递归
            return own.getOrDefault(deptId, 0);
        }
        int total = own.getOrDefault(deptId, 0);
        for (SysDept child : childDepts.getOrDefault(deptId, List.of())) {
            total += accumulateHeadcount(child.getId(), childDepts, own, totals, visiting);
        }
        visiting.remove(deptId);
        totals.put(deptId, total);
        return total;
    }

    /**
     * 计算可见部门与可见员工：status 只保留对应状态员工（保留其部门祖先链），
     * keyword 保留命中员工 + 上级链 + 部门链，命中部门名则保留整棵子树；
     * deptId 只保留该部门子树并附带祖先根链。
     */
    private OrgTreeScope resolveOrgTreeScope(
            List<SysDept> depts,
            List<HrEmployee> employees,
            Map<Long, SysDept> deptById,
            Map<Long, List<SysDept>> childDepts,
            Map<Long, HrEmployee> employeeById,
            EmployeeStatus status,
            String keyword,
            Long deptId,
            boolean includeResigned) {
        Set<Long> visibleEmployees = new HashSet<>();
        for (HrEmployee employee : employees) {
            if (status != null) {
                if (employee.getStatus() == status) {
                    visibleEmployees.add(employee.getId());
                }
            } else if (includeResigned || employee.getStatus() != EmployeeStatus.TERMINATED) {
                // 默认隐藏已离职员工，仅“全部员工”筛选时展示
                visibleEmployees.add(employee.getId());
            }
        }
        Set<Long> visibleDepts = new HashSet<>(deptById.keySet());

        if (StringUtils.isNotBlank(keyword)) {
            String lowerKeyword = keyword.trim().toLowerCase(Locale.ROOT);
            Set<Long> keepEmployees = new HashSet<>();
            Set<Long> keepDepts = new HashSet<>();
            Set<Long> keywordSubtreeDepts = new HashSet<>();
            // 部门名命中：保留该部门整棵子树（含空部门）
            for (SysDept dept : depts) {
                if (!containsKeyword(dept.getName(), lowerKeyword)) {
                    continue;
                }
                Set<Long> subtree = collectDeptSubtree(dept.getId(), childDepts);
                keywordSubtreeDepts.addAll(subtree);
                keepDepts.addAll(subtree);
                keepDepts.addAll(collectDeptChain(dept.getId(), deptById));
            }
            // 员工姓名 / 工号命中：命中者 + 上级链 + 部门链
            for (HrEmployee employee : employees) {
                if (!visibleEmployees.contains(employee.getId())) {
                    continue;
                }
                if (!containsKeyword(employee.getName(), lowerKeyword)
                        && !containsKeyword(employee.getEmployeeNo(), lowerKeyword)) {
                    continue;
                }
                keepEmployees.add(employee.getId());
                keepDepts.addAll(collectDeptChain(employee.getDeptId(), deptById));
                HrEmployee cursor = employee;
                Set<Long> walked = new HashSet<>();
                while (cursor.getLeaderId() != null && walked.add(cursor.getLeaderId())) {
                    HrEmployee leader = employeeById.get(cursor.getLeaderId());
                    if (leader == null || !visibleEmployees.contains(leader.getId())) {
                        break;
                    }
                    keepEmployees.add(leader.getId());
                    keepDepts.addAll(collectDeptChain(leader.getDeptId(), deptById));
                    cursor = leader;
                }
            }
            // 部门名命中的子树内员工整体保留（仍受状态过滤约束）
            for (HrEmployee employee : employees) {
                if (visibleEmployees.contains(employee.getId())
                        && employee.getDeptId() != null
                        && keywordSubtreeDepts.contains(employee.getDeptId())) {
                    keepEmployees.add(employee.getId());
                }
            }
            visibleEmployees.retainAll(keepEmployees);
            visibleDepts.retainAll(keepDepts);
        }

        if (status != null) {
            // 无命中员工的部门不返回，仅保留命中员工的部门祖先链
            Set<Long> keepDepts = new HashSet<>();
            for (HrEmployee employee : employees) {
                if (visibleEmployees.contains(employee.getId())) {
                    keepDepts.addAll(collectDeptChain(employee.getDeptId(), deptById));
                }
            }
            visibleDepts.retainAll(keepDepts);
        }

        if (deptId != null) {
            Set<Long> subtree = collectDeptSubtree(deptId, childDepts);
            Set<Long> deptScope = new HashSet<>(subtree);
            deptScope.addAll(collectDeptChain(deptId, deptById));
            visibleDepts.retainAll(deptScope);
            visibleEmployees.removeIf(id -> {
                HrEmployee employee = employeeById.get(id);
                return employee == null
                        || employee.getDeptId() == null
                        || !subtree.contains(employee.getDeptId());
            });
        }

        // 部门已不可见的员工不再单独渲染，避免出现游离节点
        visibleEmployees.removeIf(id -> {
            HrEmployee employee = employeeById.get(id);
            if (employee == null) {
                return true;
            }
            Long deptKey = employee.getDeptId();
            if (deptKey == null || !deptById.containsKey(deptKey)) {
                return false;
            }
            return !visibleDepts.contains(deptKey);
        });
        return new OrgTreeScope(visibleDepts, visibleEmployees);
    }

    private boolean containsKeyword(String value, String lowerKeyword) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(lowerKeyword);
    }

    /** 组装某部门（deptId 为 null 表示未分配部门）下的员工根节点。 */
    private List<OrgTreeNodeVO> buildEmployeeRoots(Long deptId, OrgTreeContext context) {
        List<HrEmployee> members =
                (deptId == null
                                ? context.unassignedMembers
                                : context.membersByDept.getOrDefault(deptId, List.of()))
                        .stream()
                        .filter(employee -> context.visibleEmployeeIds.contains(employee.getId()))
                        .sorted(employeeComparator(context))
                        .toList();
        if (members.isEmpty()) {
            return List.of();
        }
        List<OrgTreeNodeVO> roots = new ArrayList<>();
        Set<Long> rendered = new HashSet<>();
        for (HrEmployee employee : members) {
            if (isEmployeeRoot(employee, deptId, context)) {
                roots.add(buildEmployeeNode(employee, deptId, context, rendered, new HashSet<>()));
            }
        }
        // 环上的员工没有可挂载的根：提升为根节点，递归时再截断回边
        for (HrEmployee employee : members) {
            if (!rendered.contains(employee.getId())) {
                log.warn(
                        "员工组织树上级关系存在环，已将员工 {}（部门 {}）提升为根节点",
                        employee.getId(),
                        deptId);
                roots.add(buildEmployeeNode(employee, deptId, context, rendered, new HashSet<>()));
            }
        }
        return roots;
    }

    /** 员工根 = 无上级 / 上级不在本部门 / 上级已离职 / 上级被过滤掉。 */
    private boolean isEmployeeRoot(HrEmployee employee, Long deptId, OrgTreeContext context) {
        Long leaderId = employee.getLeaderId();
        if (leaderId == null) {
            return true;
        }
        HrEmployee leader = context.employeeById.get(leaderId);
        if (leader == null || leader.getStatus() == EmployeeStatus.TERMINATED) {
            return true;
        }
        if (!Objects.equals(deptBucket(leader.getDeptId(), context), deptId)) {
            return true;
        }
        return !context.visibleEmployeeIds.contains(leaderId);
    }

    private OrgTreeNodeVO buildEmployeeNode(
            HrEmployee employee,
            Long deptId,
            OrgTreeContext context,
            Set<Long> rendered,
            Set<Long> path) {
        OrgTreeNodeVO node = toEmployeeNode(employee, context);
        rendered.add(employee.getId());
        path.add(employee.getId());
        List<HrEmployee> children =
                context.subordinates.getOrDefault(employee.getId(), List.of()).stream()
                        .filter(sub -> Objects.equals(deptBucket(sub.getDeptId(), context), deptId))
                        .filter(sub -> context.visibleEmployeeIds.contains(sub.getId()))
                        .sorted(employeeComparator(context))
                        .toList();
        for (HrEmployee child : children) {
            if (path.contains(child.getId())) {
                log.warn(
                        "员工组织树检测到上级环路，已截断：{} -> {}",
                        employee.getId(),
                        child.getId());
                continue;
            }
            node.getChildren().add(buildEmployeeNode(child, deptId, context, rendered, path));
        }
        path.remove(employee.getId());
        return node;
    }

    private OrgTreeNodeVO toEmployeeNode(HrEmployee employee, OrgTreeContext context) {
        OrgTreeNodeVO node = new OrgTreeNodeVO();
        node.setId(employee.getId());
        node.setNodeType(OrgTreeNodeVO.NodeType.EMPLOYEE);
        node.setName(employee.getName());
        node.setDeptId(employee.getDeptId());
        node.setLeaderId(employee.getLeaderId());
        node.setEmployeeNo(employee.getEmployeeNo());
        node.setStatus(employee.getStatus());
        node.setLevelId(employee.getLevelId());
        node.setCrossDeptLeader(false);
        if (employee.getPositionId() != null) {
            node.setPositionName(context.positionNameById.get(employee.getPositionId()));
        }
        HrJobLevel level =
                employee.getLevelId() == null ? null : context.levelById.get(employee.getLevelId());
        if (level != null) {
            node.setLevelName(level.getName());
            node.setLevelCode(level.getCode());
        }
        HrEmployee leader =
                employee.getLeaderId() == null
                        ? null
                        : context.employeeById.get(employee.getLeaderId());
        if (leader != null) {
            node.setLeaderName(leader.getName());
            if (!Objects.equals(
                    deptBucket(leader.getDeptId(), context),
                    deptBucket(employee.getDeptId(), context))) {
                node.setCrossDeptLeader(true);
                node.setLeaderDeptName(deptName(leader.getDeptId(), context));
            }
        }
        return node;
    }

    private OrgTreeNodeVO toDeptNode(
            SysDept dept,
            Map<Long, String> deptLeaderNames,
            Map<Long, Integer> headcounts,
            Map<Long, Integer> totalHeadcounts) {
        OrgTreeNodeVO node = new OrgTreeNodeVO();
        node.setId(dept.getId());
        node.setNodeType(OrgTreeNodeVO.NodeType.DEPT);
        node.setName(dept.getName());
        node.setParentId(dept.getParentId());
        node.setLeaderName(
                dept.getLeaderId() == null ? null : deptLeaderNames.get(dept.getLeaderId()));
        node.setHeadcount(headcounts.getOrDefault(dept.getId(), 0));
        node.setTotalHeadcount(totalHeadcounts.getOrDefault(dept.getId(), 0));
        return node;
    }

    private OrgTreeNodeVO toUnassignedNode(List<OrgTreeNodeVO> children) {
        OrgTreeNodeVO node = new OrgTreeNodeVO();
        node.setId(UNASSIGNED_ID);
        node.setNodeType(OrgTreeNodeVO.NodeType.UNASSIGNED);
        node.setName(UNASSIGNED_NAME);
        node.getChildren().addAll(children);
        return node;
    }

    /** 部门归属桶：未分配（dept_id 为空或部门已删除）统一归入 null。 */
    private Long deptBucket(Long deptId, OrgTreeContext context) {
        return deptId != null && context.deptById.containsKey(deptId) ? deptId : null;
    }

    private String deptName(Long deptId, OrgTreeContext context) {
        if (deptId == null) {
            return UNASSIGNED_NAME;
        }
        SysDept dept = context.deptById.get(deptId);
        return dept == null ? null : dept.getName();
    }

    /** 职级 rank_no 降序（无职级排最后），同职级按姓名、工号升序。 */
    private Comparator<HrEmployee> employeeComparator(OrgTreeContext context) {
        return (left, right) -> {
            int rank = compareRank(rankOf(left, context), rankOf(right, context));
            if (rank != 0) {
                return rank;
            }
            int name = safe(left.getName()).compareTo(safe(right.getName()));
            if (name != 0) {
                return name;
            }
            return safe(left.getEmployeeNo()).compareTo(safe(right.getEmployeeNo()));
        };
    }

    private Integer rankOf(HrEmployee employee, OrgTreeContext context) {
        return employee.getLevelId() == null ? null : context.levelRanks.get(employee.getLevelId());
    }

    private int compareRank(Integer left, Integer right) {
        if (left == null && right == null) {
            return 0;
        }
        if (left == null) {
            return 1;
        }
        if (right == null) {
            return -1;
        }
        return Integer.compare(right, left);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    /** 某次组装使用的可见范围。 */
    private record OrgTreeScope(Set<Long> visibleDeptIds, Set<Long> visibleEmployeeIds) {}

    /** 组织树组装所需的批量查询结果与索引。 */
    private static final class OrgTreeContext {

        private final Map<Long, SysDept> deptById;
        private final Map<Long, HrEmployee> employeeById;
        private final Map<Long, HrJobLevel> levelById;
        private final Map<Long, Integer> levelRanks;
        private final Map<Long, String> positionNameById;
        private final Map<Long, List<HrEmployee>> membersByDept;
        private final List<HrEmployee> unassignedMembers;
        private final Map<Long, List<HrEmployee>> subordinates;
        private final Set<Long> visibleEmployeeIds;

        private OrgTreeContext(
                Map<Long, SysDept> deptById,
                Map<Long, HrEmployee> employeeById,
                Map<Long, HrJobLevel> levelById,
                Map<Long, Integer> levelRanks,
                Map<Long, String> positionNameById,
                Map<Long, List<HrEmployee>> membersByDept,
                List<HrEmployee> unassignedMembers,
                Map<Long, List<HrEmployee>> subordinates,
                Set<Long> visibleEmployeeIds) {
            this.deptById = deptById;
            this.employeeById = employeeById;
            this.levelById = levelById;
            this.levelRanks = levelRanks;
            this.positionNameById = positionNameById;
            this.membersByDept = membersByDept;
            this.unassignedMembers = unassignedMembers;
            this.subordinates = subordinates;
            this.visibleEmployeeIds = visibleEmployeeIds;
        }
    }

    private HrEmployee requireEmployee(Long id) {
        HrEmployee employee = id == null ? null : employeeMapper.selectById(id);
        if (employee == null) {
            throw BusinessException.of(ErrorCode.BIZ_NOT_FOUND, "员工档案不存在");
        }
        return employee;
    }

    private HrEmployee requireCurrentEmployee() {
        Long userId = SecurityUtils.getCurrentUserId();
        HrEmployee employee =
                userId == null
                        ? null
                        : employeeMapper.selectOne(
                                new LambdaQueryWrapper<HrEmployee>()
                                        .eq(HrEmployee::getUserId, userId)
                                        .last("LIMIT 1"));
        if (employee == null) {
            throw BusinessException.of(ErrorCode.BIZ_NOT_FOUND, "当前账号未绑定员工档案");
        }
        return employee;
    }

    private Long parseInstanceId(String instanceId) {
        if (StringUtils.isBlank(instanceId)) {
            return null;
        }
        try {
            return Long.valueOf(instanceId.trim());
        } catch (NumberFormatException e) {
            log.warn("BPM 实例 ID 非数字，无法回写申请单：{}", instanceId);
            return null;
        }
    }

    private String label(ChangeType type) {
        return switch (type) {
            case REGULAR -> "转正";
            case TRANSFER -> "调岗";
            case RESIGN -> "离职";
            case PROMOTION -> "晋升";
            case LEADER_TRANSFER -> "上级转交";
        };
    }
}
