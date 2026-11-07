package com.ceams.module.ai.tool;

import com.ceams.common.exception.BusinessException;
import com.ceams.common.exception.ErrorCode;
import com.ceams.module.ai.config.AiProperties;
import com.ceams.module.bank.service.ReconciliationService;
import com.ceams.module.bank.vo.BankMatchCandidatesVO;
import com.ceams.module.bank.vo.MatchCandidateVO;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** 单条银行流水匹配候选只读工具（仅 FINANCE / SYS_ADMIN）。 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BankMatchCandidatesTool implements ReadOnlyTool {

    private final ReconciliationService reconciliationService;
    private final AiProperties aiProperties;

    @Override
    public String name() {
        return "queryBankMatchCandidates";
    }

    @Override
    public String description() {
        return "查询单条银行流水的系统侧匹配候选（付款交易 / 总账），返回匹配分与理由。"
                + "statementId 必填；daysTolerance 默认 3（0~30），limit 默认 5（1~10）。"
                + "结果仅为匹配建议，不会自动确认对账，需人工核实；仅 FINANCE / SYS_ADMIN 可调用。";
    }

    @Override
    public Map<String, Object> parameterSchema() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("statementId", ToolSchemas.text("银行流水 ID，必填，可传数字或数字字符串"));
        properties.put("daysTolerance", ToolSchemas.integer("日期容差天数，默认 3，范围 0~30", 0, 30));
        properties.put("limit", ToolSchemas.integer("候选返回条数，默认 5，范围 1~10", 1, 10));
        return ToolSchemas.object(properties, "statementId");
    }

    @Override
    public ToolResult execute(Map<String, Object> args) {
        if (!ToolSecurity.isFinanceOrAdmin()) {
            return ToolResult.denied("当前角色无权查看该数据");
        }
        Long statementId = ToolArgs.longValue(args, "statementId");
        if (statementId == null) {
            throw BusinessException.of(ErrorCode.VALID_ILLEGAL_ARGUMENT, "参数 statementId 不能为空");
        }
        Integer daysTolerance = intValue(args, "daysTolerance");
        Integer limit = intValue(args, "limit");
        BankMatchCandidatesVO candidatesVO;
        try {
            candidatesVO = reconciliationService.matchCandidates(statementId, daysTolerance, limit);
        } catch (BusinessException e) {
            log.debug("银行流水匹配候选查询失败：statementId={}，原因={}", statementId, e.getMessage());
            return ToolResult.empty();
        }

        List<Map<String, Object>> rows = new ArrayList<>();
        Map<String, Object> summaryRow = new LinkedHashMap<>();
        summaryRow.put("summary", true);
        summaryRow.put("statementId", candidatesVO.getStatementId());
        summaryRow.put("candidateCount", candidatesVO.getCandidateCount());
        summaryRow.put("truncated", candidatesVO.isTruncated());
        summaryRow.put("daysTolerance", candidatesVO.getDaysTolerance());
        summaryRow.put("note", candidatesVO.getDisclaimer());
        rows.add(summaryRow);
        if (candidatesVO.getCandidates() != null) {
            for (MatchCandidateVO candidate : candidatesVO.getCandidates()) {
                rows.add(toCandidateRow(candidate));
            }
        }
        return ToolResult.capped(
                rows,
                candidatesVO.getCandidateCount() + 1,
                candidatesVO.isTruncated(),
                aiProperties.getMaxToolRows());
    }

    private static Map<String, Object> toCandidateRow(MatchCandidateVO candidate) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("source", candidate.getSource());
        row.put("sourceId", candidate.getSourceId());
        row.put("transactionDate", candidate.getTransactionDate());
        row.put("amount", candidate.getAmount());
        row.put("amountDiff", candidate.getAmountDiff());
        row.put("dateDiffDays", candidate.getDateDiffDays());
        row.put("score", candidate.getScore());
        row.put(
                "reasons",
                candidate.getReasons() == null ? null : String.join("；", candidate.getReasons()));
        row.put("description", candidate.getDescription());
        return row;
    }

    /** 读取整数参数；越界交由业务服务按默认值兜底。 */
    private static Integer intValue(Map<String, Object> args, String key) {
        Long value = ToolArgs.longValue(args, key);
        if (value == null) {
            return null;
        }
        if (value > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        if (value < Integer.MIN_VALUE) {
            return Integer.MIN_VALUE;
        }
        return value.intValue();
    }
}
