package com.ceams.module.bank.importer;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ceams.common.exception.BusinessException;
import com.ceams.common.exception.ErrorCode;
import com.ceams.module.bank.entity.BankStatement;
import com.ceams.module.bank.enums.BankStatementStatus;
import com.ceams.module.bank.enums.TransactionType;
import com.ceams.module.bank.mapper.BankStatementMapper;
import com.ceams.module.bank.vo.StatementImportResultVO;
import com.ceams.module.payment.service.PaymentAccountQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 银行流水导入器实现：解析 + 校验 + 按 account_id + bank_reference 去重 + 逐条落库。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StatementImporterImpl implements StatementImporter {

    private final CsvStatementImportParser csvStatementImportParser;
    private final BankStatementMapper bankStatementMapper;
    private final PaymentAccountQueryService paymentAccountQueryService;

    @Override
    public StatementImportResultVO importStatements(Long accountId, InputStream in, String fileName) {
        if (accountId == null) {
            throw BusinessException.of(ErrorCode.BIZ_UNPROCESSABLE, "付款账户ID不能为空");
        }
        if (!paymentAccountQueryService.exists(accountId)) {
            throw BusinessException.of(ErrorCode.BIZ_NOT_FOUND, "付款账户不存在");
        }

        List<StatementRow> rows = csvStatementImportParser.parse(in, fileName);
        StatementImportResultVO result = new StatementImportResultVO();
        result.setTotal(rows.size());

        Set<String> existingRefs = loadExistingReferences(accountId);
        List<StatementImportResultVO.ErrorItem> errors = new ArrayList<>();

        int success = 0;
        int skipped = 0;
        int rowNo = 2; // 表头占第 1 行，数据行从 2 开始

        for (StatementRow row : rows) {
            rowNo++;
            String reason = validate(row);
            if (reason != null) {
                errors.add(new StatementImportResultVO.ErrorItem(rowNo, reason));
                continue;
            }
            String reference = StringUtils.trimToNull(row.getBankReference());
            if (reference != null && existingRefs.contains(reference)) {
                skipped++;
                continue;
            }

            BankStatement statement = new BankStatement();
            statement.setAccountId(accountId);
            statement.setTransactionDate(row.getTransactionDate().atStartOfDay());
            statement.setAmount(row.getAmount());
            statement.setType(normalizeType(row.getType()));
            statement.setDescription(row.getDescription());
            statement.setCounterparty(row.getCounterparty());
            statement.setBankReference(reference);
            statement.setBalance(row.getBalance());
            statement.setStatus(BankStatementStatus.UNRECONCILED);
            bankStatementMapper.insert(statement);
            success++;

            if (reference != null) {
                existingRefs.add(reference);
            }
        }

        result.setSuccess(success);
        result.setSkipped(skipped);
        result.setFailed(errors.size());
        result.setErrors(errors);
        return result;
    }

    private Set<String> loadExistingReferences(Long accountId) {
        List<BankStatement> existing = bankStatementMapper.selectList(
                new LambdaQueryWrapper<BankStatement>()
                        .eq(BankStatement::getAccountId, accountId)
                        .isNotNull(BankStatement::getBankReference)
                        .select(BankStatement::getBankReference));
        return existing.stream()
                .map(BankStatement::getBankReference)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toSet());
    }

    /** 返回 null 表示校验通过，否则返回失败原因。 */
    private String validate(StatementRow row) {
        if (row.getTransactionDate() == null) {
            return "交易日期缺失或格式错误";
        }
        if (row.getAmount() == null) {
            return "金额缺失或格式错误";
        }
        if (normalizeType(row.getType()) == null) {
            return "收支类型无法识别（应为 INCOME/EXPENSE/收入/支出/借/贷）";
        }
        return null;
    }

    /** 将各银行导出的类型文本归一化为 {@link TransactionType}，无法识别返回 null。 */
    private TransactionType normalizeType(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String value = raw.trim().toUpperCase(Locale.ROOT);
        return switch (value) {
            case "INCOME", "收入", "贷", "CREDIT", "CR", "C" -> TransactionType.INCOME;
            case "EXPENSE", "支出", "借", "DEBIT", "DR", "D" -> TransactionType.EXPENSE;
            default -> null;
        };
    }
}
