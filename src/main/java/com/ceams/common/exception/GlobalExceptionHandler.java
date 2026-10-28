package com.ceams.common.exception;

import com.ceams.common.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器：统一转换为 {@link ApiResponse}，HTTP 状态码严格对应 RFC7807。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException ex) {
        ErrorCode errorCode = ex.getErrorCode();
        log.warn("BusinessException: code={}, message={}", errorCode.getCode(), ex.getMessage());
        return build(errorCode.getHttpStatus(), errorCode.getCode(), errorCode.name());
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public ResponseEntity<ApiResponse<Void>> handleValidation(Exception ex) {
        String detail = "";
        if (ex instanceof MethodArgumentNotValidException validEx) {
            detail = validEx.getBindingResult().getFieldErrors().stream()
                    .map(e -> e.getField() + ": " + e.getDefaultMessage())
                    .findFirst()
                    .orElse(ErrorCode.VALID_ILLEGAL_ARGUMENT.getDescription());
        } else if (ex instanceof BindException bindEx) {
            detail = bindEx.getBindingResult().getFieldErrors().stream()
                    .map(e -> e.getField() + ": " + e.getDefaultMessage())
                    .findFirst()
                    .orElse(ErrorCode.VALID_ILLEGAL_ARGUMENT.getDescription());
        }
        log.warn("ValidationException: {}", detail);
        return build(HttpStatus.BAD_REQUEST, ErrorCode.VALID_ILLEGAL_ARGUMENT.getCode(),
                ErrorCode.VALID_ILLEGAL_ARGUMENT.name());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnreadable(HttpMessageNotReadableException ex) {
        log.warn("Malformed JSON: {}", ex.getMessage());
        return build(HttpStatus.BAD_REQUEST, ErrorCode.VALID_MALFORMED_JSON.getCode(),
                ErrorCode.VALID_MALFORMED_JSON.name());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        log.warn("TypeMismatch: {}", ex.getMessage());
        return build(HttpStatus.BAD_REQUEST, ErrorCode.VALID_ILLEGAL_ARGUMENT.getCode(),
                ErrorCode.VALID_ILLEGAL_ARGUMENT.name());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException ex) {
        log.warn("AccessDenied: {}", ex.getMessage());
        return build(HttpStatus.FORBIDDEN, ErrorCode.AUTH_ACCESS_DENIED.getCode(),
                ErrorCode.AUTH_ACCESS_DENIED.name());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception ex) {
        log.error("Unexpected exception", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.SYSTEM_INTERNAL_ERROR.getCode(),
                ErrorCode.SYSTEM_INTERNAL_ERROR.name());
    }

    private ResponseEntity<ApiResponse<Void>> build(HttpStatus status, int code, String message) {
        return ResponseEntity.status(status).body(ApiResponse.error(code, message));
    }
}