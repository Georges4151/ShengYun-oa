package com.ceams.util;

import com.ceams.model.entity.BankStatement;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class BankStatementParser {

    /**
     * 解析银行流水文件
     * 
     * @param file      文件
     * @param accountId 账户ID
     * @param format    银行格式
     * @return 银行流水列表
     */
    public static List<BankStatement> parseBankStatementFile(File file, Integer accountId, String format) {
        List<BankStatement> statements = new ArrayList<>();

        try {
            if (file.getName().toLowerCase().endsWith(".csv")) {
                statements = parseCSVFile(file, accountId, format);
            } else if (file.getName().toLowerCase().endsWith(".xlsx")
                    || file.getName().toLowerCase().endsWith(".xls")) {
                statements = parseExcelFile(file, accountId, format);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return statements;
    }

    /**
     * 解析CSV文件
     * 
     * @param file      文件
     * @param accountId 账户ID
     * @param format    银行格式
     * @return 银行流水列表
     */
    private static List<BankStatement> parseCSVFile(File file, Integer accountId, String format) throws Exception {
        List<BankStatement> statements = new ArrayList<>();

        try (CSVParser csvParser = new CSVParser(
                new InputStreamReader(new FileInputStream(file), "UTF-8"),
                CSVFormat.DEFAULT.withFirstRecordAsHeader())) {

            for (CSVRecord record : csvParser) {
                BankStatement statement = parseCSVRecord(record, accountId, format);
                if (statement != null) {
                    statements.add(statement);
                }
            }
        }

        return statements;
    }

    /**
     * 解析Excel文件
     * 
     * @param file      文件
     * @param accountId 账户ID
     * @param format    银行格式
     * @return 银行流水列表
     */
    private static List<BankStatement> parseExcelFile(File file, Integer accountId, String format) throws Exception {
        List<BankStatement> statements = new ArrayList<>();

        try (Workbook workbook = WorkbookFactory.create(new FileInputStream(file))) {
            Sheet sheet = workbook.getSheetAt(0);

            // 跳过表头
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row != null) {
                    BankStatement statement = parseExcelRow(row, accountId, format);
                    if (statement != null) {
                        statements.add(statement);
                    }
                }
            }
        }

        return statements;
    }

    /**
     * 解析CSV记录
     * 
     * @param record    CSV记录
     * @param accountId 账户ID
     * @param format    银行格式
     * @return 银行流水
     */
    private static BankStatement parseCSVRecord(CSVRecord record, Integer accountId, String format) {
        BankStatement statement = new BankStatement();
        statement.setAccountId(accountId);
        statement.setCreateTime(new Date());
        statement.setStatus("unreconciled");

        try {
            switch (format.toLowerCase()) {
                case "icbc" -> parseICBCFormat(record, statement);
                case "abc" -> parseABCFormat(record, statement);
                case "ccb" -> parseCCBFormat(record, statement);
                case "boc" -> parseBOCFormat(record, statement);
                default -> parseGenericFormat(record, statement);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        return statement;
    }

    /**
     * 解析Excel行
     * 
     * @param row       Excel行
     * @param accountId 账户ID
     * @param format    银行格式
     * @return 银行流水
     */
    private static BankStatement parseExcelRow(Row row, Integer accountId, String format) {
        BankStatement statement = new BankStatement();
        statement.setAccountId(accountId);
        statement.setCreateTime(new Date());
        statement.setStatus("unreconciled");

        try {
            switch (format.toLowerCase()) {
                case "icbc" -> parseICBCExcelRow(row, statement);
                case "abc" -> parseABCExcelRow(row, statement);
                case "ccb" -> parseCCBExcelRow(row, statement);
                case "boc" -> parseBOCExcelRow(row, statement);
                default -> parseGenericExcelRow(row, statement);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        return statement;
    }

    /**
     * 解析工商银行格式
     */
    private static void parseICBCFormat(CSVRecord record, BankStatement statement) throws ParseException {
        statement.setTransactionDate(parseDate(record.get("交易日期")));
        statement.setAmount(parseAmount(record.get("金额")));
        statement.setType(record.get("交易类型"));
        statement.setDescription(record.get("摘要"));
        statement.setCounterparty(record.get("对方户名"));
        statement.setBankReference(record.get("交易编号"));
        statement.setBalance(parseAmount(record.get("余额")));
    }

    /**
     * 解析农业银行格式
     */
    private static void parseABCFormat(CSVRecord record, BankStatement statement) throws ParseException {
        statement.setTransactionDate(parseDate(record.get("交易日期")));
        statement.setAmount(parseAmount(record.get("金额")));
        statement.setType(record.get("交易类型"));
        statement.setDescription(record.get("摘要"));
        statement.setCounterparty(record.get("对方户名"));
        statement.setBankReference(record.get("交易流水号"));
        statement.setBalance(parseAmount(record.get("余额")));
    }

    /**
     * 解析建设银行格式
     */
    private static void parseCCBFormat(CSVRecord record, BankStatement statement) throws ParseException {
        statement.setTransactionDate(parseDate(record.get("交易日期")));
        statement.setAmount(parseAmount(record.get("发生额")));
        statement.setType(record.get("交易类型"));
        statement.setDescription(record.get("摘要"));
        statement.setCounterparty(record.get("对方名称"));
        statement.setBankReference(record.get("交易编号"));
        statement.setBalance(parseAmount(record.get("余额")));
    }

    /**
     * 解析中国银行格式
     */
    private static void parseBOCFormat(CSVRecord record, BankStatement statement) throws ParseException {
        statement.setTransactionDate(parseDate(record.get("交易日期")));
        statement.setAmount(parseAmount(record.get("金额")));
        statement.setType(record.get("交易类型"));
        statement.setDescription(record.get("摘要"));
        statement.setCounterparty(record.get("对方户名"));
        statement.setBankReference(record.get("交易序号"));
        statement.setBalance(parseAmount(record.get("余额")));
    }

    /**
     * 解析通用格式
     */
    private static void parseGenericFormat(CSVRecord record, BankStatement statement) throws ParseException {
        // 尝试通用字段映射
        statement.setTransactionDate(parseDate(record.get("日期")));
        statement.setAmount(parseAmount(record.get("金额")));
        statement.setType(record.get("类型"));
        statement.setDescription(record.get("摘要"));
        statement.setCounterparty(record.get("对方"));
        statement.setBankReference(record.get("流水号"));
        statement.setBalance(parseAmount(record.get("余额")));
    }

    /**
     * 解析工商银行Excel格式
     */
    private static void parseICBCExcelRow(Row row, BankStatement statement) throws ParseException {
        statement.setTransactionDate(parseDate(getCellValue(row.getCell(0))));
        statement.setAmount(parseAmount(getCellValue(row.getCell(1))));
        statement.setType(getCellValue(row.getCell(2)));
        statement.setDescription(getCellValue(row.getCell(3)));
        statement.setCounterparty(getCellValue(row.getCell(4)));
        statement.setBankReference(getCellValue(row.getCell(5)));
        statement.setBalance(parseAmount(getCellValue(row.getCell(6))));
    }

    /**
     * 解析农业银行Excel格式
     */
    private static void parseABCExcelRow(Row row, BankStatement statement) throws ParseException {
        statement.setTransactionDate(parseDate(getCellValue(row.getCell(0))));
        statement.setAmount(parseAmount(getCellValue(row.getCell(1))));
        statement.setType(getCellValue(row.getCell(2)));
        statement.setDescription(getCellValue(row.getCell(3)));
        statement.setCounterparty(getCellValue(row.getCell(4)));
        statement.setBankReference(getCellValue(row.getCell(5)));
        statement.setBalance(parseAmount(getCellValue(row.getCell(6))));
    }

    /**
     * 解析建设银行Excel格式
     */
    private static void parseCCBExcelRow(Row row, BankStatement statement) throws ParseException {
        statement.setTransactionDate(parseDate(getCellValue(row.getCell(0))));
        statement.setAmount(parseAmount(getCellValue(row.getCell(1))));
        statement.setType(getCellValue(row.getCell(2)));
        statement.setDescription(getCellValue(row.getCell(3)));
        statement.setCounterparty(getCellValue(row.getCell(4)));
        statement.setBankReference(getCellValue(row.getCell(5)));
        statement.setBalance(parseAmount(getCellValue(row.getCell(6))));
    }

    /**
     * 解析中国银行Excel格式
     */
    private static void parseBOCExcelRow(Row row, BankStatement statement) throws ParseException {
        statement.setTransactionDate(parseDate(getCellValue(row.getCell(0))));
        statement.setAmount(parseAmount(getCellValue(row.getCell(1))));
        statement.setType(getCellValue(row.getCell(2)));
        statement.setDescription(getCellValue(row.getCell(3)));
        statement.setCounterparty(getCellValue(row.getCell(4)));
        statement.setBankReference(getCellValue(row.getCell(5)));
        statement.setBalance(parseAmount(getCellValue(row.getCell(6))));
    }

    /**
     * 解析通用Excel格式
     */
    private static void parseGenericExcelRow(Row row, BankStatement statement) throws ParseException {
        statement.setTransactionDate(parseDate(getCellValue(row.getCell(0))));
        statement.setAmount(parseAmount(getCellValue(row.getCell(1))));
        statement.setType(getCellValue(row.getCell(2)));
        statement.setDescription(getCellValue(row.getCell(3)));
        statement.setCounterparty(getCellValue(row.getCell(4)));
        statement.setBankReference(getCellValue(row.getCell(5)));
        statement.setBalance(parseAmount(getCellValue(row.getCell(6))));
    }

    /**
     * 解析日期
     */
    private static Date parseDate(String dateStr) throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        return sdf.parse(dateStr);
    }

    /**
     * 解析金额
     */
    private static BigDecimal parseAmount(String amountStr) {
        amountStr = amountStr.replaceAll(",", "").replaceAll("\\s", "");
        return new BigDecimal(amountStr);
    }

    /**
     * 获取Excel单元格值
     */
    private static String getCellValue(Cell cell) {
        if (cell == null) {
            return "";
        }

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                    return sdf.format(cell.getDateCellValue());
                } else {
                    return String.valueOf(cell.getNumericCellValue());
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return "";
        }
    }
}