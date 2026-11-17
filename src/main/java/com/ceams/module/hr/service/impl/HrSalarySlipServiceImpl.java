package com.ceams.module.hr.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ceams.common.exception.BusinessException;
import com.ceams.common.exception.ErrorCode;
import com.ceams.common.response.PageResult;
import com.ceams.common.security.SecurityUtils;
import com.ceams.module.bpm.enums.AuditAction;
import com.ceams.module.bpm.service.BpmAuditService;
import com.ceams.module.hr.dto.SalarySlipPublishRequest;
import com.ceams.module.hr.entity.HrEmployee;
import com.ceams.module.hr.entity.HrSalaryCalc;
import com.ceams.module.hr.entity.HrSalarySlip;
import com.ceams.module.hr.enums.PayrollStatus;
import com.ceams.module.hr.enums.SalarySlipReadStatus;
import com.ceams.module.hr.mapper.HrEmployeeMapper;
import com.ceams.module.hr.mapper.HrSalaryCalcMapper;
import com.ceams.module.hr.mapper.HrSalarySlipMapper;
import com.ceams.module.hr.service.HrEmployeeService;
import com.ceams.module.hr.service.HrSalarySlipService;
import com.ceams.module.hr.support.SalarySlipCodec;
import com.ceams.module.hr.support.SalarySlipCodec.SalarySlipSnapshot;
import com.ceams.module.hr.support.SimpleXlsxWriter;
import com.ceams.module.hr.util.HrPageUtil;
import com.ceams.module.hr.vo.SalarySlipPublishResultVO;
import com.ceams.module.hr.vo.SalarySlipVO;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
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
 * 工资条服务实现。
 *
 * <p>发布口径：
 * <ul>
 *   <li>仅处理该月 status=CONFIRMED 的核算记录；DRAFT 不发布，计入 skipped 并给出中文告警；
 *   <li>按唯一键 (employee_id, calc_month) 先查后插，已存在的工资条跳过并计数，保证重复发布幂等；
 *   <li>明细金额加密后写入 content_json，姓名 / 工号等非金额字段明文可读。
 * </ul>
 *
 * <p>日志只输出月份与条数，禁止输出任何明文金额。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HrSalarySlipServiceImpl implements HrSalarySlipService {

    /** 导出列顺序：工号、姓名、核算月、十项金额。 */
    private static final List<String> EXPORT_HEADERS =
            List.of(
                    "工号",
                    "姓名",
                    "核算月",
                    "基本工资",
                    "岗位工资",
                    "绩效工资",
                    "补贴",
                    "考勤扣款",
                    "社保",
                    "公积金",
                    "个税",
                    "应发合计",
                    "实发合计");

    private final HrSalarySlipMapper slipMapper;
    private final HrSalaryCalcMapper calcMapper;
    private final HrEmployeeMapper employeeMapper;
    private final HrEmployeeService employeeService;
    private final SalarySlipCodec codec;
    private final BpmAuditService bpmAuditService;

    /** 审计目标类型：工资条（按月批量发布，targetId 为空）。 */
    private static final String AUDIT_TARGET = "HR_SALARY_SLIP";

    // ------------------------------------------------------------------
    // 生成发布
    // ------------------------------------------------------------------

    @Override
    @Transactional
    public SalarySlipPublishResultVO publish(SalarySlipPublishRequest request) {
        YearMonth month = parseMonth(request.getCalcMonth());
        String monthText = month.toString();
        List<HrSalaryCalc> calcs = loadMonthCalcs(monthText, request.getEmployeeIds());
        if (calcs.isEmpty()) {
            log.info("工资条发布：月份 {} 未命中任何核算记录", monthText);
            return new SalarySlipPublishResultVO(monthText, 0, 0, 0, new ArrayList<>());
        }

        Map<Long, HrEmployee> employees = loadEmployees(calcs);
        Map<Long, HrSalarySlip> existing = loadExistingSlips(monthText, calcs);
        LocalDateTime now = LocalDateTime.now();
        int draftCount = 0;
        int lockedCount = 0;
        int published = 0;
        int skipped = 0;
        for (HrSalaryCalc calc : calcs) {
            if (calc.getStatus() != PayrollStatus.CONFIRMED) {
                skipped++;
                if (calc.getStatus() == PayrollStatus.DRAFT) {
                    draftCount++;
                } else {
                    lockedCount++;
                }
                continue;
            }
            if (calc.getEmployeeId() == null || existing.containsKey(calc.getEmployeeId())) {
                skipped++;
                continue;
            }
            HrSalarySlip slip = new HrSalarySlip();
            slip.setEmployeeId(calc.getEmployeeId());
            slip.setCalcId(calc.getId());
            slip.setCalcMonth(monthText);
            slip.setContentJson(codec.encode(calc, employees.get(calc.getEmployeeId())));
            slip.setPublishedAt(now);
            slip.setReadStatus(SalarySlipReadStatus.PENDING);
            slipMapper.insert(slip);
            published++;
        }

        List<String> warnings = new ArrayList<>();
        if (draftCount > 0) {
            warnings.add("存在未确认的核算记录，已跳过");
        }
        if (lockedCount > 0) {
            warnings.add("存在已锁定的核算记录，已跳过");
        }
        log.info(
                "工资条发布完成：月份 {}，总数 {}，发布 {}，跳过 {}",
                monthText,
                calcs.size(),
                published,
                skipped);
        // 工资条发布属敏感动作，按月写全局审计快照（不含任何明文金额）
        Map<String, Object> after = new LinkedHashMap<>();
        after.put("calcMonth", monthText);
        after.put("publishedCount", published);
        after.put("skippedCount", skipped);
        bpmAuditService.record(
                AUDIT_TARGET,
                null,
                AuditAction.PUBLISH,
                null,
                after,
                null,
                SecurityUtils.getCurrentUserIdOrDefault());
        return new SalarySlipPublishResultVO(monthText, calcs.size(), published, skipped, warnings);
    }

    // ------------------------------------------------------------------
    // 查询 / 已读
    // ------------------------------------------------------------------

    @Override
    public PageResult<SalarySlipVO> page(
            long page,
            long size,
            String calcMonth,
            Long employeeId,
            SalarySlipReadStatus readStatus) {
        Page<HrSalarySlip> p = HrPageUtil.resolve(page, size);
        LambdaQueryWrapper<HrSalarySlip> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.isNotBlank(calcMonth)) {
            wrapper.eq(HrSalarySlip::getCalcMonth, parseMonth(calcMonth).toString());
        }
        if (employeeId != null) {
            wrapper.eq(HrSalarySlip::getEmployeeId, employeeId);
        }
        if (readStatus != null) {
            wrapper.eq(HrSalarySlip::getReadStatus, readStatus);
        }
        wrapper.orderByDesc(HrSalarySlip::getCalcMonth).orderByAsc(HrSalarySlip::getEmployeeId);
        IPage<HrSalarySlip> result = slipMapper.selectPage(p, wrapper);
        List<SalarySlipVO> records =
                result.getRecords().stream().map(this::toVO).toList();
        return new PageResult<>(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    @Transactional
    public List<SalarySlipVO> listMine() {
        Long employeeId = employeeService.currentEmployeeId();
        List<HrSalarySlip> slips =
                slipMapper.selectList(
                        new LambdaQueryWrapper<HrSalarySlip>()
                                .eq(HrSalarySlip::getEmployeeId, employeeId)
                                .orderByDesc(HrSalarySlip::getCalcMonth)
                                .orderByDesc(HrSalarySlip::getCreateTime));
        // 本人查看列表即视为已读：待读工资条回写 READ，避免重复展示未读标记
        LocalDateTime now = LocalDateTime.now();
        for (HrSalarySlip slip : slips) {
            if (slip.getReadStatus() != SalarySlipReadStatus.READ) {
                slip.setReadStatus(SalarySlipReadStatus.READ);
                slip.setReadAt(now);
                slipMapper.updateById(slip);
            }
        }
        return slips.stream().map(this::toVO).toList();
    }

    @Override
    @Transactional
    public SalarySlipVO markRead(Long id) {
        HrSalarySlip slip = requireSlip(id);
        Long employeeId = employeeService.currentEmployeeId();
        if (!Objects.equals(slip.getEmployeeId(), employeeId)) {
            throw BusinessException.of(ErrorCode.AUTH_ACCESS_DENIED, "只能标记本人工资条为已读");
        }
        if (slip.getReadStatus() == SalarySlipReadStatus.READ) {
            return toVO(slip);
        }
        slip.setReadStatus(SalarySlipReadStatus.READ);
        slip.setReadAt(LocalDateTime.now());
        slipMapper.updateById(slip);
        log.info("工资条 {} 已读回写完成，员工 {}", slip.getId(), employeeId);
        return toVO(slip);
    }

    // ------------------------------------------------------------------
    // 导出
    // ------------------------------------------------------------------

    @Override
    public void export(String calcMonth, OutputStream out) {
        YearMonth month = parseMonth(calcMonth);
        List<HrSalarySlip> slips =
                slipMapper.selectList(
                        new LambdaQueryWrapper<HrSalarySlip>()
                                .eq(HrSalarySlip::getCalcMonth, month.toString())
                                .orderByAsc(HrSalarySlip::getEmployeeId));
        List<List<Object>> rows = new ArrayList<>(slips.size());
        for (HrSalarySlip slip : slips) {
            SalarySlipSnapshot snapshot = codec.decode(slip.getContentJson());
            List<Object> row = new ArrayList<>(EXPORT_HEADERS.size());
            row.add(snapshot == null ? null : snapshot.employeeNo());
            row.add(snapshot == null ? null : snapshot.employeeName());
            row.add(month.toString());
            row.add(snapshot == null ? null : snapshot.baseSalary());
            row.add(snapshot == null ? null : snapshot.postSalary());
            row.add(snapshot == null ? null : snapshot.performanceAmount());
            row.add(snapshot == null ? null : snapshot.allowance());
            row.add(snapshot == null ? null : snapshot.attendanceDeduction());
            row.add(snapshot == null ? null : snapshot.socialInsurance());
            row.add(snapshot == null ? null : snapshot.housingFund());
            row.add(snapshot == null ? null : snapshot.tax());
            row.add(snapshot == null ? null : snapshot.grossPay());
            row.add(snapshot == null ? null : snapshot.netPay());
            rows.add(row);
        }
        SimpleXlsxWriter.write(out, EXPORT_HEADERS, rows);
        log.info("工资条导出完成：月份 {}，条数 {}", month, rows.size());
    }

    // ------------------------------------------------------------------
    // 通用装配
    // ------------------------------------------------------------------

    private SalarySlipVO toVO(HrSalarySlip slip) {
        SalarySlipVO vo = new SalarySlipVO();
        vo.setId(slip.getId());
        vo.setEmployeeId(slip.getEmployeeId());
        vo.setCalcId(slip.getCalcId());
        vo.setCalcMonth(slip.getCalcMonth());
        vo.setReadStatus(slip.getReadStatus());
        vo.setPublishedAt(slip.getPublishedAt());
        vo.setReadAt(slip.getReadAt());
        vo.setCreateTime(slip.getCreateTime());
        SalarySlipSnapshot snapshot = codec.decode(slip.getContentJson());
        if (snapshot == null) {
            return vo;
        }
        vo.setEmployeeNo(snapshot.employeeNo());
        vo.setEmployeeName(snapshot.employeeName());
        vo.setDeptId(snapshot.deptId());
        vo.setBaseSalary(snapshot.baseSalary());
        vo.setPostSalary(snapshot.postSalary());
        vo.setPerformanceAmount(snapshot.performanceAmount());
        vo.setAllowance(snapshot.allowance());
        vo.setAttendanceDeduction(snapshot.attendanceDeduction());
        vo.setSocialInsurance(snapshot.socialInsurance());
        vo.setHousingFund(snapshot.housingFund());
        vo.setTax(snapshot.tax());
        vo.setGrossPay(snapshot.grossPay());
        vo.setNetPay(snapshot.netPay());
        vo.setAttendanceDays(snapshot.attendanceDays());
        vo.setAbsentDays(snapshot.absentDays());
        vo.setLateCount(snapshot.lateCount());
        vo.setPerformanceCoefficient(snapshot.performanceCoefficient());
        return vo;
    }

    /** 该月核算记录（含 DRAFT / LOCKED，用于跳过计数与告警）。 */
    private List<HrSalaryCalc> loadMonthCalcs(String monthText, List<Long> employeeIds) {
        LambdaQueryWrapper<HrSalaryCalc> wrapper =
                new LambdaQueryWrapper<HrSalaryCalc>().eq(HrSalaryCalc::getCalcMonth, monthText);
        if (employeeIds != null && !employeeIds.isEmpty()) {
            wrapper.in(HrSalaryCalc::getEmployeeId, employeeIds);
        }
        wrapper.orderByAsc(HrSalaryCalc::getEmployeeId);
        return calcMapper.selectList(wrapper);
    }

    /** 该月已存在的工资条，按员工 ID 索引，用于幂等跳过。 */
    private Map<Long, HrSalarySlip> loadExistingSlips(
            String monthText, List<HrSalaryCalc> calcs) {
        List<Long> employeeIds =
                calcs.stream()
                        .map(HrSalaryCalc::getEmployeeId)
                        .filter(Objects::nonNull)
                        .distinct()
                        .toList();
        if (employeeIds.isEmpty()) {
            return Map.of();
        }
        List<HrSalarySlip> slips =
                slipMapper.selectList(
                        new LambdaQueryWrapper<HrSalarySlip>()
                                .eq(HrSalarySlip::getCalcMonth, monthText)
                                .in(HrSalarySlip::getEmployeeId, employeeIds));
        Map<Long, HrSalarySlip> existing = new HashMap<>(slips.size());
        for (HrSalarySlip slip : slips) {
            existing.putIfAbsent(slip.getEmployeeId(), slip);
        }
        return existing;
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

    private HrSalarySlip requireSlip(Long id) {
        HrSalarySlip slip = id == null ? null : slipMapper.selectById(id);
        if (slip == null) {
            throw BusinessException.of(ErrorCode.BIZ_NOT_FOUND, "工资条不存在");
        }
        return slip;
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
}
