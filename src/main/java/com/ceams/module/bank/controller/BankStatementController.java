package com.ceams.module.bank.controller;

import com.ceams.common.exception.BusinessException;
import com.ceams.common.exception.ErrorCode;
import com.ceams.common.response.ApiResponse;
import com.ceams.common.response.PageResult;
import com.ceams.module.bank.dto.BankStatementCreateRequest;
import com.ceams.module.bank.dto.BankStatementUpdateRequest;
import com.ceams.module.bank.enums.BankStatementStatus;
import com.ceams.module.bank.enums.TransactionType;
import com.ceams.module.bank.importer.StatementImporter;
import com.ceams.module.bank.service.BankStatementService;
import com.ceams.module.bank.vo.BankStatementVO;
import com.ceams.module.bank.vo.StatementImportResultVO;
import jakarta.validation.Valid;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** 银行流水管理接口。 */
@RestController
@RequestMapping("/api/v1/bank/statements")
@RequiredArgsConstructor
public class BankStatementController {

    private final BankStatementService bankStatementService;
    private final StatementImporter statementImporter;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<PageResult<BankStatementVO>> page(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size,
            @RequestParam(required = false) Long accountId,
            @RequestParam(required = false) BankStatementStatus status,
            @RequestParam(required = false) TransactionType type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    LocalDateTime endDate) {
        return ApiResponse.success(
                bankStatementService.page(page, size, accountId, status, type, startDate, endDate));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SYS_ADMIN','FINANCE')")
    public ApiResponse<String> create(@Valid @RequestBody BankStatementCreateRequest request) {
        return ApiResponse.success(bankStatementService.create(request));
    }

    @PostMapping("/import")
    @PreAuthorize("hasAnyRole('SYS_ADMIN','FINANCE')")
    public ApiResponse<StatementImportResultVO> importStatements(
            @RequestParam("accountId") Long accountId, @RequestParam("file") MultipartFile file) {
        try (InputStream in = file.getInputStream()) {
            return ApiResponse.success(
                    statementImporter.importStatements(accountId, in, file.getOriginalFilename()));
        } catch (IOException e) {
            throw BusinessException.of(ErrorCode.BIZ_UNPROCESSABLE, "读取流水文件失败");
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<BankStatementVO> detail(@PathVariable Long id) {
        return ApiResponse.success(bankStatementService.detail(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SYS_ADMIN','FINANCE')")
    public ApiResponse<Void> update(
            @PathVariable Long id, @Valid @RequestBody BankStatementUpdateRequest request) {
        bankStatementService.update(id, request);
        return ApiResponse.success();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SYS_ADMIN','FINANCE')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        bankStatementService.delete(id);
        return ApiResponse.success();
    }
}
