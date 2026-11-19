package com.ceams.module.hr.support;

import com.ceams.common.crypto.SensitiveEncryptor;
import com.ceams.common.exception.BusinessException;
import com.ceams.common.exception.ErrorCode;
import com.ceams.module.hr.entity.HrEmployee;
import com.ceams.module.hr.entity.HrSalaryCalc;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.math.BigDecimal;
import java.math.RoundingMode;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

/**
 * 工资条明细编解码器：负责 {@code hr_salary_slip.content_json} 的生成与解析。
 *
 * <p>安全约束：十个金额字段在明细 JSON 中一律写入 AES-256-GCM 密文（复用 {@link SensitiveEncryptor}，
 * 与 {@link SalarySnapshotCodec} 口径一致），库内不出现明文金额；解密仅在具备查看权限的接口输出时发生，
 * 日志禁止打印解密结果。
 *
 * <p>明文字段仅限：核算月、工号、姓名、部门 ID，以及考勤天数、缺勤天数、迟到次数、绩效系数等业务度量值。
 */
@Component
@RequiredArgsConstructor
public class SalarySlipCodec {

    /** 明细字段：核算月。 */
    private static final String FIELD_CALC_MONTH = "calcMonth";

    /** 明细字段：工号。 */
    private static final String FIELD_EMPLOYEE_NO = "employeeNo";

    /** 明细字段：姓名。 */
    private static final String FIELD_EMPLOYEE_NAME = "employeeName";

    /** 明细字段：部门 ID。 */
    private static final String FIELD_DEPT_ID = "deptId";

    /** 明细字段：基本工资。 */
    private static final String FIELD_BASE_SALARY = "baseSalary";

    /** 明细字段：岗位工资。 */
    private static final String FIELD_POST_SALARY = "postSalary";

    /** 明细字段：绩效工资。 */
    private static final String FIELD_PERFORMANCE_AMOUNT = "performanceAmount";

    /** 明细字段：补贴。 */
    private static final String FIELD_ALLOWANCE = "allowance";

    /** 明细字段：考勤扣款。 */
    private static final String FIELD_ATTENDANCE_DEDUCTION = "attendanceDeduction";

    /** 明细字段：社保。 */
    private static final String FIELD_SOCIAL_INSURANCE = "socialInsurance";

    /** 明细字段：公积金。 */
    private static final String FIELD_HOUSING_FUND = "housingFund";

    /** 明细字段：个人所得税。 */
    private static final String FIELD_TAX = "tax";

    /** 明细字段：应发合计。 */
    private static final String FIELD_GROSS_PAY = "grossPay";

    /** 明细字段：实发合计。 */
    private static final String FIELD_NET_PAY = "netPay";

    /** 明细字段：应出勤天数。 */
    private static final String FIELD_ATTENDANCE_DAYS = "attendanceDays";

    /** 明细字段：缺勤天数。 */
    private static final String FIELD_ABSENT_DAYS = "absentDays";

    /** 明细字段：迟到 / 早退次数。 */
    private static final String FIELD_LATE_COUNT = "lateCount";

    /** 明细字段：绩效系数。 */
    private static final String FIELD_PERFORMANCE_COEFFICIENT = "performanceCoefficient";

    /** 金额精度（与 hr_salary_calc 落库口径一致）。 */
    private static final int MONEY_SCALE = 2;

    private final SensitiveEncryptor encryptor;
    private final ObjectMapper objectMapper;

    /**
     * 生成工资条明细 JSON：金额以密文写入，明文不落库。
     *
     * @param calc 已确认的核算记录（金额字段为 TypeHandler 解密后的明文数值字符串）
     * @param employee 员工档案（可为 {@code null}，此时工号 / 姓名 / 部门为空）
     * @return 明细 JSON 字符串
     */
    public String encode(HrSalaryCalc calc, HrEmployee employee) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put(FIELD_CALC_MONTH, calc.getCalcMonth());
        putText(node, FIELD_EMPLOYEE_NO, employee == null ? null : employee.getEmployeeNo());
        putText(node, FIELD_EMPLOYEE_NAME, employee == null ? null : employee.getName());
        if (employee == null || employee.getDeptId() == null) {
            node.putNull(FIELD_DEPT_ID);
        } else {
            node.put(FIELD_DEPT_ID, employee.getDeptId());
        }
        putEncrypted(node, FIELD_BASE_SALARY, calc.getBaseSalaryEnc());
        putEncrypted(node, FIELD_POST_SALARY, calc.getPostSalaryEnc());
        putEncrypted(node, FIELD_PERFORMANCE_AMOUNT, calc.getPerformanceAmountEnc());
        putEncrypted(node, FIELD_ALLOWANCE, calc.getAllowanceEnc());
        putEncrypted(node, FIELD_ATTENDANCE_DEDUCTION, calc.getAttendanceDeductionEnc());
        putEncrypted(node, FIELD_SOCIAL_INSURANCE, calc.getSocialInsuranceEnc());
        putEncrypted(node, FIELD_HOUSING_FUND, calc.getHousingFundEnc());
        putEncrypted(node, FIELD_TAX, calc.getTaxEnc());
        putEncrypted(node, FIELD_GROSS_PAY, calc.getGrossPayEnc());
        putEncrypted(node, FIELD_NET_PAY, calc.getNetPayEnc());
        putDecimal(node, FIELD_ATTENDANCE_DAYS, calc.getAttendanceDays());
        putDecimal(node, FIELD_ABSENT_DAYS, calc.getAbsentDays());
        if (calc.getLateCount() == null) {
            node.putNull(FIELD_LATE_COUNT);
        } else {
            node.put(FIELD_LATE_COUNT, calc.getLateCount());
        }
        putDecimal(node, FIELD_PERFORMANCE_COEFFICIENT, calc.getPerformanceCoefficient());
        return node.toString();
    }

    /**
     * 解析并解密工资条明细；空字符串返回 {@code null}。
     *
     * @param contentJson 明细 JSON
     * @return 解密后的工资条明细
     */
    public SalarySlipSnapshot decode(String contentJson) {
        if (StringUtils.isBlank(contentJson)) {
            return null;
        }
        JsonNode node = readTree(contentJson);
        return new SalarySlipSnapshot(
                text(node, FIELD_CALC_MONTH),
                text(node, FIELD_EMPLOYEE_NO),
                text(node, FIELD_EMPLOYEE_NAME),
                parseLong(node.get(FIELD_DEPT_ID)),
                parseAmount(node, FIELD_BASE_SALARY),
                parseAmount(node, FIELD_POST_SALARY),
                parseAmount(node, FIELD_PERFORMANCE_AMOUNT),
                parseAmount(node, FIELD_ALLOWANCE),
                parseAmount(node, FIELD_ATTENDANCE_DEDUCTION),
                parseAmount(node, FIELD_SOCIAL_INSURANCE),
                parseAmount(node, FIELD_HOUSING_FUND),
                parseAmount(node, FIELD_TAX),
                parseAmount(node, FIELD_GROSS_PAY),
                parseAmount(node, FIELD_NET_PAY),
                decimal(node, FIELD_ATTENDANCE_DAYS),
                decimal(node, FIELD_ABSENT_DAYS),
                integer(node, FIELD_LATE_COUNT),
                decimal(node, FIELD_PERFORMANCE_COEFFICIENT));
    }

    private void putText(ObjectNode node, String field, String value) {
        if (value == null) {
            node.putNull(field);
            return;
        }
        node.put(field, value);
    }

    private void putDecimal(ObjectNode node, String field, BigDecimal value) {
        if (value == null) {
            node.putNull(field);
            return;
        }
        node.put(field, value);
    }

    /** 金额先归一到 2 位小数再加密，避免密文中残留不同精度的明文形态。 */
    private void putEncrypted(ObjectNode node, String field, String plaintext) {
        if (StringUtils.isBlank(plaintext)) {
            node.putNull(field);
            return;
        }
        BigDecimal amount = money(plaintext);
        node.put(field, encryptor.encrypt(amount.toPlainString()));
    }

    private JsonNode readTree(String json) {
        try {
            return objectMapper.readTree(json);
        } catch (JsonProcessingException e) {
            throw BusinessException.of(ErrorCode.BIZ_UNPROCESSABLE, "工资条明细 JSON 解析失败");
        }
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    private Long parseLong(JsonNode value) {
        if (value == null || value.isNull() || StringUtils.isBlank(value.asText())) {
            return null;
        }
        return value.asLong();
    }

    private BigDecimal decimal(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull() || StringUtils.isBlank(value.asText())) {
            return null;
        }
        try {
            return value.isNumber() ? value.decimalValue() : new BigDecimal(value.asText().trim());
        } catch (NumberFormatException e) {
            throw BusinessException.of(ErrorCode.BIZ_UNPROCESSABLE, "工资条明细数值字段非法");
        }
    }

    private Integer integer(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull() || StringUtils.isBlank(value.asText())) {
            return null;
        }
        return value.asInt();
    }

    /** 金额字段：先解密再转 BigDecimal。 */
    private BigDecimal parseAmount(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull() || StringUtils.isBlank(value.asText())) {
            return null;
        }
        try {
            return new BigDecimal(encryptor.decrypt(value.asText()).trim());
        } catch (NumberFormatException e) {
            throw BusinessException.of(ErrorCode.BIZ_UNPROCESSABLE, "工资条明细金额非法");
        }
    }

    private BigDecimal money(String plaintext) {
        try {
            return new BigDecimal(plaintext.trim()).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        } catch (NumberFormatException e) {
            throw BusinessException.of(ErrorCode.BIZ_UNPROCESSABLE, "工资条明细金额非法");
        }
    }

    /** 解密后的工资条明细（金额均为 BigDecimal，禁止写入日志）。 */
    public record SalarySlipSnapshot(
            String calcMonth,
            String employeeNo,
            String employeeName,
            Long deptId,
            BigDecimal baseSalary,
            BigDecimal postSalary,
            BigDecimal performanceAmount,
            BigDecimal allowance,
            BigDecimal attendanceDeduction,
            BigDecimal socialInsurance,
            BigDecimal housingFund,
            BigDecimal tax,
            BigDecimal grossPay,
            BigDecimal netPay,
            BigDecimal attendanceDays,
            BigDecimal absentDays,
            Integer lateCount,
            BigDecimal performanceCoefficient) {}
}
