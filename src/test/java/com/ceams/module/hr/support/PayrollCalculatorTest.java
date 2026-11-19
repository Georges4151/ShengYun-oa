package com.ceams.module.hr.support;

import static org.assertj.core.api.Assertions.assertThat;

import com.ceams.module.hr.config.HrPayrollProperties;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * 工资核算纯逻辑单元测试：考勤扣款、社保 / 公积金基数、个税累进、实发为负与绩效系数。
 *
 * <p>所有金额断言均使用 {@code compareTo} / {@code isEqualByComparingTo}，避免 BigDecimal equals
 * 受 scale 影响。
 */
class PayrollCalculatorTest {

    private HrPayrollProperties config;

    @BeforeEach
    void setUp() {
        config = new HrPayrollProperties();
    }

    @Test
    void calcMonthShouldComputeFullMonthWithoutDeduction() {
        PayrollCalculator.PayrollResult result =
                PayrollCalculator.calcMonth(
                        input("10000", "5000", "3000", "1000", "1", "0", "0", "0", 0), config);

        assertThat(result.attendanceDeduction()).isEqualByComparingTo("0.00");
        assertThat(result.performanceAmount()).isEqualByComparingTo("3000.00");
        assertThat(result.socialInsurance()).isEqualByComparingTo("1575.00");
        assertThat(result.housingFund()).isEqualByComparingTo("1050.00");
        assertThat(result.tax()).isEqualByComparingTo("927.50");
        assertThat(result.grossPay()).isEqualByComparingTo("19000.00");
        assertThat(result.netPay()).isEqualByComparingTo("15447.50");
        assertThat(result.warnings()).isEmpty();
    }

    @Test
    void calcMonthShouldDeductTwoAbsentDays() {
        PayrollCalculator.PayrollResult result =
                PayrollCalculator.calcMonth(
                        input("10000", "5000", "0", "0", "1", "2", "0", "0", 0), config);

        // 日工资 = 15000 / 21.75 = 689.6552（4 位），× 2 = 1379.3104 → 1379.31
        assertThat(result.attendanceDeduction()).isEqualByComparingTo("1379.31");
    }

    @Test
    void calcMonthShouldDeductOnePersonalLeaveDay() {
        PayrollCalculator.PayrollResult result =
                PayrollCalculator.calcMonth(
                        input("10000", "5000", "0", "0", "1", "0", "1", "0", 0), config);

        assertThat(result.attendanceDeduction()).isEqualByComparingTo("689.66");
    }

    @Test
    void calcMonthShouldDeductSickLeaveAtConfiguredRate() {
        PayrollCalculator.PayrollResult result =
                PayrollCalculator.calcMonth(
                        input("10000", "5000", "0", "0", "1", "0", "0", "2", 0), config);

        // 689.6552 × 2 × 0.4 = 551.72416 → 551.72
        assertThat(result.attendanceDeduction()).isEqualByComparingTo("551.72");
    }

    @Test
    void calcMonthShouldDeductLateTimes() {
        PayrollCalculator.PayrollResult result =
                PayrollCalculator.calcMonth(
                        input("10000", "5000", "0", "0", "1", "0", "0", "0", 3), config);

        assertThat(result.attendanceDeduction()).isEqualByComparingTo("150.00");
    }

    @Test
    void calcMonthShouldTruncateDeductionAtGrossAndWarn() {
        PayrollCalculator.PayrollResult result =
                PayrollCalculator.calcMonth(
                        input("10000", "5000", "0", "0", "1", "40", "0", "0", 0), config);

        assertThat(result.attendanceDeduction()).isEqualByComparingTo("15000.00");
        assertThat(result.warnings()).anyMatch(warning -> warning.contains("截断"));
    }

    @Test
    void calcSocialInsuranceShouldUseLowerLimit() {
        config.setSocialBaseMin(new BigDecimal("6000"));

        BigDecimal social =
                PayrollCalculator.calcSocialInsurance(
                        new BigDecimal("2000"), new BigDecimal("1000"), config);

        assertThat(social).isEqualByComparingTo("630.00");
    }

    @Test
    void calcSocialInsuranceAndHousingFundShouldUseUpperLimit() {
        config.setSocialBaseMax(new BigDecimal("20000"));

        assertThat(
                        PayrollCalculator.calcSocialInsurance(
                                new BigDecimal("20000"), new BigDecimal("10000"), config))
                .isEqualByComparingTo("2100.00");
        assertThat(
                        PayrollCalculator.calcHousingFund(
                                new BigDecimal("20000"), new BigDecimal("10000"), config))
                .isEqualByComparingTo("1400.00");
    }

    @Test
    void calcTaxShouldCrossProgressiveBrackets() {
        assertThat(PayrollCalculator.calcTax(new BigDecimal("3000"), config))
                .isEqualByComparingTo("90.00");
        assertThat(PayrollCalculator.calcTax(new BigDecimal("3001"), config))
                .isEqualByComparingTo("90.10");
        assertThat(PayrollCalculator.calcTax(new BigDecimal("12001"), config))
                .isEqualByComparingTo("990.20");
        assertThat(PayrollCalculator.calcTax(new BigDecimal("25000"), config))
                .isEqualByComparingTo("3590.00");
        assertThat(PayrollCalculator.calcTax(new BigDecimal("-1"), config))
                .isEqualByComparingTo("0.00");
    }

    @Test
    void calcMonthShouldKeepNegativeNetPayAndWarn() {
        PayrollCalculator.PayrollResult result =
                PayrollCalculator.calcMonth(
                        input("1000", "0", "0", "0", "1", "30", "0", "0", 0), config);

        assertThat(result.attendanceDeduction()).isEqualByComparingTo("1000.00");
        // 1000 - 1000 - 105 - 70 - 0 = -175，不置零
        assertThat(result.netPay()).isEqualByComparingTo("-175.00");
        assertThat(result.warnings()).anyMatch(warning -> warning.contains("实发工资为负"));
    }

    @Test
    void calcMonthShouldApplyPerformanceCoefficientBelowOne() {
        PayrollCalculator.PayrollResult result =
                PayrollCalculator.calcMonth(
                        input("10000", "5000", "5000", "0", "0.8", "0", "0", "0", 0), config);

        assertThat(result.performanceAmount()).isEqualByComparingTo("4000.00");
        assertThat(result.grossPay()).isEqualByComparingTo("19000.00");
    }

    @Test
    void calcMonthShouldApplyPerformanceCoefficientAboveOne() {
        PayrollCalculator.PayrollResult result =
                PayrollCalculator.calcMonth(
                        input("10000", "5000", "5000", "0", "1.2", "0", "0", "0", 0), config);

        assertThat(result.performanceAmount()).isEqualByComparingTo("6000.00");
        assertThat(result.grossPay()).isEqualByComparingTo("21000.00");
    }

    private PayrollCalculator.PayrollInput input(
            String base,
            String post,
            String performanceBase,
            String allowance,
            String coefficient,
            String absentDays,
            String personalLeaveDays,
            String sickLeaveDays,
            int lateCount) {
        return new PayrollCalculator.PayrollInput(
                new BigDecimal(base),
                new BigDecimal(post),
                new BigDecimal(performanceBase),
                new BigDecimal(allowance),
                new BigDecimal(coefficient),
                new BigDecimal(absentDays),
                new BigDecimal(personalLeaveDays),
                new BigDecimal(sickLeaveDays),
                lateCount);
    }
}
