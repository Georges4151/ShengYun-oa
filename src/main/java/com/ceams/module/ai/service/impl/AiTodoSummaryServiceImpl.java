package com.ceams.module.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ceams.common.exception.BusinessException;
import com.ceams.common.exception.ErrorCode;
import com.ceams.module.ai.client.ChatMessage;
import com.ceams.module.ai.client.OllamaChatResult;
import com.ceams.module.ai.client.AiChatClient;
import com.ceams.module.ai.config.AiProperties;
import com.ceams.module.ai.service.AiTodoSummaryService;
import com.ceams.module.ai.vo.AiTodoSummaryVO;
import com.ceams.module.bpm.entity.BpmInstance;
import com.ceams.module.bpm.entity.BpmTask;
import com.ceams.module.bpm.enums.BpmDataScope;
import com.ceams.module.bpm.enums.BpmTaskStatus;
import com.ceams.module.bpm.mapper.BpmInstanceMapper;
import com.ceams.module.bpm.mapper.BpmTaskMapper;
import com.ceams.module.bpm.support.BpmSecurityGuard;
import com.ceams.module.iam.entity.SysUser;
import com.ceams.module.iam.mapper.UserMapper;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * AI 待办摘要服务实现。
 *
 * <p>数据口径与待办列表完全一致：状态固定 {@code PENDING}，数据范围复用 {@link BpmSecurityGuard}
 * （ALL 不限 / DEPT 同部门及子部门 / SELF 仅本人）。全部为只读查询，AI 仅用于可选润色，
 * 且润色结果必须通过数字保全校验，否则回退规则模板。
 */
@Slf4j
@Service
public class AiTodoSummaryServiceImpl implements AiTodoSummaryService {

    /** 服务端统一时区。 */
    static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");

    /** 规则模板的模型标识。 */
    private static final String MODEL_TEMPLATE = "TEMPLATE";

    /** 免责声明固定文案。 */
    private static final String DISCLAIMER = "以上待办数据由系统实时汇总，AI 内容仅供参考，请以待办中心为准。";

    /** 最近到期清单默认条数。 */
    private static final int DEFAULT_UPCOMING_LIMIT = 5;

    /** 最近到期清单最大条数。 */
    private static final int MAX_UPCOMING_LIMIT = 20;

    /** 业务类型展示顺序（未知类型追加在末尾）。 */
    private static final List<String> BIZ_ORDER = List.of("EXPENSE", "CHANGE_REQUEST");

    /** 业务类型中文标签。 */
    private static final Map<String, String> BIZ_LABELS =
            Map.of("EXPENSE", "报销", "CHANGE_REQUEST", "项目变更");

    /** 到期时间展示格式。 */
    private static final DateTimeFormatter DUE_FORMATTER = DateTimeFormatter.ofPattern("MM-dd HH:mm");

    /** 润色系统提示：只允许复述，禁止改动任何数字。 */
    private static final String POLISH_SYSTEM_PROMPT =
            "你是 CEAMS 建筑企业账目管理系统的文字编辑。只能复述用户给出的待办摘要内容，"
                    + "禁止新增、删除或推算任何数字，所有数字与时间必须原样保留，不得改写；"
                    + "不得虚构事项；使用中文，150 字以内。";

    private final AiProperties aiProperties;
    private final AiChatClient aiChatClient;
    private final BpmTaskMapper taskMapper;
    private final BpmInstanceMapper instanceMapper;
    private final UserMapper userMapper;
    private final BpmSecurityGuard securityGuard;
    private final Clock clock;

    /** 生产构造器：使用 Asia/Shanghai 系统时钟。 */
    @Autowired
    public AiTodoSummaryServiceImpl(
            AiProperties aiProperties,
            AiChatClient aiChatClient,
            BpmTaskMapper taskMapper,
            BpmInstanceMapper instanceMapper,
            UserMapper userMapper,
            BpmSecurityGuard securityGuard) {
        this(
                aiProperties,
                aiChatClient,
                taskMapper,
                instanceMapper,
                userMapper,
                securityGuard,
                Clock.system(BUSINESS_ZONE));
    }

    /** 可注入 Clock 的构造器，便于单测固定时间。 */
    AiTodoSummaryServiceImpl(
            AiProperties aiProperties,
            AiChatClient aiChatClient,
            BpmTaskMapper taskMapper,
            BpmInstanceMapper instanceMapper,
            UserMapper userMapper,
            BpmSecurityGuard securityGuard,
            Clock clock) {
        this.aiProperties = aiProperties;
        this.aiChatClient = aiChatClient;
        this.taskMapper = taskMapper;
        this.instanceMapper = instanceMapper;
        this.userMapper = userMapper;
        this.securityGuard = securityGuard;
        this.clock = clock;
    }

    @Override
    public AiTodoSummaryVO summary() {
        Long current = securityGuard.currentUserId();
        if (current == null) {
            throw BusinessException.of(ErrorCode.AUTH_ACCESS_DENIED, "未登录不可查询待办摘要");
        }
        LocalDateTime now = LocalDateTime.now(clock);

        long totalPending = count(baseWrapper(current));
        long overdue = count(baseWrapper(current).lt(BpmTask::getDueAt, now));
        LocalDate today = LocalDate.now(clock);
        LocalDateTime dayStart = today.atStartOfDay();
        LocalDateTime dayEnd = today.plusDays(1).atStartOfDay();
        long dueToday =
                count(
                        baseWrapper(current)
                                .ge(BpmTask::getDueAt, dayStart)
                                .lt(BpmTask::getDueAt, dayEnd));

        List<AiTodoSummaryVO.BizTypeCount> byBizType = loadDistribution(current);
        List<AiTodoSummaryVO.UpcomingItem> upcoming = loadUpcoming(current, now);

        AiTodoSummaryVO vo = new AiTodoSummaryVO();
        vo.setGeneratedAt(now);
        vo.setTotalPending(totalPending);
        vo.setOverdue(overdue);
        vo.setDueToday(dueToday);
        vo.setByBizType(byBizType);
        vo.setUpcoming(upcoming);
        vo.setAiEnabled(aiChatClient.isEnabled());
        vo.setDisclaimer(DISCLAIMER);

        String template = buildTemplate(today, totalPending, overdue, dueToday, byBizType, upcoming);
        String content = template;
        String model = MODEL_TEMPLATE;
        if (aiChatClient.isEnabled() && aiProperties.getTodoSummary().isPolishEnabled()) {
            String polished = polish(template);
            if (StringUtils.hasText(polished) && preservesNumbers(template, polished, today)) {
                content = polished.trim();
                model = aiChatClient.textModel();
            } else {
                log.warn("AI 待办摘要润色未通过数字保全校验，回退规则模板");
            }
        }
        vo.setContent(content);
        vo.setModel(model);
        return vo;
    }

    /** 统计总数（空结果按 0 处理）。 */
    private long count(LambdaQueryWrapper<BpmTask> wrapper) {
        Long value = taskMapper.selectCount(wrapper);
        return value == null ? 0L : value;
    }

    /**
     * 待办基础条件：状态 PENDING + 当前数据范围。
     *
     * <p>与 {@code BpmTaskServiceImpl#applyAssigneeScope} 保持同一口径；组织数据不足时按本人兜底，避免越权。
     */
    private LambdaQueryWrapper<BpmTask> baseWrapper(Long current) {
        LambdaQueryWrapper<BpmTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BpmTask::getStatus, BpmTaskStatus.PENDING);
        BpmDataScope scope = securityGuard.resolveDataScope();
        if (scope == BpmDataScope.ALL) {
            return wrapper;
        }
        if (scope == BpmDataScope.DEPT) {
            List<Long> visible = securityGuard.visibleUserIds();
            if (visible == null || visible.isEmpty()) {
                wrapper.eq(BpmTask::getAssigneeId, current);
                return wrapper;
            }
            wrapper.in(BpmTask::getAssigneeId, visible);
            return wrapper;
        }
        wrapper.eq(BpmTask::getAssigneeId, current);
        return wrapper;
    }

    /** 按业务类型汇总待办数量：先按实例分组，再批量查实例业务类型，避免逐条查询。 */
    private List<AiTodoSummaryVO.BizTypeCount> loadDistribution(Long current) {
        QueryWrapper<BpmTask> wrapper = new QueryWrapper<>();
        wrapper.select("instance_id AS instanceId", "COUNT(*) AS cnt")
                .eq("status", BpmTaskStatus.PENDING.name())
                .groupBy("instance_id");
        applyRawScope(wrapper, current);
        List<Map<String, Object>> rows = taskMapper.selectMaps(wrapper);
        if (rows == null || rows.isEmpty()) {
            return List.of();
        }

        Map<Long, Long> countByInstance = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            Long instanceId = asLong(firstValue(row, "instanceId", "instance_id"));
            Long count = asLong(firstValue(row, "cnt", "CNT", "count"));
            if (instanceId == null || count == null) {
                continue;
            }
            countByInstance.merge(instanceId, count, Long::sum);
        }
        if (countByInstance.isEmpty()) {
            return List.of();
        }

        List<BpmInstance> instances = instanceMapper.selectBatchIds(countByInstance.keySet());
        Map<Long, String> bizTypeByInstance = new LinkedHashMap<>();
        if (instances != null) {
            for (BpmInstance instance : instances) {
                if (instance != null && instance.getId() != null) {
                    bizTypeByInstance.put(instance.getId(), instance.getBizType());
                }
            }
        }

        Map<String, Long> countByBizType = new LinkedHashMap<>();
        for (Map.Entry<Long, Long> entry : countByInstance.entrySet()) {
            String bizType = bizTypeByInstance.get(entry.getKey());
            if (!StringUtils.hasText(bizType)) {
                bizType = "UNKNOWN";
            }
            countByBizType.merge(bizType, entry.getValue(), Long::sum);
        }

        List<AiTodoSummaryVO.BizTypeCount> result = new ArrayList<>();
        for (String bizType : BIZ_ORDER) {
            Long count = countByBizType.remove(bizType);
            if (count != null && count > 0) {
                result.add(bizCount(bizType, count));
            }
        }
        List<String> rest = new ArrayList<>(countByBizType.keySet());
        rest.sort(String::compareTo);
        for (String bizType : rest) {
            Long count = countByBizType.get(bizType);
            if (count != null && count > 0) {
                result.add(bizCount(bizType, count));
            }
        }
        return result;
    }

    /** 最近到期清单：按 dueAt 升序取前 N 条，并批量补业务类型与发起人姓名。 */
    private List<AiTodoSummaryVO.UpcomingItem> loadUpcoming(Long current, LocalDateTime now) {
        int limit = resolveUpcomingLimit();
        Page<BpmTask> page = new Page<>(1, limit, false);
        LambdaQueryWrapper<BpmTask> wrapper =
                baseWrapper(current).isNotNull(BpmTask::getDueAt).orderByAsc(BpmTask::getDueAt);
        List<BpmTask> tasks = taskMapper.selectPage(page, wrapper).getRecords();
        if (tasks == null || tasks.isEmpty()) {
            return List.of();
        }

        Set<Long> instanceIds = new LinkedHashSet<>();
        for (BpmTask task : tasks) {
            if (task.getInstanceId() != null) {
                instanceIds.add(task.getInstanceId());
            }
        }
        Map<Long, BpmInstance> instanceMap = new LinkedHashMap<>();
        if (!instanceIds.isEmpty()) {
            List<BpmInstance> instances = instanceMapper.selectBatchIds(instanceIds);
            if (instances != null) {
                for (BpmInstance instance : instances) {
                    if (instance != null && instance.getId() != null) {
                        instanceMap.put(instance.getId(), instance);
                    }
                }
            }
        }

        Set<Long> initiatorIds = new HashSet<>();
        for (BpmInstance instance : instanceMap.values()) {
            if (instance.getInitiatorId() != null) {
                initiatorIds.add(instance.getInitiatorId());
            }
        }
        Map<Long, String> userNameMap = new LinkedHashMap<>();
        if (!initiatorIds.isEmpty()) {
            List<SysUser> users = userMapper.selectBatchIds(initiatorIds);
            if (users != null) {
                for (SysUser user : users) {
                    if (user != null && user.getId() != null) {
                        userNameMap.put(user.getId(), user.getName());
                    }
                }
            }
        }

        List<AiTodoSummaryVO.UpcomingItem> items = new ArrayList<>(tasks.size());
        for (BpmTask task : tasks) {
            BpmInstance instance = instanceMap.get(task.getInstanceId());
            String bizType = instance == null ? null : instance.getBizType();
            AiTodoSummaryVO.UpcomingItem item = new AiTodoSummaryVO.UpcomingItem();
            item.setTaskId(task.getId());
            item.setInstanceId(task.getInstanceId());
            item.setBizType(bizType);
            item.setLabel(labelOf(bizType));
            item.setInitiatorName(
                    instance == null || instance.getInitiatorId() == null
                            ? null
                            : userNameMap.get(instance.getInitiatorId()));
            item.setDueAt(task.getDueAt());
            item.setOverdue(task.getDueAt() != null && task.getDueAt().isBefore(now));
            items.add(item);
        }
        return items;
    }

    /** 原始列查询的数据范围条件（与 Lambda 口径一致）。 */
    private void applyRawScope(QueryWrapper<BpmTask> wrapper, Long current) {
        BpmDataScope scope = securityGuard.resolveDataScope();
        if (scope == BpmDataScope.ALL) {
            return;
        }
        if (scope == BpmDataScope.DEPT) {
            List<Long> visible = securityGuard.visibleUserIds();
            if (visible == null || visible.isEmpty()) {
                wrapper.eq("assignee_id", current);
                return;
            }
            wrapper.in("assignee_id", visible);
            return;
        }
        wrapper.eq("assignee_id", current);
    }

    /** 生成固定口径的规则模板。 */
    private String buildTemplate(
            LocalDate today,
            long totalPending,
            long overdue,
            long dueToday,
            List<AiTodoSummaryVO.BizTypeCount> byBizType,
            List<AiTodoSummaryVO.UpcomingItem> upcoming) {
        StringBuilder text = new StringBuilder();
        text.append("CEAMS 待办摘要（").append(today).append("）\n");
        text.append("一、待办总计 ")
                .append(totalPending)
                .append(" 条，其中已超时 ")
                .append(overdue)
                .append(" 条，今日到期 ")
                .append(dueToday)
                .append(" 条。\n");
        text.append("二、按业务类型：").append(distributionText(byBizType)).append("。\n");
        if (upcoming == null || upcoming.isEmpty()) {
            text.append("三、最近到期：暂无。\n");
        } else {
            text.append("三、最近到期：\n");
            for (AiTodoSummaryVO.UpcomingItem item : upcoming) {
                text.append("- ").append(item.getLabel()).append("（任务#").append(item.getTaskId()).append("）");
                if (StringUtils.hasText(item.getInitiatorName())) {
                    text.append("，发起人 ").append(item.getInitiatorName());
                }
                if (item.getDueAt() != null) {
                    text.append("，到期 ").append(item.getDueAt().format(DUE_FORMATTER));
                }
                if (item.isOverdue()) {
                    text.append("（已超时）");
                }
                text.append("。\n");
            }
        }
        if (overdue > 0) {
            text.append("建议：优先处理 ").append(overdue).append(" 条已超时任务。");
        } else if (totalPending > 0) {
            text.append("建议：按到期时间顺序处理待办。");
        } else {
            text.append("建议：当前无待办，可安排其他工作。");
        }
        return text.toString();
    }

    /** 分布文案：报销 2 条、项目变更 1 条；无数据输出「暂无」。 */
    private String distributionText(List<AiTodoSummaryVO.BizTypeCount> byBizType) {
        if (byBizType == null || byBizType.isEmpty()) {
            return "暂无";
        }
        List<String> parts = new ArrayList<>(byBizType.size());
        for (AiTodoSummaryVO.BizTypeCount item : byBizType) {
            parts.add(item.getLabel() + " " + item.getCount() + " 条");
        }
        return String.join("、", parts);
    }

    /** 可选 AI 润色：任何异常返回 null，由调用方回退模板。 */
    private String polish(String template) {
        try {
            List<ChatMessage> messages =
                    List.of(
                            ChatMessage.system(POLISH_SYSTEM_PROMPT),
                            ChatMessage.user("请润色下面的 CEAMS 待办摘要：\n" + template));
            OllamaChatResult result = aiChatClient.chatStream(messages, null);
            return result == null ? null : result.getContent();
        } catch (Exception e) {
            log.warn("AI 待办摘要润色失败，回退规则模板：{}", e.getMessage());
            return null;
        }
    }

    /**
     * 数字保全校验：模板与润色文本剔除日期、归一化千分位后，数字集合必须完全一致。
     *
     * <p>直接复用经营日报同款校验，避免两套口径漂移。
     */
    private boolean preservesNumbers(String template, String polished, LocalDate today) {
        return AiDailySummaryServiceImpl.preservesNumbers(template, polished, today);
    }

    private int resolveUpcomingLimit() {
        int configured = aiProperties.getTodoSummary().getUpcomingLimit();
        if (configured < 1) {
            return DEFAULT_UPCOMING_LIMIT;
        }
        return Math.min(configured, MAX_UPCOMING_LIMIT);
    }

    private AiTodoSummaryVO.BizTypeCount bizCount(String bizType, long count) {
        AiTodoSummaryVO.BizTypeCount item = new AiTodoSummaryVO.BizTypeCount();
        item.setBizType(bizType);
        item.setLabel(labelOf(bizType));
        item.setCount(count);
        return item;
    }

    /** 业务类型中文名；空白返回「未知」，未知编码原样展示。 */
    private static String labelOf(String bizType) {
        if (!StringUtils.hasText(bizType)) {
            return "未知";
        }
        return BIZ_LABELS.getOrDefault(bizType, bizType);
    }

    private static Object firstValue(Map<String, Object> row, String... keys) {
        for (String key : keys) {
            if (row.containsKey(key)) {
                return row.get(key);
            }
        }
        return null;
    }

    private static Long asLong(Object value) {
        return value instanceof Number number ? number.longValue() : null;
    }
}
