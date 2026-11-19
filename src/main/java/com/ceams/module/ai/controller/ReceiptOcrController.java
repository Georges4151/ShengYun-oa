package com.ceams.module.ai.controller;

import com.ceams.common.response.ApiResponse;
import com.ceams.module.ai.dto.ReceiptOcrRequest;
import com.ceams.module.ai.service.ReceiptOcrService;
import com.ceams.module.ai.vo.ReceiptOcrResultVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 票据图片识别接口（只读：识别结果仅返回给前端，不落库）。 */
@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class ReceiptOcrController {

    private final ReceiptOcrService receiptOcrService;

    /**
     * 识别已上传票据图片的编号、金额、日期、类型与支付方式。
     *
     * <p>AI 未启用返回 422「AI 未启用」；模型不可达或输出不可解析返回 422「AI 识别失败，请手动填写或稍后重试」。
     */
    @PostMapping("/receipt-ocr")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<ReceiptOcrResultVO> receiptOcr(@Valid @RequestBody ReceiptOcrRequest request) {
        return ApiResponse.success(receiptOcrService.recognize(request));
    }
}
