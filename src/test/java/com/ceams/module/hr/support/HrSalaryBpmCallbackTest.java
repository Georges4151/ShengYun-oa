package com.ceams.module.hr.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.ceams.common.crypto.SensitiveEncryptor;
import com.ceams.module.bpm.entity.BpmInstance;
import com.ceams.module.bpm.enums.AuditAction;
import com.ceams.module.bpm.enums.BpmInstanceStatus;
import com.ceams.module.bpm.service.BpmAuditService;
import com.ceams.module.hr.entity.HrSalary;
import com.ceams.module.hr.entity.HrSalaryChangeApply;
import com.ceams.module.hr.enums.SalaryChangeStatus;
import com.ceams.module.hr.enums.SalaryChangeType;
import com.ceams.module.hr.enums.SalaryStatus;
import com.ceams.module.hr.mapper.HrSalaryChangeApplyMapper;
import com.ceams.module.hr.mapper.HrSalaryMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Base64;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** 薪资审批回调单元测试：通过后换版、驳回不动台账、重复回调幂等。 */
@ExtendWith(MockitoExtension.class)
class HrSalaryBpmCallbackTest {

    private static final Long EMPLOYEE_ID = 900L;
    private static final Long APPLY_ID = 7001L;

    @Mock private HrSalaryMapper salaryMapper;
    @Mock private HrSalaryChangeApplyMapper changeApplyMapper;
    @Mock private BpmAuditService bpmAuditService;

    private SalarySnapshotCodec snapshotCodec;
    private HrSalaryBpmCallback callback;

    @BeforeEach
    void setUp() {
        String secret =
                Base64.getEncoder()
                        .encodeToString(
                                "0123456789abcdef0123456789abcdef"
                                        .getBytes(StandardCharsets.UTF_8));
        snapshotCodec = new SalarySnapshotCodec(new SensitiveEncryptor(secret), new ObjectMapper());
        callback =
                new HrSalaryBpmCallback(
                        salaryMapper, changeApplyMapper, snapshotCodec, bpmAuditService);
    }

    @Test
    void approvedShouldTurnOldActiveToHistoryAndInsertNewActive() {
        HrSalaryChangeApply apply = pendingApply();
        HrSalary oldActive = activeSalary();
        when(changeApplyMapper.selectById(APPLY_ID)).thenReturn(apply);
        when(salaryMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        when(salaryMapper.selectOne(any(Wrapper.class))).thenReturn(oldActive);

        callback.onInstanceFinished(instance(), BpmInstanceStatus.APPROVED);

        assertThat(oldActive.getStatus()).isEqualTo(SalaryStatus.HISTORY);
        verify(salaryMapper).updateById(oldActive);

        ArgumentCaptor<HrSalary> insertCaptor = ArgumentCaptor.forClass(HrSalary.class);
        verify(salaryMapper).insert(insertCaptor.capture());
        HrSalary created = insertCaptor.getValue();
        assertThat(created.getEmployeeId()).isEqualTo(EMPLOYEE_ID);
        assertThat(created.getStatus()).isEqualTo(SalaryStatus.ACTIVE);
        assertThat(created.getChangeApplyId()).isEqualTo(APPLY_ID);
        assertThat(created.getEffectiveDate()).isEqualTo(LocalDate.of(2026, 12, 1));
        assertThat(new BigDecimal(created.getBaseSalaryEnc())).isEqualByComparingTo("9000.00");
        assertThat(new BigDecimal(created.getPostSalaryEnc())).isEqualByComparingTo("5000.00");

        assertThat(apply.getStatus()).isEqualTo(SalaryChangeStatus.APPROVED);
        verify(changeApplyMapper).updateById(apply);
        verify(bpmAuditService)
                .record(
                        anyString(),
                        anyLong(),
                        any(AuditAction.class),
                        any(),
                        any(),
                        anyString(),
                        any());
    }

    @Test
    void rejectedShouldOnlyUpdateApplyStatusAndKeepLedgerUntouched() {
        HrSalaryChangeApply apply = pendingApply();
        when(changeApplyMapper.selectById(APPLY_ID)).thenReturn(apply);

        callback.onInstanceFinished(instance(), BpmInstanceStatus.REJECTED);

        assertThat(apply.getStatus()).isEqualTo(SalaryChangeStatus.REJECTED);
        verify(changeApplyMapper).updateById(apply);
        verify(salaryMapper, never()).selectOne(any(Wrapper.class));
        verify(salaryMapper, never()).insert(any(HrSalary.class));
        verify(salaryMapper, never()).updateById(any(HrSalary.class));
        verify(bpmAuditService, never())
                .record(anyString(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void repeatedCallbackShouldBeIdempotentWhenApplyAlreadyApproved() {
        HrSalaryChangeApply apply = pendingApply();
        apply.setStatus(SalaryChangeStatus.APPROVED);
        when(changeApplyMapper.selectById(APPLY_ID)).thenReturn(apply);

        callback.onInstanceFinished(instance(), BpmInstanceStatus.APPROVED);

        assertThat(apply.getStatus()).isEqualTo(SalaryChangeStatus.APPROVED);
        verify(salaryMapper, never()).insert(any(HrSalary.class));
        verify(salaryMapper, never()).updateById(any(HrSalary.class));
        verify(changeApplyMapper, never()).updateById(any(HrSalaryChangeApply.class));
    }

    @Test
    void callbackShouldSkipInsertWhenLedgerAlreadyGeneratedButStatusNotApproved() {
        HrSalaryChangeApply apply = pendingApply();
        when(changeApplyMapper.selectById(APPLY_ID)).thenReturn(apply);
        when(salaryMapper.selectCount(any(Wrapper.class))).thenReturn(1L);

        callback.onInstanceFinished(instance(), BpmInstanceStatus.APPROVED);

        assertThat(apply.getStatus()).isEqualTo(SalaryChangeStatus.APPROVED);
        verify(salaryMapper, never()).selectOne(any(Wrapper.class));
        verify(salaryMapper, never()).insert(any(HrSalary.class));
        verify(salaryMapper, never()).updateById(any(HrSalary.class));
    }

    private HrSalaryChangeApply pendingApply() {
        HrSalaryChangeApply apply = new HrSalaryChangeApply();
        apply.setId(APPLY_ID);
        apply.setEmployeeId(EMPLOYEE_ID);
        apply.setChangeType(SalaryChangeType.ADJUST);
        apply.setEffectiveDate(LocalDate.of(2026, 12, 1));
        apply.setBeforeSnapshot(
                snapshotCodec.encode(
                        EMPLOYEE_ID,
                        LocalDate.of(2026, 1, 1),
                        new BigDecimal("8000.00"),
                        new BigDecimal("4000.00"),
                        new BigDecimal("1000.00"),
                        new BigDecimal("500.00")));
        apply.setAfterSnapshot(
                snapshotCodec.encode(
                        EMPLOYEE_ID,
                        LocalDate.of(2026, 12, 1),
                        new BigDecimal("9000.00"),
                        new BigDecimal("5000.00"),
                        new BigDecimal("1000.00"),
                        new BigDecimal("500.00")));
        apply.setStatus(SalaryChangeStatus.APPROVING);
        return apply;
    }

    private HrSalary activeSalary() {
        HrSalary salary = new HrSalary();
        salary.setId(4001L);
        salary.setEmployeeId(EMPLOYEE_ID);
        salary.setEffectiveDate(LocalDate.of(2026, 1, 1));
        salary.setBaseSalaryEnc("8000.00");
        salary.setStatus(SalaryStatus.ACTIVE);
        return salary;
    }

    private BpmInstance instance() {
        BpmInstance instance = new BpmInstance();
        instance.setId(9001L);
        instance.setBizId(APPLY_ID);
        return instance;
    }
}
