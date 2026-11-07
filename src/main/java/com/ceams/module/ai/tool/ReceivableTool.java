package com.ceams.module.ai.tool;

import com.ceams.common.exception.BusinessException;
import com.ceams.module.ai.config.AiProperties;
import com.ceams.module.project.enums.ApprovalStatus;
import com.ceams.module.project.service.ProgressPaymentService;
import com.ceams.module.project.service.ProjectService;
import com.ceams.module.project.vo.ProgressPaymentVO;
import com.ceams.module.project.vo.ProjectVO;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** 项目进度款只读工具。 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReceivableTool implements ReadOnlyTool {

    /** 审批通过后超过该天数仍未付清，视为逾期。 */
    private static final int OVERDUE_DAYS = 30;

    private final ProgressPaymentService progressPaymentService;
    private final ProjectService projectService;
    private final AiProperties aiProperties;

    @Override
    public String name() {
        return "queryReceivable";
    }

    @Override
    public String description() {
        return "查询项目进度款：申请金额、批准金额、实付金额、未付余额与逾期标记。"
                + "projectId 可选，用于限定单个项目；status 可选 DRAFT/PENDING/APPROVED/REJECTED。"
                + "STAFF 角色只能看到本人提交的进度款申请。";
    }

    @Override
    public Map<String, Object> parameterSchema() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("projectId", ToolSchemas.text("项目 ID，可传数字或数字字符串，可选"));
        properties.put(
                "status",
                ToolSchemas.choice("审批状态，可选", "DRAFT", "PENDING", "APPROVED", "REJECTED"));
        properties.put("limit", ToolSchemas.integer("返回行数，默认 20，最大 200", 1, 200));
        return ToolSchemas.object(properties);
    }

    @Override
    public ToolResult execute(Map<String, Object> args) {
        int limit = ToolArgs.limit(args, aiProperties.getMaxToolRows());
        Long projectId = ToolArgs.longValue(args, "projectId");
        ApprovalStatus status = ToolArgs.enumValue(args, "status", ApprovalStatus.class);
        Long userId = ToolSecurity.currentUserId();
        boolean unrestricted = ToolSecurity.isFinanceOrAdmin();
        if (!unrestricted && userId == null) {
            return ToolResult.denied("无法识别当前用户，已拒绝查询进度款数据");
        }
        int pageSize = Math.min(limit, PageCollector.MAX_PAGE_SIZE);
        PageCollector.Result<ProgressPaymentVO> collected =
                PageCollector.collect(
                        page ->
                                unrestricted
                                        ? progressPaymentService.page(
                                                page, pageSize, projectId, status)
                                        : progressPaymentService.pageForApplicant(
                                                page, pageSize, projectId, status, userId),
                        limit);
        Map<Long, String> projectNames = new HashMap<>();
        List<Map<String, Object>> rows = new ArrayList<>();
        for (ProgressPaymentVO vo : collected.items()) {
            rows.add(toRow(vo, projectNames));
        }
        return ToolResult.capped(
                rows,
                (int) Math.min(collected.total(), Integer.MAX_VALUE),
                aiProperties.getMaxToolRows());
    }

    private Map<String, Object> toRow(ProgressPaymentVO vo, Map<Long, String> projectNames) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("projectId", vo.getProjectId() == null ? null : String.valueOf(vo.getProjectId()));
        row.put("projectName", projectName(vo.getProjectId(), projectNames));
        row.put("paymentPeriod", vo.getPaymentPeriod());
        row.put("applicationAmount", amount(vo.getApplicationAmount()));
        row.put("approvedAmount", amount(vo.getApprovedAmount()));
        row.put("paymentAmount", amount(vo.getPaymentAmount()));
        row.put("outstandingAmount", outstanding(vo));
        row.put("status", vo.getStatus() == null ? null : vo.getStatus().name());
        row.put("applicationDate", vo.getApplicationDate());
        row.put("approvalDate", vo.getApprovalDate());
        row.put("paymentDate", vo.getPaymentDate());
        row.put("overdue", isOverdue(vo));
        return row;
    }

    private String projectName(Long projectId, Map<Long, String> cache) {
        if (projectId == null) {
            return null;
        }
        return cache.computeIfAbsent(projectId, this::loadProjectName);
    }

    private String loadProjectName(Long projectId) {
        try {
            ProjectVO project = projectService.detail(projectId);
            return project == null ? null : project.getProjectName();
        } catch (BusinessException ex) {
            log.debug("进度款关联项目 {} 不存在，忽略项目名", projectId);
            return null;
        }
    }

    private static BigDecimal outstanding(ProgressPaymentVO vo) {
        BigDecimal approved =
                vo.getApprovedAmount() == null ? vo.getApplicationAmount() : vo.getApprovedAmount();
        if (approved == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal paid = vo.getPaymentAmount() == null ? BigDecimal.ZERO : vo.getPaymentAmount();
        BigDecimal remaining = approved.subtract(paid);
        return remaining.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : remaining;
    }

    private static boolean isOverdue(ProgressPaymentVO vo) {
        if (vo.getStatus() != ApprovalStatus.APPROVED) {
            return false;
        }
        BigDecimal approved =
                vo.getApprovedAmount() == null ? vo.getApplicationAmount() : vo.getApprovedAmount();
        BigDecimal paid = vo.getPaymentAmount() == null ? BigDecimal.ZERO : vo.getPaymentAmount();
        if (approved == null || paid.compareTo(approved) >= 0) {
            return false;
        }
        LocalDate approvalDate = vo.getApprovalDate();
        return approvalDate != null
                && approvalDate.plusDays(OVERDUE_DAYS).isBefore(LocalDate.now());
    }

    private static BigDecimal amount(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
