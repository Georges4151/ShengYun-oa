package com.ceams.module.hr.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ceams.common.crypto.SensitiveEncryptor;
import com.ceams.common.exception.BusinessException;
import com.ceams.common.exception.ErrorCode;
import com.ceams.common.response.PageResult;
import com.ceams.common.security.SecurityUtils;
import com.ceams.module.bpm.enums.AuditAction;
import com.ceams.module.bpm.service.BpmAuditService;
import com.ceams.module.hr.config.HrPayrollProperties;
import com.ceams.module.hr.dto.PayrollAdjustRequest;
import com.ceams.module.hr.dto.PayrollCalcRequest;
import com.ceams.module.hr.dto.PayrollConfirmRequest;
import com.ceams.module.hr.entity.HrCheckin;
import com.ceams.module.hr.entity.HrEmployee;
import com.ceams.module.hr.entity.HrLeaveRequest;
import com.ceams.module.hr.entity.HrPerformanceReview;
import com.ceams.module.hr.entity.HrSalary;
import com.ceams.module.hr.entity.HrSalaryCalc;
import com.ceams.module.hr.enums.CheckinStatus;
import com.ceams.module.hr.enums.EmployeeStatus;
import com.ceams.module.hr.enums.HrApprovalStatus;
import com.ceams.module.hr.enums.LeaveType;
import com.ceams.module.hr.enums.PayrollStatus;
import com.ceams.module.hr.enums.PerformanceReviewStatus;
import com.ceams.module.hr.enums.SalaryStatus;
import com.ceams.module.hr.mapper.HrCheckinMapper;
import com.ceams.module.hr.mapper.HrEmployeeMapper;
import com.ceams.module.hr.mapper.HrLeaveRequestMapper;
import com.ceams.module.hr.mapper.HrPerformanceReviewMapper;
import com.ceams.module.hr.mapper.HrSalaryCalcMapper;
import com.ceams.module.hr.mapper.HrSalaryMapper;
import com.ceams.module.hr.service.HrPayrollService;
import com.ceams.module.hr.support.HrPayrollBatchPersister;
import com.ceams.module.hr.support.PayrollCalculator;
import com.ceams.module.hr.util.HrPageUtil;
import com.ceams.module.hr.vo.PayrollCalcResultVO;
import com.ceams.module.hr.vo.PayrollCostDeptVO;
import com.ceams.module.hr.vo.PayrollCostSummaryVO;
import com.ceams.module.hr.vo.SalaryCalcVO;
import com.ceams.module.iam.entity.SysDept;
import com.ceams.module.iam.mapper.DeptMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 月度工资核算服务实现。
 *
 * <p>核算口径要点：
 * <ul>
 *   <li>薪资：{@code hr_salary} 中 status=ACTIVE 且 effective_date ≤ 当月最后一天，每人取生效日期最大的一条；
 *   <li>缺勤：当月 {@code hr_checkin} 中 status=ABSENT 的去重日期数；
 *   <li>迟到：当月 {@code hr_checkin} 中 status=LATE 的记录数；
 *   <li>事假 / 病假：当月已审批通过的 {@code hr_leave_request}，按 leave_type=PERSONAL/SICK 累计 days；
 *   <li>绩效系数：同月 {@code hr_performance_review} 中 status=APPROVED 的 coefficient，无记录取 1.0，
 *       多条取创建时间最新的一条。
 * </ul>
 *
 * <p>金额全程 BigDecimal，禁止出现在日志中；日志仅输出月份与计数。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HrPayrollServiceImpl implements HrPayrollService {

    /** 批量核算分片大小：每批独立事务，避免长事务。 */
    private static final int BATCH_SIZE = 200;

    /** 金额精度。 */
    private static final int MONEY_SCALE = 2;

    /** 绩效系数精度（与 hr_salary_calc.performance_coefficient DECIMAL(6,4) 一致）。 */
    private static final int COEFFICIENT_SCALE = 4;

    /** 审计目标类型（沿用 P2 的自定义 targetType 约定）。 */
    private static final String AUDIT_TARGET = "HR_SALARY_CALC";

    /** 审计脱敏字段清单（金额仅以密文进入审计 JSON）。 */
    private static final String AUDIT_MASKED_FIELDS =
            "baseSalary,postSalary,attendanceDeduction,performanceAmount,allowance,"
                    + "socialInsurance,housingFund,tax,grossPay,netPay";

    private final HrSalaryCalcMapper calcMapper;
    private final HrSalaryMapper salaryMapper;
    private final HrEmployeeMapper employeeMapper;
    private final HrCheckinMapper checkinMapper;
    private final HrLeaveRequestMapper leaveRequestMapper;
    private final HrPerformanceReviewMapper reviewMapper;
    private final HrPayrollBatchPersister batchPersister;
    private final HrPayrollProperties payrollProperties;
    private final BpmAuditService bpmAuditService;
    private final SensitiveEncryptor encryptor;
    private final DeptMapper deptMapper;

    // ------------------------------------------------------------------
    // 初始化 / 重算
    // ------------------------------------------------------------------

    @Override
    public PayrollCalcResultVO calculate(PayrollCalcRequest request) {
        YearMonth month = parseMonth(request.getCalcMonth());
        String monthText = month.toString();
        List<HrEmployee> candidates = loadCandidates(request);
        List<String> warnings = new ArrayList<>();
        if (candidates.isEmpty()) {
            log.info("月度工资核算：月份 {} 未命中任何在职员工", monthText);
            return new PayrollCalcResultVO(monthText, 0, 0, 0, 0, 0, warnings);
        }

        Map<Long, HrSalaryCalc> existing =
                loadExisting(monthText, candidates.stream().map(HrEmployee::getId).toList());

        // 已确认 / 已锁定的记录直接跳过，不允许批量核算覆盖
        int locked = 0;
        List<HrEmployee> pending = new ArrayList<>(candidates.size());
        for (HrEmployee employee : candidates) {
            HrSalaryCalc calc = existing.get(employee.getId());
            if (calc != null && calc.getStatus() != PayrollStatus.DRAFT) {
                locked++;
                warnings.add(employeeLabel(employee) + "当月核算" + statusLabel(calc.getStatus()) + "，跳过重算");
                continue;
            }
            pending.add(employee);
        }

        int created = 0;
        int recalculated = 0;
        int skipped = 0;
        for (int from = 0; from < pending.size(); from += BATCH_SIZE) {
            List<HrEmployee> batch =
                    pending.subList(from, Math.min(from + BATCH_SIZE, pending.size()));
            BatchOutcome outcome = processBatch(batch, existing, month, warnings);
            created += outcome.created();
            recalculated += outcome.recalculated();
            skipped += outcome.skipped();
        }
        log.info(
                "月度工资核算完成：月份 {}，总人数 {}，新建 {}，重算 {}，跳过 {}，锁定 {}",
                monthText,
                candidates.size(),
                created,
                recalculated,
                skipped,
                locked);
        return new PayrollCalcResultVO(
                monthText, candidates.size(), created, recalculated, skipped, locked, warnings);
    }

    /** 处理一个 200 人批次：加载薪资 / 考勤 / 请假 / 绩效并落库。 */
    private BatchOutcome processBatch(
            List<HrEmployee> batch,
            Map<Long, HrSalaryCalc> existing,
            YearMonth month,
            List<String> warnings) {
        List<Long> employeeIds = batch.stream().map(HrEmployee::getId).toList();
        Map<Long, HrSalary> salaries = loadSalaries(employeeIds, month.atEndOfMonth());

        List<EmployeeSalary> payable = new ArrayList<>(batch.size());
        int skipped = 0;
        for (HrEmployee employee : batch) {
            HrSalary salary = salaries.get(employee.getId());
            if (salary == null) {
                skipped++;
                warnings.add(employeeLabel(employee) + "无生效薪资，跳过核算");
                continue;
            }
            payable.add(new EmployeeSalary(employee, salary));
        }
        if (payable.isEmpty()) {
            return new BatchOutcome(0, 0, skipped);
        }

        List<Long> payableIds = payable.stream().map(item -> item.employee().getId()).toList();
        Map<Long, AttendanceSummary> attendance = loadAttendance(payableIds, month);
        Map<Long, LeaveSummary> leaves = loadLeaves(payableIds, month);
        Map<Long, BigDecimal> coefficients = loadCoefficients(payableIds, month);

        List<HrSalaryCalc> calculations = new ArrayList<>(payable.size());
        int created = 0;
        int recalculated = 0;
        for (EmployeeSalary item : payable) {
            Long employeeId = item.employee().getId();
            HrSalaryCalc target = existing.get(employeeId);
            HrSalaryCalc calc = target == null ? new HrSalaryCalc() : target;
            fillCalc(
                    calc,
                    item,
                    attendance.get(employeeId),
                    leaves.get(employeeId),
                    coefficients.get(employeeId),
                    month,
                    warnings);
            calculations.add(calc);
            if (target == null) {
                created++;
            } else {
                recalculated++;
            }
        }
        batchPersister.persistBatch(calculations);
        return new BatchOutcome(created, recalculated, skipped);
    }

    /** 填充核算实体：所有金额以明文数值字符串写入，由 TypeHandler 加密落库。 */
    private void fillCalc(
            HrSalaryCalc calc,
            EmployeeSalary item,
            AttendanceSummary attendance,
            LeaveSummary leave,
            BigDecimal coefficient,
            YearMonth month,
            List<String> warnings) {
        BigDecimal base = nvl(parseAmount(item.salary().getBaseSalaryEnc()));
        BigDecimal post = nvl(parseAmount(item.salary().getPostSalaryEnc()));
        BigDecimal performanceBase = nvl(parseAmount(item.salary().getPerformanceBaseEnc()));
        BigDecimal allowance = nvl(parseAmount(item.salary().getAllowanceEnc()));
        AttendanceSummary att =
                attendance == null ? new AttendanceSummary(BigDecimal.ZERO, 0) : attendance;
        LeaveSummary lv =
                leave == null ? new LeaveSummary(BigDecimal.ZERO, BigDecimal.ZERO) : leave;
        BigDecimal coef = coefficient == null ? BigDecimal.ONE : coefficient;

        PayrollCalculator.PayrollResult result =
                PayrollCalculator.calcMonth(
                        new PayrollCalculator.PayrollInput(
                                base,
                                post,
                                performanceBase,
                                allowance,
                                coef,
                                att.absentDays(),
                                lv.personalDays(),
                                lv.sickDays(),
                                att.lateCount()),
                        payrollProperties);
        for (String warning : result.warnings()) {
            warnings.add(employeeLabel(item.employee()) + warning);
        }

        calc.setEmployeeId(item.employee().getId());
        calc.setCalcMonth(month.toString());
        calc.setBaseSalaryEnc(toPlain(base));
        calc.setPostSalaryEnc(toPlain(post));
        calc.setAttendanceDays(
                nvl(payrollProperties.getWorkingDays()).setScale(MONEY_SCALE, RoundingMode.HALF_UP));
        calc.setAbsentDays(nvl(att.absentDays()).setScale(MONEY_SCALE, RoundingMode.HALF_UP));
        calc.setLateCount(att.lateCount());
        calc.setAttendanceDeductionEnc(toPlain(result.attendanceDeduction()));
        calc.setPerformanceCoefficient(coef.setScale(COEFFICIENT_SCALE, RoundingMode.HALF_UP));
        calc.setPerformanceAmountEnc(toPlain(result.performanceAmount()));
        calc.setAllowanceEnc(toPlain(allowance));
        calc.setSocialInsuranceEnc(toPlain(result.socialInsurance()));
        calc.setHousingFundEnc(toPlain(result.housingFund()));
        calc.setTaxEnc(toPlain(result.tax()));
        calc.setGrossPayEnc(toPlain(result.grossPay()));
        calc.setNetPayEnc(toPlain(result.netPay()));
        calc.setStatus(PayrollStatus.DRAFT);
    }

    /** 命中范围的员工：在职 / 试用期，可按部门与员工 ID 过滤。 */
    private List<HrEmployee> loadCandidates(PayrollCalcRequest request) {
        LambdaQueryWrapper<HrEmployee> wrapper =
                new LambdaQueryWrapper<HrEmployee>()
                        .in(
                                HrEmployee::getStatus,
                                List.of(EmployeeStatus.PROBATION, EmployeeStatus.ACTIVE));
        if (request.getDeptId() != null) {
            wrapper.eq(HrEmployee::getDeptId, request.getDeptId());
        }
        if (request.getEmployeeIds() != null && !request.getEmployeeIds().isEmpty()) {
            wrapper.in(HrEmployee::getId, request.getEmployeeIds());
        }
        wrapper.orderByAsc(HrEmployee::getId);
        return employeeMapper.selectList(wrapper);
    }

    /** 当月已有核算记录（含 CONFIRMED/LOCKED，用于跳过判断）。 */
    private Map<Long, HrSalaryCalc> loadExisting(String monthText, List<Long> employeeIds) {
        if (employeeIds.isEmpty()) {
            return Map.of();
        }
        return calcMapper
                .selectList(
                        new LambdaQueryWrapper<HrSalaryCalc>()
                                .eq(HrSalaryCalc::getCalcMonth, monthText)
                                .in(HrSalaryCalc::getEmployeeId, employeeIds))
                .stream()
                .collect(
                        Collectors.toMap(
                                HrSalaryCalc::getEmployeeId, Function.identity(), (a, b) -> a));
    }

    /** 生效薪资：status=ACTIVE 且生效日期不晚于当月最后一天，每员工取生效日期最大的一条。 */
    private Map<Long, HrSalary> loadSalaries(List<Long> employeeIds, LocalDate monthEnd) {
        List<HrSalary> salaries =
                salaryMapper.selectList(
                        new LambdaQueryWrapper<HrSalary>()
                                .in(HrSalary::getEmployeeId, employeeIds)
                                .eq(HrSalary::getStatus, SalaryStatus.ACTIVE)
                                .le(HrSalary::getEffectiveDate, monthEnd));
        Map<Long, HrSalary> selected = new HashMap<>();
        for (HrSalary salary : salaries) {
            if (salary.getEmployeeId() == null) {
                continue;
            }
            selected.merge(salary.getEmployeeId(), salary, (a, b) -> isLater(b, a) ? b : a);
        }
        return selected;
    }

    /** 缺勤去重天数与迟到次数。 */
    private Map<Long, AttendanceSummary> loadAttendance(List<Long> employeeIds, YearMonth month) {
        List<HrCheckin> checkins =
                checkinMapper.selectList(
                        new LambdaQueryWrapper<HrCheckin>()
                                .in(HrCheckin::getEmployeeId, employeeIds)
                                .ge(HrCheckin::getCheckinDate, month.atDay(1))
                                .le(HrCheckin::getCheckinDate, month.atEndOfMonth())
                                .in(
                                        HrCheckin::getStatus,
                                        List.of(CheckinStatus.ABSENT, CheckinStatus.LATE)));
        Map<Long, Set<LocalDate>> absentDates = new HashMap<>();
        Map<Long, Integer> lateCounts = new HashMap<>();
        for (HrCheckin checkin : checkins) {
            if (checkin.getEmployeeId() == null) {
                continue;
            }
            if (checkin.getStatus() == CheckinStatus.ABSENT && checkin.getCheckinDate() != null) {
                absentDates
                        .computeIfAbsent(checkin.getEmployeeId(), key -> new HashSet<>())
                        .add(checkin.getCheckinDate());
            } else if (checkin.getStatus() == CheckinStatus.LATE) {
                lateCounts.merge(checkin.getEmployeeId(), 1, Integer::sum);
            }
        }
        Map<Long, AttendanceSummary> summaries = new HashMap<>();
        for (Long employeeId : employeeIds) {
            int days = absentDates.getOrDefault(employeeId, Collections.emptySet()).size();
            summaries.put(
                    employeeId,
                    new AttendanceSummary(
                            BigDecimal.valueOf(days).setScale(MONEY_SCALE, RoundingMode.HALF_UP),
                            lateCounts.getOrDefault(employeeId, 0)));
        }
        return summaries;
    }

    /**
     * 事假 / 病假天数：当月已审批通过的请假单，按请假开始日期归属核算月，累计 days。
     *
     * <p>口径说明：跨月请假单不做按日拆分，整单归属开始日期所在月份，避免按比例拆分带来的舍入争议。
     */
    private Map<Long, LeaveSummary> loadLeaves(List<Long> employeeIds, YearMonth month) {
        List<HrLeaveRequest> leaves =
                leaveRequestMapper.selectList(
                        new LambdaQueryWrapper<HrLeaveRequest>()
                                .in(HrLeaveRequest::getEmployeeId, employeeIds)
                                .eq(HrLeaveRequest::getApprovalStatus, HrApprovalStatus.APPROVED)
                                .in(
                                        HrLeaveRequest::getLeaveType,
                                        List.of(LeaveType.PERSONAL, LeaveType.SICK))
                                .ge(HrLeaveRequest::getStartDate, month.atDay(1))
                                .le(HrLeaveRequest::getStartDate, month.atEndOfMonth()));
        Map<Long, BigDecimal> personal = new HashMap<>();
        Map<Long, BigDecimal> sick = new HashMap<>();
        for (HrLeaveRequest leave : leaves) {
            if (leave.getEmployeeId() == null || leave.getDays() == null) {
                continue;
            }
            if (leave.getLeaveType() == LeaveType.PERSONAL) {
                personal.merge(leave.getEmployeeId(), leave.getDays(), BigDecimal::add);
            } else if (leave.getLeaveType() == LeaveType.SICK) {
                sick.merge(leave.getEmployeeId(), leave.getDays(), BigDecimal::add);
            }
        }
        Map<Long, LeaveSummary> summaries = new HashMap<>();
        for (Long employeeId : employeeIds) {
            summaries.put(
                    employeeId,
                    new LeaveSummary(
                            nvl(personal.get(employeeId)), nvl(sick.get(employeeId))));
        }
        return summaries;
    }

    /** 绩效系数：同月 APPROVED 的 coefficient，无记录取 1.0，多条取创建时间最新的一条。 */
    private Map<Long, BigDecimal> loadCoefficients(List<Long> employeeIds, YearMonth month) {
        List<HrPerformanceReview> reviews =
                reviewMapper.selectList(
                        new LambdaQueryWrapper<HrPerformanceReview>()
                                .in(HrPerformanceReview::getEmployeeId, employeeIds)
                                .eq(HrPerformanceReview::getPeriod, month.toString())
                                .eq(HrPerformanceReview::getStatus, PerformanceReviewStatus.APPROVED));
        Map<Long, HrPerformanceReview> latest = new HashMap<>();
        for (HrPerformanceReview review : reviews) {
            if (review.getEmployeeId() == null || review.getCoefficient() == null) {
                continue;
            }
            latest.merge(review.getEmployeeId(), review, (a, b) -> isNewer(b, a) ? b : a);
        }
        Map<Long, BigDecimal> coefficients = new HashMap<>();
        latest.forEach((employeeId, review) -> coefficients.put(employeeId, review.getCoefficient()));
        return coefficients;
    }

    // ------------------------------------------------------------------
    // 明细分页
    // ------------------------------------------------------------------

    @Override
    public PageResult<SalaryCalcVO> page(
            long page, long size, String calcMonth, PayrollStatus status, Long employeeId) {
        Page<HrSalaryCalc> p = HrPageUtil.resolve(page, size);
        LambdaQueryWrapper<HrSalaryCalc> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.isNotBlank(calcMonth)) {
            wrapper.eq(HrSalaryCalc::getCalcMonth, parseMonth(calcMonth).toString());
        }
        if (status != null) {
            wrapper.eq(HrSalaryCalc::getStatus, status);
        }
        if (employeeId != null) {
            wrapper.eq(HrSalaryCalc::getEmployeeId, employeeId);
        }
        wrapper.orderByDesc(HrSalaryCalc::getCalcMonth).orderByAsc(HrSalaryCalc::getEmployeeId);
        IPage<HrSalaryCalc> result = calcMapper.selectPage(p, wrapper);
        Map<Long, HrEmployee> employees = loadEmployees(result.getRecords());
        List<SalaryCalcVO> records =
                result.getRecords().stream()
                        .map(calc -> toVO(calc, employees.get(calc.getEmployeeId())))
                        .toList();
        return new PageResult<>(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    // ------------------------------------------------------------------
    // 手工调整
    // ------------------------------------------------------------------

    @Override
    @Transactional
    public SalaryCalcVO adjust(Long id, PayrollAdjustRequest request) {
        HrSalaryCalc calc = requireCalc(id);
        if (calc.getStatus() != PayrollStatus.DRAFT) {
            throw BusinessException.of(
                    ErrorCode.BIZ_STATE_CONFLICT,
                    "仅草稿状态的核算记录允许调整，已" + statusLabel(calc.getStatus()));
        }
        if (request == null || isNoAdjustment(request)) {
            throw BusinessException.of(ErrorCode.VALID_ILLEGAL_ARGUMENT, "至少填写一项调整内容");
        }
        requireNonNegative(request.getAllowance(), "补贴");
        requireNonNegative(request.getAttendanceDeduction(), "考勤扣款");
        requireNonNegative(request.getPerformanceCoefficient(), "绩效系数");
        requireNonNegative(request.getSocialInsurance(), "社保");
        requireNonNegative(request.getHousingFund(), "公积金");
        requireNonNegative(request.getTax(), "个人所得税");

        Map<String, Object> beforeSnapshot = amountsSnapshot(calc);

        BigDecimal base = nvl(parseAmount(calc.getBaseSalaryEnc()));
        BigDecimal post = nvl(parseAmount(calc.getPostSalaryEnc()));
        BigDecimal allowance =
                request.getAllowance() != null
                        ? money(request.getAllowance())
                        : nvl(parseAmount(calc.getAllowanceEnc()));
        BigDecimal attendanceDeduction =
                request.getAttendanceDeduction() != null
                        ? money(request.getAttendanceDeduction())
                        : nvl(parseAmount(calc.getAttendanceDeductionEnc()));
        BigDecimal socialInsurance =
                request.getSocialInsurance() != null
                        ? money(request.getSocialInsurance())
                        : nvl(parseAmount(calc.getSocialInsuranceEnc()));
        BigDecimal housingFund =
                request.getHousingFund() != null
                        ? money(request.getHousingFund())
                        : nvl(parseAmount(calc.getHousingFundEnc()));
        BigDecimal tax =
                request.getTax() != null
                        ? money(request.getTax())
                        : nvl(parseAmount(calc.getTaxEnc()));
        BigDecimal coefficient =
                calc.getPerformanceCoefficient() == null
                        ? BigDecimal.ONE
                        : calc.getPerformanceCoefficient();
        BigDecimal performanceAmount = nvl(parseAmount(calc.getPerformanceAmountEnc()));

        // 绩效系数变化时按「生效薪资中的绩效基数 × 新系数」重算绩效工资
        if (request.getPerformanceCoefficient() != null) {
            HrSalary salary = selectActiveSalary(calc.getEmployeeId());
            if (salary == null) {
                throw BusinessException.of(
                        ErrorCode.BIZ_UNPROCESSABLE,
                        "员工当前无生效薪资，无法按绩效系数重算绩效工资");
            }
            BigDecimal performanceBase = nvl(parseAmount(salary.getPerformanceBaseEnc()));
            coefficient = request.getPerformanceCoefficient().setScale(COEFFICIENT_SCALE, RoundingMode.HALF_UP);
            performanceAmount =
                    performanceBase
                            .multiply(coefficient)
                            .setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        }

        BigDecimal grossPay =
                base.add(post).add(performanceAmount).add(allowance)
                        .setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        // 未显式指定个税时，按调整后的应发与社保、公积金重算，避免补贴上调后实发虚高
        if (request.getTax() == null) {
            BigDecimal taxableIncome =
                    grossPay
                            .subtract(socialInsurance)
                            .subtract(housingFund)
                            .subtract(nvl(payrollProperties.getTaxThreshold()));
            tax = PayrollCalculator.calcTax(taxableIncome, payrollProperties);
        }
        BigDecimal netPay =
                grossPay
                        .subtract(attendanceDeduction)
                        .subtract(socialInsurance)
                        .subtract(housingFund)
                        .subtract(tax)
                        .setScale(MONEY_SCALE, RoundingMode.HALF_UP);

        calc.setAllowanceEnc(toPlain(allowance));
        calc.setAttendanceDeductionEnc(toPlain(attendanceDeduction));
        calc.setPerformanceCoefficient(coefficient.setScale(COEFFICIENT_SCALE, RoundingMode.HALF_UP));
        calc.setPerformanceAmountEnc(toPlain(performanceAmount));
        calc.setSocialInsuranceEnc(toPlain(socialInsurance));
        calc.setHousingFundEnc(toPlain(housingFund));
        calc.setTaxEnc(toPlain(tax));
        calc.setGrossPayEnc(toPlain(grossPay));
        calc.setNetPayEnc(toPlain(netPay));
        calcMapper.updateById(calc);

        bpmAuditService.record(
                AUDIT_TARGET,
                calc.getId(),
                AuditAction.ADJUST,
                beforeSnapshot,
                amountsSnapshot(calc),
                AUDIT_MASKED_FIELDS,
                SecurityUtils.getCurrentUserIdOrDefault());
        log.info(
                "月度核算 {} 手工调整完成，员工 {}，月份 {}",
                calc.getId(),
                calc.getEmployeeId(),
                calc.getCalcMonth());
        return toVO(calc, employeeMapper.selectById(calc.getEmployeeId()));
    }

    // ------------------------------------------------------------------
    // 批量确认
    // ------------------------------------------------------------------

    @Override
    @Transactional
    public int confirm(PayrollConfirmRequest request) {
        YearMonth month = parseMonth(request.getCalcMonth());
        LambdaQueryWrapper<HrSalaryCalc> wrapper =
                new LambdaQueryWrapper<HrSalaryCalc>()
                        .eq(HrSalaryCalc::getCalcMonth, month.toString())
                        .eq(HrSalaryCalc::getStatus, PayrollStatus.DRAFT);
        if (request.getEmployeeIds() != null && !request.getEmployeeIds().isEmpty()) {
            wrapper.in(HrSalaryCalc::getEmployeeId, request.getEmployeeIds());
        }
        List<HrSalaryCalc> drafts = calcMapper.selectList(wrapper);
        if (drafts.isEmpty()) {
            log.info("月度工资核算批量确认：月份 {}，无待确认记录", month);
            return 0;
        }
        Long operatorId = SecurityUtils.getCurrentUserIdOrDefault();
        LocalDateTime now = LocalDateTime.now();
        for (HrSalaryCalc calc : drafts) {
            calc.setStatus(PayrollStatus.CONFIRMED);
            calc.setConfirmedBy(operatorId);
            calc.setConfirmedAt(now);
            calcMapper.updateById(calc);
        }
        Map<String, Object> after = new LinkedHashMap<>();
        after.put("calcMonth", month.toString());
        after.put("confirmedCount", drafts.size());
        bpmAuditService.record(
                AUDIT_TARGET, null, AuditAction.CONFIRM, null, after, null, operatorId);
        log.info("月度工资核算批量确认：月份 {}，确认 {} 条", month, drafts.size());
        return drafts.size();
    }

    // ------------------------------------------------------------------
    // 通用装配
    // ------------------------------------------------------------------

    @Override
    public PayrollCostSummaryVO costSummary(String month, Long deptId) {
        YearMonth parsed = parseMonth(month);
        String monthText = parsed.toString();
        PayrollCostSummaryVO summary = new PayrollCostSummaryVO();
        summary.setMonth(monthText);
        initZeroAmounts(summary);

        // 部门过滤：先解析员工集合，部门下无员工时直接返回全 0
        List<Long> employeeIds = null;
        if (deptId != null) {
            employeeIds =
                    employeeMapper
                            .selectList(
                                    new LambdaQueryWrapper<HrEmployee>()
                                            .eq(HrEmployee::getDeptId, deptId))
                            .stream()
                            .map(HrEmployee::getId)
                            .filter(Objects::nonNull)
                            .toList();
            if (employeeIds.isEmpty()) {
                return summary;
            }
        }

        LambdaQueryWrapper<HrSalaryCalc> wrapper =
                new LambdaQueryWrapper<HrSalaryCalc>()
                        .eq(HrSalaryCalc::getCalcMonth, monthText)
                        .ne(HrSalaryCalc::getStatus, PayrollStatus.DRAFT);
        if (employeeIds != null) {
            wrapper.in(HrSalaryCalc::getEmployeeId, employeeIds);
        }
        List<HrSalaryCalc> calcs = calcMapper.selectList(wrapper);

        // 一次性批量载入员工，得到 employeeId → deptId，避免按部门逐次查询
        Map<Long, Long> employeeDept = loadEmployeeDeptMap(calcs);
        Map<Long, DeptCostAccumulator> deptCosts = new LinkedHashMap<>();

        BigDecimal grossPay = BigDecimal.ZERO;
        BigDecimal netPay = BigDecimal.ZERO;
        BigDecimal socialInsurance = BigDecimal.ZERO;
        BigDecimal housingFund = BigDecimal.ZERO;
        BigDecimal tax = BigDecimal.ZERO;
        Set<Long> headcountIds = new HashSet<>();
        for (HrSalaryCalc calc : calcs) {
            // 双保险：草稿不计入成本口径
            if (calc.getStatus() == PayrollStatus.DRAFT) {
                continue;
            }
            BigDecimal gross = nvl(parseAmount(calc.getGrossPayEnc()));
            BigDecimal net = nvl(parseAmount(calc.getNetPayEnc()));
            BigDecimal social = nvl(parseAmount(calc.getSocialInsuranceEnc()));
            BigDecimal fund = nvl(parseAmount(calc.getHousingFundEnc()));
            BigDecimal personalTax = nvl(parseAmount(calc.getTaxEnc()));
            grossPay = grossPay.add(gross);
            netPay = netPay.add(net);
            socialInsurance = socialInsurance.add(social);
            housingFund = housingFund.add(fund);
            tax = tax.add(personalTax);
            Long employeeId = calc.getEmployeeId();
            if (employeeId != null) {
                headcountIds.add(employeeId);
            }
            Long costDeptId = employeeId == null ? null : employeeDept.get(employeeId);
            DeptCostAccumulator accumulator =
                    deptCosts.computeIfAbsent(costDeptId, DeptCostAccumulator::new);
            accumulator.add(employeeId, gross, net, social, fund, personalTax);
        }

        long headcount = headcountIds.size();
        summary.setHeadcount(headcount);
        summary.setGrossPay(money(grossPay));
        summary.setNetPay(money(netPay));
        summary.setSocialInsurance(money(socialInsurance));
        summary.setHousingFund(money(housingFund));
        summary.setTax(money(tax));
        summary.setAvgNetPay(
                headcount == 0
                        ? money(BigDecimal.ZERO)
                        : netPay.divide(
                                BigDecimal.valueOf(headcount), MONEY_SCALE, RoundingMode.HALF_UP));
        summary.setByDept(buildDeptCosts(deptCosts));
        log.info("人力成本汇总：月份 {}，部门 {}，人数 {}，记录 {} 条", monthText, deptId, headcount, calcs.size());
        return summary;
    }

    /** 批量载入员工所属部门；员工不存在或部门为空时按未分配部门处理。 */
    private Map<Long, Long> loadEmployeeDeptMap(Collection<HrSalaryCalc> calcs) {
        Map<Long, Long> employeeDept = new HashMap<>();
        Set<Long> employeeIds = new HashSet<>();
        for (HrSalaryCalc calc : calcs) {
            if (calc.getEmployeeId() != null) {
                employeeIds.add(calc.getEmployeeId());
            }
        }
        if (employeeIds.isEmpty()) {
            return employeeDept;
        }
        List<HrEmployee> employees = employeeMapper.selectBatchIds(employeeIds);
        if (employees == null) {
            return employeeDept;
        }
        for (HrEmployee employee : employees) {
            if (employee != null && employee.getId() != null) {
                employeeDept.put(employee.getId(), employee.getDeptId());
            }
        }
        return employeeDept;
    }

    /** 组装按部门分组明细：部门 ID 升序，未分配部门（deptId 为空）排在最后。 */
    private List<PayrollCostDeptVO> buildDeptCosts(Map<Long, DeptCostAccumulator> deptCosts) {
        if (deptCosts.isEmpty()) {
            return new ArrayList<>();
        }
        Map<Long, String> deptNames = loadDeptNames(deptCosts.keySet().stream().filter(Objects::nonNull).toList());
        List<PayrollCostDeptVO> result = new ArrayList<>(deptCosts.size());
        for (DeptCostAccumulator accumulator : deptCosts.values()) {
            result.add(accumulator.toVO(deptNames));
        }
        result.sort(
                Comparator.comparing(
                        PayrollCostDeptVO::getDeptId, Comparator.nullsLast(Comparator.naturalOrder())));
        return result;
    }

    /** 批量查部门名称；部门不存在或名称为空由调用方回落空串。 */
    private Map<Long, String> loadDeptNames(Collection<Long> deptIds) {
        if (deptIds == null || deptIds.isEmpty()) {
            return Map.of();
        }
        List<SysDept> depts = deptMapper.selectBatchIds(deptIds);
        if (depts == null) {
            return Map.of();
        }
        Map<Long, String> names = new HashMap<>();
        for (SysDept dept : depts) {
            if (dept != null && dept.getId() != null && StringUtils.isNotBlank(dept.getName())) {
                names.put(dept.getId(), dept.getName());
            }
        }
        return names;
    }

    /** 全 0 初始化（金额统一 2 位小数，避免前端出现 null）。 */
    private void initZeroAmounts(PayrollCostSummaryVO summary) {
        summary.setGrossPay(money(BigDecimal.ZERO));
        summary.setNetPay(money(BigDecimal.ZERO));
        summary.setSocialInsurance(money(BigDecimal.ZERO));
        summary.setHousingFund(money(BigDecimal.ZERO));
        summary.setTax(money(BigDecimal.ZERO));
        summary.setAvgNetPay(money(BigDecimal.ZERO));
    }

    private SalaryCalcVO toVO(HrSalaryCalc calc, HrEmployee employee) {
        SalaryCalcVO vo = new SalaryCalcVO();
        vo.setId(calc.getId());
        vo.setEmployeeId(calc.getEmployeeId());
        if (employee != null) {
            vo.setEmployeeNo(employee.getEmployeeNo());
            vo.setEmployeeName(employee.getName());
        }
        vo.setCalcMonth(calc.getCalcMonth());
        vo.setBaseSalary(parseAmount(calc.getBaseSalaryEnc()));
        vo.setPostSalary(parseAmount(calc.getPostSalaryEnc()));
        vo.setAttendanceDays(calc.getAttendanceDays());
        vo.setAbsentDays(calc.getAbsentDays());
        vo.setLateCount(calc.getLateCount());
        vo.setAttendanceDeduction(parseAmount(calc.getAttendanceDeductionEnc()));
        vo.setPerformanceCoefficient(calc.getPerformanceCoefficient());
        vo.setPerformanceAmount(parseAmount(calc.getPerformanceAmountEnc()));
        vo.setAllowance(parseAmount(calc.getAllowanceEnc()));
        vo.setSocialInsurance(parseAmount(calc.getSocialInsuranceEnc()));
        vo.setHousingFund(parseAmount(calc.getHousingFundEnc()));
        vo.setTax(parseAmount(calc.getTaxEnc()));
        vo.setGrossPay(parseAmount(calc.getGrossPayEnc()));
        vo.setNetPay(parseAmount(calc.getNetPayEnc()));
        vo.setStatus(calc.getStatus());
        vo.setConfirmedBy(calc.getConfirmedBy());
        vo.setConfirmedAt(calc.getConfirmedAt());
        vo.setCreateTime(calc.getCreateTime());
        vo.setUpdateTime(calc.getUpdateTime());
        return vo;
    }

    private Map<Long, HrEmployee> loadEmployees(List<HrSalaryCalc> calcs) {
        List<Long> employeeIds =
                calcs.stream()
                        .map(HrSalaryCalc::getEmployeeId)
                        .filter(Objects::nonNull)
                        .distinct()
                        .toList();
        if (employeeIds.isEmpty()) {
            return Map.of();
        }
        return employeeMapper.selectBatchIds(employeeIds).stream()
                .collect(Collectors.toMap(HrEmployee::getId, Function.identity(), (a, b) -> a));
    }

    /** 审计快照：金额先加密再写入 JSON，表中不出现明文金额。 */
    private Map<String, Object> amountsSnapshot(HrSalaryCalc calc) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("baseSalary", encryptAmount(calc.getBaseSalaryEnc()));
        snapshot.put("postSalary", encryptAmount(calc.getPostSalaryEnc()));
        snapshot.put("attendanceDeduction", encryptAmount(calc.getAttendanceDeductionEnc()));
        snapshot.put("performanceAmount", encryptAmount(calc.getPerformanceAmountEnc()));
        snapshot.put("allowance", encryptAmount(calc.getAllowanceEnc()));
        snapshot.put("socialInsurance", encryptAmount(calc.getSocialInsuranceEnc()));
        snapshot.put("housingFund", encryptAmount(calc.getHousingFundEnc()));
        snapshot.put("tax", encryptAmount(calc.getTaxEnc()));
        snapshot.put("grossPay", encryptAmount(calc.getGrossPayEnc()));
        snapshot.put("netPay", encryptAmount(calc.getNetPayEnc()));
        snapshot.put("performanceCoefficient", calc.getPerformanceCoefficient());
        snapshot.put("status", calc.getStatus());
        return snapshot;
    }

    private Object encryptAmount(String plaintext) {
        return StringUtils.isBlank(plaintext) ? null : encryptor.encrypt(plaintext);
    }

    private HrSalaryCalc requireCalc(Long id) {
        HrSalaryCalc calc = id == null ? null : calcMapper.selectById(id);
        if (calc == null) {
            throw BusinessException.of(ErrorCode.BIZ_NOT_FOUND, "工资核算记录不存在");
        }
        return calc;
    }

    private HrSalary selectActiveSalary(Long employeeId) {
        if (employeeId == null) {
            return null;
        }
        return salaryMapper.selectOne(
                new LambdaQueryWrapper<HrSalary>()
                        .eq(HrSalary::getEmployeeId, employeeId)
                        .eq(HrSalary::getStatus, SalaryStatus.ACTIVE)
                        .last("LIMIT 1"));
    }

    private boolean isNoAdjustment(PayrollAdjustRequest request) {
        return request.getAllowance() == null
                && request.getAttendanceDeduction() == null
                && request.getPerformanceCoefficient() == null
                && request.getSocialInsurance() == null
                && request.getHousingFund() == null
                && request.getTax() == null;
    }

    private void requireNonNegative(BigDecimal value, String label) {
        if (value != null && value.compareTo(BigDecimal.ZERO) < 0) {
            throw BusinessException.of(ErrorCode.VALID_ILLEGAL_ARGUMENT, label + "不能为负数");
        }
    }

    private YearMonth parseMonth(String value) {
        if (StringUtils.isBlank(value)) {
            throw BusinessException.of(ErrorCode.VALID_REQUIRED_FIELD, "核算月不能为空");
        }
        try {
            return YearMonth.parse(value.trim());
        } catch (DateTimeParseException e) {
            throw BusinessException.of(
                    ErrorCode.VALID_ILLEGAL_ARGUMENT, "核算月格式应为 yyyy-MM：" + value);
        }
    }

    private String employeeLabel(HrEmployee employee) {
        if (employee == null) {
            return "员工未知：";
        }
        return StringUtils.isBlank(employee.getName())
                ? "员工 " + employee.getId() + "："
                : "员工【" + employee.getName() + "】：";
    }

    private String statusLabel(PayrollStatus status) {
        if (status == null) {
            return "状态未知";
        }
        return switch (status) {
            case DRAFT -> "草稿";
            case CONFIRMED -> "已确认";
            case LOCKED -> "已锁定";
        };
    }

    private boolean isLater(HrSalary candidate, HrSalary current) {
        if (candidate.getEffectiveDate() == null) {
            return false;
        }
        if (current.getEffectiveDate() == null) {
            return true;
        }
        return candidate.getEffectiveDate().isAfter(current.getEffectiveDate());
    }

    /** 绩效考评单新旧比较：创建时间优先，其次主键 ID；空值视为最旧。 */
    private boolean isNewer(HrPerformanceReview candidate, HrPerformanceReview current) {
        if (current == null) {
            return true;
        }
        LocalDateTime candidateTime = candidate.getCreateTime();
        LocalDateTime currentTime = current.getCreateTime();
        if (candidateTime != null && currentTime != null) {
            int compared = candidateTime.compareTo(currentTime);
            if (compared != 0) {
                return compared > 0;
            }
        } else if (candidateTime != null) {
            return true;
        } else if (currentTime != null) {
            return false;
        }
        Long candidateId = candidate.getId();
        Long currentId = current.getId();
        return candidateId != null && (currentId == null || candidateId > currentId);
    }

    /** 金额解析：实体字段在 TypeHandler 解密后为明文数值字符串。 */
    private BigDecimal parseAmount(String value) {
        return StringUtils.isBlank(value) ? null : new BigDecimal(value.trim());
    }

    private BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private BigDecimal money(BigDecimal value) {
        return value.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    private String toPlain(BigDecimal value) {
        return value == null ? null : value.toPlainString();
    }

    /** 批次内员工与其生效薪资。 */
    private record EmployeeSalary(HrEmployee employee, HrSalary salary) {}

    /** 考勤汇总：缺勤天数与迟到次数。 */
    private record AttendanceSummary(BigDecimal absentDays, int lateCount) {}

    /** 请假汇总：事假与病假天数。 */
    private record LeaveSummary(BigDecimal personalDays, BigDecimal sickDays) {}

    /** 单个批次处理结果。 */
    private record BatchOutcome(int created, int recalculated, int skipped) {}

    /** 部门成本累加器：同一趟循环内完成分组，避免按部门重复查询。 */
    private final class DeptCostAccumulator {

        private final Long deptId;
        private final Set<Long> employeeIds = new HashSet<>();
        private BigDecimal grossPay = BigDecimal.ZERO;
        private BigDecimal netPay = BigDecimal.ZERO;
        private BigDecimal socialInsurance = BigDecimal.ZERO;
        private BigDecimal housingFund = BigDecimal.ZERO;
        private BigDecimal tax = BigDecimal.ZERO;

        private DeptCostAccumulator(Long deptId) {
            this.deptId = deptId;
        }

        private void add(
                Long employeeId,
                BigDecimal gross,
                BigDecimal net,
                BigDecimal social,
                BigDecimal fund,
                BigDecimal personalTax) {
            if (employeeId != null) {
                employeeIds.add(employeeId);
            }
            grossPay = grossPay.add(nvl(gross));
            netPay = netPay.add(nvl(net));
            socialInsurance = socialInsurance.add(nvl(social));
            housingFund = housingFund.add(nvl(fund));
            tax = tax.add(nvl(personalTax));
        }

        private PayrollCostDeptVO toVO(Map<Long, String> deptNames) {
            PayrollCostDeptVO vo = new PayrollCostDeptVO();
            vo.setDeptId(deptId);
            if (deptId == null) {
                vo.setDeptName("未分配部门");
            } else {
                vo.setDeptName(deptNames.getOrDefault(deptId, ""));
            }
            long deptHeadcount = employeeIds.size();
            vo.setHeadcount(deptHeadcount);
            vo.setGrossPay(money(grossPay));
            vo.setNetPay(money(netPay));
            vo.setSocialInsurance(money(socialInsurance));
            vo.setHousingFund(money(housingFund));
            vo.setTax(money(tax));
            vo.setAvgNetPay(
                    deptHeadcount == 0
                            ? money(BigDecimal.ZERO)
                            : netPay.divide(
                                    BigDecimal.valueOf(deptHeadcount),
                                    MONEY_SCALE,
                                    RoundingMode.HALF_UP));
            return vo;
        }
    }
}
