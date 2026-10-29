package com.ceams.module.report.vo;

import java.math.BigDecimal;
import lombok.Data;

/** Dashboard 概览视图对象：供首页卡片展示。 */
@Data
public class DashboardVO {

    /** 待审批报销数（biz_receipt.approval_status = PENDING）。 */
    private Long pendingReceiptCount = 0L;

    /** 待审批用印申请数（biz_electronic_seal_application.status = PENDING）。 */
    private Long pendingSealApplicationCount = 0L;

    /** 当前用户未读通知数（biz_user_notification.is_read = 0）。 */
    private Long unreadNotificationCount = 0L;

    /** 项目总数。 */
    private Long projectCount = 0L;

    /** 进行中项目数。 */
    private Long inProgressProjectCount = 0L;

    /** 本月收入（biz_account 汇总）。 */
    private BigDecimal monthIncome = BigDecimal.ZERO;

    /** 本月支出（biz_account 汇总）。 */
    private BigDecimal monthExpense = BigDecimal.ZERO;

    /** 累计收入（biz_account 汇总）。 */
    private BigDecimal totalIncome = BigDecimal.ZERO;

    /** 累计支出（biz_account 汇总）。 */
    private BigDecimal totalExpense = BigDecimal.ZERO;
}
