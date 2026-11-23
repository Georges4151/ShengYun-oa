package com.ceams.module.ai.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ceams.common.exception.BusinessException;
import com.ceams.common.exception.ErrorCode;
import com.ceams.module.ai.vo.ReceiptOcrResultVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

/** {@link ReceiptOcrServiceImpl} 纯解析/归一化逻辑单元测试（不依赖 Ollama 与文件系统）。 */
class ReceiptOcrServiceImplTest {

    private final ReceiptOcrServiceImpl service =
            new ReceiptOcrServiceImpl(null, null, new ObjectMapper());

    @Test
    void 解析模型输出_兼容Markdown代码块并归一化字段() throws Exception {
        String raw =
                "```json\n"
                        + "{\"receiptNumber\":\" 12345678 \",\"amount\":\"128.50\","
                        + "\"receiptDate\":\"2026-11-01\",\"type\":\"meal\","
                        + "\"paymentMethod\":\"wechat\"}\n```";

        ReceiptOcrResultVO result = service.parseModelOutput(raw);

        assertEquals("12345678", result.getReceiptNumber());
        assertEquals("128.50", result.getAmount());
        assertEquals("2026-11-01", result.getReceiptDate());
        assertEquals("MEAL", result.getType());
        assertEquals("WECHAT", result.getPaymentMethod());
        assertEquals(raw, result.getRaw());
    }

    @Test
    void 解析模型输出_非法金额日期枚举统一置空() throws Exception {
        String raw =
                "先说明一句：{"
                        + "\"receiptNumber\":\"A-1\",\"amount\":\"¥1,280.00\","
                        + "\"receiptDate\":\"2026/11/01\",\"type\":\"FOOD\","
                        + "\"paymentMethod\":\"UNKNOWN\"} 以上。";

        ReceiptOcrResultVO result = service.parseModelOutput(raw);

        assertEquals("A-1", result.getReceiptNumber());
        assertNull(result.getAmount());
        assertNull(result.getReceiptDate());
        assertNull(result.getType());
        assertNull(result.getPaymentMethod());
    }

    @Test
    void 解析模型输出_金额为零或负数置空() throws Exception {
        assertNull(service.parseModelOutput("{\"amount\":\"0.00\"}").getAmount());
        assertNull(service.parseModelOutput("{\"amount\":\"-5\"}").getAmount());
    }

    @Test
    void 解析模型输出_字段为null或缺失均返回空() throws Exception {
        ReceiptOcrResultVO result = service.parseModelOutput("{\"receiptNumber\":null}");

        assertNull(result.getReceiptNumber());
        assertNull(result.getAmount());
        assertNull(result.getReceiptDate());
        assertNull(result.getType());
        assertNull(result.getPaymentMethod());
    }

    @Test
    void 解析模型输出_超长编号与原始文本被截断() throws Exception {
        String longNumber = "9".repeat(150);
        String raw =
                "{\"receiptNumber\":\"" + longNumber + "\",\"note\":\"" + "x".repeat(5000) + "\"}";

        ReceiptOcrResultVO result = service.parseModelOutput(raw);

        assertEquals(100, result.getReceiptNumber().length());
        assertEquals(ReceiptOcrServiceImpl.MAX_RAW_LENGTH, result.getRaw().length());
    }

    @Test
    void 解析模型输出_无JSON对象抛异常() {
        assertThrows(IllegalArgumentException.class, () -> service.parseModelOutput("无法识别"));
        assertThrows(IllegalArgumentException.class, () -> service.parseModelOutput(""));
        assertThrows(IllegalArgumentException.class, () -> service.parseModelOutput(null));
    }

    @Test
    void 归一化文件名_兼容URL与纯文件名() {
        assertEquals(
                "1752560000000000001.png",
                ReceiptOcrServiceImpl.normalizeFileName(
                        "/api/v1/files/1752560000000000001.png"));
        assertEquals(
                "1752560000000000001.png",
                ReceiptOcrServiceImpl.normalizeFileName(
                        "http://localhost:8080/api/v1/files/1752560000000000001.png?t=1"));
        assertEquals(
                "1752560000000000001.jpg",
                ReceiptOcrServiceImpl.normalizeFileName("1752560000000000001.jpg"));
    }

    @Test
    void 归一化文件名_PDF与路径穿越被拒绝() {
        BusinessException pdf =
                assertThrows(
                        BusinessException.class,
                        () -> ReceiptOcrServiceImpl.normalizeFileName("1752560000000000001.pdf"));
        assertEquals(ErrorCode.VALID_ILLEGAL_ARGUMENT, pdf.getErrorCode());
        assertTrue(pdf.getMessage().contains("仅支持图片票据识别"));

        assertThrows(
                BusinessException.class,
                () -> ReceiptOcrServiceImpl.normalizeFileName("../etc/passwd.png"));
        assertThrows(
                BusinessException.class,
                () -> ReceiptOcrServiceImpl.normalizeFileName("1752560000000000001.gif"));
        assertThrows(BusinessException.class, () -> ReceiptOcrServiceImpl.normalizeFileName(" "));
    }
}
