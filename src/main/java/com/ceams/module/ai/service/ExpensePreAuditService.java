package com.ceams.module.ai.service;

import com.ceams.module.ai.vo.ReceiptPreAuditVO;
import java.util.List;

/** 报销预审服务（只读规则提示，绝不自动驳回）。 */
public interface ExpensePreAuditService {

    /** 单张票据预审；票据不存在抛 {@code BIZ_NOT_FOUND}。 */
    ReceiptPreAuditVO audit(Long receiptId);

    /** 待审批票据预审清单（limit 1~50，非法值兜底 20，创建时间倒序）。 */
    List<ReceiptPreAuditVO> pendingList(int limit);
}
