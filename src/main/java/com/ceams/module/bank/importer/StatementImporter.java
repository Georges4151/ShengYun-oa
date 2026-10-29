package com.ceams.module.bank.importer;

import com.ceams.module.bank.vo.StatementImportResultVO;
import java.io.InputStream;

/**
 * 银行流水落库组件：负责校验、按 account_id + bank_reference 去重、批量落库。
 *
 * <p>解析交给 {@link StatementImportParser}，本组件只关注业务落库。
 */
public interface StatementImporter {

    /**
     * 导入银行流水。
     *
     * @param accountId 付款账户ID（逻辑引用 biz_payment_account.id）
     * @param in 文件输入流（调用方负责关闭）
     * @param fileName 文件名（用于格式判断，可为空）
     * @return 导入结果统计（成功/跳过/失败）
     */
    StatementImportResultVO importStatements(Long accountId, InputStream in, String fileName);
}
