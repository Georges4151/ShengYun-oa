package com.ceams.module.hr.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.ceams.module.bpm.enums.BpmBizType;
import com.ceams.module.hr.entity.HrCheckinSupplement;
import com.ceams.module.hr.entity.HrEmployee;
import com.ceams.module.hr.entity.HrEmployeeChange;
import com.ceams.module.hr.entity.HrLeaveRequest;
import com.ceams.module.hr.entity.HrPerformanceReview;
import com.ceams.module.hr.entity.HrSalaryChangeApply;
import com.ceams.module.hr.enums.EmployeeStatus;
import com.ceams.module.hr.mapper.HrCheckinSupplementMapper;
import com.ceams.module.hr.mapper.HrEmployeeChangeMapper;
import com.ceams.module.hr.mapper.HrEmployeeMapper;
import com.ceams.module.hr.mapper.HrLeaveRequestMapper;
import com.ceams.module.hr.mapper.HrPerformanceReviewMapper;
import com.ceams.module.hr.mapper.HrSalaryChangeApplyMapper;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** HR 直属上级解析单元测试：按业务单据定位员工、上级链缺失与账号绑定校验。 */
@ExtendWith(MockitoExtension.class)
class HrBpmLeaderProviderTest {

    private static final Long BIZ_ID = 6001L;
    private static final Long EMPLOYEE_ID = 11L;
    private static final Long LEADER_ID = 12L;
    private static final Long LEADER_USER_ID = 22L;

    @Mock private HrEmployeeMapper employeeMapper;
    @Mock private HrEmployeeChangeMapper employeeChangeMapper;
    @Mock private HrPerformanceReviewMapper performanceReviewMapper;
    @Mock private HrLeaveRequestMapper leaveRequestMapper;
    @Mock private HrCheckinSupplementMapper checkinSupplementMapper;
    @Mock private HrSalaryChangeApplyMapper salaryChangeApplyMapper;

    private HrBpmLeaderProvider provider;

    @BeforeEach
    void setUp() {
        provider =
                new HrBpmLeaderProvider(
                        employeeMapper,
                        employeeChangeMapper,
                        performanceReviewMapper,
                        leaveRequestMapper,
                        checkinSupplementMapper,
                        salaryChangeApplyMapper);
    }

    @Test
    void 入转调离单据按申请员工解析直属上级() {
        HrEmployeeChange change = new HrEmployeeChange();
        change.setId(BIZ_ID);
        change.setEmployeeId(EMPLOYEE_ID);
        when(employeeChangeMapper.selectById(BIZ_ID)).thenReturn(change);
        stubEmployeeChain();

        List<BpmBizType> types =
                List.of(
                        BpmBizType.HR_REGULAR,
                        BpmBizType.HR_TRANSFER,
                        BpmBizType.HR_RESIGN,
                        BpmBizType.HR_PROMOTION);
        for (BpmBizType type : types) {
            assertThat(provider.resolveLeaderUserId(type, BIZ_ID)).isEqualTo(LEADER_USER_ID);
        }
    }

    @Test
    void 请假单据按申请人解析直属上级() {
        HrLeaveRequest leave = new HrLeaveRequest();
        leave.setId(BIZ_ID);
        leave.setEmployeeId(EMPLOYEE_ID);
        when(leaveRequestMapper.selectById(BIZ_ID)).thenReturn(leave);
        stubEmployeeChain();

        assertThat(provider.resolveLeaderUserId(BpmBizType.HR_LEAVE, BIZ_ID))
                .isEqualTo(LEADER_USER_ID);
    }

    @Test
    void 绩效单据按被考评人解析直属上级() {
        HrPerformanceReview review = new HrPerformanceReview();
        review.setId(BIZ_ID);
        review.setEmployeeId(EMPLOYEE_ID);
        when(performanceReviewMapper.selectById(BIZ_ID)).thenReturn(review);
        stubEmployeeChain();

        assertThat(provider.resolveLeaderUserId(BpmBizType.HR_PERFORMANCE, BIZ_ID))
                .isEqualTo(LEADER_USER_ID);
    }

    @Test
    void 补卡单据按申请员工解析直属上级() {
        HrCheckinSupplement supplement = new HrCheckinSupplement();
        supplement.setId(BIZ_ID);
        supplement.setEmployeeId(EMPLOYEE_ID);
        when(checkinSupplementMapper.selectById(BIZ_ID)).thenReturn(supplement);
        stubEmployeeChain();

        assertThat(provider.resolveLeaderUserId(BpmBizType.HR_CHECKIN_SUPPLEMENT, BIZ_ID))
                .isEqualTo(LEADER_USER_ID);
    }

    @Test
    void 薪资变动与更正按目标员工解析直属上级() {
        HrSalaryChangeApply apply = new HrSalaryChangeApply();
        apply.setId(BIZ_ID);
        apply.setEmployeeId(EMPLOYEE_ID);
        when(salaryChangeApplyMapper.selectById(BIZ_ID)).thenReturn(apply);
        stubEmployeeChain();

        assertThat(provider.resolveLeaderUserId(BpmBizType.HR_SALARY_CHANGE, BIZ_ID))
                .isEqualTo(LEADER_USER_ID);
        assertThat(provider.resolveLeaderUserId(BpmBizType.HR_SALARY_CORRECTION, BIZ_ID))
                .isEqualTo(LEADER_USER_ID);
    }

    @Test
    void 员工未维护上级时返回空() {
        stubChange(EMPLOYEE_ID);
        when(employeeMapper.selectById(EMPLOYEE_ID))
                .thenReturn(employee(EMPLOYEE_ID, null, EmployeeStatus.ACTIVE, 21L));

        assertThat(provider.resolveLeaderUserId(BpmBizType.HR_PROMOTION, BIZ_ID)).isNull();
    }

    @Test
    void 上级员工不存在时返回空() {
        stubChange(EMPLOYEE_ID);
        when(employeeMapper.selectById(EMPLOYEE_ID))
                .thenReturn(employee(EMPLOYEE_ID, LEADER_ID, EmployeeStatus.ACTIVE, 21L));
        when(employeeMapper.selectById(LEADER_ID)).thenReturn(null);

        assertThat(provider.resolveLeaderUserId(BpmBizType.HR_PROMOTION, BIZ_ID)).isNull();
    }

    @Test
    void 上级已离职时返回空() {
        stubChange(EMPLOYEE_ID);
        when(employeeMapper.selectById(EMPLOYEE_ID))
                .thenReturn(employee(EMPLOYEE_ID, LEADER_ID, EmployeeStatus.ACTIVE, 21L));
        when(employeeMapper.selectById(LEADER_ID))
                .thenReturn(employee(LEADER_ID, null, EmployeeStatus.TERMINATED, LEADER_USER_ID));

        assertThat(provider.resolveLeaderUserId(BpmBizType.HR_PROMOTION, BIZ_ID)).isNull();
    }

    @Test
    void 上级未绑定系统账号时返回空() {
        stubChange(EMPLOYEE_ID);
        when(employeeMapper.selectById(EMPLOYEE_ID))
                .thenReturn(employee(EMPLOYEE_ID, LEADER_ID, EmployeeStatus.ACTIVE, 21L));
        when(employeeMapper.selectById(LEADER_ID))
                .thenReturn(employee(LEADER_ID, null, EmployeeStatus.ACTIVE, null));

        assertThat(provider.resolveLeaderUserId(BpmBizType.HR_PROMOTION, BIZ_ID)).isNull();
    }

    @Test
    void 业务单据不存在时返回空且不再查员工() {
        stubChange(null);

        assertThat(provider.resolveLeaderUserId(BpmBizType.HR_PROMOTION, BIZ_ID)).isNull();
        verifyNoInteractions(employeeMapper);
    }

    @Test
    void 未覆盖业务类型直接返回空() {
        assertThat(provider.resolveLeaderUserId(BpmBizType.EXPENSE, BIZ_ID)).isNull();
        verifyNoInteractions(
                employeeMapper,
                employeeChangeMapper,
                performanceReviewMapper,
                leaveRequestMapper,
                checkinSupplementMapper,
                salaryChangeApplyMapper);
    }

    @Test
    void 业务类型或单据为空时返回空() {
        assertThat(provider.resolveLeaderUserId(null, BIZ_ID)).isNull();
        assertThat(provider.resolveLeaderUserId(BpmBizType.HR_LEAVE, null)).isNull();
        verifyNoInteractions(
                employeeMapper,
                employeeChangeMapper,
                performanceReviewMapper,
                leaveRequestMapper,
                checkinSupplementMapper,
                salaryChangeApplyMapper);
    }

    @Test
    void 按系统用户解析其员工档案的直属上级() {
        HrEmployee employee = employee(EMPLOYEE_ID, LEADER_ID, EmployeeStatus.ACTIVE, 21L);
        when(employeeMapper.selectOne(any(Wrapper.class))).thenReturn(employee);
        when(employeeMapper.selectById(EMPLOYEE_ID)).thenReturn(employee);
        when(employeeMapper.selectById(LEADER_ID))
                .thenReturn(employee(LEADER_ID, null, EmployeeStatus.ACTIVE, LEADER_USER_ID));

        assertThat(provider.resolveLeaderUserIdByUser(21L)).isEqualTo(LEADER_USER_ID);
    }

    @Test
    void 用户未绑定员工档案时返回空() {
        when(employeeMapper.selectOne(any(Wrapper.class))).thenReturn(null);

        assertThat(provider.resolveLeaderUserIdByUser(21L)).isNull();
    }

    @Test
    void 用户为空时返回空且不查库() {
        assertThat(provider.resolveLeaderUserIdByUser(null)).isNull();
        verifyNoInteractions(
                employeeMapper,
                employeeChangeMapper,
                performanceReviewMapper,
                leaveRequestMapper,
                checkinSupplementMapper,
                salaryChangeApplyMapper);
    }

    // ------------------------------------------------------------------
    // 夹具
    // ------------------------------------------------------------------

    private void stubChange(Long employeeId) {
        HrEmployeeChange change = null;
        if (employeeId != null) {
            change = new HrEmployeeChange();
            change.setId(BIZ_ID);
            change.setEmployeeId(employeeId);
        }
        when(employeeChangeMapper.selectById(BIZ_ID)).thenReturn(change);
    }

    /** 员工 11 -> 上级 12，上级绑定账号 22。 */
    private void stubEmployeeChain() {
        when(employeeMapper.selectById(EMPLOYEE_ID))
                .thenReturn(employee(EMPLOYEE_ID, LEADER_ID, EmployeeStatus.ACTIVE, 21L));
        when(employeeMapper.selectById(LEADER_ID))
                .thenReturn(employee(LEADER_ID, null, EmployeeStatus.ACTIVE, LEADER_USER_ID));
    }

    private HrEmployee employee(
            Long id, Long leaderId, EmployeeStatus status, Long userId) {
        HrEmployee employee = new HrEmployee();
        employee.setId(id);
        employee.setEmployeeNo("E" + id);
        employee.setName("员工" + id);
        employee.setLeaderId(leaderId);
        employee.setStatus(status);
        employee.setUserId(userId);
        return employee;
    }
}
