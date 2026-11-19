package com.ceams.module.ai.vo;

import lombok.Data;

/**
 * 票据图片识别结果。
 *
 * <p>所有字段均为「尽力识别」结果，无法识别的字段统一为 {@code null}，由前端提示用户手动补录； {@code raw}
 * 保留模型原始输出，便于排查识别质量问题。
 */
@Data
public class ReceiptOcrResultVO {

    /** 票据编号 / 发票号码；未识别为 null。 */
    private String receiptNumber;

    /** 金额（字符串，避免浮点精度问题）；未识别或校验不通过为 null。 */
    private String amount;

    /** 票据日期（yyyy-MM-dd）；未识别为 null。 */
    private String receiptDate;

    /** 票据类型，取 ReceiptType 枚举名；未识别或不在白名单内为 null。 */
    private String type;

    /** 支付方式，取 PaymentMethod 枚举名；未识别或不在白名单内为 null。 */
    private String paymentMethod;

    /** 模型原始输出（截断到 4000 字符），便于排查识别问题。 */
    private String raw;
}
