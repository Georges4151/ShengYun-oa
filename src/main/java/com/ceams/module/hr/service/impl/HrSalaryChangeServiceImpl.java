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
import com.ceams.module.hr.dto.SalaryChangeApplyRequest;
import com.ceams.module.hr.entity.HrEmployee;
import com.ceams.module.hr.entity.HrSalary;
import com.ceams.module.hr.entity.HrSalaryChangeApply;
import com.ceams.module.hr.enums.SalaryChangeStatus;
import com.ceams.module.hr.enums.SalaryChangeType;
import com.ceams.module.hr.enums.SalaryStatus;
import com.ceams.module.hr.mapper.HrEmployeeMapper;
import com.ceams.module.hr.mapper.HrSalaryChangeApplyMapper;
import com.ceams.module.hr.mapper.HrSalaryMapper;
import com.ceams.module.hr.service.HrSalaryChangeService;
import com.ceams.module.hr.support.SalarySnapshotCodec;
import com.ceams.module.hr.util.HrPageUtil;
import com.ceams.module.hr.vo.SalaryChangeVO;
import com.ceams.module.iam.entity.SysUser;
import com.ceams.module.iam.mapper.UserMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 薪资变动申请服务实现。
 *
 * <p>提交即落库申请单（status=APPROVING）并发起 BPM；发起失败由事务回滚，保证不留下孤儿申请单。
 * 变更前后金额只以密文写入快照 JSON，台账变更由 {@code HrSalaryBpmCallback} 在审批通过后完成。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HrSalaryChangeServiceImpl implements HrSalaryChangeService {

    /** 金额统一保留 2 位小数。 */
    private static final int MONEY_SCALE = 2;

    private final HrSalaryChangeApplyMapper changeApplyMapper;
    private final HrSalaryMapper salaryMapper;
    private final HrEmployeeMapper employeeMapper;
    private final UserMapper userMapper;
    private final SalarySnapshotCodec snapshotCodec;
    private final BpmInstanceService bpmInstanceService;

    @Override
    @Transactional
    public String applyChange(SalaryChangeApplyRequest request) {
        if (request.getChangeType() == null) {
            throw BusinessException.of(ErrorCode.VALID_REQUIRED_FIELD, "变动类型不能为空");
        }
        if (request.getEffectiveDate() == null) {
            throw BusinessException.of(ErrorCode.VALID_REQUIRED_FIELD, "生效日期不能为空");
        }
        requireEmployee(request.getEmployeeId());
        HrSalary current = selectActive(request.getEmployeeId());
        if (current == null && request.getChangeType() != SalaryChangeType.ADJUST) {
            throw BusinessException.of(
                    ErrorCode.BIZ_STATE_CONFLICT,
                    "该员工尚无生效薪资，仅支持以「薪资调整」类型首次建档");
        }
        if (isNoneProvided(request)) {
            throw BusinessException.of(ErrorCode.VALID_ILLEGAL_ARGUMENT, "至少填写一项薪资金额");
        }
        Long approvingCount =
                changeApplyMapper.selectCount(
                        new LambdaQueryWrapper<HrSalaryChangeApply>()
                                .eq(HrSalaryChangeApply::getEmployeeId, request.getEmployeeId())
                                .eq(HrSalaryChangeApply::getStatus, SalaryChangeStatus.APPROVING));
        if (approvingCount != null && approvingCount > 0) {
            throw BusinessException.of(
                    ErrorCode.BIZ_STATE_CONFLICT, "该员工存在审批中的薪资变动申请，请勿重复提交");
        }

        BigDecimal currentBase = current == null ? null : parseAmount(current.getBaseSalaryEnc());
        BigDecimal currentPost = current == null ? null : parseAmount(current.getPostSalaryEnc());
        BigDecimal currentPerformance =
                current == null ? null : parseAmount(current.getPerformanceBaseEnc());
        BigDecimal currentAllowance = current == null ? null : parseAmount(current.getAllowanceEnc());

        BigDecimal baseSalary =
                resolveAmount(request.getBaseSalary(), currentBase, "基本工资", current == null);
        BigDecimal postSalary =
                resolveAmount(request.getPostSalary(), currentPost, "岗位工资", false);
        BigDecimal performanceBase =
                resolveAmount(
                        request.getPerformanceBase(), currentPerformance, "绩效基数", false);
        BigDecimal allowance =
                resolveAmount(request.getAllowance(), currentAllowance, "固定补贴", false);

        HrSalaryChangeApply apply = new HrSalaryChangeApply();
        apply.setEmployeeId(request.getEmployeeId());
        apply.setApplicantId(SecurityUtils.getCurrentUserId());
        apply.setChangeType(request.getChangeType());
        apply.setBeforeSnapshot(
                current == null
                        ? null
                        : snapshotCodec.encode(
                                current.getEmployeeId(),
                                current.getEffectiveDate(),
                                currentBase,
                                currentPost,
                                currentPerformance,
                                currentAllowance));
        apply.setAfterSnapshot(
                snapshotCodec.encode(
                        request.getEmployeeId(),
                        request.getEffectiveDate(),
                        baseSalary,
                        postSalary,
                        performanceBase,
                        allowance));
        apply.setEffectiveDate(request.getEffectiveDate());
        apply.setReason(request.getReason());
        apply.setStatus(SalaryChangeStatus.APPROVING);
        changeApplyMapper.insert(apply);

        BpmBizType bizType = request.getChangeType().bpmBizType();
        InstanceStartRequest start = new InstanceStartRequest();
        start.setTemplateCode(bizType.name());
        start.setBizType(bizType.name());
        start.setBizId(apply.getId());
        start.setVariables(
                buildVariables(request.getEmployeeId(), request.getChangeType(), request));
        String instanceId = bpmInstanceService.start(start);

        apply.setBpmInstanceId(parseInstanceId(instanceId));
        changeApplyMapper.updateById(apply);
        log.info(
                "员工 {} 发起{}申请，申请单 {}，BPM 实例 {}",
                request.getEmployeeId(),
                label(request.getChangeType()),
                apply.getId(),
                instanceId);
        return String.valueOf(apply.getId());
    }

    @Override
    public PageResult<SalaryChangeVO> pageChanges(
            long page,
            long size,
            Long employeeId,
            SalaryChangeType changeType,
            SalaryChangeStatus status) {
        Page<HrSalaryChangeApply> p = HrPageUtil.resolve(page, size);
        LambdaQueryWrapper<HrSalaryChangeApply> wrapper = new LambdaQueryWrapper<>();
        if (employeeId != null) {
            wrapper.eq(HrSalaryChangeApply::getEmployeeId, employeeId);
        }
        if (changeType != null) {
            wrapper.eq(HrSalaryChangeApply::getChangeType, changeType);
        }
        if (status != null) {
            wrapper.eq(HrSalaryChangeApply::getStatus, status);
        }
        wrapper.orderByDesc(HrSalaryChangeApply::getCreateTime);
        IPage<HrSalaryChangeApply> result = changeApplyMapper.selectPage(p, wrapper);
        return toPageResult(result);
    }

    @Override
    public PageResult<SalaryChangeVO> myChanges(long page, long size) {
        Page<HrSalaryChangeApply> p = HrPageUtil.resolve(page, size);
        Long applicantId = SecurityUtils.getCurrentUserId();
        if (applicantId == null) {
            return new PageResult<>(List.of(), 0, p.getCurrent(), p.getSize());
        }
        IPage<HrSalaryChangeApply> result =
                changeApplyMapper.selectPage(
                        p,
                        new LambdaQueryWrapper<HrSalaryChangeApply>()
                                .eq(HrSalaryChangeApply::getApplicantId, applicantId)
                                .orderByDesc(HrSalaryChangeApply::getCreateTime));
        return toPageResult(result);
    }

    /** 组装流程变量（模板不使用金额变量，避免明文金额写入 bpm_instance_variable）。 */
    private Map<String, String> buildVariables(
            Long employeeId, SalaryChangeType changeType, SalaryChangeApplyRequest request) {
        Map<String, String> variables = new LinkedHashMap<>();
        variables.put("employeeId", String.valueOf(employeeId));
        variables.put("changeType", changeType.name());
        variables.put("effectiveDate", request.getEffectiveDate().toString());
        return variables;
    }

    private PageResult<SalaryChangeVO> toPageResult(IPage<HrSalaryChangeApply> result) {
        Map<Long, HrEmployee> employees = loadEmployees(result.getRecords());
        Map<Long, SysUser> applicants = loadApplicants(result.getRecords());
        List<SalaryChangeVO> records =
                result.getRecords().stream()
                        .map(
                                apply ->
                                        toVO(
                                                apply,
                                                employees.get(apply.getEmployeeId()),
                                                applicants.get(apply.getApplicantId())))
                        .toList();
        return new PageResult<>(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    private SalaryChangeVO toVO(
            HrSalaryChangeApply apply, HrEmployee employee, SysUser applicant) {
        SalaryChangeVO vo = new SalaryChangeVO();
        vo.setId(apply.getId());
        vo.setEmployeeId(apply.getEmployeeId());
        if (employee != null) {
            vo.setEmployeeNo(employee.getEmployeeNo());
            vo.setEmployeeName(employee.getName());
        }
        vo.setApplicantId(apply.getApplicantId());
        if (applicant != null) {
            vo.setApplicantName(applicant.getName());
        }
        vo.setChangeType(apply.getChangeType());
        vo.setEffectiveDate(apply.getEffectiveDate());
        vo.setBeforeSummary(snapshotCodec.summarize(apply.getBeforeSnapshot()));
        vo.setAfterSummary(snapshotCodec.summarize(apply.getAfterSnapshot()));
        vo.setReason(apply.getReason());
        vo.setStatus(apply.getStatus());
        vo.setBpmInstanceId(apply.getBpmInstanceId());
        vo.setCreateTime(apply.getCreateTime());
        return vo;
    }

    private Map<Long, HrEmployee> loadEmployees(List<HrSalaryChangeApply> applies) {
        List<Long> employeeIds =
                applies.stream()
                        .map(HrSalaryChangeApply::getEmployeeId)
                        .filter(Objects::nonNull)
                        .distinct()
                        .toList();
        if (employeeIds.isEmpty()) {
            return Map.of();
        }
        return employeeMapper.selectBatchIds(employeeIds).stream()
                .collect(Collectors.toMap(HrEmployee::getId, Function.identity(), (a, b) -> a));
    }

    private Map<Long, SysUser> loadApplicants(List<HrSalaryChangeApply> applies) {
        List<Long> applicantIds =
                applies.stream()
                        .map(HrSalaryChangeApply::getApplicantId)
                        .filter(Objects::nonNull)
                        .distinct()
                        .toList();
        if (applicantIds.isEmpty()) {
            return Map.of();
        }
        return userMapper.selectBatchIds(applicantIds).stream()
                .collect(Collectors.toMap(SysUser::getId, Function.identity(), (a, b) -> a));
    }

    private HrSalary selectActive(Long employeeId) {
        return salaryMapper.selectOne(
                new LambdaQueryWrapper<HrSalary>()
                        .eq(HrSalary::getEmployeeId, employeeId)
                        .eq(HrSalary::getStatus, SalaryStatus.ACTIVE)
                        .last("LIMIT 1"));
    }

    private HrEmployee requireEmployee(Long employeeId) {
        HrEmployee employee = employeeId == null ? null : employeeMapper.selectById(employeeId);
        if (employee == null) {
            throw BusinessException.of(ErrorCode.BIZ_NOT_FOUND, "员工档案不存在");
        }
        return employee;
    }

    /** 四个金额全部为空视为无效申请。 */
    private boolean isNoneProvided(SalaryChangeApplyRequest request) {
        return request.getBaseSalary() == null
                && request.getPostSalary() == null
                && request.getPerformanceBase() == null
                && request.getAllowance() == null;
    }

    /**
     * 金额解析：请求值优先；为空时沿用当前生效台账（首次建档时基本工资必填）。
     */
    private BigDecimal resolveAmount(
            BigDecimal requested, BigDecimal fallback, String label, boolean required) {
        if (requested == null) {
            if (required && fallback == null) {
                throw BusinessException.of(ErrorCode.VALID_REQUIRED_FIELD, label + "不能为空");
            }
            return fallback == null ? null : fallback.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        }
        if (requested.compareTo(BigDecimal.ZERO) < 0) {
            throw BusinessException.of(ErrorCode.VALID_ILLEGAL_ARGUMENT, label + "不能为负数");
        }
        return requested.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    /** 库内密文经 TypeHandler 解密后为明文数值字符串，此处转为 BigDecimal。 */
    private BigDecimal parseAmount(String value) {
        return StringUtils.isBlank(value) ? null : new BigDecimal(value.trim());
    }

    private Long parseInstanceId(String instanceId) {
        if (StringUtils.isBlank(instanceId)) {
            return null;
        }
        try {
            return Long.valueOf(instanceId.trim());
        } catch (NumberFormatException e) {
            log.warn("BPM 实例 ID 非数字，无法回写薪资变动申请：{}", instanceId);
            return null;
        }
    }

    private String label(SalaryChangeType type) {
        return switch (type) {
            case ADJUST -> "薪资调整";
            case PROMOTION -> "晋升调薪";
            case CORRECTION -> "薪资更正";
        };
    }
}
