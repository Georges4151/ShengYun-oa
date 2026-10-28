package com.ceams.module.bank.importer;

import java.io.InputStream;
import java.util.List;

/**
 * 银行流水导入解析器抽象。
 *
 * <p>将各银行导出的流水文件解析为统一的 {@link StatementRow} 列表；首期实现 CSV，
 * 后续可扩展 Excel（.xlsx）等格式。</p>
 */
public interface StatementImportParser {

    /**
     * 解析流水文件。
     *
     * @param in       文件输入流（调用方负责关闭）
     * @param fileName 文件名（用于格式判断，可为空）
     * @return 解析出的流水行列表
     */
    List<StatementRow> parse(InputStream in, String fileName);
}
