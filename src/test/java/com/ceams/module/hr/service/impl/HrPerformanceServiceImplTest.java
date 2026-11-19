package com.ceams.module.hr.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ceams.common.exception.BusinessException;
import com.ceams.common.exception.ErrorCode;
import com.ceams.common.response.PageResult;
import com.ceams.module.bpm.dto.InstanceStartRequest;
import com.ceams.module.bpm.dto.TaskApproveRequest;
import com.ceams.module.bpm.enums.BpmInstanceStatus;
import com.ceams.module.bpm.enums.BpmTaskStatus;
import com.ceams.module.bpm.service.BpmInstanceService;
import com.ceams.module.bpm.service.BpmTaskService;
import com.ceams.module.bpm.vo.InstanceDetailVO;
import com.ceams.module.bpm.vo.InstanceVO;
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
import com.ceams.module.hr.enums.PeriodType;
import com.ceams.module.hr.mapper.HrEmployeeMapper;
import com.ceams.module.hr.mapper.HrPerformanceReviewMapper;
import com.ceams.module.hr.mapper.HrPerformanceTemplateMapper;
import com.ceams.module.hr.service.HrEmployeeService;
import com.ceams.module.hr.vo.PerformanceBatchResultVO;
import com.ceams.module.hr.vo.PerformanceReviewVO;
import com.ceams.module.iam.entity.SysUser;
import com.ceams.module.iam.mapper.UserMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

/** 绩效服务单元测试：模板校验、批量生成去重、自评 / 上级打分 / HR 复核定稿。 */
@ExtendWith(MockitoExtension.class)
class HrPerformanceServiceImplTest {

    private static final Long REVIEW_ID = 8001L;
    private static final Long TEMPLATE_ID = 9000L;
    private static final Long EMPLOYEE_ID = 900L;
    private static final Long LEADER_USER_ID = 12L;

    /** 两考评项（60 / 40）模板 JSON。 */
    private static final String ITEMS_JSON =
            "[{\"name\":\"业绩\",\"weight\":60},{\"name\":\"能力\",\"weight\":40}]";

    /** 三段系数规则 JSON。 */
    private static final String RULES_JSON =
            "[{\"minScore\":0,\"maxScore\":60,\"coefficient\":0.8},"
                    + "{\"minScore\":60,\"maxScore\":80,\"coefficient\":1.0},"
                    + "{\"minScore\":80,\"maxScore\":100,\"coefficient\":1.2}]";

    @Mock private HrPerformanceTemplateMapper templateMapper;
    @Mock private HrPerformanceReviewMapper reviewMapper;
    @Mock private HrEmployeeMapper employeeMapper;
    @Mock private UserMapper userMapper;
    @Mock private HrEmployeeService employeeService;
    @Mock private BpmInstanceService bpmInstanceService;
    @Mock private BpmTaskService bpmTaskService;

    private HrPerformanceServiceImpl performanceService;

    @BeforeEach
    void setUp() {
        performanceService =
                new HrPerformanceServiceImpl(
                        templateMapper,
                        reviewMapper,
                        employeeMapper,
                        userMapper,
                        employeeService,
                        bpmInstanceService,
                        bpmTaskService,
                        new ObjectMapper());
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void saveTemplateShouldRejectWeightSumNotHundred() {
        PerformanceTemplateSaveRequest request = templateRequest();
        request.getItems().get(1).setWeight(new BigDecimal("30"));

        assertThatThrownBy(() -> performanceService.saveTemplate(null, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("权重合计必须为 100");
        verify(templateMapper, never()).insert(any(HrPerformanceTemplate.class));
    }

    @Test
    void saveTemplateShouldRejectOverlappingScoreRules() {
        PerformanceTemplateSaveRequest request = templateRequest();
        request.getScoreRules().get(1).setMinScore(new BigDecimal("50"));

        assertThatThrownBy(() -> performanceService.saveTemplate(null, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("区间重叠");
        verify(templateMapper, never()).insert(any(HrPerformanceTemplate.class));
    }

    @Test
    void saveTemplateShouldCreateTemplate() {
        when(templateMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        when(templateMapper.insert(any(HrPerformanceTemplate.class)))
                .thenAnswer(
                        invocation -> {
                            HrPerformanceTemplate template = invocation.getArgument(0);
                            template.setId(TEMPLATE_ID);
                            return 1;
                        });

        String id = performanceService.saveTemplate(null, templateRequest());

        assertThat(id).isEqualTo("9000");
        ArgumentCaptor<HrPerformanceTemplate> captor =
                ArgumentCaptor.forClass(HrPerformanceTemplate.class);
        verify(templateMapper).insert(captor.capture());
        HrPerformanceTemplate saved = captor.getValue();
        assertThat(saved.getTotalWeight()).isEqualTo(100);
        assertThat(saved.getStatus()).isEqualTo("ENABLED");
        assertThat(saved.getItemsJson()).contains("业绩").contains("60");
        // 规则按起始分升序落库，保证后续系数映射顺序稳定
        assertThat(saved.getScoreRulesJson()).startsWith("[{\"minScore\":0,");
    }

    @Test
    void saveTemplateShouldRejectDuplicateCode() {
        when(templateMapper.selectCount(any(Wrapper.class))).thenReturn(1L);

        assertThatThrownBy(() -> performanceService.saveTemplate(null, templateRequest()))
                .isInstanceOf(BusinessException.class)
                .satisfies(
                        ex ->
                                assertThat(((BusinessException) ex).getErrorCode())
                                        .isEqualTo(ErrorCode.BIZ_DUPLICATE_CODE));
        verify(templateMapper, never()).insert(any(HrPerformanceTemplate.class));
    }

    @Test
    void batchGenerateShouldSkipExistingReview() {
        when(templateMapper.selectById(TEMPLATE_ID)).thenReturn(template());
        when(employeeMapper.selectList(any(Wrapper.class)))
                .thenReturn(List.of(employee(101L, 11L), employee(102L, 12L)));
        HrPerformanceReview existed = new HrPerformanceReview();
        existed.setEmployeeId(101L);
        when(reviewMapper.selectList(any(Wrapper.class))).thenReturn(List.of(existed));
        when(userMapper.selectBatchIds(any()))
                .thenReturn(List.of(user(11L, LEADER_USER_ID), user(12L, null)));
        when(reviewMapper.insert(any(HrPerformanceReview.class)))
                .thenAnswer(
                        invocation -> {
                            HrPerformanceReview review = invocation.getArgument(0);
                            review.setId(REVIEW_ID);
                            return 1;
                        });

        PerformanceBatchResultVO result = performanceService.batchGenerate(batchRequest());

        assertThat(result.getTotal()).isEqualTo(2);
        assertThat(result.getCreated()).isEqualTo(1);
        assertThat(result.getSkipped()).isEqualTo(1);
        ArgumentCaptor<HrPerformanceReview> captor =
                ArgumentCaptor.forClass(HrPerformanceReview.class);
        verify(reviewMapper).insert(captor.capture());
        HrPerformanceReview created = captor.getValue();
        assertThat(created.getEmployeeId()).isEqualTo(102L);
        assertThat(created.getStatus()).isEqualTo(PerformanceReviewStatus.DRAFT);
        assertThat(created.getLeaderId()).isNull();
    }

    @Test
    void submitSelfShouldCalculateWeightedScoreAndWriteBackInstanceId() {
        when(employeeService.currentEmployeeId()).thenReturn(EMPLOYEE_ID);
        HrPerformanceReview review = review(PerformanceReviewStatus.DRAFT);
        review.setLeaderId(null);
        when(reviewMapper.selectById(REVIEW_ID)).thenReturn(review);
        when(templateMapper.selectById(TEMPLATE_ID)).thenReturn(template());
        when(employeeMapper.selectById(EMPLOYEE_ID)).thenReturn(employee(EMPLOYEE_ID, 11L));
        when(userMapper.selectById(11L)).thenReturn(user(11L, LEADER_USER_ID));
        when(bpmInstanceService.start(any(InstanceStartRequest.class))).thenReturn("8110");

        performanceService.submitSelf(REVIEW_ID, selfRequest("90", "80"));

        // 60 × 90 / 100 + 40 × 80 / 100 = 86.00
        assertThat(review.getSelfScore()).isEqualByComparingTo("86.00");
        assertThat(review.getStatus()).isEqualTo(PerformanceReviewStatus.SELF_SUBMITTED);
        assertThat(review.getBpmInstanceId()).isEqualTo(8110L);
        assertThat(review.getLeaderId()).isEqualTo(LEADER_USER_ID);
        assertThat(review.getSelfSubmittedAt()).isNotNull();

        ArgumentCaptor<InstanceStartRequest> startCaptor =
                ArgumentCaptor.forClass(InstanceStartRequest.class);
        verify(bpmInstanceService).start(startCaptor.capture());
        InstanceStartRequest start = startCaptor.getValue();
        assertThat(start.getTemplateCode()).isEqualTo("HR_PERFORMANCE");
        assertThat(start.getBizType()).isEqualTo("HR_PERFORMANCE");
        assertThat(start.getBizId()).isEqualTo(REVIEW_ID);
        // 流程变量只放标识，不放分数 / 评语
        assertThat(start.getVariables())
                .containsEntry("employeeId", "900")
                .containsEntry("period", "2026-11")
                .containsEntry("templateId", "9000")
                .doesNotContainKey("selfScore");
        verify(reviewMapper).updateById(review);
    }

    @Test
    void submitSelfShouldRejectNonDraftReview() {
        when(employeeService.currentEmployeeId()).thenReturn(EMPLOYEE_ID);
        when(reviewMapper.selectById(REVIEW_ID))
                .thenReturn(review(PerformanceReviewStatus.SELF_SUBMITTED));

        assertThatThrownBy(() -> performanceService.submitSelf(REVIEW_ID, selfRequest("90", "80")))
                .isInstanceOf(BusinessException.class)
                .satisfies(
                        ex ->
                                assertThat(((BusinessException) ex).getErrorCode())
                                        .isEqualTo(ErrorCode.BIZ_STATE_CONFLICT));
        verify(bpmInstanceService, never()).start(any(InstanceStartRequest.class));
    }

    @Test
    void submitLeaderShouldSetLeaderDoneAndScore() {
        loginAs(LEADER_USER_ID);
        HrPerformanceReview review = review(PerformanceReviewStatus.SELF_SUBMITTED);
        review.setLeaderId(LEADER_USER_ID);
        when(reviewMapper.selectById(REVIEW_ID)).thenReturn(review);
        when(templateMapper.selectById(TEMPLATE_ID)).thenReturn(template());

        performanceService.submitLeader(REVIEW_ID, leaderRequest("80", "70"));

        // 60 × 80 / 100 + 40 × 70 / 100 = 76.00
        assertThat(review.getLeaderScore()).isEqualByComparingTo("76.00");
        assertThat(review.getStatus()).isEqualTo(PerformanceReviewStatus.LEADER_DONE);
        assertThat(review.getLeaderComment()).isEqualTo("季度表现良好");
        verify(reviewMapper).updateById(review);
    }

    @Test
    void submitLeaderShouldRejectNonSuperior() {
        loginAs(99L);
        HrPerformanceReview review = review(PerformanceReviewStatus.SELF_SUBMITTED);
        review.setLeaderId(LEADER_USER_ID);
        when(reviewMapper.selectById(REVIEW_ID)).thenReturn(review);

        assertThatThrownBy(() -> performanceService.submitLeader(REVIEW_ID, leaderRequest("80", "70")))
                .isInstanceOf(BusinessException.class)
                .satisfies(
                        ex ->
                                assertThat(((BusinessException) ex).getErrorCode())
                                        .isEqualTo(ErrorCode.AUTH_ACCESS_DENIED));
        verify(reviewMapper, never()).updateById(any(HrPerformanceReview.class));
    }

    @Test
    void reviewShouldMapCoefficientAndApprove() {
        HrPerformanceReview review = review(PerformanceReviewStatus.LEADER_DONE);
        review.setLeaderComment("上级评语");
        when(reviewMapper.selectById(REVIEW_ID)).thenReturn(review);
        when(templateMapper.selectById(TEMPLATE_ID)).thenReturn(template());

        PerformanceReviewRequest request = new PerformanceReviewRequest();
        request.setFinalScore(new BigDecimal("85"));
        request.setComment("同意");
        performanceService.review(REVIEW_ID, request);

        // 85 落在 [80,100] 段 → 1.2
        assertThat(review.getCoefficient()).isEqualByComparingTo("1.2");
        assertThat(review.getFinalScore()).isEqualByComparingTo("85.00");
        assertThat(review.getStatus()).isEqualTo(PerformanceReviewStatus.APPROVED);
        assertThat(review.getLeaderComment()).contains("上级评语").contains("HR复核：同意");
        verify(reviewMapper).updateById(review);
        // 未关联流程实例时不做 BPM 推进
        verify(bpmInstanceService, never()).detail(anyLong());
    }

    @Test
    void reviewShouldIncludeUpperBoundInLastRuleSegment() {
        HrPerformanceReview review = review(PerformanceReviewStatus.LEADER_DONE);
        when(reviewMapper.selectById(REVIEW_ID)).thenReturn(review);
        when(templateMapper.selectById(TEMPLATE_ID)).thenReturn(template());

        PerformanceReviewRequest request = new PerformanceReviewRequest();
        request.setFinalScore(new BigDecimal("100"));
        performanceService.review(REVIEW_ID, request);

        // 100 属于最后一段（包含上界），取 1.2
        assertThat(review.getCoefficient()).isEqualByComparingTo("1.2");
        assertThat(review.getFinalScore()).isEqualByComparingTo("100.00");
    }

    @Test
    void reviewShouldCompleteHrTaskAfterApprove() {
        loginAs(7L);
        HrPerformanceReview review = review(PerformanceReviewStatus.LEADER_DONE);
        review.setBpmInstanceId(8110L);
        when(reviewMapper.selectById(REVIEW_ID)).thenReturn(review);
        when(templateMapper.selectById(TEMPLATE_ID)).thenReturn(template());

        TaskVO task = new TaskVO();
        task.setId(501L);
        task.setAssigneeId(7L);
        task.setTaskVersion(3);
        task.setStatus(BpmTaskStatus.PENDING);
        NodeVO node = new NodeVO();
        node.setNodeKey("hr_review");
        node.setTasks(List.of(task));
        InstanceVO instance = new InstanceVO();
        instance.setId(8110L);
        instance.setStatus(BpmInstanceStatus.RUNNING);
        instance.setVersion(0);
        InstanceDetailVO detail = new InstanceDetailVO();
        detail.setInstance(instance);
        detail.setNodes(List.of(node));
        when(bpmInstanceService.detail(8110L)).thenReturn(detail);

        PerformanceReviewRequest request = new PerformanceReviewRequest();
        request.setFinalScore(new BigDecimal("85"));
        request.setComment("同意");
        performanceService.review(REVIEW_ID, request);

        ArgumentCaptor<TaskApproveRequest> approveCaptor =
                ArgumentCaptor.forClass(TaskApproveRequest.class);
        verify(bpmTaskService).approve(eq(501L), approveCaptor.capture());
        assertThat(approveCaptor.getValue().getTaskVersion()).isEqualTo(3);
        assertThat(approveCaptor.getValue().getInstanceVersion()).isEqualTo(0);
        assertThat(approveCaptor.getValue().getComment()).isEqualTo("同意");
        // 先落定考评单状态，再推进 BPM，保证回调幂等命中不会覆盖定稿数据
        InOrder order = inOrder(reviewMapper, bpmTaskService);
        order.verify(reviewMapper).updateById(review);
        order.verify(bpmTaskService).approve(eq(501L), any(TaskApproveRequest.class));
    }

    @Test
    void pageReviewsShouldMarkOwnDraftAsSelfEvaluatable() {
        mockReviewPage(review(PerformanceReviewStatus.DRAFT));
        when(employeeService.currentEmployeeId()).thenReturn(EMPLOYEE_ID);

        PageResult<PerformanceReviewVO> result = performanceService.pageReviews(1, 10, null, null, null, null);

        assertThat(result.getRecords()).hasSize(1);
        assertThat(result.getRecords().get(0).getCanSelfEvaluate()).isTrue();
    }

    @Test
    void pageReviewsShouldNotMarkNonDraftAsSelfEvaluatable() {
        // 已提交自评的工单，本人也只能查看，不能再次提交
        mockReviewPage(review(PerformanceReviewStatus.SELF_SUBMITTED));
        when(employeeService.currentEmployeeId()).thenReturn(EMPLOYEE_ID);

        PageResult<PerformanceReviewVO> result = performanceService.pageReviews(1, 10, null, null, null, null);

        assertThat(result.getRecords().get(0).getCanSelfEvaluate()).isFalse();
    }

    @Test
    void pageReviewsShouldNotFailWhenCurrentUserUnbound() {
        mockReviewPage(review(PerformanceReviewStatus.DRAFT));
        when(employeeService.currentEmployeeId())
                .thenThrow(BusinessException.of(ErrorCode.BIZ_NOT_FOUND, "未绑定员工档案"));

        PageResult<PerformanceReviewVO> result = performanceService.pageReviews(1, 10, null, null, null, null);

        assertThat(result.getRecords()).hasSize(1);
        assertThat(result.getRecords().get(0).getCanSelfEvaluate()).isFalse();
    }

    /** 构造单条分页数据，并预置员工 / 模板批量查询。 */
    private void mockReviewPage(HrPerformanceReview review) {
        Page<HrPerformanceReview> page = new Page<>(1, 10, 1);
        page.setRecords(List.of(review));
        when(reviewMapper.selectPage(any(Page.class), any())).thenReturn(page);
        when(employeeMapper.selectBatchIds(any()))
                .thenReturn(List.of(employee(EMPLOYEE_ID, LEADER_USER_ID)));
        when(templateMapper.selectBatchIds(any())).thenReturn(List.of(template()));
    }

    // ------------------------------------------------------------------
    // 测试数据
    // ------------------------------------------------------------------

    private void loginAs(Long userId) {
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(userId, null, List.of()));
    }

    private PerformanceTemplateSaveRequest templateRequest() {
        PerformanceTemplateSaveRequest request = new PerformanceTemplateSaveRequest();
        request.setCode("PERF_MONTH");
        request.setName("月度绩效模板");
        request.setPeriodType(PeriodType.MONTH);
        request.setStatus("ENABLED");

        PerformanceItem first = new PerformanceItem();
        first.setName("业绩");
        first.setWeight(new BigDecimal("60"));
        PerformanceItem second = new PerformanceItem();
        second.setName("能力");
        second.setWeight(new BigDecimal("40"));
        request.setItems(List.of(first, second));

        PerformanceScoreRule low = new PerformanceScoreRule();
        low.setMinScore(new BigDecimal("0"));
        low.setMaxScore(new BigDecimal("60"));
        low.setCoefficient(new BigDecimal("0.8"));
        PerformanceScoreRule middle = new PerformanceScoreRule();
        middle.setMinScore(new BigDecimal("60"));
        middle.setMaxScore(new BigDecimal("80"));
        middle.setCoefficient(new BigDecimal("1.0"));
        PerformanceScoreRule high = new PerformanceScoreRule();
        high.setMinScore(new BigDecimal("80"));
        high.setMaxScore(new BigDecimal("100"));
        high.setCoefficient(new BigDecimal("1.2"));
        request.setScoreRules(List.of(low, middle, high));
        return request;
    }

    private HrPerformanceTemplate template() {
        HrPerformanceTemplate template = new HrPerformanceTemplate();
        template.setId(TEMPLATE_ID);
        template.setCode("PERF_MONTH");
        template.setName("月度绩效模板");
        template.setPeriodType(PeriodType.MONTH);
        template.setItemsJson(ITEMS_JSON);
        template.setScoreRulesJson(RULES_JSON);
        template.setTotalWeight(100);
        template.setStatus("ENABLED");
        return template;
    }

    private HrPerformanceReview review(PerformanceReviewStatus status) {
        HrPerformanceReview review = new HrPerformanceReview();
        review.setId(REVIEW_ID);
        review.setEmployeeId(EMPLOYEE_ID);
        review.setTemplateId(TEMPLATE_ID);
        review.setPeriod("2026-11");
        review.setStatus(status);
        return review;
    }

    private PerformanceReviewBatchRequest batchRequest() {
        PerformanceReviewBatchRequest request = new PerformanceReviewBatchRequest();
        request.setTemplateId(TEMPLATE_ID);
        request.setPeriod("2026-11");
        return request;
    }

    private PerformanceSelfRequest selfRequest(String firstScore, String secondScore) {
        PerformanceSelfRequest request = new PerformanceSelfRequest();
        request.setItems(List.of(scoreItem("业绩", "60", firstScore), scoreItem("能力", "40", secondScore)));
        request.setComment("本月自评");
        return request;
    }

    private PerformanceLeaderRequest leaderRequest(String firstScore, String secondScore) {
        PerformanceLeaderRequest request = new PerformanceLeaderRequest();
        request.setItems(List.of(scoreItem("业绩", "60", firstScore), scoreItem("能力", "40", secondScore)));
        request.setComment("季度表现良好");
        return request;
    }

    private PerformanceScoreItem scoreItem(String name, String weight, String score) {
        PerformanceScoreItem item = new PerformanceScoreItem();
        item.setName(name);
        item.setWeight(new BigDecimal(weight));
        item.setScore(new BigDecimal(score));
        return item;
    }

    private HrEmployee employee(Long id, Long userId) {
        HrEmployee employee = new HrEmployee();
        employee.setId(id);
        employee.setUserId(userId);
        employee.setEmployeeNo("E" + id);
        employee.setName("员工" + id);
        employee.setStatus(EmployeeStatus.ACTIVE);
        return employee;
    }

    private SysUser user(Long id, Long leaderId) {
        SysUser user = new SysUser();
        user.setId(id);
        user.setName("用户" + id);
        user.setLeaderId(leaderId);
        return user;
    }
}
