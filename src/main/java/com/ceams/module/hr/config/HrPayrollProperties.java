package com.ceams.module.hr.config;

import java.math.BigDecimal;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * HR 月度工资核算配置（{@code ceams.hr.payroll.*}）。
 *
 * <p>与 {@code HrCheckinProperties} 保持一致：使用 {@code @Component + @ConfigurationProperties}
 * 由组件扫描直接注册，无需额外的 {@code @EnableConfigurationProperties}，避免重复注册。
 *
 * <p>全部数值使用 {@link BigDecimal}，禁止 {@code double}/{@code float}，确保金额计算精度可控。
 */
@Data
@Component
@ConfigurationProperties(prefix = "ceams.hr.payroll")
public class HrPayrollProperties {

    /** 月计薪天数，日工资 = (基本工资 + 岗位工资) / workingDays。 */
    private BigDecimal workingDays = new BigDecimal("21.75");

    /** 社保个人缴费比例。 */
    private BigDecimal socialInsuranceRate = new BigDecimal("0.105");

    /** 公积金个人缴费比例。 */
    private BigDecimal housingFundRate = new BigDecimal("0.07");

    /** 社保 / 公积金缴费基数下限。 */
    private BigDecimal socialBaseMin = BigDecimal.ZERO;

    /** 社保 / 公积金缴费基数上限。 */
    private BigDecimal socialBaseMax = new BigDecimal("999999");

    /** 个税起征点（每月）。 */
    private BigDecimal taxThreshold = new BigDecimal("5000");

    /** 每次迟到 / 早退扣款金额。 */
    private BigDecimal lateDeduction = new BigDecimal("50");

    /** 病假扣款比例：病假扣款 = 日工资 × 病假天数 × 该比例。 */
    private BigDecimal sickDeductionRate = new BigDecimal("0.4");
}
