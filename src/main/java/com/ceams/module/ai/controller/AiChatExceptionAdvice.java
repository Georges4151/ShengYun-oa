package com.ceams.module.ai.controller;

import com.ceams.common.exception.BusinessException;
import com.ceams.common.exception.ErrorCode;
import com.ceams.common.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * {@code /api/v1/ai/chat} 专用的 JSON 错误输出。
 *
 * <p>该端点声明 {@code produces = text/event-stream}，且前端以 {@code Accept: text/event-stream} 发起请求；
 * 若交给全局异常处理器协商，JSON 响应会因不可接受的媒体类型变成 406。这里显式指定 {@code Content-Type: application/json}，让「AI 未启用 /
 * 参数非法」等情况稳定返回 JSON。
 */
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = AiChatController.class)
public class AiChatExceptionAdvice {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException ex) {
        ErrorCode errorCode = ex.getErrorCode();
        log.warn("AI 对话前置校验失败：{} - {}", errorCode.name(), ex.getMessage());
        return json(errorCode.getHttpStatus(), errorCode.getCode(), ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        String detail =
                ex.getBindingResult().getFieldErrors().stream()
                        .map(error -> error.getField() + ": " + error.getDefaultMessage())
                        .findFirst()
                        .orElse(ErrorCode.VALID_ILLEGAL_ARGUMENT.getDescription());
        log.warn("AI 对话参数校验失败：{}", detail);
        return json(HttpStatus.BAD_REQUEST, ErrorCode.VALID_ILLEGAL_ARGUMENT.getCode(), detail);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnreadable(HttpMessageNotReadableException ex) {
        log.warn("AI 对话请求体解析失败：{}", ex.getMessage());
        return json(
                HttpStatus.BAD_REQUEST,
                ErrorCode.VALID_MALFORMED_JSON.getCode(),
                ErrorCode.VALID_MALFORMED_JSON.getDescription());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception ex) {
        log.error("AI 对话接口异常", ex);
        return json(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ErrorCode.SYSTEM_INTERNAL_ERROR.getCode(),
                ErrorCode.SYSTEM_INTERNAL_ERROR.getDescription());
    }

    private static ResponseEntity<ApiResponse<Void>> json(
            HttpStatus status, int code, String message) {
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .body(ApiResponse.error(code, message));
    }
}
