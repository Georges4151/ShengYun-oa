package com.ceams.module.hr.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ceams.common.exception.BusinessException;
import com.ceams.common.exception.ErrorCode;
import com.ceams.common.response.PageResult;
import com.ceams.common.security.SecurityUtils;
import com.ceams.module.bpm.dto.InstanceStartRequest;
import com.ceams.module.bpm.dto.TaskApproveRequest;
import com.ceams.module.bpm.enums.BpmBizType;
import com.ceams.module.bpm.enums.BpmInstanceStatus;
import com.ceams.module.bpm.enums.BpmTaskStatus;
import com.ceams.module.bpm.service.BpmInstanceService;
import com.ceams.module.bpm.service.BpmTaskService;
import com.ceams.module.bpm.vo.InstanceDetailVO;
import com.ceams.module.bpm.vo.NodeVO;
import com.ceams.module.bpm.vo.TaskVO;
import com.ceams.module.hr.dto.PerformanceItem;
import com.ceams.module.hr.dto.PerformanceLeaderRequest;
import com.ceams.module.hr.dto.PerformanceReviewBatchRequest;
import com.ceams.module.hr.dto.PerformanceReviewRequest;
import com.ceams.module.hr.dto.PerformanceScoreItem;
import com.ceams.module.hr.dto.PerformanceScoreRule;
import com.ceams.module.hr.dto.PerformanceSelfRequest;
import com.ceams.module.hr.dto.PerformanceTemplateSaveRequest;
import com.ceams.module.hr.entity.HrEmployee;
import com.ceams.module.hr.entity.HrPerformanceReview;
import com.ceams.module.hr.entity.HrPerformanceTemplate;
import com.ceams.module.hr.enums.EmployeeStatus;
import com.ceams.module.hr.enums.PerformanceReviewStatus;
import com.ceams.module.hr.mapper.HrEmployeeMapper;
import com.ceams.module.hr.mapper.HrPerformanceReviewMapper;
import com.ceams.module.hr.mapper.HrPerformanceTemplateMapper;
import com.ceams.module.hr.service.HrEmployeeService;
import com.ceams.module.hr.service.HrPerformanceService;
import com.ceams.module.hr.util.HrPageUtil;
import com.ceams.module.hr.vo.PerformanceBatchResultVO;
import com.ceams.module.hr.vo.PerformanceReviewVO;
import com.ceams.module.hr.vo.PerformanceTemplateVO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * HR 绩效服务实现。
 *
 * <p>核心规则：模板权重合计必须为 100、系数规则区间连续不重叠；自评提交即发起 BPM 并回写实例 ID，
 * 发起失败整体回滚；HR 复核定稿后同步完成 BPM 复核任务，保证业务状态与流程状态一致。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HrPerformanceServiceImpl implements HrPerformanceService {

    /** 分数落库精度。 */
    private static final int SCORE_SCALE = 2;

    /** 中间计算精度。 */
    private static final int CALC_SCALE = 4;

    /** 满分 / 权重合计。 */
    private static final BigDecimal FULL_SCORE = new BigDecimal("100");

    /** 模板状态：启用。 */
    private static final String STATUS_ENABLED = "ENABLED";

    /** 模板状态：停用。 */
    private static final String STATUS_DISABLED = "DISABLED";

    /** 绩效流程模板编码。 */
    private static final String TEMPLATE_CODE = BpmBizType.HR_PERFORMANCE.name();

    /** HR 复核节点编码（与 V32 流程模板保持一致）。 */
    private static final String HR_REVIEW_NODE_KEY = "hr_review";

    /** 评语字段最大长度（与 hr_performance_review 列定义一致）。 */
    private static final int COMMENT_MAX_LENGTH = 1000;

    private final HrPerformanceTemplateMapper templateMapper;
    private final HrPerformanceReviewMapper reviewMapper;
    private final HrEmployeeMapper employeeMapper;
    private final HrEmployeeService employeeService;
    private final BpmInstanceService bpmInstanceService;
    private final BpmTaskService bpmTaskService;
    private final ObjectMapper objectMapper;

    // ------------------------------------------------------------------
    // 模板
    // ------------------------------------------------------------------

    @Override
    @Transactional
    public String saveTemplate(Long id, PerformanceTemplateSaveRequest request) {
        List<PerformanceItem> items = validateItems(request.getItems());
        List<PerformanceScoreRule> rules = validateRules(request.getScoreRules());
        String code = request.getCode().trim();
        Long duplicated =
                templateMapper.selectCount(
                        new LambdaQueryWrapper<HrPerformanceTemplate>()
                                .eq(HrPerformanceTemplate::getCode, code)
                                .ne(id != null, HrPerformanceTemplate::getId, id));
        if (duplicated != null && duplicated > 0) {
            throw BusinessException.of(ErrorCode.BIZ_DUPLICATE_CODE, "模板编码已存在：" + code);
        }

        HrPerformanceTemplate template;
        if (id == null) {
            template = new HrPerformanceTemplate();
            template.setStatus(resolveStatus(request.getStatus(), STATUS_ENABLED));
        } else {
            template = requireTemplate(id);
            template.setStatus(resolveStatus(request.getStatus(), template.getStatus()));
        }
        template.setCode(code);
        template.setName(request.getName().trim());
        template.setPositionId(request.getPositionId());
        template.setPeriodType(request.getPeriodType());
        template.setItemsJson(toJson(items));
        template.setScoreRulesJson(toJson(rules));
        template.setTotalWeight(FULL_SCORE.intValue());
        if (id == null) {
            templateMapper.insert(template);
            log.info("新建绩效模板 {}（编码 {}）", template.getId(), template.getCode());
        } else {
            templateMapper.updateById(template);
            log.info("修改绩效模板 {}（编码 {}）", template.getId(), template.getCode());
        }
        return String.valueOf(template.getId());
    }

    @Override
    public List<PerformanceTemplateVO> listTemplates() {
        List<HrPerformanceTemplate> templates =
                templateMapper.selectList(
                        new LambdaQueryWrapper<HrPerformanceTemplate>()
                                .orderByDesc(HrPerformanceTemplate::getCreateTime));
        return templates.stream().map(this::toTemplateVO).toList();
    }

    // ------------------------------------------------------------------
    // 考评单
    // ------------------------------------------------------------------

    @Override
    public PageResult<PerformanceReviewVO> pageReviews(
            long page,
            long size,
            Long templateId,
            String period,
            PerformanceReviewStatus status,
            Long employeeId) {
        Page<HrPerformanceReview> p = HrPageUtil.resolve(page, size);
        LambdaQueryWrapper<HrPerformanceReview> wrapper = new LambdaQueryWrapper<>();
        if (templateId != null) {
            wrapper.eq(HrPerformanceReview::getTemplateId, templateId);
        }
        if (StringUtils.isNotBlank(period)) {
            wrapper.eq(HrPerformanceReview::getPeriod, period.trim());
        }
        if (status != null) {
            wrapper.eq(HrPerformanceReview::getStatus, status);
        }
        if (employeeId != null) {
            wrapper.eq(HrPerformanceReview::getEmployeeId, employeeId);
        }
        wrapper.orderByDesc(HrPerformanceReview::getCreateTime);
        IPage<HrPerformanceReview> result = reviewMapper.selectPage(p, wrapper);
        return toPageResult(result, resolveCurrentEmployeeIdQuietly());
    }

    @Override
    public PageResult<PerformanceReviewVO> myReviews(long page, long size) {
        Page<HrPerformanceReview> p = HrPageUtil.resolve(page, size);
        Long employeeId = employeeService.currentEmployeeId();
        IPage<HrPerformanceReview> result =
                reviewMapper.selectPage(
                        p,
                        new LambdaQueryWrapper<HrPerformanceReview>()
                                .eq(HrPerformanceReview::getEmployeeId, employeeId)
                                .orderByDesc(HrPerformanceReview::getCreateTime));
        return toPageResult(result, employeeId);
    }

    @Override
    @Transactional
    public PerformanceBatchResultVO batchGenerate(PerformanceReviewBatchRequest request) {
        HrPerformanceTemplate template = requireTemplate(request.getTemplateId());
        if (!STATUS_ENABLED.equals(template.getStatus())) {
            throw BusinessException.of(ErrorCode.BIZ_STATE_CONFLICT, "绩效模板未启用，无法生成考评单");
        }
        String period = request.getPeriod().trim();
        LambdaQueryWrapper<HrEmployee> wrapper =
                new LambdaQueryWrapper<HrEmployee>()
                        .in(
                                HrEmployee::getStatus,
                                List.of(EmployeeStatus.PROBATION, EmployeeStatus.ACTIVE));
        if (request.getDeptId() != null) {
            wrapper.eq(HrEmployee::getDeptId, request.getDeptId());
        }
        wrapper.orderByAsc(HrEmployee::getId);
        List<HrEmployee> employees = employeeMapper.selectList(wrapper);
        if (employees.isEmpty()) {
            return new PerformanceBatchResultVO(0, 0, 0);
        }

        Set<Long> existed =
                reviewMapper
                        .selectList(
                                new LambdaQueryWrapper<HrPerformanceReview>()
                                        .eq(HrPerformanceReview::getTemplateId, template.getId())
                                        .eq(HrPerformanceReview::getPeriod, period)
                                        .in(
                                                HrPerformanceReview::getEmployeeId,
                                                employees.stream().map(HrEmployee::getId).toList()))
                        .stream()
                        .map(HrPerformanceReview::getEmployeeId)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());

        Map<Long, HrEmployee> employeeMap = loadEmployeesWithLeaders(employees);
        int created = 0;
        for (HrEmployee employee : employees) {
            if (existed.contains(employee.getId())) {
                continue;
            }
            HrPerformanceReview review = new HrPerformanceReview();
            review.setEmployeeId(employee.getId());
            review.setTemplateId(template.getId());
            review.setPeriod(period);
            review.setLeaderId(resolveLeaderId(employee, employeeMap));
            review.setStatus(PerformanceReviewStatus.DRAFT);
            reviewMapper.insert(review);
            created++;
        }
        int skipped = employees.size() - created;
        log.info(
                "绩效考评单批量生成：模板 {}，周期 {}，部门 {}，总人数 {}，新建 {}，跳过 {}",
                template.getId(),
                period,
                request.getDeptId(),
                employees.size(),
                created,
                skipped);
        return new PerformanceBatchResultVO(employees.size(), created, skipped);
    }

    @Override
    @Transactional
    public void submitSelf(Long reviewId, PerformanceSelfRequest request) {
        HrPerformanceReview review = requireReview(reviewId);
        requireSelfOwner(review);
        if (review.getStatus() != PerformanceReviewStatus.DRAFT) {
            throw BusinessException.of(
                    ErrorCode.BIZ_STATE_CONFLICT, "仅待自评的考评单可提交自评");
        }
        HrPerformanceTemplate template = requireTemplate(review.getTemplateId());
        BigDecimal selfScore = calculateScore(request.getItems(), templateWeights(template));

        review.setSelfItemsJson(toJson(request.getItems()));
        review.setSelfScore(selfScore);
        review.setSelfComment(request.getComment());
        review.setSelfSubmittedAt(LocalDateTime.now());
        review.setStatus(PerformanceReviewStatus.SELF_SUBMITTED);
        if (review.getLeaderId() == null) {
            review.setLeaderId(resolveLeaderId(review.getEmployeeId()));
        }

        // 先发起流程再统一落库：发起失败抛异常，整个事务回滚，考评单不会停留在错误状态
        InstanceStartRequest start = new InstanceStartRequest();
        start.setTemplateCode(TEMPLATE_CODE);
        start.setBizType(TEMPLATE_CODE);
        start.setBizId(review.getId());
        start.setVariables(buildVariables(review));
        String instanceId = bpmInstanceService.start(start);

        review.setBpmInstanceId(parseInstanceId(instanceId));
        reviewMapper.updateById(review);
        log.info(
                "员工 {} 提交绩效自评，考评单 {}，BPM 实例 {}",
                review.getEmployeeId(),
                review.getId(),
                instanceId);
    }

    @Override
    @Transactional
    public void submitLeader(Long reviewId, PerformanceLeaderRequest request) {
        HrPerformanceReview review = requireReview(reviewId);
        Long leaderId = resolveLeaderId(review);
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId == null || leaderId == null || !leaderId.equals(currentUserId)) {
            throw BusinessException.of(ErrorCode.AUTH_ACCESS_DENIED, "仅直属上级可对该考评单打分");
        }
        if (review.getStatus() != PerformanceReviewStatus.SELF_SUBMITTED) {
            throw BusinessException.of(
                    ErrorCode.BIZ_STATE_CONFLICT, "仅已提交自评的考评单可进行上级打分");
        }
        HrPerformanceTemplate template = requireTemplate(review.getTemplateId());
        BigDecimal leaderScore = calculateScore(request.getItems(), templateWeights(template));

        review.setLeaderId(leaderId);
        review.setLeaderItemsJson(toJson(request.getItems()));
        review.setLeaderScore(leaderScore);
        review.setLeaderComment(request.getComment());
        review.setStatus(PerformanceReviewStatus.LEADER_DONE);
        reviewMapper.updateById(review);
        log.info("考评单 {} 完成上级打分，操作人 {}", review.getId(), currentUserId);
    }

    @Override
    @Transactional
    public void review(Long reviewId, PerformanceReviewRequest request) {
        HrPerformanceReview review = requireReview(reviewId);
        if (review.getStatus() != PerformanceReviewStatus.LEADER_DONE) {
            throw BusinessException.of(
                    ErrorCode.BIZ_STATE_CONFLICT, "仅上级已打分的考评单可进行 HR 复核");
        }
        HrPerformanceTemplate template = requireTemplate(review.getTemplateId());
        BigDecimal finalScore = normalizeFinalScore(request.getFinalScore());
        BigDecimal coefficient = mapCoefficient(template.getScoreRulesJson(), finalScore);

        review.setFinalScore(finalScore);
        review.setCoefficient(coefficient);
        review.setLeaderComment(appendHrComment(review.getLeaderComment(), request.getComment()));
        review.setStatus(PerformanceReviewStatus.APPROVED);
        reviewMapper.updateById(review);

        // 先落定业务状态再推进流程：回调收到 APPROVED 时因考评单已是 APPROVED 直接幂等返回，
        // 避免 final_score / coefficient 被回调节点覆盖。
        completeBpm(review, request.getComment());
        log.info("考评单 {} 完成 HR 复核并定稿", review.getId());
    }

    // ------------------------------------------------------------------
    // 模板校验：权重合计、系数区间
    // ------------------------------------------------------------------

    /** 校验考评项：名称非空且唯一、权重非负、权重合计精确等于 100。返回原列表。 */
    private List<PerformanceItem> validateItems(List<PerformanceItem> items) {
        if (items == null || items.isEmpty()) {
            throw BusinessException.of(ErrorCode.VALID_REQUIRED_FIELD, "考评项不能为空");
        }
        BigDecimal total = BigDecimal.ZERO;
        Set<String> names = new HashSet<>();
        for (PerformanceItem item : items) {
            String name = item == null ? null : StringUtils.trimToNull(item.getName());
            if (name == null) {
                throw BusinessException.of(ErrorCode.VALID_REQUIRED_FIELD, "考评项名称不能为空");
            }
            if (!names.add(name)) {
                throw BusinessException.of(ErrorCode.VALID_ILLEGAL_ARGUMENT, "考评项名称重复：" + name);
            }
            if (item.getWeight() == null) {
                throw BusinessException.of(ErrorCode.VALID_REQUIRED_FIELD, "考评项权重不能为空：" + name);
            }
            if (item.getWeight().compareTo(BigDecimal.ZERO) < 0) {
                throw BusinessException.of(ErrorCode.VALID_ILLEGAL_ARGUMENT, "考评项权重不能为负数：" + name);
            }
            if (item.getMinScore() != null
                    && item.getMaxScore() != null
                    && item.getMinScore().compareTo(item.getMaxScore()) > 0) {
                throw BusinessException.of(
                        ErrorCode.VALID_ILLEGAL_ARGUMENT, "考评项分数区间非法：" + name);
            }
            total = total.add(item.getWeight());
        }
        if (total.compareTo(FULL_SCORE) != 0) {
            throw BusinessException.of(
                    ErrorCode.VALID_ILLEGAL_ARGUMENT, "考评项权重合计必须为 100，当前为 " + total.toPlainString());
        }
        return items;
    }

    /** 校验系数规则：首段从 0 起、末段覆盖 100、相邻区间不重叠；返回按起始分升序的规则。 */
    private List<PerformanceScoreRule> validateRules(List<PerformanceScoreRule> rules) {
        if (rules == null || rules.isEmpty()) {
            throw BusinessException.of(ErrorCode.VALID_REQUIRED_FIELD, "系数规则不能为空");
        }
        for (PerformanceScoreRule rule : rules) {
            if (rule == null
                    || rule.getMinScore() == null
                    || rule.getMaxScore() == null
                    || rule.getCoefficient() == null) {
                throw BusinessException.of(
                        ErrorCode.VALID_REQUIRED_FIELD, "系数规则的分值区间与系数均不能为空");
            }
            if (rule.getMinScore().compareTo(rule.getMaxScore()) > 0) {
                throw BusinessException.of(
                        ErrorCode.VALID_ILLEGAL_ARGUMENT, "系数规则的起始分不得大于结束分");
            }
            if (rule.getCoefficient().compareTo(BigDecimal.ZERO) < 0) {
                throw BusinessException.of(ErrorCode.VALID_ILLEGAL_ARGUMENT, "绩效系数不能为负数");
            }
        }
        List<PerformanceScoreRule> sorted = new ArrayList<>(rules);
        sorted.sort(Comparator.comparing(PerformanceScoreRule::getMinScore));
        if (sorted.get(0).getMinScore().compareTo(BigDecimal.ZERO) != 0) {
            throw BusinessException.of(ErrorCode.VALID_ILLEGAL_ARGUMENT, "系数规则必须从 0 分开始");
        }
        PerformanceScoreRule last = sorted.get(sorted.size() - 1);
        if (last.getMaxScore().compareTo(FULL_SCORE) != 0) {
            throw BusinessException.of(ErrorCode.VALID_ILLEGAL_ARGUMENT, "系数规则必须覆盖至 100 分");
        }
        for (int i = 0; i < sorted.size() - 1; i++) {
            BigDecimal currentMax = sorted.get(i).getMaxScore();
            BigDecimal nextMin = sorted.get(i + 1).getMinScore();
            if (currentMax.compareTo(nextMin) > 0) {
                throw BusinessException.of(
                        ErrorCode.VALID_ILLEGAL_ARGUMENT, "系数规则区间重叠，请检查相邻区间首尾");
            }
        }
        return sorted;
    }

    // ------------------------------------------------------------------
    // 打分计算与系数映射
    // ------------------------------------------------------------------

    /** 模板考评项权重表（名称 → 权重），名称在后端重新校验以防客户端篡改。 */
    private Map<String, BigDecimal> templateWeights(HrPerformanceTemplate template) {
        List<PerformanceItem> items = parseList(template.getItemsJson(), PerformanceItem.class);
        if (items.isEmpty()) {
            throw BusinessException.of(ErrorCode.BIZ_UNPROCESSABLE, "绩效模板缺少考评项，无法计算得分");
        }
        Map<String, BigDecimal> weights = new LinkedHashMap<>();
        for (PerformanceItem item : items) {
            weights.put(item.getName(), item.getWeight() == null ? BigDecimal.ZERO : item.getWeight());
        }
        return weights;
    }

    /**
     * 按模板权重计算加权总分：单项 = 权重 × 得分 ÷ 100（中间 4 位），总分 2 位 HALF_UP 落库。
     *
     * <p>要求提交项与模板考评项一一对应（名称、权重一致），避免少填或篡改权重导致分数失真。
     */
    private BigDecimal calculateScore(
            List<PerformanceScoreItem> items, Map<String, BigDecimal> templateWeights) {
        if (items == null || items.isEmpty()) {
            throw BusinessException.of(ErrorCode.VALID_REQUIRED_FIELD, "打分项不能为空");
        }
        if (items.size() != templateWeights.size()) {
            throw BusinessException.of(ErrorCode.VALID_ILLEGAL_ARGUMENT, "打分项与模板考评项不一致");
        }
        BigDecimal total = BigDecimal.ZERO;
        Set<String> scored = new HashSet<>();
        for (PerformanceScoreItem item : items) {
            String name = item == null ? null : StringUtils.trimToNull(item.getName());
            if (name == null) {
                throw BusinessException.of(ErrorCode.VALID_REQUIRED_FIELD, "打分项名称不能为空");
            }
            BigDecimal weight = templateWeights.get(name);
            if (weight == null) {
                throw BusinessException.of(ErrorCode.VALID_ILLEGAL_ARGUMENT, "打分项与模板考评项不一致：" + name);
            }
            if (!scored.add(name)) {
                throw BusinessException.of(ErrorCode.VALID_ILLEGAL_ARGUMENT, "打分项重复：" + name);
            }
            if (item.getWeight() == null || weight.compareTo(item.getWeight()) != 0) {
                throw BusinessException.of(
                        ErrorCode.VALID_ILLEGAL_ARGUMENT, "打分项权重与模板不一致：" + name);
            }
            requireScoreInRange(item.getScore(), "评分");
            total =
                    total.add(
                            weight.multiply(item.getScore())
                                    .divide(FULL_SCORE, CALC_SCALE, RoundingMode.HALF_UP));
        }
        return total.setScale(SCORE_SCALE, RoundingMode.HALF_UP);
    }

    /**
     * 分数 → 绩效系数：落在哪段取哪段系数（左闭右开，最后一段包含 100）；
     * 低于首段取首段系数、高于末段取末段系数（最接近段兜底）。
     */
    private BigDecimal mapCoefficient(String rulesJson, BigDecimal score) {
        List<PerformanceScoreRule> rules = parseList(rulesJson, PerformanceScoreRule.class);
        if (rules.isEmpty()) {
            throw BusinessException.of(ErrorCode.BIZ_UNPROCESSABLE, "绩效模板缺少系数规则，无法定稿");
        }
        List<PerformanceScoreRule> sorted = new ArrayList<>(rules);
        sorted.sort(Comparator.comparing(PerformanceScoreRule::getMinScore));
        for (int i = 0; i < sorted.size(); i++) {
            PerformanceScoreRule rule = sorted.get(i);
            boolean lastSegment = i == sorted.size() - 1;
            boolean reachLower = score.compareTo(rule.getMinScore()) >= 0;
            boolean withinUpper =
                    score.compareTo(rule.getMaxScore()) < 0
                            || (lastSegment && score.compareTo(rule.getMaxScore()) <= 0);
            if (reachLower && withinUpper) {
                return rule.getCoefficient();
            }
        }
        return score.compareTo(sorted.get(0).getMinScore()) < 0
                ? sorted.get(0).getCoefficient()
                : sorted.get(sorted.size() - 1).getCoefficient();
    }

    /** 定稿分数校验并保留 2 位小数。 */
    private BigDecimal normalizeFinalScore(BigDecimal value) {
        requireScoreInRange(value, "定稿分数");
        return value.setScale(SCORE_SCALE, RoundingMode.HALF_UP);
    }

    /** 分数区间校验：非空且位于 0~100。 */
    private void requireScoreInRange(BigDecimal value, String label) {
        if (value == null) {
            throw BusinessException.of(ErrorCode.VALID_REQUIRED_FIELD, label + "不能为空");
        }
        if (value.compareTo(BigDecimal.ZERO) < 0 || value.compareTo(FULL_SCORE) > 0) {
            throw BusinessException.of(ErrorCode.VALID_ILLEGAL_ARGUMENT, label + "必须在 0~100 之间");
        }
    }

    // ------------------------------------------------------------------
    // BPM 集成
    // ------------------------------------------------------------------

    /** 流程变量只放非敏感标识，分数与评语一律留在业务表。 */
    private Map<String, String> buildVariables(HrPerformanceReview review) {
        Map<String, String> variables = new LinkedHashMap<>();
        variables.put("employeeId", String.valueOf(review.getEmployeeId()));
        variables.put("period", review.getPeriod());
        variables.put("templateId", String.valueOf(review.getTemplateId()));
        return variables;
    }

    /** 完成 BPM 的 HR 复核待办：仅当已发起流程且流程仍在运行时执行。 */
    private void completeBpm(HrPerformanceReview review, String comment) {
        Long instanceId = review.getBpmInstanceId();
        if (instanceId == null) {
            log.debug("考评单 {} 未关联 BPM 实例，跳过流程完成", review.getId());
            return;
        }
        InstanceDetailVO detail = bpmInstanceService.detail(instanceId);
        if (detail == null || detail.getInstance() == null) {
            throw BusinessException.of(ErrorCode.BIZ_UNPROCESSABLE, "关联的流程实例不存在，无法完成复核");
        }
        BpmInstanceStatus status = detail.getInstance().getStatus();
        if (status == BpmInstanceStatus.APPROVED) {
            log.info("考评单 {} 关联流程实例 {} 已审批通过，跳过重复完成", review.getId(), instanceId);
            return;
        }
        if (status != BpmInstanceStatus.RUNNING) {
            throw BusinessException.of(
                    ErrorCode.BIZ_STATE_CONFLICT, "关联流程实例已结束，无法完成复核");
        }
        TaskVO task = findHrReviewTask(detail, SecurityUtils.getCurrentUserId());
        if (task == null) {
            throw BusinessException.of(
                    ErrorCode.BIZ_STATE_CONFLICT, "当前用户不是 HR 复核待办处理人，或复核节点尚未到达");
        }
        TaskApproveRequest approve = new TaskApproveRequest();
        approve.setTaskVersion(task.getTaskVersion());
        approve.setInstanceVersion(detail.getInstance().getVersion());
        approve.setComment(comment);
        bpmTaskService.approve(task.getId(), approve);
    }

    /** 在实例详情中定位当前用户的 hr_review 待办。 */
    private TaskVO findHrReviewTask(InstanceDetailVO detail, Long currentUserId) {
        if (currentUserId == null || detail.getNodes() == null) {
            return null;
        }
        for (NodeVO node : detail.getNodes()) {
            if (node == null || !HR_REVIEW_NODE_KEY.equals(node.getNodeKey()) || node.getTasks() == null) {
                continue;
            }
            for (TaskVO task : node.getTasks()) {
                if (task != null
                        && task.getStatus() == BpmTaskStatus.PENDING
                        && currentUserId.equals(task.getAssigneeId())) {
                    return task;
                }
            }
        }
        return null;
    }

    // ------------------------------------------------------------------
    // 通用装配
    // ------------------------------------------------------------------

    private PageResult<PerformanceReviewVO> toPageResult(
            IPage<HrPerformanceReview> result, Long currentEmployeeId) {
        List<HrPerformanceReview> records = result.getRecords();
        Map<Long, HrEmployee> employees = loadEmployees(records);
        Map<Long, HrPerformanceTemplate> templates = loadTemplates(records);
        List<PerformanceReviewVO> vos =
                records.stream()
                        .map(
                                review ->
                                        toReviewVO(
                                                review,
                                                employees.get(review.getEmployeeId()),
                                                templates.get(review.getTemplateId()),
                                                canSelfEvaluate(review, currentEmployeeId)))
                        .toList();
        return new PageResult<>(vos, result.getTotal(), result.getCurrent(), result.getSize());
    }

    /**
     * 安全获取当前登录员工 ID：未绑定员工档案时返回 null。
     *
     * <p>列表页不能因未绑定员工而整体失败，canSelfEvaluate 统一置 false 即可。
     */
    private Long resolveCurrentEmployeeIdQuietly() {
        try {
            return employeeService.currentEmployeeId();
        } catch (BusinessException e) {
            log.debug("当前账号未绑定员工档案，canSelfEvaluate 统一置 false");
            return null;
        }
    }

    /** 是否可提交自评：本人、DRAFT 且尚未提交过自评。 */
    private boolean canSelfEvaluate(HrPerformanceReview review, Long currentEmployeeId) {
        return review.getStatus() == PerformanceReviewStatus.DRAFT
                && review.getSelfSubmittedAt() == null
                && currentEmployeeId != null
                && Objects.equals(review.getEmployeeId(), currentEmployeeId);
    }

    private PerformanceTemplateVO toTemplateVO(HrPerformanceTemplate template) {
        PerformanceTemplateVO vo = new PerformanceTemplateVO();
        vo.setId(template.getId());
        vo.setCode(template.getCode());
        vo.setName(template.getName());
        vo.setPositionId(template.getPositionId());
        vo.setPeriodType(template.getPeriodType());
        vo.setItems(parseList(template.getItemsJson(), PerformanceItem.class));
        vo.setScoreRules(parseList(template.getScoreRulesJson(), PerformanceScoreRule.class));
        vo.setTotalWeight(template.getTotalWeight());
        vo.setStatus(template.getStatus());
        vo.setCreateTime(template.getCreateTime());
        vo.setUpdateTime(template.getUpdateTime());
        return vo;
    }

    private PerformanceReviewVO toReviewVO(
            HrPerformanceReview review,
            HrEmployee employee,
            HrPerformanceTemplate template,
            boolean canSelfEvaluate) {
        PerformanceReviewVO vo = new PerformanceReviewVO();
        vo.setId(review.getId());
        vo.setEmployeeId(review.getEmployeeId());
        if (employee != null) {
            vo.setEmployeeName(employee.getName());
        }
        vo.setTemplateId(review.getTemplateId());
        if (template != null) {
            vo.setTemplateName(template.getName());
            vo.setTemplateItems(parseList(template.getItemsJson(), PerformanceItem.class));
        }
        vo.setPeriod(review.getPeriod());
        vo.setLeaderId(review.getLeaderId());
        vo.setSelfItems(parseList(review.getSelfItemsJson(), PerformanceScoreItem.class));
        vo.setSelfScore(review.getSelfScore());
        vo.setSelfComment(review.getSelfComment());
        vo.setSelfSubmittedAt(review.getSelfSubmittedAt());
        vo.setLeaderItems(parseList(review.getLeaderItemsJson(), PerformanceScoreItem.class));
        vo.setLeaderScore(review.getLeaderScore());
        vo.setLeaderComment(review.getLeaderComment());
        vo.setFinalScore(review.getFinalScore());
        vo.setCoefficient(review.getCoefficient());
        vo.setStatus(review.getStatus());
        vo.setCanSelfEvaluate(canSelfEvaluate);
        vo.setBpmInstanceId(review.getBpmInstanceId());
        vo.setCreateTime(review.getCreateTime());
        vo.setUpdateTime(review.getUpdateTime());
        return vo;
    }

    private Map<Long, HrEmployee> loadEmployees(List<HrPerformanceReview> reviews) {
        List<Long> employeeIds =
                reviews.stream()
                        .map(HrPerformanceReview::getEmployeeId)
                        .filter(Objects::nonNull)
                        .distinct()
                        .toList();
        if (employeeIds.isEmpty()) {
            return Map.of();
        }
        return employeeMapper.selectBatchIds(employeeIds).stream()
                .collect(Collectors.toMap(HrEmployee::getId, Function.identity(), (a, b) -> a));
    }

    private Map<Long, HrPerformanceTemplate> loadTemplates(List<HrPerformanceReview> reviews) {
        List<Long> templateIds =
                reviews.stream()
                        .map(HrPerformanceReview::getTemplateId)
                        .filter(Objects::nonNull)
                        .distinct()
                        .toList();
        if (templateIds.isEmpty()) {
            return Map.of();
        }
        return templateMapper.selectBatchIds(templateIds).stream()
                .collect(Collectors.toMap(HrPerformanceTemplate::getId, Function.identity(), (a, b) -> a));
    }

    /** 批量场景：以已查员工列表构造映射，并一次性补齐不在列表中的上级员工。 */
    private Map<Long, HrEmployee> loadEmployeesWithLeaders(List<HrEmployee> employees) {
        Map<Long, HrEmployee> map = new LinkedHashMap<>();
        for (HrEmployee employee : employees) {
            map.put(employee.getId(), employee);
        }
        List<Long> missingLeaderIds =
                employees.stream()
                        .map(HrEmployee::getLeaderId)
                        .filter(Objects::nonNull)
                        .filter(id -> !map.containsKey(id))
                        .distinct()
                        .toList();
        if (!missingLeaderIds.isEmpty()) {
            employeeMapper
                    .selectBatchIds(missingLeaderIds)
                    .forEach(employee -> map.putIfAbsent(employee.getId(), employee));
        }
        return map;
    }

    /** 员工直属上级用户 ID（hr_employee.leader_id），批量场景复用已加载的员工映射。 */
    private Long resolveLeaderId(HrEmployee employee, Map<Long, HrEmployee> employees) {
        if (employee == null || employee.getLeaderId() == null) {
            return null;
        }
        HrEmployee leader = employees.get(employee.getLeaderId());
        return leader == null ? null : leader.getUserId();
    }

    /** 单个员工的直属上级；考评单已快照则直接复用快照。 */
    private Long resolveLeaderId(HrPerformanceReview review) {
        if (review.getLeaderId() != null) {
            return review.getLeaderId();
        }
        return resolveLeaderId(review.getEmployeeId());
    }

    private Long resolveLeaderId(Long employeeId) {
        HrEmployee employee = employeeId == null ? null : employeeMapper.selectById(employeeId);
        if (employee == null || employee.getLeaderId() == null) {
            return null;
        }
        HrEmployee leader = employeeMapper.selectById(employee.getLeaderId());
        return leader == null ? null : leader.getUserId();
    }

    // ------------------------------------------------------------------
    // 权限与必填校验
    // ------------------------------------------------------------------

    /** 只能操作本人考评单：当前账号未绑定员工档案时与 employees/me 同样返回 404。 */
    private void requireSelfOwner(HrPerformanceReview review) {
        Long currentEmployeeId = employeeService.currentEmployeeId();
        if (!Objects.equals(currentEmployeeId, review.getEmployeeId())) {
            throw BusinessException.of(ErrorCode.AUTH_ACCESS_DENIED, "只能操作本人的绩效考评单");
        }
    }

    private HrPerformanceTemplate requireTemplate(Long id) {
        HrPerformanceTemplate template = id == null ? null : templateMapper.selectById(id);
        if (template == null) {
            throw BusinessException.of(ErrorCode.BIZ_NOT_FOUND, "绩效模板不存在");
        }
        return template;
    }

    private HrPerformanceReview requireReview(Long id) {
        HrPerformanceReview review = id == null ? null : reviewMapper.selectById(id);
        if (review == null) {
            throw BusinessException.of(ErrorCode.BIZ_NOT_FOUND, "绩效考评单不存在");
        }
        return review;
    }

    private String resolveStatus(String requested, String fallback) {
        if (StringUtils.isBlank(requested)) {
            return StringUtils.isBlank(fallback) ? STATUS_ENABLED : fallback;
        }
        String normalized = requested.trim().toUpperCase(Locale.ROOT);
        if (!STATUS_ENABLED.equals(normalized) && !STATUS_DISABLED.equals(normalized)) {
            throw BusinessException.of(ErrorCode.VALID_ILLEGAL_ARGUMENT, "模板状态非法：" + requested);
        }
        return normalized;
    }

    private String appendHrComment(String leaderComment, String hrComment) {
        if (StringUtils.isBlank(hrComment)) {
            return leaderComment;
        }
        String appended =
                StringUtils.isBlank(leaderComment)
                        ? "HR复核：" + hrComment.trim()
                        : leaderComment + "；HR复核：" + hrComment.trim();
        if (appended.length() > COMMENT_MAX_LENGTH) {
            throw BusinessException.of(
                    ErrorCode.VALID_ILLEGAL_ARGUMENT, "复核意见过长，请精简至 " + COMMENT_MAX_LENGTH + " 字符以内");
        }
        return appended;
    }

    // ------------------------------------------------------------------
    // JSON 编解码与实例 ID 解析
    // ------------------------------------------------------------------

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.BIZ_UNPROCESSABLE, "绩效数据序列化失败", e);
        }
    }

    private <T> List<T> parseList(String json, Class<T> type) {
        if (StringUtils.isBlank(json)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(
                    json, objectMapper.getTypeFactory().constructCollectionType(List.class, type));
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.BIZ_UNPROCESSABLE, "绩效数据 JSON 解析失败", e);
        }
    }

    private Long parseInstanceId(String instanceId) {
        if (StringUtils.isBlank(instanceId)) {
            return null;
        }
        try {
            return Long.valueOf(instanceId.trim());
        } catch (NumberFormatException e) {
            log.warn("BPM 实例 ID 非数字，无法回写考评单：{}", instanceId);
            return null;
        }
    }
}
