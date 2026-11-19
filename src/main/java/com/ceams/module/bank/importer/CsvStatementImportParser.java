package com.ceams.module.bank.importer;

import com.ceams.common.exception.BusinessException;
import com.ceams.common.exception.ErrorCode;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** CSV 银行流水解析器：手写轻量 CSV 解析（支持引号包裹与双引号转义）， 通过表头别名映射到 {@link StatementRow} 字段，兼容常见中英文列名。 */
@Slf4j
@Component
public class CsvStatementImportParser implements StatementImportParser {

    /** 规范化后的表头别名 -> 规范字段名。 */
    private static final Map<String, String> ALIAS_TO_FIELD = buildAliasMap();

    private static Map<String, String> buildAliasMap() {
        Map<String, String> map = new HashMap<>();
        put(map, "accountId", "account_id", "accountid", "account", "账号", "账户号", "账户");
        put(
                map,
                "transactionDate",
                "transaction_date",
                "transactiondate",
                "date",
                "交易日期",
                "交易时间",
                "日期");
        put(map, "amount", "amount", "金额", "交易金额", "发生额");
        put(map, "type", "type", "类型", "收支类型", "借贷标志", "借贷");
        put(map, "description", "description", "summary", "摘要", "描述", "备注", "附言");
        put(
                map,
                "counterparty",
                "counterparty",
                "counterparty_name",
                "对方户名",
                "对方账户名",
                "对方单位",
                "对方");
        put(
                map,
                "bankReference",
                "bank_reference",
                "bankreference",
                "reference",
                "ref_no",
                "refno",
                "流水号",
                "银行流水号",
                "交易流水号",
                "交易参考号");
        put(map, "balance", "balance", "余额", "账户余额", "账面余额");
        return map;
    }

    private static void put(Map<String, String> map, String field, String... aliases) {
        for (String alias : aliases) {
            map.put(normalize(alias), field);
        }
    }

    @Override
    public List<StatementRow> parse(InputStream in, String fileName) {
        List<String> lines = readLines(in);
        if (lines.isEmpty()) {
            throw BusinessException.of(ErrorCode.VALID_ILLEGAL_ARGUMENT, "流水文件内容为空");
        }

        List<String> header = parseLine(lines.get(0));
        List<String> columnFields = new ArrayList<>(header.size());
        for (String head : header) {
            columnFields.add(ALIAS_TO_FIELD.get(normalize(head)));
        }

        List<StatementRow> rows = new ArrayList<>();
        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.trim().isEmpty()) {
                continue;
            }
            List<String> values = parseLine(line);
            StatementRow row = new StatementRow();
            for (int c = 0; c < columnFields.size(); c++) {
                String field = columnFields.get(c);
                if (field == null) {
                    continue;
                }
                String raw = c < values.size() ? values.get(c) : "";
                setField(row, field, raw);
            }
            rows.add(row);
        }
        return rows;
    }

    private void setField(StatementRow row, String field, String raw) {
        String value = raw == null ? "" : raw.trim();
        switch (field) {
            case "accountId" -> row.setAccountId(value);
            case "transactionDate" -> row.setTransactionDate(toLocalDate(value));
            case "amount" -> row.setAmount(toBigDecimal(value));
            case "type" -> row.setType(value);
            case "description" -> row.setDescription(value);
            case "counterparty" -> row.setCounterparty(value);
            case "bankReference" -> row.setBankReference(value);
            case "balance" -> row.setBalance(toBigDecimal(value));
            default -> {
                // 未识别的字段忽略
            }
        }
    }

    private BigDecimal toBigDecimal(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(value.replace(",", "").trim());
        } catch (NumberFormatException e) {
            log.warn("金额解析失败，忽略该字段值: {}", value);
            return null;
        }
    }

    private LocalDate toLocalDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.replace('/', '-').trim();
        try {
            return LocalDate.parse(normalized);
        } catch (Exception e) {
            log.warn("交易日期解析失败，忽略该字段值: {}", value);
            return null;
        }
    }

    private List<String> readLines(InputStream in) {
        List<String> lines = new ArrayList<>();
        try (BufferedReader reader =
                new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(stripBom(line));
            }
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.VALID_ILLEGAL_ARGUMENT, "读取流水文件失败", e);
        }
        return lines;
    }

    private String stripBom(String line) {
        if (line != null && !line.isEmpty() && line.charAt(0) == '\uFEFF') {
            return line.substring(1);
        }
        return line;
    }

    /** 简单 CSV 行解析：支持双引号包裹与相邻双引号转义。 */
    private List<String> parseLine(String line) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        current.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    current.append(c);
                }
            } else {
                if (c == '"') {
                    inQuotes = true;
                } else if (c == ',') {
                    result.add(current.toString());
                    current.setLength(0);
                } else {
                    current.append(c);
                }
            }
        }
        result.add(current.toString());
        return result;
    }

    /** 表头归一化：去 BOM/空格/下划线/连字符并转小写，使 snake_case 与 camelCase 可互相匹配。 */
    private static String normalize(String s) {
        if (s == null) {
            return "";
        }
        String value = s.replace("\uFEFF", "").trim().toLowerCase(Locale.ROOT);
        return value.replace("_", "").replace("-", "").replace(" ", "");
    }
}
