package com.ceams.module.hr.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ceams.common.exception.BusinessException;
import com.ceams.common.exception.ErrorCode;
import com.ceams.common.response.PageResult;
import com.ceams.module.hr.dto.SalaryCreateRequest;
import com.ceams.module.hr.entity.HrEmployee;
import com.ceams.module.hr.entity.HrSalary;
import com.ceams.module.hr.enums.SalaryStatus;
import com.ceams.module.hr.mapper.HrEmployeeMapper;
import com.ceams.module.hr.mapper.HrSalaryMapper;
import com.ceams.module.hr.service.HrEmployeeService;
import com.ceams.module.hr.service.HrSalaryService;
import com.ceams.module.hr.util.HrPageUtil;
import com.ceams.module.hr.vo.SalaryVO;
import java.math.BigDecimal;
import java.math.RoundingMode;
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

/** 薪资台账服务实现。 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HrSalaryServiceImpl implements HrSalaryService {

    /** 金额统一保留 2 位小数。 */
    private static final int MONEY_SCALE = 2;

    private final HrSalaryMapper salaryMapper;
    private final HrEmployeeMapper employeeMapper;
    private final HrEmployeeService employeeService;

    @Override
    @Transactional
    public String createSalary(SalaryCreateRequest request) {
        if (request.getEffectiveDate() == null) {
            throw BusinessException.of(ErrorCode.VALID_REQUIRED_FIELD, "生效日期不能为空");
        }
        requireEmployee(request.getEmployeeId());
        Long activeCount =
                salaryMapper.selectCount(
                        new LambdaQueryWrapper<HrSalary>()
                                .eq(HrSalary::getEmployeeId, request.getEmployeeId())
                                .eq(HrSalary::getStatus, SalaryStatus.ACTIVE));
        if (activeCount != null && activeCount > 0) {
            throw BusinessException.of(
                    ErrorCode.BIZ_STATE_CONFLICT, "该员工已存在生效薪资，请通过薪资变动申请调整");
        }
        BigDecimal baseSalary = normalizeAmount(request.getBaseSalary(), "基本工资", true);
        HrSalary salary = new HrSalary();
        salary.setEmployeeId(request.getEmployeeId());
        salary.setEffectiveDate(request.getEffectiveDate());
        salary.setBaseSalaryEnc(toPlain(baseSalary));
        salary.setPostSalaryEnc(
                toPlain(normalizeAmount(request.getPostSalary(), "岗位工资", false)));
        salary.setPerformanceBaseEnc(
                toPlain(normalizeAmount(request.getPerformanceBase(), "绩效基数", false)));
        salary.setAllowanceEnc(toPlain(normalizeAmount(request.getAllowance(), "固定补贴", false)));
        salary.setStatus(SalaryStatus.ACTIVE);
        salary.setRemark(request.getRemark());
        salaryMapper.insert(salary);
        log.info(
                "员工 {} 首次建立薪资台账 {}，生效日期 {}",
                salary.getEmployeeId(),
                salary.getId(),
                salary.getEffectiveDate());
        return String.valueOf(salary.getId());
    }

    @Override
    public SalaryVO mySalary() {
        Long employeeId = employeeService.currentEmployeeId();
        HrSalary salary = selectActive(employeeId);
        if (salary == null) {
            throw BusinessException.of(ErrorCode.BIZ_NOT_FOUND, "当前员工暂无生效薪资");
        }
        return toVO(salary, employeeMapper.selectById(employeeId));
    }

    @Override
    public PageResult<SalaryVO> page(
            long page, long size, Long employeeId, SalaryStatus status) {
        Page<HrSalary> p = HrPageUtil.resolve(page, size);
        LambdaQueryWrapper<HrSalary> wrapper = new LambdaQueryWrapper<>();
        if (employeeId != null) {
            wrapper.eq(HrSalary::getEmployeeId, employeeId);
        }
        if (status != null) {
            wrapper.eq(HrSalary::getStatus, status);
        }
        wrapper.orderByDesc(HrSalary::getEffectiveDate).orderByDesc(HrSalary::getCreateTime);
        IPage<HrSalary> result = salaryMapper.selectPage(p, wrapper);
        Map<Long, HrEmployee> employees = loadEmployees(result.getRecords());
        List<SalaryVO> records =
                result.getRecords().stream()
                        .map(salary -> toVO(salary, employees.get(salary.getEmployeeId())))
                        .toList();
        return new PageResult<>(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    /** 查询员工当前生效薪资（唯一键保证最多一条）。 */
    private HrSalary selectActive(Long employeeId) {
        return salaryMapper.selectOne(
                new LambdaQueryWrapper<HrSalary>()
                        .eq(HrSalary::getEmployeeId, employeeId)
                        .eq(HrSalary::getStatus, SalaryStatus.ACTIVE)
                        .last("LIMIT 1"));
    }

    private Map<Long, HrEmployee> loadEmployees(List<HrSalary> salaries) {
        List<Long> employeeIds =
                salaries.stream()
                        .map(HrSalary::getEmployeeId)
                        .filter(Objects::nonNull)
                        .distinct()
                        .toList();
        if (employeeIds.isEmpty()) {
            return Map.of();
        }
        return employeeMapper.selectBatchIds(employeeIds).stream()
                .collect(Collectors.toMap(HrEmployee::getId, Function.identity(), (a, b) -> a));
    }

    private SalaryVO toVO(HrSalary salary, HrEmployee employee) {
        SalaryVO vo = new SalaryVO();
        vo.setId(salary.getId());
        vo.setEmployeeId(salary.getEmployeeId());
        if (employee != null) {
            vo.setEmployeeNo(employee.getEmployeeNo());
            vo.setEmployeeName(employee.getName());
        }
        vo.setEffectiveDate(salary.getEffectiveDate());
        vo.setBaseSalary(parseAmount(salary.getBaseSalaryEnc()));
        vo.setPostSalary(parseAmount(salary.getPostSalaryEnc()));
        vo.setPerformanceBase(parseAmount(salary.getPerformanceBaseEnc()));
        vo.setAllowance(parseAmount(salary.getAllowanceEnc()));
        vo.setStatus(salary.getStatus());
        vo.setChangeApplyId(salary.getChangeApplyId());
        vo.setRemark(salary.getRemark());
        vo.setCreateTime(salary.getCreateTime());
        return vo;
    }

    private HrEmployee requireEmployee(Long employeeId) {
        HrEmployee employee = employeeId == null ? null : employeeMapper.selectById(employeeId);
        if (employee == null) {
            throw BusinessException.of(ErrorCode.BIZ_NOT_FOUND, "员工档案不存在");
        }
        return employee;
    }

    /** 金额校验与精度归一：负数拒绝，统一保留 2 位（HALF_UP）。 */
    private BigDecimal normalizeAmount(BigDecimal value, String label, boolean required) {
        if (value == null) {
            if (required) {
                throw BusinessException.of(ErrorCode.VALID_REQUIRED_FIELD, label + "不能为空");
            }
            return null;
        }
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            throw BusinessException.of(ErrorCode.VALID_ILLEGAL_ARGUMENT, label + "不能为负数");
        }
        return value.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    private String toPlain(BigDecimal value) {
        return value == null ? null : value.toPlainString();
    }

    /** 库内密文经 TypeHandler 解密后为明文数值字符串，此处转为 BigDecimal。 */
    private BigDecimal parseAmount(String value) {
        return StringUtils.isBlank(value) ? null : new BigDecimal(value.trim());
    }
}
