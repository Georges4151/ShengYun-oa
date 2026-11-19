package com.ceams.module.hr.support;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.junit.jupiter.api.Test;

/**
 * 极简 xlsx 写出工具单元测试：压缩包条目完整性、中文表头、数值单元格、XML 转义与空数据行。
 *
 * <p>校验方式：写出到 {@link ByteArrayOutputStream}，再用 {@link ZipInputStream} 逐条目读取并断言。
 */
class SimpleXlsxWriterTest {

    /** OOXML 最小条目集合。 */
    private static final List<String> EXPECTED_ENTRIES =
            List.of(
                    "[Content_Types].xml",
                    "_rels/.rels",
                    "xl/workbook.xml",
                    "xl/_rels/workbook.xml.rels",
                    "xl/worksheets/sheet1.xml");

    @Test
    void writeShouldProduceFiveZipEntries() {
        byte[] bytes = writeToBytes(List.of("工号"), List.of(List.<Object>of("E001")));

        Map<String, String> entries = unzip(bytes);

        assertThat(entries).containsOnlyKeys(EXPECTED_ENTRIES);
        assertThat(entries.get("xl/worksheets/sheet1.xml")).contains("<sheetData>");
    }

    @Test
    void writeShouldContainChineseHeadersAndNumericCells() {
        byte[] bytes =
                writeToBytes(
                        List.of("工号", "姓名", "应发合计"),
                        List.of(List.<Object>of("E001", "张三", new BigDecimal("12345.67"))));

        String sheet = unzip(bytes).get("xl/worksheets/sheet1.xml");

        assertThat(sheet).contains("工号").contains("姓名").contains("应发合计").contains("张三");
        assertThat(sheet).contains("<v>12345.67</v>");
        assertThat(sheet).contains("<row r=\"1\">").contains("<row r=\"2\">");
        assertThat(sheet).contains("<c r=\"C2\"><v>");
    }

    @Test
    void writeShouldEscapeSpecialCharactersAndDropIllegalControls() {
        byte[] bytes =
                writeToBytes(
                        List.of("备注"),
                        List.of(List.<Object>of("<a>&\"'</a>\u0001")));

        String sheet = unzip(bytes).get("xl/worksheets/sheet1.xml");

        assertThat(sheet).contains("&lt;a&gt;&amp;&quot;&apos;&lt;/a&gt;");
        assertThat(sheet).doesNotContain("\u0001");
    }

    @Test
    void writeShouldProduceValidStructureWithoutRows() {
        byte[] bytes = writeToBytes(List.of("列一"), List.of());

        Map<String, String> entries = unzip(bytes);

        assertThat(entries).containsOnlyKeys(EXPECTED_ENTRIES);
        assertThat(entries.get("xl/worksheets/sheet1.xml"))
                .contains("<sheetData>")
                .contains("<row r=\"1\">")
                .contains("</sheetData>");
        assertThat(entries.get("xl/workbook.xml")).contains("工资条");
    }

    private byte[] writeToBytes(List<String> headers, List<List<Object>> rows) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        SimpleXlsxWriter.write(out, headers, rows);
        return out.toByteArray();
    }

    private Map<String, String> unzip(byte[] bytes) {
        Map<String, String> entries = new LinkedHashMap<>();
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(bytes))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                entries.put(
                        entry.getName(),
                        new String(zip.readAllBytes(), StandardCharsets.UTF_8));
            }
        } catch (Exception e) {
            throw new IllegalStateException("测试解压 xlsx 失败", e);
        }
        return entries;
    }
}
