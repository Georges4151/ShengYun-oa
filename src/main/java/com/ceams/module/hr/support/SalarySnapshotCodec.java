package com.ceams.module.hr.support;

import com.ceams.common.crypto.SensitiveEncryptor;
import com.ceams.common.exception.BusinessException;
import com.ceams.common.exception.ErrorCode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

/**
 * 薪资快照编解码器：负责变更前后快照 JSON 的生成与解析。
 *
 * <p>安全约束：四个金额字段在快照 JSON 中一律写入 AES-256-GCM 密文（复用 {@link SensitiveEncryptor}），
 * 密文可安全落库；解密仅在回调落台账或具备薪资查看权限的接口输出摘要时发生。
 */
@Component
@RequiredArgsConstructor
public class SalarySnapshotCodec {

    /** 快照字段：员工 ID。 */
    private static final String FIELD_EMPLOYEE_ID = "employeeId";

    /** 快照字段：生效日期。 */
    private static final String FIELD_EFFECTIVE_DATE = "effectiveDate";

    /** 快照字段：基本工资。 */
    private static final String FIELD_BASE_SALARY = "baseSalary";

    /** 快照字段：岗位工资。 */
    private static final String FIELD_POST_SALARY = "postSalary";

    /** 快照字段：绩效基数。 */
    private static final String FIELD_PERFORMANCE_BASE = "performanceBase";

    /** 快照字段：固定补贴。 */
    private static final String FIELD_ALLOWANCE = "allowance";

    private final SensitiveEncryptor encryptor;
    private final ObjectMapper objectMapper;

    /**
     * 生成快照 JSON：金额以密文写入，明文不落库。
     *
     * @param employeeId 员工 ID
     * @param effectiveDate 生效日期
     * @param baseSalary 基本工资（明文，仅用于加密）
     * @param postSalary 岗位工资（明文，仅用于加密）
     * @param performanceBase 绩效基数（明文，仅用于加密）
     * @param allowance 固定补贴（明文，仅用于加密）
     * @return 快照 JSON 字符串
     */
    public String encode(
            Long employeeId,
            LocalDate effectiveDate,
            BigDecimal baseSalary,
            BigDecimal postSalary,
            BigDecimal performanceBase,
            BigDecimal allowance) {
        ObjectNode node = objectMapper.createObjectNode();
        putText(node, FIELD_EMPLOYEE_ID, employeeId == null ? null : String.valueOf(employeeId));
        putText(node, FIELD_EFFECTIVE_DATE, effectiveDate == null ? null : effectiveDate.toString());
        putEncrypted(node, FIELD_BASE_SALARY, baseSalary);
        putEncrypted(node, FIELD_POST_SALARY, postSalary);
        putEncrypted(node, FIELD_PERFORMANCE_BASE, performanceBase);
        putEncrypted(node, FIELD_ALLOWANCE, allowance);
        return node.toString();
    }

    /**
     * 解析并解密快照；空字符串返回 {@code null}。
     *
     * @param snapshotJson 快照 JSON
     * @return 解密后的快照
     */
    public SalarySnapshot decode(String snapshotJson) {
        if (StringUtils.isBlank(snapshotJson)) {
            return null;
        }
        JsonNode node = readTree(snapshotJson);
        return new SalarySnapshot(
                parseLong(text(node, FIELD_EMPLOYEE_ID)),
                parseDate(text(node, FIELD_EFFECTIVE_DATE)),
                parseAmount(decrypt(text(node, FIELD_BASE_SALARY))),
                parseAmount(decrypt(text(node, FIELD_POST_SALARY))),
                parseAmount(decrypt(text(node, FIELD_PERFORMANCE_BASE))),
                parseAmount(decrypt(text(node, FIELD_ALLOWANCE))));
    }

    /**
     * 生成可读摘要（解密金额），仅用于具备薪资查看权限的接口输出，禁止写入日志。
     *
     * @param snapshotJson 快照 JSON
     * @return 摘要文本；空快照返回 {@code null}
     */
    public String summarize(String snapshotJson) {
        SalarySnapshot snapshot = decode(snapshotJson);
        if (snapshot == null) {
            return null;
        }
        return "基本工资 "
                + format(snapshot.baseSalary())
                + "；岗位工资 "
                + format(snapshot.postSalary())
                + "；绩效基数 "
                + format(snapshot.performanceBase())
                + "；固定补贴 "
                + format(snapshot.allowance());
    }

    /** 快照 JSON 转 JsonNode（用于审计快照，金额仍为密文）；空字符串返回 {@code null}。 */
    public JsonNode toJsonNode(String snapshotJson) {
        if (StringUtils.isBlank(snapshotJson)) {
            return null;
        }
        return readTree(snapshotJson);
    }

    private void putText(ObjectNode node, String field, String value) {
        if (value == null) {
            node.putNull(field);
            return;
        }
        node.put(field, value);
    }

    private void putEncrypted(ObjectNode node, String field, BigDecimal value) {
        if (value == null) {
            node.putNull(field);
            return;
        }
        node.put(field, encryptor.encrypt(value.toPlainString()));
    }

    private JsonNode readTree(String json) {
        try {
            return objectMapper.readTree(json);
        } catch (JsonProcessingException e) {
            throw BusinessException.of(ErrorCode.BIZ_UNPROCESSABLE, "薪资快照 JSON 解析失败");
        }
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    private String decrypt(String ciphertext) {
        return StringUtils.isBlank(ciphertext) ? null : encryptor.decrypt(ciphertext);
    }

    private Long parseLong(String value) {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        try {
            return Long.valueOf(value.trim());
        } catch (NumberFormatException e) {
            throw BusinessException.of(ErrorCode.BIZ_UNPROCESSABLE, "薪资快照员工 ID 非法");
        }
    }

    private LocalDate parseDate(String value) {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        try {
            return LocalDate.parse(value.trim());
        } catch (DateTimeParseException e) {
            throw BusinessException.of(ErrorCode.BIZ_UNPROCESSABLE, "薪资快照生效日期非法");
        }
    }

    private BigDecimal parseAmount(String value) {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException e) {
            throw BusinessException.of(ErrorCode.BIZ_UNPROCESSABLE, "薪资快照金额非法");
        }
    }

    private String format(BigDecimal value) {
        return value == null ? "-" : value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    /** 解密后的薪资快照。 */
    public record SalarySnapshot(
            Long employeeId,
            LocalDate effectiveDate,
            BigDecimal baseSalary,
            BigDecimal postSalary,
            BigDecimal performanceBase,
            BigDecimal allowance) {}
}
