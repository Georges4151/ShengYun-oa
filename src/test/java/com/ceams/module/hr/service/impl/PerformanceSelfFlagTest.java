package com.ceams.module.hr.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ceams.common.response.PageResult;
import com.ceams.module.bpm.service.BpmInstanceService;
import com.ceams.module.bpm.service.BpmTaskService;
import com.ceams.module.hr.entity.HrEmployee;
import com.ceams.module.hr.entity.HrPerformanceReview;
import com.ceams.module.hr.entity.HrPerformanceTemplate;
import com.ceams.module.hr.enums.EmployeeStatus;
import com.ceams.module.hr.enums.PerformanceReviewStatus;
import com.ceams.module.hr.mapper.HrEmployeeMapper;
import com.ceams.module.hr.mapper.HrPerformanceReviewMapper;
import com.ceams.module.hr.mapper.HrPerformanceTemplateMapper;
import com.ceams.module.hr.service.HrEmployeeService;
import com.ceams.module.hr.vo.PerformanceReviewVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * 绩效考评单「是否可自评」标志单元测试（M6 文档 9.10 节）。
 *
 * <p>规则：仅状态为 DRAFT 且考评单所属员工为当前登录员工时，canSelfEvaluate 才为 true。
 */
@ExtendWith(MockitoExtension.class)
class PerformanceSelfFlagTest {

    /** 当前登录员工 ID。 */
    private static final Long CURRENT_EMPLOYEE_ID = 900L;

    /** 其他员工 ID。 */
    private static final Long OTHER_EMPLOYEE_ID = 901L;

    /** 模板 ID。 */
    private static final Long TEMPLATE_ID = 9000L;

    @Mock private HrPerformanceTemplateMapper templateMapper;
    @Mock private HrPerformanceReviewMapper reviewMapper;
    @Mock private HrEmployeeMapper employeeMapper;
    @Mock private HrEmployeeService employeeService;
    @Mock private BpmInstanceService bpmInstanceService;
    @Mock private BpmTaskService bpmTaskService;

    private HrPerformanceServiceImpl performanceService;

    /** 通过公开分页方法驱动：同一批数据中，本人 DRAFT 可自评，他人 DRAFT 不可自评。 */
    @Test
    void draftReviewShouldOnlyBeSelfEvaluatableByOwner() {
        performanceService =
                new HrPerformanceServiceImpl(
                        templateMapper,
                        reviewMapper,
                        employeeMapper,
                        employeeService,
                        bpmInstanceService,
                        bpmTaskService,
                        new ObjectMapper());

        // 两条待自评考评单：第一条属于当前员工，第二条属于其他员工
        HrPerformanceReview ownReview = draftReview(8001L, CURRENT_EMPLOYEE_ID);
        HrPerformanceReview otherReview = draftReview(8002L, OTHER_EMPLOYEE_ID);
        Page<HrPerformanceReview> page = new Page<>(1, 10, 2);
        page.setRecords(List.of(ownReview, otherReview));

        when(reviewMapper.selectPage(any(Page.class), any())).thenReturn(page);
        when(employeeService.currentEmployeeId()).thenReturn(CURRENT_EMPLOYEE_ID);
        when(employeeMapper.selectBatchIds(any()))
                .thenReturn(List.of(employee(CURRENT_EMPLOYEE_ID), employee(OTHER_EMPLOYEE_ID)));
        when(templateMapper.selectBatchIds(any())).thenReturn(List.of(template()));

        PageResult<PerformanceReviewVO> result =
                performanceService.pageReviews(1, 10, null, null, null, null);

        assertThat(result.getRecords()).hasSize(2);
        // 第一组：DRAFT 且 employeeId 等于当前员工 → 可自评
        PerformanceReviewVO own = result.getRecords().get(0);
        assertThat(own.getEmployeeId()).isEqualTo(CURRENT_EMPLOYEE_ID);
        assertThat(own.getCanSelfEvaluate()).isTrue();
        // 第二组：DRAFT 但 employeeId 非当前员工 → 不可自评
        PerformanceReviewVO other = result.getRecords().get(1);
        assertThat(other.getEmployeeId()).isEqualTo(OTHER_EMPLOYEE_ID);
        assertThat(other.getCanSelfEvaluate()).isFalse();
    }

    // ------------------------------------------------------------------
    // 测试数据
    // ------------------------------------------------------------------

    private HrPerformanceReview draftReview(Long id, Long employeeId) {
        HrPerformanceReview review = new HrPerformanceReview();
        review.setId(id);
        review.setEmployeeId(employeeId);
        review.setTemplateId(TEMPLATE_ID);
        review.setPeriod("2026-11");
        review.setStatus(PerformanceReviewStatus.DRAFT);
        return review;
    }

    private HrEmployee employee(Long id) {
        HrEmployee employee = new HrEmployee();
        employee.setId(id);
        employee.setEmployeeNo("E" + id);
        employee.setName("员工" + id);
        employee.setStatus(EmployeeStatus.ACTIVE);
        return employee;
    }

    private HrPerformanceTemplate template() {
        HrPerformanceTemplate template = new HrPerformanceTemplate();
        template.setId(TEMPLATE_ID);
        template.setCode("PERF_MONTH");
        template.setName("月度绩效模板");
        template.setItemsJson("[{\"name\":\"业绩\",\"weight\":100}]");
        template.setTotalWeight(100);
        template.setStatus("ENABLED");
        return template;
    }
}
