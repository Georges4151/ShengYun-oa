package com.ceams.module.ai.service.impl;

import com.ceams.common.response.PageResult;
import com.ceams.module.ai.config.AiProperties;
import com.ceams.module.ai.service.ExpensePreAuditService;
import com.ceams.module.ai.vo.PreAuditWarningVO;
import com.ceams.module.ai.vo.ReceiptPreAuditVO;
import com.ceams.module.expense.enums.VerificationStatus;
import com.ceams.module.expense.service.ReceiptService;
import com.ceams.module.expense.vo.ReceiptVO;
import com.ceams.module.project.service.BudgetService;
import com.ceams.module.project.vo.BudgetVO;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 报销预审服务实现。
 *
 * <p>全部数据来源于既有服务的只读方法（票据明细 / 待审批清单 / 预算分页 / 重复候选查询）， 只产出风险提示，不改变任何审批状态、不调用任何写方法。
 */
@Slf4j
@Service
public class ExpensePreAuditServiceImpl implements ExpensePreAuditService {

    /** 服务端统一时区。 */
    static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");

    /** 待审批清单默认条数。 */
    private static final int DEFAULT_PENDING_LIMIT = 20;

    /** 待审批清单最大条数。 */
    private static final int MAX_PENDING_LIMIT = 50;

    /** 预算汇总单页条数。 */
    private static final int BUDGET_PAGE_SIZE = 50;

    /** 风险级别：高危。 */
    private static final String LEVEL_HIGH = "HIGH";

    /** 风险级别：中危。 */
    private static final String LEVEL_MEDIUM = "MEDIUM";

    /** 风险级别：低危。 */
    private static final String LEVEL_LOW = "LOW";

    /** 风险级别：无风险。 */
    private static final String LEVEL_NONE = "NONE";

    /** 规则：单笔金额超阈值。 */
    private static final String RULE_AMOUNT_THRESHOLD = "AMOUNT_THRESHOLD";

    /** 规则：项目预算使用率告警。 */
    private static final String RULE_BUDGET_USAGE = "BUDGET_USAGE";

    /** 规则：同票号重复。 */
    private static final String RULE_DUPLICATE_NUMBER = "DUPLICATE_NUMBER";

    /** 规则：同金额同日重复。 */
    private static final String RULE_DUPLICATE_AMOUNT = "DUPLICATE_AMOUNT";

    /** 规则：未上传发票附件。 */
    private static final String RULE_MISSING_INVOICE = "MISSING_INVOICE";

    /** 规则：发票待核验。 */
    private static final String RULE_VERIFICATION_PENDING = "VERIFICATION_PENDING";

    /** 规则：发票核验未通过。 */
    private static final String RULE_VERIFICATION_REJECTED = "VERIFICATION_REJECTED";

    /** 百分比基数。 */
    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private final AiProperties aiProperties;
    private final ReceiptService receiptService;
    private final BudgetService budgetService;
    private final Clock clock;

    /** 生产环境构造：使用 Asia/Shanghai 的系统时钟。 */
    @Autowired
    public ExpensePreAuditServiceImpl(
            AiProperties aiProperties, ReceiptService receiptService, BudgetService budgetService) {
        this(aiProperties, receiptService, budgetService, Clock.system(BUSINESS_ZONE));
    }

    /** 可注入 Clock 的构造器，便于单测固定时区与时间窗口。 */
    ExpensePreAuditServiceImpl(
            AiProperties aiProperties,
            ReceiptService receiptService,
            BudgetService budgetService,
            Clock clock) {
        this.aiProperties = aiProperties;
        this.receiptService = receiptService;
        this.budgetService = budgetService;
        this.clock = clock;
    }

    @Override
    public ReceiptPreAuditVO audit(Long receiptId) {
        ReceiptVO receipt = receiptService.detail(receiptId);
        return analyze(receipt);
    }

    @Override
    public List<ReceiptPreAuditVO> pendingList(int limit) {
        int safeLimit = limit <= 0 ? DEFAULT_PENDING_LIMIT : Math.min(limit, MAX_PENDING_LIMIT);
        ReceiptService.PendingApprovalPage page =
                receiptService.pendingApprovals(null, null, null, safeLimit);
        if (page == null || page.items().isEmpty()) {
            return List.of();
        }
        List<ReceiptPreAuditVO> result = new ArrayList<>();
        for (ReceiptVO receipt : page.items()) {
            if (receipt == null) {
                continue;
            }
            try {
                result.add(analyze(receipt));
            } catch (Exception e) {
                log.warn("待审批票据预审失败，已跳过：receiptId={}，原因={}", receipt.getId(), e.getMessage());
            }
        }
        return result;
    }

    /** 单张票据预审：逐条规则独立执行，单条失败只记 warn 不影响其余规则。 */
    private ReceiptPreAuditVO analyze(ReceiptVO receipt) {
        List<PreAuditWarningVO> warnings = new ArrayList<>();
        evaluate(receipt, RULE_AMOUNT_THRESHOLD, () -> evaluateAmount(warnings, receipt));
        evaluate(receipt, RULE_BUDGET_USAGE, () -> evaluateBudget(warnings, receipt));
        evaluate(receipt, RULE_DUPLICATE_NUMBER, () -> evaluateDuplicateNumber(warnings, receipt));
        evaluate(receipt, RULE_DUPLICATE_AMOUNT, () -> evaluateDuplicateAmount(warnings, receipt));
        evaluate(receipt, RULE_MISSING_INVOICE, () -> evaluateMissingInvoice(warnings, receipt));
        evaluate(
                receipt,
                RULE_VERIFICATION_PENDING + "/" + RULE_VERIFICATION_REJECTED,
                () -> evaluateVerification(warnings, receipt));

        ReceiptPreAuditVO vo = new ReceiptPreAuditVO();
        vo.setReceiptId(receipt.getId());
        vo.setReceiptNumber(receipt.getReceiptNumber());
        vo.setReporterName(receipt.getReporterName());
        vo.setProjectId(receipt.getProjectId());
        vo.setAmount(receipt.getAmount());
        vo.setReceiptDate(receipt.getReceiptDate());
        vo.setRiskLevel(aggregateRisk(warnings));
        vo.setWarnings(List.copyOf(warnings));
        return vo;
    }

    /** 金额异常：单笔金额超过预审阈值。 */
    private void evaluateAmount(List<PreAuditWarningVO> warnings, ReceiptVO receipt) {
        BigDecimal amount = receipt.getAmount();
        BigDecimal threshold = aiProperties.getPreaudit().getAmountThreshold();
        if (amount == null || threshold == null || amount.compareTo(threshold) <= 0) {
            return;
        }
        warnings.add(
                warn(
                        RULE_AMOUNT_THRESHOLD,
                        LEVEL_MEDIUM,
                        "单笔金额 "
                                + formatAmount(amount)
                                + " 超过预审阈值 "
                                + formatAmount(threshold)
                                + "，请重点核对发票与合同。"));
    }

    /** 预算使用率：汇总项目全部预算的额度与已用额，按配置阈值提示。 */
    private void evaluateBudget(List<PreAuditWarningVO> warnings, ReceiptVO receipt) {
        Long projectId = receipt.getProjectId();
        if (projectId == null) {
            return;
        }
        PageResult<BudgetVO> page = budgetService.page(1, BUDGET_PAGE_SIZE, projectId);
        if (page == null || page.getRecords() == null || page.getRecords().isEmpty()) {
            return;
        }
        BigDecimal total = BigDecimal.ZERO;
        BigDecimal used = BigDecimal.ZERO;
        for (BudgetVO budget : page.getRecords()) {
            if (budget == null) {
                continue;
            }
            total = total.add(nvl(budget.getAmount()));
            used = used.add(nvl(budget.getUsedAmount()));
        }
        if (total.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        BigDecimal usagePercent = used.multiply(HUNDRED).divide(total, 1, RoundingMode.HALF_UP);
        int highPercent = aiProperties.getPreaudit().getBudgetUsageHighPercent();
        int warnPercent = aiProperties.getPreaudit().getBudgetUsageWarnPercent();
        if (usagePercent.compareTo(BigDecimal.valueOf(highPercent)) >= 0) {
            warnings.add(
                    warn(
                            RULE_BUDGET_USAGE,
                            LEVEL_HIGH,
                            "项目预算使用率 " + formatPercent(usagePercent) + "，已超预算，请核对预算调整审批。"));
        } else if (usagePercent.compareTo(BigDecimal.valueOf(warnPercent)) >= 0) {
            warnings.add(
                    warn(
                            RULE_BUDGET_USAGE,
                            LEVEL_MEDIUM,
                            "项目预算使用率 " + formatPercent(usagePercent) + "，接近上限，请关注预算余额。"));
        }
    }

    /** 重复报销：同票号。 */
    private void evaluateDuplicateNumber(List<PreAuditWarningVO> warnings, ReceiptVO receipt) {
        String receiptNumber = receipt.getReceiptNumber();
        if (!StringUtils.hasText(receiptNumber)) {
            return;
        }
        List<ReceiptVO> duplicates =
                receiptService.findPotentialDuplicates(
                        receipt.getId(), receiptNumber, null, null, null);
        if (duplicates == null || duplicates.isEmpty()) {
            return;
        }
        warnings.add(
                warn(
                        RULE_DUPLICATE_NUMBER,
                        LEVEL_HIGH,
                        "票号 "
                                + receiptNumber
                                + " 已存在相同编号的其他报销单（"
                                + duplicates.size()
                                + " 笔），疑似重复提交。"));
    }

    /** 重复报销：同金额且同日。 */
    private void evaluateDuplicateAmount(List<PreAuditWarningVO> warnings, ReceiptVO receipt) {
        BigDecimal amount = receipt.getAmount();
        LocalDateTime receiptDate = receipt.getReceiptDate();
        if (amount == null || receiptDate == null) {
            return;
        }
        LocalDate day = receiptDate.atZone(clock.getZone()).toLocalDate();
        LocalDateTime dayStart = day.atStartOfDay();
        LocalDateTime dayEnd = LocalDateTime.of(day, LocalTime.of(23, 59, 59));
        List<ReceiptVO> duplicates =
                receiptService.findPotentialDuplicates(
                        receipt.getId(), null, amount, dayStart, dayEnd);
        if (duplicates == null || duplicates.isEmpty()) {
            return;
        }
        warnings.add(
                warn(
                        RULE_DUPLICATE_AMOUNT,
                        LEVEL_MEDIUM,
                        "同日存在 "
                                + duplicates.size()
                                + " 笔相同金额 "
                                + formatAmount(amount)
                                + " 的报销，请核对是否重复。"));
    }

    /** 要件缺失：未上传发票附件。 */
    private void evaluateMissingInvoice(List<PreAuditWarningVO> warnings, ReceiptVO receipt) {
        if (StringUtils.hasText(receipt.getFilePath())) {
            return;
        }
        warnings.add(warn(RULE_MISSING_INVOICE, LEVEL_HIGH, "未上传发票附件，报销要件不完整。"));
    }

    /** 发票核验状态：待核验提示、核验未通过高危。 */
    private void evaluateVerification(List<PreAuditWarningVO> warnings, ReceiptVO receipt) {
        VerificationStatus status = receipt.getVerificationStatus();
        if (status == VerificationStatus.REJECTED) {
            warnings.add(warn(RULE_VERIFICATION_REJECTED, LEVEL_HIGH, "发票核验未通过，需补充要件后重新提交。"));
            return;
        }
        if (status == null || status == VerificationStatus.PENDING) {
            warnings.add(warn(RULE_VERIFICATION_PENDING, LEVEL_MEDIUM, "财务尚未核验发票，建议核验后再审批。"));
        }
    }

    /** 规则执行包装：单条规则异常只记 warn，保留已计算出的提示。 */
    private void evaluate(ReceiptVO receipt, String rule, Runnable action) {
        try {
            action.run();
        } catch (Exception e) {
            log.warn(
                    "报销预审规则 {} 计算失败，已跳过：receiptId={}，原因={}", rule, receipt.getId(), e.getMessage());
        }
    }

    /** 汇总风险级别：有 HIGH 取 HIGH，其次 MEDIUM、LOW，无提示为 NONE。 */
    private static String aggregateRisk(List<PreAuditWarningVO> warnings) {
        boolean high = false;
        boolean medium = false;
        boolean low = false;
        for (PreAuditWarningVO warning : warnings) {
            if (LEVEL_HIGH.equals(warning.getLevel())) {
                high = true;
            } else if (LEVEL_MEDIUM.equals(warning.getLevel())) {
                medium = true;
            } else if (LEVEL_LOW.equals(warning.getLevel())) {
                low = true;
            }
        }
        if (high) {
            return LEVEL_HIGH;
        }
        if (medium) {
            return LEVEL_MEDIUM;
        }
        if (low) {
            return LEVEL_LOW;
        }
        return LEVEL_NONE;
    }

    private static PreAuditWarningVO warn(String rule, String level, String message) {
        return new PreAuditWarningVO(rule, level, message);
    }

    /** 金额格式化：{@code ¥} + 千分位两位小数。 */
    private static String formatAmount(BigDecimal value) {
        return "¥" + String.format(Locale.CHINA, "%,.2f", nvl(value));
    }

    /** 百分比格式化：保留 1 位小数。 */
    private static String formatPercent(BigDecimal value) {
        return String.format(Locale.CHINA, "%.1f%%", nvl(value));
    }

    private static BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
