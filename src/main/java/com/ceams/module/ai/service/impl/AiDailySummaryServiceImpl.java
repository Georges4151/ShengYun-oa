package com.ceams.module.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ceams.common.exception.BusinessException;
import com.ceams.common.exception.ErrorCode;
import com.ceams.module.ai.client.ChatMessage;
import com.ceams.module.ai.client.OllamaChatResult;
import com.ceams.module.ai.client.OllamaClient;
import com.ceams.module.ai.config.AiProperties;
import com.ceams.module.ai.entity.AiDailySummary;
import com.ceams.module.ai.mapper.AiDailySummaryMapper;
import com.ceams.module.ai.service.AiDailySummaryService;
import com.ceams.module.ai.vo.AiDailySummaryVO;
import com.ceams.module.bank.service.BankStatementService;
import com.ceams.module.expense.service.ReceiptService;
import com.ceams.module.iam.service.UserService;
import com.ceams.module.notification.dto.NotificationCreateRequest;
import com.ceams.module.notification.enums.ContentType;
import com.ceams.module.notification.enums.NotificationPriority;
import com.ceams.module.notification.enums.NotificationType;
import com.ceams.module.notification.service.NotificationService;
import com.ceams.module.report.dto.FinancialReportQuery;
import com.ceams.module.report.service.ReportService;
import com.ceams.module.report.vo.FinancialReportItemVO;
import com.ceams.module.report.vo.FinancialReportVO;
import com.ceams.module.seal.service.ElectronicSealUsageService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * AI 经营日报摘要服务实现。
 *
 * <p>数据来源全部为既有服务的只读方法：报销汇总 / 待审批清单 / 财务报表 / 银行未对账 / 用印汇总。 所有数字均来自数据库，AI 仅用于可选润色，且必须通过数字保全校验。
 */
@Slf4j
@Service
public class AiDailySummaryServiceImpl implements AiDailySummaryService {

    /** 服务端统一时区。 */
    static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");

    /** 摘要周期类型：按日。 */
    private static final String PERIOD_DAILY = "DAILY";

    /** 模板生成时的模型标识（未使用 AI 润色）。 */
    private static final String MODEL_TEMPLATE = "TEMPLATE";

    /** 列表默认条数。 */
    private static final int DEFAULT_LIST_LIMIT = 10;

    /** 列表最大条数。 */
    private static final int MAX_LIST_LIMIT = 30;

    /** 资金流水来源类型：账户流水。 */
    private static final String SOURCE_ACCOUNT = "ACCOUNT";

    /** 资金流水来源类型：付款交易。 */
    private static final String SOURCE_PAYMENT_TRANSACTION = "PAYMENT_TRANSACTION";

    /** 收支类型：收入。 */
    private static final String TYPE_INCOME = "INCOME";

    /** 收支类型：支出。 */
    private static final String TYPE_EXPENSE = "EXPENSE";

    /** 摘要通知接收角色。 */
    private static final List<String> NOTIFY_ROLE_CODES = List.of("FINANCE", "SYS_ADMIN");

    /** 无数据时的分布摘要文案。 */
    private static final String NO_DATA = "暂无";

    /** 数字字面量正则（用于 AI 润色的数字保全校验）。 */
    private static final Pattern NUMBER_PATTERN = Pattern.compile("\\d+(?:\\.\\d+)?");

    /** 千分位逗号（仅当两侧都是数字时匹配），用于数字比对前的归一化。 */
    private static final Pattern THOUSANDS_SEPARATOR_PATTERN = Pattern.compile("(?<=\\d),(?=\\d)");

    /** 润色系统提示：只允许复述，禁止改动任何数字。 */
    private static final String POLISH_SYSTEM_PROMPT =
            "你是 CEAMS 建筑企业账目管理系统的文字编辑。只能复述用户给出的经营日报内容，"
                    + "禁止新增、删除或推算任何数字，所有数字与金额必须原样保留，不得改写；"
                    + "不得虚构事项；使用中文，150 字以内。";

    /** 审批状态中文标签（保持固定顺序）。 */
    private static final Map<String, String> APPROVAL_STATUS_LABELS = new LinkedHashMap<>();

    /** 核验状态中文标签（保持固定顺序）。 */
    private static final Map<String, String> VERIFICATION_STATUS_LABELS = new LinkedHashMap<>();

    static {
        APPROVAL_STATUS_LABELS.put("DRAFT", "草稿");
        APPROVAL_STATUS_LABELS.put("PENDING", "待审批");
        APPROVAL_STATUS_LABELS.put("APPROVED", "已通过");
        APPROVAL_STATUS_LABELS.put("REJECTED", "已驳回");
        APPROVAL_STATUS_LABELS.put("UNKNOWN", "未知");

        VERIFICATION_STATUS_LABELS.put("PENDING", "待核验");
        VERIFICATION_STATUS_LABELS.put("VERIFIED", "已核验");
        VERIFICATION_STATUS_LABELS.put("REJECTED", "核验驳回");
        VERIFICATION_STATUS_LABELS.put("UNKNOWN", "未知");
    }

    private final AiProperties aiProperties;
    private final OllamaClient ollamaClient;
    private final AiDailySummaryMapper summaryMapper;
    private final ReceiptService receiptService;
    private final ReportService reportService;
    private final BankStatementService bankStatementService;
    private final ElectronicSealUsageService sealUsageService;
    private final NotificationService notificationService;
    private final UserService userService;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    /** 生产环境构造：使用 Asia/Shanghai 的系统时钟。 */
    @Autowired
    public AiDailySummaryServiceImpl(
            AiProperties aiProperties,
            OllamaClient ollamaClient,
            AiDailySummaryMapper summaryMapper,
            ReceiptService receiptService,
            ReportService reportService,
            BankStatementService bankStatementService,
            ElectronicSealUsageService sealUsageService,
            NotificationService notificationService,
            UserService userService,
            ObjectMapper objectMapper) {
        this(
                aiProperties,
                ollamaClient,
                summaryMapper,
                receiptService,
                reportService,
                bankStatementService,
                sealUsageService,
                notificationService,
                userService,
                objectMapper,
                Clock.system(BUSINESS_ZONE));
    }

    /** 可注入 Clock 的构造器，便于单测固定「昨日」。 */
    AiDailySummaryServiceImpl(
            AiProperties aiProperties,
            OllamaClient ollamaClient,
            AiDailySummaryMapper summaryMapper,
            ReceiptService receiptService,
            ReportService reportService,
            BankStatementService bankStatementService,
            ElectronicSealUsageService sealUsageService,
            NotificationService notificationService,
            UserService userService,
            ObjectMapper objectMapper,
            Clock clock) {
        this.aiProperties = aiProperties;
        this.ollamaClient = ollamaClient;
        this.summaryMapper = summaryMapper;
        this.receiptService = receiptService;
        this.reportService = reportService;
        this.bankStatementService = bankStatementService;
        this.sealUsageService = sealUsageService;
        this.notificationService = notificationService;
        this.userService = userService;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Override
    public List<AiDailySummaryVO> list(int limit) {
        int safeLimit = limit >= 1 && limit <= MAX_LIST_LIMIT ? limit : DEFAULT_LIST_LIMIT;
        LambdaQueryWrapper<AiDailySummary> wrapper =
                new LambdaQueryWrapper<AiDailySummary>()
                        .orderByDesc(AiDailySummary::getSummaryDate)
                        .orderByDesc(AiDailySummary::getId);
        Page<AiDailySummary> page = new Page<>(1, safeLimit, false);
        List<AiDailySummary> records = summaryMapper.selectPage(page, wrapper).getRecords();
        if (records == null || records.isEmpty()) {
            return List.of();
        }
        return records.stream().map(this::toVO).toList();
    }

    @Override
    public AiDailySummaryVO latest() {
        LambdaQueryWrapper<AiDailySummary> wrapper =
                new LambdaQueryWrapper<AiDailySummary>()
                        .orderByDesc(AiDailySummary::getSummaryDate)
                        .last("LIMIT 1");
        List<AiDailySummary> records = summaryMapper.selectList(wrapper);
        return records == null || records.isEmpty() ? null : toVO(records.get(0));
    }

    @Override
    public AiDailySummaryVO byDate(LocalDate date) {
        if (date == null) {
            return null;
        }
        AiDailySummary entity = summaryMapper.selectOne(queryByDate(date));
        return entity == null ? null : toVO(entity);
    }

    @Override
    public AiDailySummaryVO generate(LocalDate date, TriggerType trigger) {
        LocalDate yesterday = yesterday();
        LocalDate target = date == null ? yesterday : date;
        if (target.isAfter(yesterday)) {
            throw BusinessException.of(ErrorCode.VALID_ILLEGAL_ARGUMENT, "日报摘要日期不得晚于昨日");
        }
        TriggerType safeTrigger = trigger == null ? TriggerType.MANUAL : trigger;
        // 昨日允许 AI 润色；补录历史日期统一走模板，避免模型改写历史口径。
        boolean allowPolish = !target.isBefore(yesterday);
        return doGenerate(target, safeTrigger, allowPolish, true);
    }

    @Override
    public void catchUpMissing() {
        LocalDate yesterday = yesterday();
        int maxDays = Math.max(0, aiProperties.getSummary().getMaxCatchUpDays());
        for (int i = 0; i < maxDays; i++) {
            LocalDate target = yesterday.minusDays(i);
            try {
                if (byDate(target) == null) {
                    // 历史补录只落库、不建通知，避免首次部署/停机多日后通知轰炸。
                    doGenerate(target, TriggerType.SCHEDULED, target.equals(yesterday), false);
                }
            } catch (Exception e) {
                log.warn("补录经营日报失败：date={}，原因={}", target, e.getMessage());
            }
        }
    }

    /**
     * 生成并落库：同日已存在则覆盖更新，且不重复创建通知；否则先插入摘要，再按需创建通知并回填通知 ID。
     *
     * <p>先落库后建通知可避免插入失败时产生孤立通知；通知创建失败只记 warn，不影响摘要生成。
     */
    private AiDailySummaryVO doGenerate(
            LocalDate date, TriggerType trigger, boolean allowPolish, boolean notify) {
        SummaryData data = collectData(date);
        String template = buildTemplate(date, data);
        String content = template;
        String model = MODEL_TEMPLATE;
        if (allowPolish
                && aiProperties.isEnabled()
                && aiProperties.getSummary().isPolishEnabled()) {
            String polished = polish(date, template);
            if (StringUtils.hasText(polished) && preservesNumbers(template, polished, date)) {
                content = polished;
                model = aiProperties.getModel();
            } else {
                log.warn("经营日报润色结果未通过数字保全校验，回退模板：date={}", date);
            }
        }

        String metricsJson = writeJson(data.metrics());
        AiDailySummary existing = summaryMapper.selectOne(queryByDate(date));
        if (existing != null) {
            existing.setContent(content);
            existing.setMetricsJson(metricsJson);
            existing.setModel(model);
            existing.setTriggerType(trigger.name());
            summaryMapper.updateById(existing);
            return toVO(existing, data.metrics());
        }

        AiDailySummary entity = new AiDailySummary();
        entity.setSummaryDate(date);
        entity.setPeriodType(PERIOD_DAILY);
        entity.setContent(content);
        entity.setMetricsJson(metricsJson);
        entity.setModel(model);
        entity.setTriggerType(trigger.name());
        // 先插入摘要，成功后再创建通知并回填 ID，避免插入失败产生孤立通知。
        summaryMapper.insert(entity);
        if (notify) {
            Long notificationId = createSummaryNotification(date, content);
            if (notificationId != null) {
                entity.setNotificationId(notificationId);
                summaryMapper.updateById(entity);
            }
        }
        return toVO(entity, data.metrics());
    }

    /** 按 Asia/Shanghai 统计当日 [00:00:00, 23:59:59] 的经营指标。 */
    private SummaryData collectData(LocalDate date) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.atTime(23, 59, 59);

        ReceiptService.ExpenseSummary receipt = receiptService.summary(start, end, null, null);
        long receiptCount = receipt == null ? 0L : receipt.totalCount();
        BigDecimal receiptAmount = receipt == null ? BigDecimal.ZERO : nvl(receipt.totalAmount());
        Map<String, Long> approvalDistribution =
                receipt == null || receipt.approvalStatusDistribution() == null
                        ? Map.of()
                        : receipt.approvalStatusDistribution();
        Map<String, Long> verificationDistribution =
                receipt == null || receipt.verificationStatusDistribution() == null
                        ? Map.of()
                        : receipt.verificationStatusDistribution();

        ReceiptService.PendingApprovalPage pending =
                receiptService.pendingApprovals(null, null, null, 20);
        long pendingCount = pending == null ? 0L : pending.pendingCount();
        BigDecimal pendingAmount = pending == null ? BigDecimal.ZERO : nvl(pending.pendingAmount());

        BigDecimal income = BigDecimal.ZERO;
        BigDecimal expense = BigDecimal.ZERO;
        FinancialReportQuery query = new FinancialReportQuery();
        query.setMonth(YearMonth.from(date).toString());
        FinancialReportVO report = reportService.financial(query);
        List<FinancialReportItemVO> items =
                report == null || report.getItems() == null ? List.of() : report.getItems();
        for (FinancialReportItemVO item : items) {
            if (item == null || item.getTransactionDate() == null) {
                continue;
            }
            if (!date.equals(item.getTransactionDate().toLocalDate())
                    || !isCashflowSource(item.getSourceType())) {
                continue;
            }
            BigDecimal amount = nvl(item.getAmount());
            if (TYPE_INCOME.equals(item.getType())) {
                income = income.add(amount);
            } else if (TYPE_EXPENSE.equals(item.getType())) {
                expense = expense.add(amount);
            }
        }

        BankStatementService.UnmatchedSummary unmatched =
                bankStatementService.unmatchedSummary(start, end, null);
        long unmatchedCount = unmatched == null ? 0L : unmatched.unmatchedCount();
        BigDecimal unmatchedAmount =
                unmatched == null ? BigDecimal.ZERO : nvl(unmatched.totalAmount());

        ElectronicSealUsageService.UsageSummary seal =
                sealUsageService.summary(start, end, null, null);
        long sealCount = seal == null ? 0L : seal.totalCount();

        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("receiptCount", receiptCount);
        metrics.put("receiptAmount", receiptAmount);
        metrics.put("pendingCount", pendingCount);
        metrics.put("pendingAmount", pendingAmount);
        metrics.put("income", income);
        metrics.put("expense", expense);
        metrics.put("net", income.subtract(expense));
        metrics.put("unmatchedCount", unmatchedCount);
        metrics.put("unmatchedAmount", unmatchedAmount);
        metrics.put("sealCount", sealCount);
        return new SummaryData(metrics, approvalDistribution, verificationDistribution);
    }

    /** 生成固定口径的模板文本（无 AI 时使用）。 */
    private String buildTemplate(LocalDate date, SummaryData data) {
        Map<String, Object> metrics = data.metrics();
        long receiptCount = longValue(metrics.get("receiptCount"));
        long pendingCount = longValue(metrics.get("pendingCount"));
        long unmatchedCount = longValue(metrics.get("unmatchedCount"));
        long sealCount = longValue(metrics.get("sealCount"));
        BigDecimal receiptAmount = decimalValue(metrics.get("receiptAmount"));
        BigDecimal pendingAmount = decimalValue(metrics.get("pendingAmount"));
        BigDecimal income = decimalValue(metrics.get("income"));
        BigDecimal expense = decimalValue(metrics.get("expense"));
        BigDecimal net = decimalValue(metrics.get("net"));
        BigDecimal unmatchedAmount = decimalValue(metrics.get("unmatchedAmount"));
        return "CEAMS 经营日报（"
                + date
                + "）\n"
                + "一、报销：新增 "
                + receiptCount
                + " 笔，合计 "
                + formatAmount(receiptAmount)
                + "；审批状态："
                + distributionSummary(data.approvalDistribution(), APPROVAL_STATUS_LABELS)
                + "；核验状态："
                + distributionSummary(data.verificationDistribution(), VERIFICATION_STATUS_LABELS)
                + "。\n"
                + "二、待办：当前待审批报销 "
                + pendingCount
                + " 笔，合计 "
                + formatAmount(pendingAmount)
                + "。\n"
                + "三、资金：收入 "
                + formatAmount(income)
                + "，支出 "
                + formatAmount(expense)
                + "，净额 "
                + formatSignedAmount(net)
                + "。\n"
                + "四、对账：未对账银行流水 "
                + unmatchedCount
                + " 笔，合计 "
                + formatAmount(unmatchedAmount)
                + "。\n"
                + "五、用印："
                + sealCount
                + " 次。\n"
                + "说明：以上数据由系统按当日业务实时汇总，仅供内部管理参考，请以财务复核为准。";
    }

    /**
     * 可选 AI 润色：无工具调用，仅复述数字；任何异常都回退模板。
     *
     * @return 润色后的文本；失败/空结果返回 null
     */
    private String polish(LocalDate date, String template) {
        try {
            List<ChatMessage> messages =
                    List.of(
                            ChatMessage.system(POLISH_SYSTEM_PROMPT),
                            ChatMessage.user("请润色下面的 CEAMS 经营日报（" + date + "）：\n" + template));
            OllamaChatResult result = ollamaClient.chatStream(messages, null);
            if (result == null || !StringUtils.hasText(result.getContent())) {
                return null;
            }
            return result.getContent().trim();
        } catch (Exception e) {
            log.warn("经营日报 AI 润色失败，回退模板：date={}，原因={}", date, e.getMessage());
            return null;
        }
    }

    /**
     * 数字保全校验：模板与润色文本都先剔除日期、归一化千分位，再按数字集合双向比对。
     *
     * <p>既要求模板中所有数字都出现在润色结果，也要求润色结果不出现模板之外的任何数字， 从而拦截模型新增/推算的数字。包级可见便于单测直接覆盖。
     */
    static boolean preservesNumbers(String template, String polished, LocalDate date) {
        if (!StringUtils.hasText(template) || !StringUtils.hasText(polished) || date == null) {
            return false;
        }
        String base = normalizeNumbers(template.replace(date.toString(), " "));
        String candidate = normalizeNumbers(polished.replace(date.toString(), " "));
        Set<String> templateNumbers = collectNumbers(base);
        Set<String> polishedNumbers = collectNumbers(candidate);
        return templateNumbers.containsAll(polishedNumbers)
                && polishedNumbers.containsAll(templateNumbers);
    }

    /** 提取文本中的数字字面量（保持出现顺序、自动去重）。 */
    private static Set<String> collectNumbers(String text) {
        Set<String> numbers = new LinkedHashSet<>();
        Matcher matcher = NUMBER_PATTERN.matcher(text);
        while (matcher.find()) {
            numbers.add(matcher.group());
        }
        return numbers;
    }

    /** 生成摘要通知（定向投递给 FINANCE / SYS_ADMIN 活跃用户）；失败返回 null 且不影响摘要。 */
    private Long createSummaryNotification(LocalDate date, String content) {
        try {
            List<Long> targetUserIds = userService.listActiveUserIdsByRoleCodes(NOTIFY_ROLE_CODES);
            NotificationCreateRequest request = new NotificationCreateRequest();
            request.setTitle("经营日报｜" + date);
            request.setContent(content);
            request.setContentType(ContentType.TEXT);
            request.setNotificationType(NotificationType.SYSTEM);
            request.setPriority(NotificationPriority.NORMAL);
            request.setSenderName("CEAMS 系统");
            request.setValidStartTime(LocalDateTime.now(clock));
            request.setLinkUrl("ceams://ai/summary?date=" + date);
            String notificationId =
                    notificationService.createAndPublishToUsers(request, targetUserIds);
            return parseNotificationId(notificationId);
        } catch (Exception e) {
            log.warn("经营日报通知创建失败，不影响摘要生成：date={}，原因={}", date, e.getMessage());
            return null;
        }
    }

    private AiDailySummaryVO toVO(AiDailySummary entity) {
        return toVO(entity, readMetrics(entity.getMetricsJson()));
    }

    private AiDailySummaryVO toVO(AiDailySummary entity, Map<String, Object> metrics) {
        if (entity == null) {
            return null;
        }
        AiDailySummaryVO vo = new AiDailySummaryVO();
        vo.setId(entity.getId());
        vo.setSummaryDate(entity.getSummaryDate());
        vo.setPeriodType(entity.getPeriodType());
        vo.setContent(entity.getContent());
        vo.setModel(entity.getModel());
        vo.setTriggerType(entity.getTriggerType());
        vo.setNotificationId(entity.getNotificationId());
        vo.setMetrics(metrics == null ? Map.of() : metrics);
        vo.setCreateTime(entity.getCreateTime());
        vo.setUpdateTime(entity.getUpdateTime());
        return vo;
    }

    private Map<String, Object> readMetrics(String json) {
        if (!StringUtils.hasText(json)) {
            return Map.of();
        }
        try {
            Map<String, Object> raw =
                    objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
            Map<String, Object> metrics = new LinkedHashMap<>();
            for (Map.Entry<String, Object> entry : raw.entrySet()) {
                metrics.put(entry.getKey(), normalizeMetricValue(entry.getKey(), entry.getValue()));
            }
            return metrics;
        } catch (Exception e) {
            log.warn("经营日报指标反序列化失败：{}", e.getMessage());
            return Map.of();
        }
    }

    /** 从 JSON 还原时保持指标类型约定：计数为 Long、金额为 BigDecimal。 */
    private static Object normalizeMetricValue(String key, Object value) {
        if (!(value instanceof Number number)) {
            return value;
        }
        if (key.endsWith("Count")) {
            return number.longValue();
        }
        return new BigDecimal(number.toString());
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            log.warn("经营日报指标序列化失败：{}", e.getMessage());
            return "{}";
        }
    }

    private LocalDate yesterday() {
        return LocalDate.now(clock).minusDays(1);
    }

    private static LambdaQueryWrapper<AiDailySummary> queryByDate(LocalDate date) {
        return new LambdaQueryWrapper<AiDailySummary>()
                .eq(AiDailySummary::getSummaryDate, date)
                .eq(AiDailySummary::getPeriodType, PERIOD_DAILY);
    }

    private static boolean isCashflowSource(String sourceType) {
        return SOURCE_ACCOUNT.equals(sourceType) || SOURCE_PAYMENT_TRANSACTION.equals(sourceType);
    }

    /** 按固定顺序输出非零分布项；无数据输出「暂无」。 */
    private static String distributionSummary(
            Map<String, Long> distribution, Map<String, String> labels) {
        if (distribution == null || distribution.isEmpty()) {
            return NO_DATA;
        }
        List<String> parts = new ArrayList<>();
        for (Map.Entry<String, String> label : labels.entrySet()) {
            Long count = distribution.get(label.getKey());
            if (count != null && count > 0) {
                parts.add(label.getValue() + " " + count + " 笔");
            }
        }
        for (Map.Entry<String, Long> entry : distribution.entrySet()) {
            if (labels.containsKey(entry.getKey())
                    || entry.getValue() == null
                    || entry.getValue() <= 0) {
                continue;
            }
            parts.add(entry.getKey() + " " + entry.getValue() + " 笔");
        }
        return parts.isEmpty() ? NO_DATA : String.join("、", parts);
    }

    private static String formatAmount(BigDecimal value) {
        BigDecimal safe = nvl(value);
        String sign = safe.signum() < 0 ? "-" : "";
        return sign + "¥" + String.format(Locale.CHINA, "%,.2f", safe.abs());
    }

    private static String formatSignedAmount(BigDecimal value) {
        BigDecimal safe = nvl(value);
        return safe.signum() > 0 ? "+" + formatAmount(safe) : formatAmount(safe);
    }

    private static long longValue(Object value) {
        return value instanceof Number number ? number.longValue() : 0L;
    }

    private static BigDecimal decimalValue(Object value) {
        return value instanceof BigDecimal decimal ? decimal : BigDecimal.ZERO;
    }

    private static BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private static String normalizeNumbers(String text) {
        return THOUSANDS_SEPARATOR_PATTERN.matcher(text).replaceAll("");
    }

    private static Long parseNotificationId(String rawId) {
        if (!StringUtils.hasText(rawId)) {
            return null;
        }
        try {
            return Long.parseLong(rawId.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** 一次摘要计算所需的指标与分布明细。 */
    private record SummaryData(
            Map<String, Object> metrics,
            Map<String, Long> approvalDistribution,
            Map<String, Long> verificationDistribution) {}
}
