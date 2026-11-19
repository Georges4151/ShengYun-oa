package com.ceams.module.hr.support;

import com.ceams.module.hr.config.HrPayrollProperties;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * 月度工资核算纯计算器。
 *
 * <p>设计约束：不依赖 Spring 容器、不做任何 IO、不写日志、不操作数据库，全部输入输出显式传参，
 * 便于在单元测试中覆盖各种边界口径。
 *
 * <p>精度口径：中间计算保留 4 位小数，落库金额统一 2 位小数，舍入模式 {@link RoundingMode#HALF_UP}；
 * 全程 {@link BigDecimal}，禁止 {@code double}/{@code float}。
 */
public final class PayrollCalculator {

    /** 中间计算精度。 */
    public static final int CALC_SCALE = 4;

    /** 金额落库精度。 */
    public static final int MONEY_SCALE = 2;

    /** 七级累进税率分档上限（最后一级为无上限）。 */
    private static final BigDecimal[] TAX_BRACKET_LIMITS = {
        new BigDecimal("3000"),
        new BigDecimal("12000"),
        new BigDecimal("25000"),
        new BigDecimal("35000"),
        new BigDecimal("55000"),
        new BigDecimal("80000")
    };

    /** 各级税率。 */
    private static final BigDecimal[] TAX_RATES = {
        new BigDecimal("0.03"),
        new BigDecimal("0.10"),
        new BigDecimal("0.20"),
        new BigDecimal("0.25"),
        new BigDecimal("0.30"),
        new BigDecimal("0.35"),
        new BigDecimal("0.45")
    };

    /** 各级速算扣除数。 */
    private static final BigDecimal[] TAX_QUICK_DEDUCTIONS = {
        BigDecimal.ZERO,
        new BigDecimal("210"),
        new BigDecimal("1410"),
        new BigDecimal("2660"),
        new BigDecimal("4410"),
        new BigDecimal("7160"),
        new BigDecimal("15160")
    };

    private PayrollCalculator() {}

    /**
     * 日工资 = (基本工资 + 岗位工资) / 月计薪天数，保留 4 位中间精度。
     *
     * @param baseSalary 基本工资
     * @param postSalary 岗位工资
     * @param config 核算配置
     * @return 日工资（4 位小数）
     */
    public static BigDecimal dailyWage(
            BigDecimal baseSalary, BigDecimal postSalary, HrPayrollProperties config) {
        BigDecimal total = nvl(baseSalary).add(nvl(postSalary));
        return total.divide(config.getWorkingDays(), CALC_SCALE, RoundingMode.HALF_UP);
    }

    /**
     * 考勤扣款 = 缺勤扣款 + 事假扣款 + 病假扣款 + 迟到扣款。
     *
     * <p>上限为 (基本工资 + 岗位工资)，此处以「日工资 × 月计薪天数」等价推导；超出上限时截断并回传标记，
     * 供调用方写入告警。
     *
     * @param dailyWage 日工资
     * @param absentDays 缺勤天数
     * @param personalLeaveDays 事假天数
     * @param sickLeaveDays 病假天数
     * @param lateCount 迟到 / 早退次数
     * @param config 核算配置
     * @return 扣款金额与是否截断
     */
    public static AttendanceDeduction calcAttendanceDeduction(
            BigDecimal dailyWage,
            BigDecimal absentDays,
            BigDecimal personalLeaveDays,
            BigDecimal sickLeaveDays,
            int lateCount,
            HrPayrollProperties config) {
        BigDecimal cap = dailyWage.multiply(config.getWorkingDays());
        return calcAttendanceDeduction(
                dailyWage, absentDays, personalLeaveDays, sickLeaveDays, lateCount, cap, config);
    }

    /**
     * 考勤扣款（显式指定上限，供月度核算使用精确的「基本工资 + 岗位工资」作为上限）。
     *
     * @param dailyWage 日工资
     * @param absentDays 缺勤天数
     * @param personalLeaveDays 事假天数
     * @param sickLeaveDays 病假天数
     * @param lateCount 迟到 / 早退次数
     * @param cap 扣款上限
     * @param config 核算配置
     * @return 扣款金额与是否截断
     */
    public static AttendanceDeduction calcAttendanceDeduction(
            BigDecimal dailyWage,
            BigDecimal absentDays,
            BigDecimal personalLeaveDays,
            BigDecimal sickLeaveDays,
            int lateCount,
            BigDecimal cap,
            HrPayrollProperties config) {
        BigDecimal amount =
                dailyWage
                        .multiply(nvl(absentDays))
                        .add(dailyWage.multiply(nvl(personalLeaveDays)))
                        .add(
                                dailyWage
                                        .multiply(nvl(sickLeaveDays))
                                        .multiply(nvl(config.getSickDeductionRate())))
                        .add(nvl(config.getLateDeduction()).multiply(BigDecimal.valueOf(lateCount)));
        boolean truncated = cap != null && amount.compareTo(cap) > 0;
        if (truncated) {
            amount = cap;
        }
        return new AttendanceDeduction(money(amount), truncated);
    }

    /**
     * 社保 = clamp(基本工资 + 岗位工资, 基数下限, 基数上限) × 社保比例。
     *
     * @param baseSalary 基本工资
     * @param postSalary 岗位工资
     * @param config 核算配置
     * @return 社保金额（2 位小数）
     */
    public static BigDecimal calcSocialInsurance(
            BigDecimal baseSalary, BigDecimal postSalary, HrPayrollProperties config) {
        return clampBase(baseSalary, postSalary, config)
                .multiply(nvl(config.getSocialInsuranceRate()))
                .setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    /**
     * 公积金 = clamp(基本工资 + 岗位工资, 基数下限, 基数上限) × 公积金比例。
     *
     * @param baseSalary 基本工资
     * @param postSalary 岗位工资
     * @param config 核算配置
     * @return 公积金金额（2 位小数）
     */
    public static BigDecimal calcHousingFund(
            BigDecimal baseSalary, BigDecimal postSalary, HrPayrollProperties config) {
        return clampBase(baseSalary, postSalary, config)
                .multiply(nvl(config.getHousingFundRate()))
                .setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    /**
     * 个税：内置七级累进税率（含速算扣除数），应纳税所得额小于 0 按 0 处理。
     *
     * @param taxableIncome 应纳税所得额
     * @param config 核算配置（本期税率表内置，config 预留给后续可配置化）
     * @return 个税金额（2 位小数）
     */
    public static BigDecimal calcTax(BigDecimal taxableIncome, HrPayrollProperties config) {
        BigDecimal taxable = nvl(taxableIncome);
        if (taxable.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        }
        int level = TAX_RATES.length - 1;
        for (int i = 0; i < TAX_BRACKET_LIMITS.length; i++) {
            if (taxable.compareTo(TAX_BRACKET_LIMITS[i]) <= 0) {
                level = i;
                break;
            }
        }
        return taxable
                .multiply(TAX_RATES[level])
                .subtract(TAX_QUICK_DEDUCTIONS[level])
                .setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    /**
     * 整月核算：考勤扣款、绩效工资、社保、公积金、个税、应发与实发。
     *
     * <p>实发为负时不置零，仅在 warnings 中给出中文告警，由人工复核处理。
     *
     * @param input 核算输入
     * @param config 核算配置
     * @return 核算结果
     */
    public static PayrollResult calcMonth(PayrollInput input, HrPayrollProperties config) {
        List<String> warnings = new ArrayList<>();
        BigDecimal base = nvl(input.baseSalary());
        BigDecimal post = nvl(input.postSalary());
        BigDecimal performanceBase = nvl(input.performanceBase());
        BigDecimal allowance = nvl(input.allowance());
        BigDecimal coefficient =
                input.performanceCoefficient() == null
                        ? BigDecimal.ONE
                        : input.performanceCoefficient();

        BigDecimal daily = dailyWage(base, post, config);
        AttendanceDeduction deduction =
                calcAttendanceDeduction(
                        daily,
                        input.absentDays(),
                        input.personalLeaveDays(),
                        input.sickLeaveDays(),
                        input.lateCount(),
                        base.add(post),
                        config);
        if (deduction.truncated()) {
            warnings.add("考勤扣款已按应发工资上限截断");
        }

        BigDecimal performanceAmount =
                performanceBase
                        .multiply(coefficient)
                        .setScale(CALC_SCALE, RoundingMode.HALF_UP);
        BigDecimal socialInsurance = calcSocialInsurance(base, post, config);
        BigDecimal housingFund = calcHousingFund(base, post, config);
        BigDecimal grossPay =
                base.add(post).add(performanceAmount).add(allowance);
        BigDecimal taxableIncome =
                grossPay.subtract(socialInsurance).subtract(housingFund).subtract(nvl(config.getTaxThreshold()));
        BigDecimal tax = calcTax(taxableIncome, config);
        BigDecimal netPay =
                grossPay
                        .subtract(deduction.amount())
                        .subtract(socialInsurance)
                        .subtract(housingFund)
                        .subtract(tax);
        if (netPay.compareTo(BigDecimal.ZERO) < 0) {
            warnings.add("实发工资为负，请核查核算数据");
        }
        return new PayrollResult(
                deduction.amount(),
                money(performanceAmount),
                socialInsurance,
                housingFund,
                tax,
                money(grossPay),
                money(netPay),
                List.copyOf(warnings));
    }

    /** 缴费基数：基本工资 + 岗位工资，并夹在配置的上下限之间。 */
    private static BigDecimal clampBase(
            BigDecimal baseSalary, BigDecimal postSalary, HrPayrollProperties config) {
        BigDecimal base = nvl(baseSalary).add(nvl(postSalary));
        BigDecimal min = nvl(config.getSocialBaseMin());
        BigDecimal max = config.getSocialBaseMax() == null ? base : config.getSocialBaseMax();
        if (base.compareTo(min) < 0) {
            return min;
        }
        return base.compareTo(max) > 0 ? max : base;
    }

    private static BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private static BigDecimal money(BigDecimal value) {
        return value.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    /** 核算输入（金额均可空，服务端按 0 处理；绩效系数为空按 1.0 处理）。 */
    public record PayrollInput(
            BigDecimal baseSalary,
            BigDecimal postSalary,
            BigDecimal performanceBase,
            BigDecimal allowance,
            BigDecimal performanceCoefficient,
            BigDecimal absentDays,
            BigDecimal personalLeaveDays,
            BigDecimal sickLeaveDays,
            int lateCount) {}

    /** 核算结果（金额均为 2 位小数）。 */
    public record PayrollResult(
            BigDecimal attendanceDeduction,
            BigDecimal performanceAmount,
            BigDecimal socialInsurance,
            BigDecimal housingFund,
            BigDecimal tax,
            BigDecimal grossPay,
            BigDecimal netPay,
            List<String> warnings) {}

    /** 考勤扣款结果：金额与是否被上限截断。 */
    public record AttendanceDeduction(BigDecimal amount, boolean truncated) {}
}
