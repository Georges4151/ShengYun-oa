package com.ceams.module.ai.service;

import com.ceams.module.ai.dto.ReceiptOcrRequest;
import com.ceams.module.ai.vo.ReceiptOcrResultVO;

/**
 * 票据图片识别服务（只读：读取已上传图片，调用本地视觉模型，不落库、不修改票据）。
 */
public interface ReceiptOcrService {

    /**
     * 识别票据图片中的编号、金额、日期、类型与支付方式。
     *
     * @param request 识别请求（filePath 支持纯文件名或上传接口返回的 URL）
     * @return 识别结果（无法识别的字段为 null）
     */
    ReceiptOcrResultVO recognize(ReceiptOcrRequest request);
}
