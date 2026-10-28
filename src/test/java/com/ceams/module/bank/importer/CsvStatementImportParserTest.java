package com.ceams.module.bank.importer;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * {@link CsvStatementImportParser} 单元测试：标准表头、中文别名、引号转义、重复流水号识别。
 */
class CsvStatementImportParserTest {

    private final CsvStatementImportParser parser = new CsvStatementImportParser();

    @Test
    void 标准表头解析() {
        String csv = "account_id,transaction_date,amount,type,description,counterparty,bank_reference,balance\n"
                + "ACC001,2026-10-01,1280.50,CREDIT,收到工程款,建筑公司A,REF001,5000.00\n"
                + "ACC001,2026-10-02,-300.00,DEBIT,支付材料款,材料商B,REF002,4700.00\n";

        List<StatementRow> rows = parser.parse(toStream(csv), "a.csv");

        assertEquals(2, rows.size());
        StatementRow first = rows.get(0);
        assertEquals("ACC001", first.getAccountId());
        assertEquals(LocalDate.of(2026, 10, 1), first.getTransactionDate());
        assertEquals(new BigDecimal("1280.50"), first.getAmount());
        assertEquals("CREDIT", first.getType());
        assertEquals("收到工程款", first.getDescription());
        assertEquals("建筑公司A", first.getCounterparty());
        assertEquals("REF001", first.getBankReference());
        assertEquals(new BigDecimal("5000.00"), first.getBalance());
    }

    @Test
    void 中文表头别名解析() {
        String csv = "账号,交易日期,金额,类型,摘要,对方户名,流水号,余额\n"
                + "ACC001,2026/10/01,100.00,收入,测试摘要,对方A,REF100,200.00\n";

        List<StatementRow> rows = parser.parse(toStream(csv), "a.csv");

        assertEquals(1, rows.size());
        StatementRow row = rows.get(0);
        assertEquals("ACC001", row.getAccountId());
        assertEquals(LocalDate.of(2026, 10, 1), row.getTransactionDate());
        assertEquals(new BigDecimal("100.00"), row.getAmount());
        assertEquals("REF100", row.getBankReference());
        assertEquals(new BigDecimal("200.00"), row.getBalance());
    }

    @Test
    void 引号转义解析() {
        String csv = "account_id,description,bank_reference\n"
                + "ACC001,\"收到款项, 含税\",REF001\n";

        List<StatementRow> rows = parser.parse(toStream(csv), "a.csv");

        assertEquals(1, rows.size());
        assertEquals("收到款项, 含税", rows.get(0).getDescription());
    }

    @Test
    void 重复流水号可识别() {
        String csv = "account_id,transaction_date,amount,type,description,counterparty,bank_reference,balance\n"
                + "ACC001,2026-10-01,10.00,CREDIT,第一笔,对方A,REF_DUP,100.00\n"
                + "ACC001,2026-10-01,10.00,CREDIT,重复第二笔,对方A,REF_DUP,110.00\n";

        List<StatementRow> rows = parser.parse(toStream(csv), "a.csv");

        assertEquals(2, rows.size());
        // 解析层正确填充流水号，重复值可被上层去重逻辑识别
        assertEquals("REF_DUP", rows.get(0).getBankReference());
        assertEquals(rows.get(0).getBankReference(), rows.get(1).getBankReference());
    }

    private InputStream toStream(String s) {
        return new ByteArrayInputStream(s.getBytes(StandardCharsets.UTF_8));
    }
}
