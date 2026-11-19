package com.ceams.module.hr.support;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 极简 xlsx 写出工具：仅依赖 JDK，手工拼装最小合法 OOXML 包。
 *
 * <p>设计约束：不引入 Apache POI 等第三方依赖；单工作表、无样式、无共享字符串表；
 * 字符串使用 {@code t="inlineStr"}，数值使用裸 {@code <v>}；表头不加粗（避免额外维护 styles.xml）。
 *
 * <p>纯函数式实现：除写出目标流外不持有任何状态；写出完成后关闭 {@link ZipOutputStream}
 * （会一并关闭传入流，便于直接写 Servlet 响应）。
 */
public final class SimpleXlsxWriter {

    /** 默认工作表名称。 */
    private static final String SHEET_NAME = "工资条";

    /** Excel 工作表名最大长度。 */
    private static final int MAX_SHEET_NAME_LENGTH = 31;

    /** XML 声明。 */
    private static final String XML_DECL = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>";

    /** 关系命名空间。 */
    private static final String REL_NAMESPACE =
            "http://schemas.openxmlformats.org/officeDocument/2006/relationships";

    private SimpleXlsxWriter() {}

    /**
     * 写出单工作表 xlsx。
     *
     * @param out 输出流（写到结束后关闭）
     * @param headers 表头文本（按列顺序）
     * @param rows 数据行；{@link BigDecimal} 与其他 {@link Number} 写数值单元格，其余按字符串写出
     * @throws IllegalStateException 压缩包写出失败时抛出
     */
    public static void write(OutputStream out, List<String> headers, List<List<Object>> rows) {
        write(out, SHEET_NAME, headers, rows);
    }

    /**
     * 按指定工作表名写出单工作表 xlsx。
     *
     * @param out 输出流（写到结束后关闭）
     * @param sheetName 工作表名；空白回退为默认名，超长按 Excel 限制截断
     * @param headers 表头文本（按列顺序）
     * @param rows 数据行；{@link BigDecimal} 与其他 {@link Number} 写数值单元格，其余按字符串写出
     * @throws IllegalStateException 压缩包写出失败时抛出
     */
    public static void write(
            OutputStream out,
            String sheetName,
            List<String> headers,
            List<List<Object>> rows) {
        String resolvedSheetName = resolveSheetName(sheetName);
        try (ZipOutputStream zip = new ZipOutputStream(out, StandardCharsets.UTF_8)) {
            putEntry(zip, "[Content_Types].xml", contentTypes());
            putEntry(zip, "_rels/.rels", rootRelationships());
            putEntry(zip, "xl/workbook.xml", workbook(resolvedSheetName));
            putEntry(zip, "xl/_rels/workbook.xml.rels", workbookRelationships());
            putEntry(zip, "xl/worksheets/sheet1.xml", worksheet(headers, rows));
            zip.finish();
        } catch (IOException e) {
            throw new IllegalStateException(resolvedSheetName + " xlsx 写出失败", e);
        }
    }

    /** 工作表名最小安全处理：空白回退默认值，超过 31 个字符按码点截断。 */
    private static String resolveSheetName(String sheetName) {
        if (sheetName == null || sheetName.isBlank()) {
            return SHEET_NAME;
        }
        if (sheetName.codePointCount(0, sheetName.length()) <= MAX_SHEET_NAME_LENGTH) {
            return sheetName;
        }
        return sheetName.substring(
                0, sheetName.offsetByCodePoints(0, MAX_SHEET_NAME_LENGTH));
    }

    private static void putEntry(ZipOutputStream zip, String name, String content)
            throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(content.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    private static String contentTypes() {
        return XML_DECL
                + "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">"
                + "<Default Extension=\"rels\""
                + " ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>"
                + "<Default Extension=\"xml\" ContentType=\"application/xml\"/>"
                + "<Override PartName=\"/xl/workbook.xml\""
                + " ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>"
                + "<Override PartName=\"/xl/worksheets/sheet1.xml\""
                + " ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>"
                + "</Types>";
    }

    private static String rootRelationships() {
        return XML_DECL
                + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
                + "<Relationship Id=\"rId1\" Type=\""
                + REL_NAMESPACE
                + "/officeDocument\" Target=\"xl/workbook.xml\"/>"
                + "</Relationships>";
    }

    private static String workbook(String sheetName) {
        return XML_DECL
                + "<workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\""
                + " xmlns:r=\""
                + REL_NAMESPACE
                + "\"><sheets><sheet name=\""
                + escape(sheetName)
                + "\" sheetId=\"1\" r:id=\"rId1\"/></sheets></workbook>";
    }

    private static String workbookRelationships() {
        return XML_DECL
                + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
                + "<Relationship Id=\"rId1\" Type=\""
                + REL_NAMESPACE
                + "/worksheet\" Target=\"worksheets/sheet1.xml\"/>"
                + "</Relationships>";
    }

    /** 工作表：首行表头，其后数据行；行列下标均从 1 开始。 */
    private static String worksheet(List<String> headers, List<List<Object>> rows) {
        StringBuilder sheet =
                new StringBuilder(4096)
                        .append(XML_DECL)
                        .append(
                                "<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">")
                        .append("<sheetData>");
        int rowIndex = 1;
        if (headers != null && !headers.isEmpty()) {
            appendRow(sheet, rowIndex++, headers.stream().map(value -> (Object) value).toList());
        }
        if (rows != null) {
            for (List<Object> row : rows) {
                appendRow(sheet, rowIndex++, row);
            }
        }
        return sheet.append("</sheetData></worksheet>").toString();
    }

    private static void appendRow(StringBuilder sheet, int rowIndex, List<Object> values) {
        sheet.append("<row r=\"").append(rowIndex).append("\">");
        if (values != null) {
            for (int column = 0; column < values.size(); column++) {
                appendCell(sheet, rowIndex, column, values.get(column));
            }
        }
        sheet.append("</row>");
    }

    private static void appendCell(StringBuilder sheet, int rowIndex, int column, Object value) {
        if (value == null) {
            return;
        }
        String reference = columnName(column) + rowIndex;
        if (value instanceof Number number) {
            sheet.append("<c r=\"")
                    .append(reference)
                    .append("\"><v>")
                    .append(numberText(number))
                    .append("</v></c>");
            return;
        }
        sheet.append("<c r=\"")
                .append(reference)
                .append("\" t=\"inlineStr\"><is><t xml:space=\"preserve\">")
                .append(escape(String.valueOf(value)))
                .append("</t></is></c>");
    }

    /** 数值文本：BigDecimal 保留原精度（禁止科学计数法），其余 Number 直接转字符串。 */
    private static String numberText(Number number) {
        if (number instanceof BigDecimal decimal) {
            return decimal.toPlainString();
        }
        return number.toString();
    }

    /** 0 基列下标转 Excel 列名（A、B … Z、AA …）。 */
    private static String columnName(int column) {
        StringBuilder name = new StringBuilder();
        int value = column + 1;
        while (value > 0) {
            int remainder = (value - 1) % 26;
            name.insert(0, (char) ('A' + remainder));
            value = (value - 1) / 26;
        }
        return name.toString();
    }

    /** XML 文本转义：移除非法控制字符后转义 {@code & < > " '}。 */
    private static String escape(String raw) {
        StringBuilder escaped = new StringBuilder(raw.length() + 16);
        for (int i = 0; i < raw.length(); ) {
            int codePoint = raw.codePointAt(i);
            i += Character.charCount(codePoint);
            if (!isValidXmlChar(codePoint)) {
                continue;
            }
            switch (codePoint) {
                case '&' -> escaped.append("&amp;");
                case '<' -> escaped.append("&lt;");
                case '>' -> escaped.append("&gt;");
                case '"' -> escaped.append("&quot;");
                case '\'' -> escaped.append("&apos;");
                default -> escaped.appendCodePoint(codePoint);
            }
        }
        return escaped.toString();
    }

    /** XML 1.0 合法字符范围（去除 0x00-0x1F 中除制表、换行、回车外的控制字符）。 */
    private static boolean isValidXmlChar(int codePoint) {
        return codePoint == 0x9
                || codePoint == 0xA
                || codePoint == 0xD
                || (codePoint >= 0x20 && codePoint <= 0xD7FF)
                || (codePoint >= 0xE000 && codePoint <= 0xFFFD)
                || (codePoint >= 0x10000 && codePoint <= 0x10FFFF);
    }
}
