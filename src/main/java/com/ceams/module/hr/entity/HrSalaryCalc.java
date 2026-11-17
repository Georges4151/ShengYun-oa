package com.ceams.module.hr.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ceams.common.base.BaseEntity;
import com.ceams.common.crypto.SensitiveTypeHandler;
import com.ceams.module.hr.enums.PayrollStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * HR 月度薪资核算台账（hr_salary_calc）。
 *
 * <p>金额字段一律以 {@code _enc} 列落库，由 {@link SensitiveTypeHandler} 做 AES-256-GCM 加解密；
 * 实体中的值为明文数值字符串，禁止写入日志。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "hr_salary_calc", autoResultMap = true)
public class HrSalaryCalc extends BaseEntity {

    /** 员工 ID（逻辑引用 hr_employee.id）。 */
    private Long employeeId;

    /** 核算月（yyyy-MM）。 */
    private String calcMonth;

    /** 基本工资（加密存储）。 */
    @TableField(typeHandler = SensitiveTypeHandler.class)
    private String baseSalaryEnc;

    /** 岗位工资（加密存储）。 */
    @TableField(typeHandler = SensitiveTypeHandler.class)
    private String postSalaryEnc;

    /** 应出勤天数（默认取月计薪天数）。 */
    private BigDecimal attendanceDays;

    /** 缺勤天数（当月 hr_checkin.status=ABSENT 的去重日期数）。 */
    private BigDecimal absentDays;

    /** 迟到 / 早退次数。 */
    private Integer lateCount;

    /** 考勤扣款（加密存储）。 */
    @TableField(typeHandler = SensitiveTypeHandler.class)
    private String attendanceDeductionEnc;

    /** 绩效系数，默认 1.0。 */
    private BigDecimal performanceCoefficient;

    /** 绩效工资（加密存储）。 */
    @TableField(typeHandler = SensitiveTypeHandler.class)
    private String performanceAmountEnc;

    /** 补贴（加密存储）。 */
    @TableField(typeHandler = SensitiveTypeHandler.class)
    private String allowanceEnc;

    /** 社保（加密存储）。 */
    @TableField(typeHandler = SensitiveTypeHandler.class)
    private String socialInsuranceEnc;

    /** 公积金（加密存储）。 */
    @TableField(typeHandler = SensitiveTypeHandler.class)
    private String housingFundEnc;

    /** 个人所得税（加密存储）。 */
    @TableField(typeHandler = SensitiveTypeHandler.class)
    private String taxEnc;

    /** 应发合计（加密存储）。 */
    @TableField(typeHandler = SensitiveTypeHandler.class)
    private String grossPayEnc;

    /** 实发合计（加密存储）。 */
    @TableField(typeHandler = SensitiveTypeHandler.class)
    private String netPayEnc;

    /** 核算状态：DRAFT/CONFIRMED/LOCKED。 */
    private PayrollStatus status;

    /** 确认人 ID（逻辑引用 sys_user.id）。 */
    private Long confirmedBy;

    /** 确认时间。 */
    private LocalDateTime confirmedAt;
}
