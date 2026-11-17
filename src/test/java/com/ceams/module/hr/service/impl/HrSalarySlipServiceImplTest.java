package com.ceams.module.hr.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ceams.common.crypto.SensitiveEncryptor;
import com.ceams.common.exception.BusinessException;
import com.ceams.common.exception.ErrorCode;
import com.ceams.module.bpm.service.BpmAuditService;
import com.ceams.module.hr.dto.SalarySlipPublishRequest;
import com.ceams.module.hr.entity.HrEmployee;
import com.ceams.module.hr.entity.HrSalaryCalc;
import com.ceams.module.hr.entity.HrSalarySlip;
import com.ceams.module.hr.enums.PayrollStatus;
import com.ceams.module.hr.enums.SalarySlipReadStatus;
import com.ceams.module.hr.mapper.HrEmployeeMapper;
import com.ceams.module.hr.mapper.HrSalaryCalcMapper;
import com.ceams.module.hr.mapper.HrSalarySlipMapper;
import com.ceams.module.hr.service.HrEmployeeService;
import com.ceams.module.hr.support.SalarySlipCodec;
import com.ceams.module.hr.vo.SalarySlipPublishResultVO;
import com.ceams.module.hr.vo.SalarySlipVO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * 工资条服务单元测试：发布只取已确认记录、发布幂等、明细金额加密、本人查看解密与已读回写权限。
 *
 * <p>加密器使用真实 {@link SensitiveEncryptor}（测试用 32 字节 Base64 密钥），以便断言密文前缀为
 * {@code v1:} 且明细中不含明文金额。
 */
@ExtendWith(MockitoExtension.class)
class HrSalarySlipServiceImplTest {

    private static final Long EMPLOYEE_ID = 900L;

    private static final Long OTHER_EMPLOYEE_ID = 901L;

    private static final Long CALC_ID = 7001L;

    private static final Long SLIP_ID = 8001L;

    /** 测试用 AES-256 密钥（32 字节，Base64 编码）。 */
    private static final String SECRET =
            Base64.getEncoder()
                    .encodeToString(
                            "0123456789abcdef0123456789abcdef"
                                    .getBytes(StandardCharsets.UTF_8));

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock private HrSalarySlipMapper slipMapper;
    @Mock private HrSalaryCalcMapper calcMapper;
    @Mock private HrEmployeeMapper employeeMapper;
    @Mock private HrEmployeeService employeeService;
    @Mock private BpmAuditService bpmAuditService;

    private SalarySlipCodec codec;
    private HrSalarySlipServiceImpl slipService;

    @BeforeEach
    void setUp() {
        codec = new SalarySlipCodec(new SensitiveEncryptor(SECRET), objectMapper);
        slipService =
                new HrSalarySlipServiceImpl(
                        slipMapper, calcMapper, employeeMapper, employeeService, codec, bpmAuditService);
    }

    @Test
    void publishShouldOnlyPublishConfirmedAndWarnDraft() {
        when(calcMapper.selectList(any()))
                .thenReturn(
                        List.of(
                                confirmedCalc(CALC_ID, EMPLOYEE_ID, "2026-11"),
                                draftCalc(CALC_ID + 1, OTHER_EMPLOYEE_ID, "2026-11")));
        when(slipMapper.selectList(any())).thenReturn(List.of());
        when(employeeMapper.selectBatchIds(any()))
                .thenReturn(List.of(employee(EMPLOYEE_ID), employee(OTHER_EMPLOYEE_ID)));

        SalarySlipPublishResultVO result = slipService.publish(publishRequest("2026-11"));

        assertThat(result.getCalcMonth()).isEqualTo("2026-11");
        assertThat(result.getTotal()).isEqualTo(2);
        assertThat(result.getPublished()).isEqualTo(1);
        assertThat(result.getSkipped()).isEqualTo(1);
        assertThat(result.getWarnings()).contains("存在未确认的核算记录，已跳过");

        ArgumentCaptor<HrSalarySlip> captor = ArgumentCaptor.forClass(HrSalarySlip.class);
        verify(slipMapper).insert(captor.capture());
        HrSalarySlip saved = captor.getValue();
        assertThat(saved.getEmployeeId()).isEqualTo(EMPLOYEE_ID);
        assertThat(saved.getCalcId()).isEqualTo(CALC_ID);
        assertThat(saved.getCalcMonth()).isEqualTo("2026-11");
        assertThat(saved.getReadStatus()).isEqualTo(SalarySlipReadStatus.PENDING);
        assertThat(saved.getPublishedAt()).isNotNull();
    }

    @Test
    void publishShouldSkipExistingSlipForIdempotency() {
        when(calcMapper.selectList(any()))
                .thenReturn(List.of(confirmedCalc(CALC_ID, EMPLOYEE_ID, "2026-11")));
        when(slipMapper.selectList(any())).thenReturn(List.of(existingSlip(EMPLOYEE_ID)));
        when(employeeMapper.selectBatchIds(any())).thenReturn(List.of(employee(EMPLOYEE_ID)));

        SalarySlipPublishResultVO result = slipService.publish(publishRequest("2026-11"));

        assertThat(result.getTotal()).isEqualTo(1);
        assertThat(result.getPublished()).isZero();
        assertThat(result.getSkipped()).isEqualTo(1);
        verify(slipMapper, never()).insert(any(HrSalarySlip.class));
    }

    @Test
    void publishShouldEncryptAllAmountsInContentJson() throws Exception {
        when(calcMapper.selectList(any()))
                .thenReturn(List.of(confirmedCalc(CALC_ID, EMPLOYEE_ID, "2026-11")));
        when(slipMapper.selectList(any())).thenReturn(List.of());
        when(employeeMapper.selectBatchIds(any())).thenReturn(List.of(employee(EMPLOYEE_ID)));

        slipService.publish(publishRequest("2026-11"));

        ArgumentCaptor<HrSalarySlip> captor = ArgumentCaptor.forClass(HrSalarySlip.class);
        verify(slipMapper).insert(captor.capture());
        String contentJson = captor.getValue().getContentJson();

        JsonNode node = objectMapper.readTree(contentJson);
        List<String> amountFields =
                List.of(
                        "baseSalary",
                        "postSalary",
                        "performanceAmount",
                        "allowance",
                        "attendanceDeduction",
                        "socialInsurance",
                        "housingFund",
                        "tax",
                        "grossPay",
                        "netPay");
        for (String field : amountFields) {
            assertThat(node.get(field)).isNotNull();
            assertThat(node.get(field).asText()).startsWith("v1:");
        }
        assertThat(contentJson).doesNotContain("10000").doesNotContain("14647.50");
        assertThat(node.get("employeeNo").asText()).isEqualTo("E0900");
        assertThat(node.get("employeeName").asText()).isEqualTo("赵七");
        assertThat(node.get("attendanceDays").decimalValue()).isEqualByComparingTo("21.75");

        SalarySlipCodec.SalarySlipSnapshot snapshot = codec.decode(contentJson);
        assertThat(snapshot.netPay()).isEqualByComparingTo("14647.50");
        assertThat(snapshot.grossPay()).isEqualByComparingTo("18000.00");
    }

    @Test
    void listMineShouldReturnSlipsInServiceOrderWithDecryptedAmounts() {
        when(employeeService.currentEmployeeId()).thenReturn(EMPLOYEE_ID);
        when(slipMapper.selectList(any()))
                .thenReturn(
                        List.of(
                                slipOf(SLIP_ID, "2026-11", confirmedCalc(CALC_ID, EMPLOYEE_ID, "2026-11")),
                                slipOf(
                                        SLIP_ID + 1,
                                        "2026-10",
                                        confirmedCalc(CALC_ID + 1, EMPLOYEE_ID, "2026-10"))));

        List<SalarySlipVO> mine = slipService.listMine();

        assertThat(mine).hasSize(2);
        assertThat(mine.get(0).getCalcMonth()).isEqualTo("2026-11");
        assertThat(mine.get(1).getCalcMonth()).isEqualTo("2026-10");
        assertThat(mine.get(0).getEmployeeNo()).isEqualTo("E0900");
        assertThat(mine.get(0).getBaseSalary()).isEqualByComparingTo("10000.00");
        assertThat(mine.get(0).getNetPay()).isEqualByComparingTo("14647.50");
    }

    @Test
    void markReadShouldRejectOtherEmployeesSlip() {
        when(slipMapper.selectById(SLIP_ID)).thenReturn(existingSlip(OTHER_EMPLOYEE_ID));
        when(employeeService.currentEmployeeId()).thenReturn(EMPLOYEE_ID);

        assertThatThrownBy(() -> slipService.markRead(SLIP_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.AUTH_ACCESS_DENIED);
        verify(slipMapper, never()).updateById(any(HrSalarySlip.class));
    }

    @Test
    void markReadShouldBeIdempotentWhenAlreadyRead() {
        HrSalarySlip slip = existingSlip(EMPLOYEE_ID);
        slip.setReadStatus(SalarySlipReadStatus.READ);
        LocalDateTime readAt = LocalDateTime.of(2026, 11, 12, 9, 30);
        slip.setReadAt(readAt);
        when(slipMapper.selectById(SLIP_ID)).thenReturn(slip);
        when(employeeService.currentEmployeeId()).thenReturn(EMPLOYEE_ID);

        SalarySlipVO vo = slipService.markRead(SLIP_ID);

        assertThat(vo.getReadStatus()).isEqualTo(SalarySlipReadStatus.READ);
        assertThat(vo.getReadAt()).isEqualTo(readAt);
        verify(slipMapper, never()).updateById(any(HrSalarySlip.class));
    }

    @Test
    void markReadShouldWriteReadStatusAndTimeForOwner() {
        HrSalarySlip slip = existingSlip(EMPLOYEE_ID);
        when(slipMapper.selectById(SLIP_ID)).thenReturn(slip);
        when(employeeService.currentEmployeeId()).thenReturn(EMPLOYEE_ID);

        SalarySlipVO vo = slipService.markRead(SLIP_ID);

        assertThat(vo.getReadStatus()).isEqualTo(SalarySlipReadStatus.READ);
        assertThat(vo.getReadAt()).isNotNull();
        ArgumentCaptor<HrSalarySlip> captor = ArgumentCaptor.forClass(HrSalarySlip.class);
        verify(slipMapper, times(1)).updateById(captor.capture());
        assertThat(captor.getValue().getReadStatus()).isEqualTo(SalarySlipReadStatus.READ);
        assertThat(captor.getValue().getReadAt()).isNotNull();
    }

    private SalarySlipPublishRequest publishRequest(String calcMonth) {
        SalarySlipPublishRequest request = new SalarySlipPublishRequest();
        request.setCalcMonth(calcMonth);
        return request;
    }

    private HrEmployee employee(Long id) {
        HrEmployee employee = new HrEmployee();
        employee.setId(id);
        employee.setEmployeeNo(id == EMPLOYEE_ID ? "E0900" : "E0901");
        employee.setName(id == EMPLOYEE_ID ? "赵七" : "钱八");
        employee.setDeptId(10L);
        return employee;
    }

    private HrSalaryCalc confirmedCalc(Long calcId, Long employeeId, String calcMonth) {
        HrSalaryCalc calc = baseCalc(calcId, employeeId, calcMonth);
        calc.setStatus(PayrollStatus.CONFIRMED);
        return calc;
    }

    private HrSalaryCalc draftCalc(Long calcId, Long employeeId, String calcMonth) {
        HrSalaryCalc calc = baseCalc(calcId, employeeId, calcMonth);
        calc.setStatus(PayrollStatus.DRAFT);
        return calc;
    }

    private HrSalaryCalc baseCalc(Long calcId, Long employeeId, String calcMonth) {
        HrSalaryCalc calc = new HrSalaryCalc();
        calc.setId(calcId);
        calc.setEmployeeId(employeeId);
        calc.setCalcMonth(calcMonth);
        calc.setBaseSalaryEnc("10000");
        calc.setPostSalaryEnc("5000");
        calc.setAttendanceDays(new BigDecimal("21.75"));
        calc.setAbsentDays(new BigDecimal("0.00"));
        calc.setLateCount(0);
        calc.setAttendanceDeductionEnc("0.00");
        calc.setPerformanceCoefficient(new BigDecimal("1.0000"));
        calc.setPerformanceAmountEnc("2000.00");
        calc.setAllowanceEnc("1000.00");
        calc.setSocialInsuranceEnc("1575.00");
        calc.setHousingFundEnc("1050.00");
        calc.setTaxEnc("727.50");
        calc.setGrossPayEnc("18000.00");
        calc.setNetPayEnc("14647.50");
        return calc;
    }

    private HrSalarySlip existingSlip(Long employeeId) {
        HrSalarySlip slip = new HrSalarySlip();
        slip.setId(SLIP_ID);
        slip.setEmployeeId(employeeId);
        slip.setCalcMonth("2026-11");
        slip.setReadStatus(SalarySlipReadStatus.PENDING);
        return slip;
    }

    private HrSalarySlip slipOf(Long slipId, String calcMonth, HrSalaryCalc calc) {
        HrSalarySlip slip = new HrSalarySlip();
        slip.setId(slipId);
        slip.setEmployeeId(calc.getEmployeeId());
        slip.setCalcId(calc.getId());
        slip.setCalcMonth(calcMonth);
        slip.setContentJson(codec.encode(calc, employee(calc.getEmployeeId())));
        slip.setReadStatus(SalarySlipReadStatus.PENDING);
        slip.setPublishedAt(LocalDateTime.of(2026, 11, 1, 10, 0));
        return slip;
    }
}
