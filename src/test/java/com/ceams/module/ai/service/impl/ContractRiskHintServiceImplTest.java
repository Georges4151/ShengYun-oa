package com.ceams.module.ai.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.ceams.module.ai.client.OllamaChatResult;
import com.ceams.module.ai.client.AiChatClient;
import com.ceams.module.ai.config.AiProperties;
import com.ceams.module.ai.vo.ContractClausePointVO;
import com.ceams.module.ai.vo.ContractRiskHintVO;
import com.ceams.module.ai.vo.ContractRiskVO;
import com.ceams.module.project.enums.ContractStatus;
import com.ceams.module.project.service.SubcontractorContractService;
import com.ceams.module.project.vo.SubcontractorContractVO;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/** {@link ContractRiskHintServiceImpl} 固定规则与 AI 复述降级测试。 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ContractRiskHintServiceImplTest {

    private static final Long CONTRACT_ID = 1L;

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 7);

    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-11-07T02:00:00Z"), ZoneId.of("Asia/Shanghai"));

    @Mock private SubcontractorContractService subcontractorContractService;
    @Mock private AiChatClient aiChatClient;

    private AiProperties properties;

    private ContractRiskHintServiceImpl service;

    @BeforeEach
    void setUp() {
        properties = new AiProperties();
        properties.setEnabled(false);
        when(aiChatClient.isEnabled()).thenReturn(false);
        service =
                new ContractRiskHintServiceImpl(
                        properties, subcontractorContractService, aiChatClient, FIXED_CLOCK);
    }

    @Test
    void 超付命中高危规则() {
        SubcontractorContractVO contract =
                contract(new BigDecimal("100.00"), new BigDecimal("110.00"));
        stubContract(contract);

        ContractRiskHintVO vo = service.riskHints(CONTRACT_ID);

        ContractRiskVO risk = findRisk(vo.getRisks(), "OVERPAID");
        assertEquals("HIGH", risk.getLevel());
        assertTrue(risk.getDetail().contains("10.00"));
        assertEquals("HIGH", vo.getRiskLevel());
    }

    @Test
    void 过期仍为生效状态命中高危规则() {
        SubcontractorContractVO contract =
                contract(new BigDecimal("100.00"), new BigDecimal("50.00"));
        contract.setStatus(ContractStatus.ACTIVE);
        contract.setEndDate(TODAY.minusDays(6));
        stubContract(contract);

        ContractRiskHintVO vo = service.riskHints(CONTRACT_ID);

        ContractRiskVO risk = findRisk(vo.getRisks(), "EXPIRED_ACTIVE");
        assertEquals("HIGH", risk.getLevel());
        assertTrue(risk.getDetail().contains("6"));
        assertNull(riskOrNull(vo.getRisks(), "HIGH_PAYMENT_RATIO"));
    }

    @Test
    void 三十天内到期命中中危规则() {
        SubcontractorContractVO contract =
                contract(new BigDecimal("100.00"), new BigDecimal("50.00"));
        contract.setStatus(ContractStatus.ACTIVE);
        contract.setEndDate(TODAY.plusDays(13));
        stubContract(contract);

        ContractRiskHintVO vo = service.riskHints(CONTRACT_ID);

        ContractRiskVO risk = findRisk(vo.getRisks(), "ENDING_SOON");
        assertEquals("MEDIUM", risk.getLevel());
        assertTrue(risk.getDetail().contains("13"));
        assertEquals("MEDIUM", vo.getRiskLevel());
    }

    @Test
    void 缺少截止日期命中中危规则() {
        SubcontractorContractVO contract =
                contract(new BigDecimal("100.00"), new BigDecimal("50.00"));
        contract.setStatus(ContractStatus.ACTIVE);
        contract.setEndDate(null);
        stubContract(contract);

        ContractRiskHintVO vo = service.riskHints(CONTRACT_ID);

        ContractRiskVO risk = findRisk(vo.getRisks(), "MISSING_END_DATE");
        assertEquals("MEDIUM", risk.getLevel());
        assertEquals("未填写", findPoint(vo.getClausePoints(), "截止日期").getValue());
    }

    @Test
    void 缺少附件命中中危规则() {
        SubcontractorContractVO contract =
                contract(new BigDecimal("100.00"), new BigDecimal("50.00"));
        contract.setFilePath(null);
        contract.setFileName("  ");
        stubContract(contract);

        ContractRiskHintVO vo = service.riskHints(CONTRACT_ID);

        ContractRiskVO risk = findRisk(vo.getRisks(), "MISSING_ATTACHMENT");
        assertEquals("MEDIUM", risk.getLevel());
        assertEquals("未归档", findPoint(vo.getClausePoints(), "附件归档").getValue());
    }

    @Test
    void 付款比例达到九十命中中危规则() {
        SubcontractorContractVO contract =
                contract(new BigDecimal("100.00"), new BigDecimal("90.00"));
        contract.setStatus(ContractStatus.ACTIVE);
        contract.setEndDate(TODAY.plusDays(365));
        stubContract(contract);

        ContractRiskHintVO vo = service.riskHints(CONTRACT_ID);

        ContractRiskVO risk = findRisk(vo.getRisks(), "HIGH_PAYMENT_RATIO");
        assertEquals("MEDIUM", risk.getLevel());
        assertEquals(new BigDecimal("90.00"), vo.getPaidRatio());
        assertNull(riskOrNull(vo.getRisks(), "EXPIRED_ACTIVE"));
    }

    @Test
    void 合同金额为零命中低危规则() {
        SubcontractorContractVO contract = contract(BigDecimal.ZERO, BigDecimal.ZERO);
        stubContract(contract);

        ContractRiskHintVO vo = service.riskHints(CONTRACT_ID);

        ContractRiskVO risk = findRisk(vo.getRisks(), "ZERO_AMOUNT");
        assertEquals("LOW", risk.getLevel());
        assertEquals(new BigDecimal("0.00"), vo.getPaidRatio());
        assertEquals("LOW", vo.getRiskLevel());
    }

    @Test
    void 缺少备注命中低危规则() {
        SubcontractorContractVO contract =
                contract(new BigDecimal("100.00"), new BigDecimal("50.00"));
        contract.setDescription("   ");
        stubContract(contract);

        ContractRiskHintVO vo = service.riskHints(CONTRACT_ID);

        ContractRiskVO risk = findRisk(vo.getRisks(), "MISSING_DESCRIPTION");
        assertEquals("LOW", risk.getLevel());
    }

    @Test
    void AI关闭时不出复述且aiEnabled为假() {
        SubcontractorContractVO contract =
                contract(new BigDecimal("100.00"), new BigDecimal("50.00"));
        stubContract(contract);
        when(aiChatClient.isEnabled()).thenReturn(false);

        ContractRiskHintVO vo = service.riskHints(CONTRACT_ID);

        assertNull(vo.getAiNote());
        assertFalse(vo.isAiEnabled());
        verify(aiChatClient).isEnabled();
        verifyNoMoreInteractions(aiChatClient);
    }

    @Test
    void AI复述出现事实外数字时被丢弃() {
        SubcontractorContractVO contract =
                contract(new BigDecimal("100.00"), new BigDecimal("50.00"));
        contract.setStatus(ContractStatus.DRAFT);
        contract.setEndDate(TODAY.plusDays(365));
        stubContract(contract);
        when(aiChatClient.isEnabled()).thenReturn(true);
        properties.getContractRisk().setAiNoteEnabled(true);
        OllamaChatResult result = new OllamaChatResult();
        result.setContent("合同金额 100.00 元，已付 50.00 元，另有 8888 元需关注。请核实。");
        when(aiChatClient.chatStream(anyList(), isNull())).thenReturn(result);

        ContractRiskHintVO vo = service.riskHints(CONTRACT_ID);

        assertTrue(vo.isAiEnabled());
        assertNull(vo.getAiNote());
    }

    @Test
    void 无风险时风险级别为NONE() {
        SubcontractorContractVO contract =
                contract(new BigDecimal("100.00"), new BigDecimal("50.00"));
        contract.setStatus(ContractStatus.DRAFT);
        contract.setEndDate(TODAY.plusDays(365));
        stubContract(contract);

        ContractRiskHintVO vo = service.riskHints(CONTRACT_ID);

        assertTrue(vo.getRisks().isEmpty());
        assertEquals("NONE", vo.getRiskLevel());
        assertEquals("AI 建议，请核实；仅提示，不修改合同", vo.getDisclaimer());
    }

    /** 构造一份无风险基线合同（附件、备注、有效期均正常）。 */
    private static SubcontractorContractVO contract(BigDecimal amount, BigDecimal paidAmount) {
        SubcontractorContractVO contract = new SubcontractorContractVO();
        contract.setId(CONTRACT_ID);
        contract.setSubcontractorId(2L);
        contract.setContractNumber("HT-2026-001");
        contract.setContractName("某项目分包合同");
        contract.setContractAmount(amount);
        contract.setPaidAmount(paidAmount);
        contract.setStartDate(TODAY.minusDays(300));
        contract.setEndDate(TODAY.plusDays(365));
        contract.setStatus(ContractStatus.ACTIVE);
        contract.setDescription("合同要点说明");
        contract.setFilePath("/contracts/HT-2026-001.pdf");
        contract.setFileName("HT-2026-001.pdf");
        return contract;
    }

    private void stubContract(SubcontractorContractVO contract) {
        when(subcontractorContractService.detail(CONTRACT_ID)).thenReturn(contract);
    }

    private static ContractRiskVO findRisk(List<ContractRiskVO> risks, String code) {
        ContractRiskVO risk = riskOrNull(risks, code);
        if (risk == null) {
            throw new AssertionError("未命中风险：" + code);
        }
        return risk;
    }

    private static ContractRiskVO riskOrNull(List<ContractRiskVO> risks, String code) {
        return risks.stream().filter(risk -> code.equals(risk.getCode())).findFirst().orElse(null);
    }

    private static ContractClausePointVO findPoint(
            List<ContractClausePointVO> points, String label) {
        return points.stream()
                .filter(point -> label.equals(point.getLabel()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("缺少要点：" + label));
    }
}
