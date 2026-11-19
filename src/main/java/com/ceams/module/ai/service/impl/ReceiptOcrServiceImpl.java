package com.ceams.module.ai.service.impl;

import com.ceams.common.exception.BusinessException;
import com.ceams.common.exception.ErrorCode;
import com.ceams.module.ai.client.OllamaClient;
import com.ceams.module.ai.config.AiProperties;
import com.ceams.module.ai.dto.ReceiptOcrRequest;
import com.ceams.module.ai.service.ReceiptOcrService;
import com.ceams.module.ai.vo.ReceiptOcrResultVO;
import com.ceams.module.file.service.FileService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 票据图片识别服务实现：读取已上传图片 → 转 Base64 → 调用本地视觉模型 → 解析并做服务端校验。
 *
 * <p>任何模型不可达、返回空内容或输出非法 JSON 都统一降级为 {@link ErrorCode#BIZ_UNPROCESSABLE}
 * 的可读错误，不写库、不影响报销主流程。金额、日期、类型、支付方式全部经过服务端二次校验， 不信任模型直接输出。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReceiptOcrServiceImpl implements ReceiptOcrService {

    /** 图片大小上限：20MB（与文件上传上限保持一致）。 */
    static final long MAX_IMAGE_SIZE = 20L * 1024 * 1024;

    /** 模型原始输出保留的最大字符数。 */
    static final int MAX_RAW_LENGTH = 4000;

    /** 票据编号最大长度（与 ReceiptCreateRequest 校验一致）。 */
    static final int MAX_RECEIPT_NUMBER_LENGTH = 100;

    /** 上传接口返回的 URL 前缀，识别接口需要从中取最后一段作为服务端文件名。 */
    static final String FILE_URL_SEGMENT = "/api/v1/files/";

    /** 识别支持的图片扩展名（PDF 不支持识别）。 */
    static final Set<String> ALLOWED_IMAGE_EXTENSIONS =
            Set.of("jpg", "jpeg", "png", "webp");

    /** 票据类型白名单（与 ReceiptType 枚举一致）。 */
    static final Set<String> ALLOWED_TYPES =
            Set.of("TRANSPORTATION", "ACCOMMODATION", "MEAL", "OFFICE", "OTHER");

    /** 支付方式白名单（与 PaymentMethod 枚举一致）。 */
    static final Set<String> ALLOWED_PAYMENT_METHODS =
            Set.of("CASH", "BANK_TRANSFER", "WECHAT", "ALIPAY", "OTHER");

    /** 金额格式：非负数字，最多两位小数；是否大于 0 另行判断。 */
    static final Pattern AMOUNT_PATTERN = Pattern.compile("^\\d+(\\.\\d{1,2})?$");

    /** 识别失败统一提示文案。 */
    static final String RECOGNIZE_FAILED_MESSAGE = "AI 识别失败，请手动填写或稍后重试";

    /** 票据识别提示词：强约束只输出一个 JSON 对象，并要求无法识别时输出 null。 */
    static final String OCR_PROMPT =
            "你是票据（发票/收据）信息识别助手。请仔细阅读图片中的票据内容，严格按以下要求输出：\n"
                    + "1. 只输出一个 JSON 对象，禁止输出 Markdown 代码块，禁止输出任何解释文字。\n"
                    + "2. 字段定义：\n"
                    + "   - receiptNumber：票据编号/发票号码，字符串；无法识别输出 null。\n"
                    + "   - amount：金额，数字字符串，不含货币符号与千分位逗号；无法识别输出 null。\n"
                    + "   - receiptDate：开票日期，格式 yyyy-MM-dd；无法识别输出 null。\n"
                    + "   - type：票据类型，只能取 TRANSPORTATION/ACCOMMODATION/MEAL/OFFICE/OTHER 之一；"
                    + "无法判断输出 null。\n"
                    + "   - paymentMethod：支付方式，只能取 CASH/BANK_TRANSFER/WECHAT/ALIPAY/OTHER 之一；"
                    + "无法判断输出 null。\n"
                    + "3. 无法识别的字段必须输出 null，不得编造、猜测或使用占位值。\n"
                    + "输出示例："
                    + "{\"receiptNumber\":\"12345678\",\"amount\":\"128.50\",\"receiptDate\":\"2026-11-01\","
                    + "\"type\":\"MEAL\",\"paymentMethod\":\"WECHAT\"}";

    private final AiProperties aiProperties;
    private final FileService fileService;
    private final OllamaClient ollamaClient;
    private final ObjectMapper objectMapper;

    @Override
    public ReceiptOcrResultVO recognize(ReceiptOcrRequest request) {
        if (!aiProperties.isEnabled()) {
            throw BusinessException.of(ErrorCode.BIZ_UNPROCESSABLE, "AI 未启用");
        }
        String name = normalizeFileName(request == null ? null : request.getFilePath());
        byte[] bytes = fileService.readBytes(name);
        if (bytes.length > MAX_IMAGE_SIZE) {
            throw BusinessException.of(ErrorCode.VALID_ILLEGAL_ARGUMENT, "文件大小不能超过 20MB");
        }
        String base64 = Base64.getEncoder().encodeToString(bytes);

        String raw;
        try {
            raw =
                    ollamaClient.chatWithImages(
                            aiProperties.getVisionModel(), OCR_PROMPT, List.of(base64));
        } catch (Exception e) {
            log.warn("票据识别调用视觉模型失败：name={}, 原因={}", name, e.getMessage());
            throw BusinessException.of(ErrorCode.BIZ_UNPROCESSABLE, RECOGNIZE_FAILED_MESSAGE);
        }
        if (!StringUtils.hasText(raw)) {
            log.warn("票据识别视觉模型返回空内容：name={}", name);
            throw BusinessException.of(ErrorCode.BIZ_UNPROCESSABLE, RECOGNIZE_FAILED_MESSAGE);
        }
        try {
            return parseModelOutput(raw);
        } catch (Exception e) {
            log.warn("票据识别结果解析失败：name={}, 原因={}", name, e.getMessage());
            throw BusinessException.of(ErrorCode.BIZ_UNPROCESSABLE, RECOGNIZE_FAILED_MESSAGE);
        }
    }

    /**
     * 归一化请求中的文件路径：兼容纯文件名与上传接口返回的 URL。
     *
     * <p>规则：去掉 URL 查询串/锚点后，若包含 {@code /api/v1/files/} 则取其后部分，再取最后一个 {@code /} 之后的部分；
     * 文件名不得包含反斜杠与 {@code ..}，扩展名必须是 jpg/jpeg/png/webp（PDF 明确不支持）。
     */
    static String normalizeFileName(String filePath) {
        String value = filePath == null ? "" : filePath.trim();
        if (value.isEmpty()) {
            throw BusinessException.of(ErrorCode.VALID_ILLEGAL_ARGUMENT, "文件路径不能为空");
        }
        int queryIndex = value.indexOf('?');
        if (queryIndex >= 0) {
            value = value.substring(0, queryIndex);
        }
        int hashIndex = value.indexOf('#');
        if (hashIndex >= 0) {
            value = value.substring(0, hashIndex);
        }
        // 先对完整入参做路径穿越校验，再截取文件名，避免 "../x.png" 被截断后看似合法。
        if (value.contains("\\") || value.contains("..")) {
            throw BusinessException.of(ErrorCode.VALID_ILLEGAL_ARGUMENT, "非法文件名");
        }
        int urlIndex = value.indexOf(FILE_URL_SEGMENT);
        if (urlIndex >= 0) {
            value = value.substring(urlIndex + FILE_URL_SEGMENT.length());
        }
        int slashIndex = value.lastIndexOf('/');
        if (slashIndex >= 0) {
            value = value.substring(slashIndex + 1);
        }
        if (value.isEmpty()) {
            throw BusinessException.of(ErrorCode.VALID_ILLEGAL_ARGUMENT, "非法文件名");
        }
        int dotIndex = value.lastIndexOf('.');
        if (dotIndex <= 0 || dotIndex == value.length() - 1) {
            throw BusinessException.of(ErrorCode.VALID_ILLEGAL_ARGUMENT, "非法文件名");
        }
        String extension = value.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
        if ("pdf".equals(extension)) {
            throw BusinessException.of(ErrorCode.VALID_ILLEGAL_ARGUMENT, "仅支持图片票据识别");
        }
        if (!ALLOWED_IMAGE_EXTENSIONS.contains(extension)) {
            throw BusinessException.of(
                    ErrorCode.VALID_ILLEGAL_ARGUMENT, "仅支持 jpg/jpeg/png/webp 图片票据识别");
        }
        return value;
    }

    /**
     * 解析模型输出并做服务端校验/归一化。
     *
     * <p>容忍模型把 JSON 包在 Markdown 代码块或说明文字中：截取第一个 {@code &#123;} 到最后一个 {@code &#125;}
     * 再解析。无法提取 JSON 对象时抛出异常，由调用方降级为统一错误提示。
     */
    ReceiptOcrResultVO parseModelOutput(String raw) throws JsonProcessingException {
        String text = raw == null ? "" : raw.trim();
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start < 0 || end <= start) {
            throw new IllegalArgumentException("模型输出中未找到 JSON 对象");
        }
        JsonNode root = objectMapper.readTree(text.substring(start, end + 1));
        if (root == null || !root.isObject()) {
            throw new IllegalArgumentException("模型输出不是 JSON 对象");
        }

        ReceiptOcrResultVO result = new ReceiptOcrResultVO();
        result.setReceiptNumber(normalizeReceiptNumber(textValue(root, "receiptNumber")));
        result.setAmount(normalizeAmount(textValue(root, "amount")));
        result.setReceiptDate(normalizeDate(textValue(root, "receiptDate")));
        result.setType(normalizeEnum(textValue(root, "type"), ALLOWED_TYPES));
        result.setPaymentMethod(
                normalizeEnum(textValue(root, "paymentMethod"), ALLOWED_PAYMENT_METHODS));
        result.setRaw(truncate(raw, MAX_RAW_LENGTH));
        return result;
    }

    /** 取文本字段值：缺失、null 或非标量（对象/数组）一律视为未识别。 */
    private static String textValue(JsonNode root, String field) {
        JsonNode node = root.get(field);
        if (node == null || node.isNull() || node.isMissingNode() || !node.isValueNode()) {
            return null;
        }
        String value = node.asText("").trim();
        // 部分模型会把空值输出为字符串 "null"/"NULL"，这里一并归一为未识别。
        if (value.isEmpty() || "null".equalsIgnoreCase(value)) {
            return null;
        }
        return value;
    }

    /** 票据编号：去空白；超过 100 字符截断。 */
    private static String normalizeReceiptNumber(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() > MAX_RECEIPT_NUMBER_LENGTH
                ? trimmed.substring(0, MAX_RECEIPT_NUMBER_LENGTH)
                : trimmed;
    }

    /** 金额：必须匹配 {@link #AMOUNT_PATTERN} 且大于 0，否则返回 null。 */
    private static String normalizeAmount(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String trimmed = value.trim();
        if (!AMOUNT_PATTERN.matcher(trimmed).matches()) {
            return null;
        }
        try {
            if (new BigDecimal(trimmed).compareTo(BigDecimal.ZERO) <= 0) {
                return null;
            }
        } catch (NumberFormatException e) {
            return null;
        }
        return trimmed;
    }

    /** 日期：必须能被 {@link LocalDate#parse(CharSequence)} 解析，统一输出 yyyy-MM-dd。 */
    private static String normalizeDate(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return LocalDate.parse(value.trim()).toString();
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    /** 枚举值：去空白并转大写后必须在白名单内，否则返回 null。 */
    private static String normalizeEnum(String value, Set<String> allowed) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        return allowed.contains(normalized) ? normalized : null;
    }

    /** 按字符数截断，超长时直接截断（不做省略号拼接，保持原始信息）。 */
    private static String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        return value.length() > maxLength ? value.substring(0, maxLength) : value;
    }
}
