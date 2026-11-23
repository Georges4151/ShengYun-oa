package com.ceams.module.ai.service.impl;

import com.ceams.module.ai.client.ChatMessage;
import com.ceams.module.ai.client.OllamaChatResult;
import com.ceams.module.ai.client.AiChatClient;
import com.ceams.module.ai.config.AiProperties;
import com.ceams.module.ai.service.ContractRiskHintService;
import com.ceams.module.ai.vo.ContractClausePointVO;
import com.ceams.module.ai.vo.ContractRiskHintVO;
import com.ceams.module.ai.vo.ContractRiskVO;
import com.ceams.module.project.enums.ContractStatus;
import com.ceams.module.project.service.SubcontractorContractService;
import com.ceams.module.project.vo.SubcontractorContractVO;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 合同要点与风险提示服务实现。
 *
 * <p>数据来源仅为 {@link SubcontractorContractService#detail(Long)} 的只读结果（合同表无正文、无附件上传 接口，本服务不做任何文档解析），
 * 全部风险均由固定规则产出，AI 只做可选复述且必须通过数字保全校验。
 */
@Slf4j
@Service
public class ContractRiskHintServiceImpl implements ContractRiskHintService {

    /** 服务端统一时区。 */
    static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");

    /** 风险级别：高危。 */
    private static final String LEVEL_HIGH = "HIGH";

    /** 风险级别：中危。 */
    private static final String LEVEL_MEDIUM = "MEDIUM";

    /** 风险级别：低危。 */
    private static final String LEVEL_LOW = "LOW";

    /** 风险级别：无风险。 */
    private static final String LEVEL_NONE = "NONE";

    /** 规则：已付款超过合同金额。 */
    private static final String RISK_OVERPAID = "OVERPAID";

    /** 规则：合同已到期但仍为生效状态。 */
    private static final String RISK_EXPIRED_ACTIVE = "EXPIRED_ACTIVE";

    /** 规则：合同即将到期。 */
    private static final String RISK_ENDING_SOON = "ENDING_SOON";

    /** 规则：缺少截止日期。 */
    private static final String RISK_MISSING_END_DATE = "MISSING_END_DATE";

    /** 规则：附件未归档。 */
    private static final String RISK_MISSING_ATTACHMENT = "MISSING_ATTACHMENT";

    /** 规则：付款比例过高。 */
    private static final String RISK_HIGH_PAYMENT_RATIO = "HIGH_PAYMENT_RATIO";

    /** 规则：合同金额无效。 */
    private static final String RISK_ZERO_AMOUNT = "ZERO_AMOUNT";

    /** 规则：缺少备注说明。 */
    private static final String RISK_MISSING_DESCRIPTION = "MISSING_DESCRIPTION";

    /** 即将到期判定窗口（天）。 */
    private static final int ENDING_SOON_DAYS = 30;

    /** 付款比例告警阈值（百分数）。 */
    private static final BigDecimal HIGH_PAYMENT_RATIO_THRESHOLD = new BigDecimal("90");

    /** 百分比基数。 */
    private static final BigDecimal HUNDRED = new BigDecimal("100");

    /** 付款比例零值（保留两位小数）。 */
    private static final BigDecimal ZERO_RATIO = new BigDecimal("0.00");

    /** 最多返回风险条数。 */
    private static final int MAX_RISK_COUNT = 8;

    /** 空值占位文案。 */
    private static final String TEXT_UNKNOWN = "未填写";

    /** 只读声明。 */
    private static final String DISCLAIMER = "AI 建议，请核实；仅提示，不修改合同";

    /** 风险等级权重（用于降序排序）。 */
    private static final Map<String, Integer> LEVEL_RANK =
            Map.of(LEVEL_HIGH, 3, LEVEL_MEDIUM, 2, LEVEL_LOW, 1, LEVEL_NONE, 0);

    /** 同级风险固定输出顺序。 */
    private static final List<String> RISK_CODE_ORDER =
            List.of(
                    RISK_OVERPAID,
                    RISK_EXPIRED_ACTIVE,
                    RISK_ENDING_SOON,
                    RISK_MISSING_END_DATE,
                    RISK_MISSING_ATTACHMENT,
                    RISK_HIGH_PAYMENT_RATIO,
                    RISK_ZERO_AMOUNT,
                    RISK_MISSING_DESCRIPTION);

    /** AI 复述系统提示：只允许复述，禁止改动任何数字。 */
    private static final String AI_NOTE_SYSTEM_PROMPT =
            "你是 CEAMS 建筑企业账目管理系统的合同文字助手。只能复述用户给出的合同事实与风险提示，"
                    + "禁止新增、推算或改写任何数字，不得虚构事实；使用中文，输出 2~3 句，"
                    + "结尾必须保留「请核实」的意思。";

    /** 数字字面量正则（用于 AI 复述的数字保全校验）。 */
    private static final Pattern NUMBER_PATTERN = Pattern.compile("\\d+(?:\\.\\d+)?");

    /** 日期字符串正则（比对数字前剔除，避免把日期片段误判为业务数字）。 */
    private static final Pattern DATE_PATTERN = Pattern.compile("\\d{4}[-/.]\\d{1,2}[-/.]\\d{1,2}");

    /** 千分位逗号（仅当两侧都是数字时匹配），用于数字比对前的归一化。 */
    private static final Pattern THOUSANDS_SEPARATOR_PATTERN = Pattern.compile("(?<=\\d),(?=\\d)");

    private final AiProperties aiProperties;
    private final SubcontractorContractService subcontractorContractService;
    private final AiChatClient aiChatClient;
    private final Clock clock;

    /** 生产环境构造：使用 Asia/Shanghai 的系统时钟。 */
    @Autowired
    public ContractRiskHintServiceImpl(
            AiProperties aiProperties,
            SubcontractorContractService subcontractorContractService,
            AiChatClient aiChatClient) {
        this(aiProperties, subcontractorContractService, aiChatClient, Clock.system(BUSINESS_ZONE));
    }

    /** 可注入 Clock 的构造器，便于单测固定「当天」。 */
    ContractRiskHintServiceImpl(
            AiProperties aiProperties,
            SubcontractorContractService subcontractorContractService,
            AiChatClient aiChatClient,
            Clock clock) {
        this.aiProperties = aiProperties;
        this.subcontractorContractService = subcontractorContractService;
        this.aiChatClient = aiChatClient;
        this.clock = clock;
    }

    @Override
    public ContractRiskHintVO riskHints(Long contractId) {
        SubcontractorContractVO contract = subcontractorContractService.detail(contractId);
        LocalDate today = LocalDate.now(clock);

        BigDecimal contractAmount = contract.getContractAmount();
        BigDecimal paidAmount = nvl(contract.getPaidAmount());
        BigDecimal remainingAmount =
                contractAmount == null ? BigDecimal.ZERO : contractAmount.subtract(paidAmount);
        BigDecimal paidRatio = calculatePaidRatio(contractAmount, paidAmount);

        List<ContractClausePointVO> clausePoints =
                buildClausePoints(contract, paidAmount, remainingAmount, paidRatio);
        List<ContractRiskVO> risks =
                evaluateRisks(contract, contractAmount, paidAmount, paidRatio, today);

        boolean aiEnabled =
                aiChatClient.isEnabled() && aiProperties.getContractRisk().isAiNoteEnabled();

        ContractRiskHintVO vo = new ContractRiskHintVO();
        vo.setContractId(contract.getId() == null ? null : String.valueOf(contract.getId()));
        vo.setContractNumber(contract.getContractNumber());
        vo.setContractName(contract.getContractName());
        vo.setSubcontractorId(
                contract.getSubcontractorId() == null
                        ? null
                        : String.valueOf(contract.getSubcontractorId()));
        vo.setProjectId(
                contract.getProjectId() == null ? null : String.valueOf(contract.getProjectId()));
        vo.setStatus(contract.getStatus());
        vo.setContractAmount(contractAmount);
        vo.setPaidAmount(paidAmount);
        vo.setRemainingAmount(remainingAmount);
        vo.setPaidRatio(paidRatio);
        vo.setStartDate(contract.getStartDate());
        vo.setEndDate(contract.getEndDate());
        vo.setClausePoints(clausePoints);
        vo.setRisks(risks);
        vo.setRiskLevel(highestRiskLevel(risks));
        vo.setAiEnabled(aiEnabled);
        vo.setAiNote(aiEnabled ? generateAiNote(clausePoints, risks, today) : null);
        vo.setDisclaimer(DISCLAIMER);
        return vo;
    }

    private static BigDecimal calculatePaidRatio(BigDecimal contractAmount, BigDecimal paidAmount) {
        if (contractAmount == null || contractAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return ZERO_RATIO;
        }
        return paidAmount.multiply(HUNDRED).divide(contractAmount, 2, RoundingMode.HALF_UP);
    }

    private static List<ContractClausePointVO> buildClausePoints(
            SubcontractorContractVO contract,
            BigDecimal paidAmount,
            BigDecimal remainingAmount,
            BigDecimal paidRatio) {
        boolean archived =
                StringUtils.hasText(contract.getFilePath())
                        || StringUtils.hasText(contract.getFileName());
        List<ContractClausePointVO> points = new ArrayList<>();
        points.add(point("合同名称", textOrUnknown(contract.getContractName())));
        points.add(point("合同编号", textOrUnknown(contract.getContractNumber())));
        points.add(point("合同金额", formatAmount(contract.getContractAmount())));
        points.add(point("已付金额", formatAmount(paidAmount)));
        points.add(point("未付金额", formatAmount(remainingAmount)));
        points.add(point("付款比例", paidRatio.toPlainString() + "%"));
        points.add(point("生效日期", formatDate(contract.getStartDate())));
        points.add(point("截止日期", formatDate(contract.getEndDate())));
        points.add(
                point(
                        "当前状态",
                        contract.getStatus() == null ? TEXT_UNKNOWN : contract.getStatus().name()));
        points.add(point("附件归档", archived ? "已归档" : "未归档"));
        return List.copyOf(points);
    }

    /** 逐条规则独立命中；按等级降序、同级按固定顺序输出，最多 8 条。 */
    private static List<ContractRiskVO> evaluateRisks(
            SubcontractorContractVO contract,
            BigDecimal contractAmount,
            BigDecimal paidAmount,
            BigDecimal paidRatio,
            LocalDate today) {
        List<ContractRiskVO> risks = new ArrayList<>();
        boolean active = ContractStatus.ACTIVE.equals(contract.getStatus());
        LocalDate endDate = contract.getEndDate();

        boolean expiredActive = active && endDate != null && endDate.isBefore(today);
        if (contractAmount != null && paidAmount.compareTo(contractAmount) > 0) {
            risks.add(
                    risk(
                            LEVEL_HIGH,
                            RISK_OVERPAID,
                            "已付款超过合同金额",
                            "超付金额 " + formatAmount(paidAmount.subtract(contractAmount)) + " 元"));
        }
        if (expiredActive) {
            long overdueDays = ChronoUnit.DAYS.between(endDate, today);
            risks.add(
                    risk(
                            LEVEL_HIGH,
                            RISK_EXPIRED_ACTIVE,
                            "合同已到期但仍为生效状态",
                            "合同已到期 " + overdueDays + " 天"));
        }
        if (active
                && endDate != null
                && !endDate.isBefore(today)
                && !endDate.isAfter(today.plusDays(ENDING_SOON_DAYS))) {
            long remainDays = ChronoUnit.DAYS.between(today, endDate);
            risks.add(
                    risk(LEVEL_MEDIUM, RISK_ENDING_SOON, "合同即将到期", "距合同到期还有 " + remainDays + " 天"));
        }
        if (endDate == null) {
            risks.add(risk(LEVEL_MEDIUM, RISK_MISSING_END_DATE, "合同缺少截止日期", "合同未填写截止日期，无法判断是否到期"));
        }
        if (!StringUtils.hasText(contract.getFilePath())
                && !StringUtils.hasText(contract.getFileName())) {
            risks.add(risk(LEVEL_MEDIUM, RISK_MISSING_ATTACHMENT, "合同附件未归档", "合同未归档附件，请补充合同扫描件"));
        }
        if (contractAmount != null
                && contractAmount.compareTo(BigDecimal.ZERO) > 0
                && paidRatio.compareTo(HIGH_PAYMENT_RATIO_THRESHOLD) >= 0
                && !expiredActive) {
            risks.add(
                    risk(
                            LEVEL_MEDIUM,
                            RISK_HIGH_PAYMENT_RATIO,
                            "付款比例较高",
                            "付款比例已达 " + paidRatio.toPlainString() + "%，请关注后续付款风险"));
        }
        if (contractAmount == null || contractAmount.compareTo(BigDecimal.ZERO) <= 0) {
            risks.add(risk(LEVEL_LOW, RISK_ZERO_AMOUNT, "合同金额无效", "合同金额缺失或无效，请补录合同金额"));
        }
        if (!StringUtils.hasText(contract.getDescription())) {
            risks.add(risk(LEVEL_LOW, RISK_MISSING_DESCRIPTION, "合同缺少备注说明", "合同未填写备注说明，建议补充合同要点"));
        }

        risks.sort(
                Comparator.comparingInt(
                                (ContractRiskVO item) ->
                                        LEVEL_RANK.getOrDefault(item.getLevel(), 0))
                        .reversed()
                        .thenComparingInt(item -> RISK_CODE_ORDER.indexOf(item.getCode())));
        if (risks.size() > MAX_RISK_COUNT) {
            return List.copyOf(risks.subList(0, MAX_RISK_COUNT));
        }
        return List.copyOf(risks);
    }

    private static String highestRiskLevel(List<ContractRiskVO> risks) {
        int highest = 0;
        for (ContractRiskVO risk : risks) {
            highest = Math.max(highest, LEVEL_RANK.getOrDefault(risk.getLevel(), 0));
        }
        if (highest >= 3) {
            return LEVEL_HIGH;
        }
        if (highest == 2) {
            return LEVEL_MEDIUM;
        }
        if (highest == 1) {
            return LEVEL_LOW;
        }
        return LEVEL_NONE;
    }

    /**
     * 可选 AI 复述：任何异常、空结果或数字校验失败都降级为 null，接口仍正常返回。
     *
     * @return 通过校验的复述文本；失败返回 null
     */
    private String generateAiNote(
            List<ContractClausePointVO> clausePoints, List<ContractRiskVO> risks, LocalDate today) {
        try {
            String facts = buildFacts(clausePoints, risks, today);
            List<ChatMessage> messages =
                    List.of(
                            ChatMessage.system(AI_NOTE_SYSTEM_PROMPT),
                            ChatMessage.user("请仅复述以下合同事实与风险提示：\n" + facts));
            OllamaChatResult result = aiChatClient.chatStream(messages, null);
            if (result == null || !StringUtils.hasText(result.getContent())) {
                return null;
            }
            String note = result.getContent().trim();
            if (!preservesNumbers(facts, note, today)) {
                log.warn("合同风险 AI 复述未通过数字保全校验，已丢弃");
                return null;
            }
            return note;
        } catch (Exception e) {
            log.warn("合同风险 AI 复述失败，已降级为无 AI 提示：原因={}", e.getMessage());
            return null;
        }
    }

    private static String buildFacts(
            List<ContractClausePointVO> clausePoints, List<ContractRiskVO> risks, LocalDate today) {
        StringBuilder facts = new StringBuilder();
        facts.append("合同事实：\n");
        for (ContractClausePointVO point : clausePoints) {
            facts.append(point.getLabel()).append("：").append(point.getValue()).append('\n');
        }
        facts.append("风险提示：\n");
        if (risks.isEmpty()) {
            facts.append("无\n");
        } else {
            for (ContractRiskVO risk : risks) {
                facts.append(risk.getLevel())
                        .append(' ')
                        .append(risk.getCode())
                        .append(' ')
                        .append(risk.getTitle())
                        .append("；")
                        .append(risk.getDetail())
                        .append('\n');
            }
        }
        facts.append("当天日期：").append(today).append('\n');
        return facts.toString();
    }

    /**
     * 数字保全校验：事实文本与复述文本都先剔除日期、去千分位，再按数字集合双向比对。
     *
     * <p>既要求事实中的数字都出现在复述中，也要求复述不出现事实之外的数字。包级可见便于单测覆盖。
     */
    static boolean preservesNumbers(String facts, String note, LocalDate today) {
        if (!StringUtils.hasText(facts) || !StringUtils.hasText(note) || today == null) {
            return false;
        }
        Set<String> factNumbers = collectNumbers(normalizeNumbers(facts, today));
        Set<String> noteNumbers = collectNumbers(normalizeNumbers(note, today));
        return factNumbers.containsAll(noteNumbers) && noteNumbers.containsAll(factNumbers);
    }

    private static String normalizeNumbers(String text, LocalDate today) {
        String withoutDate =
                DATE_PATTERN.matcher(text.replace(today.toString(), " ")).replaceAll(" ");
        return THOUSANDS_SEPARATOR_PATTERN.matcher(withoutDate).replaceAll("");
    }

    private static Set<String> collectNumbers(String text) {
        Set<String> numbers = new LinkedHashSet<>();
        Matcher matcher = NUMBER_PATTERN.matcher(text);
        while (matcher.find()) {
            numbers.add(matcher.group());
        }
        return numbers;
    }

    private static ContractClausePointVO point(String label, String value) {
        return new ContractClausePointVO(label, value);
    }

    private static ContractRiskVO risk(String level, String code, String title, String detail) {
        return new ContractRiskVO(level, code, title, detail);
    }

    private static String textOrUnknown(String value) {
        return StringUtils.hasText(value) ? value : TEXT_UNKNOWN;
    }

    private static String formatAmount(BigDecimal amount) {
        return amount == null ? TEXT_UNKNOWN : amount.toPlainString();
    }

    private static String formatDate(LocalDate date) {
        return date == null ? TEXT_UNKNOWN : date.toString();
    }

    private static BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
