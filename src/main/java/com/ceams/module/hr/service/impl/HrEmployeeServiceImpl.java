package com.ceams.module.hr.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
import com.ceams.module.hr.enums.ChangeType;
import com.ceams.module.hr.enums.EmployeeStatus;
import com.ceams.module.hr.enums.HrApprovalStatus;
import com.ceams.module.hr.mapper.HrEmployeeChangeMapper;
import com.ceams.module.hr.mapper.HrEmployeeMapper;
import com.ceams.module.hr.service.HrEmployeeService;
import com.ceams.module.hr.util.HrPageUtil;
import com.ceams.module.hr.vo.EmployeeChangeVO;
import com.ceams.module.hr.vo.EmployeeVO;
import com.ceams.module.hr.vo.HeadcountDeptVO;
import com.ceams.module.iam.entity.SysDept;
import com.ceams.module.iam.mapper.DeptMapper;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
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
    private final HrEmployeeConverter employeeConverter;
    private final HrEmployeeChangeConverter changeConverter;
    private final BpmInstanceService bpmInstanceService;
    private final DeptMapper deptMapper;

    @Override
    public PageResult<EmployeeVO> page(
            long page, long size, Long deptId, EmployeeStatus status, String keyword) {
        Page<HrEmployee> p = HrPageUtil.resolve(page, size);
        LambdaQueryWrapper<HrEmployee> wrapper = new LambdaQueryWrapper<>();
        if (deptId != null) {
            wrapper.eq(HrEmployee::getDeptId, deptId);
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
        return new PageResult<>(
                employeeConverter.toVOList(result.getRecords()),
                result.getTotal(),
                result.getCurrent(),
                result.getSize());
    }

    @Override
    public EmployeeVO detail(Long id) {
        return employeeConverter.toVO(requireEmployee(id));
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
        return changeConverter.toVOList(changes);
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
        };
    }
}
