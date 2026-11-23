package com.ceams.module.project.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.ceams.common.enums.ContractApprovalStatus;
import com.ceams.common.exception.BusinessException;
import com.ceams.common.exception.ErrorCode;
import com.ceams.module.bpm.dto.InstanceStartRequest;
import com.ceams.module.bpm.service.BpmInstanceService;
import com.ceams.module.project.converter.SubcontractorContractConverter;
import com.ceams.module.project.entity.Project;
import com.ceams.module.project.entity.Subcontractor;
import com.ceams.module.project.entity.SubcontractorContract;
import com.ceams.module.project.mapper.BudgetMapper;
import com.ceams.module.project.mapper.ProjectMapper;
import com.ceams.module.project.mapper.SettlementProofMapper;
import com.ceams.module.project.mapper.SubcontractorContractMapper;
import com.ceams.module.project.mapper.SubcontractorMapper;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** 分包合同发起审批单元测试：模板参数、金额变量、重复提交拦截。 */
@ExtendWith(MockitoExtension.class)
class SubcontractorContractServiceImplTest {

    private static final Long CONTRACT_ID = 7301L;
    private static final Long SUBCONTRACTOR_ID = 21L;
    private static final Long PROJECT_ID = 31L;

    @Mock private SubcontractorContractMapper subcontractorContractMapper;
    @Mock private SubcontractorMapper subcontractorMapper;
    @Mock private ProjectMapper projectMapper;
    @Mock private BudgetMapper budgetMapper;
    @Mock private SettlementProofMapper settlementProofMapper;
    @Mock private SubcontractorContractConverter subcontractorContractConverter;
    @Mock private BpmInstanceService bpmInstanceService;

    private SubcontractorContractServiceImpl service;

    @BeforeEach
    void setUp() {
        service =
                new SubcontractorContractServiceImpl(
                        subcontractorContractMapper,
                        subcontractorMapper,
                        projectMapper,
                        budgetMapper,
                        settlementProofMapper,
                        subcontractorContractConverter,
                        bpmInstanceService);
    }

    @Test
    void submitShouldStartFlowAndWritePending() {
        when(subcontractorContractMapper.selectById(CONTRACT_ID))
                .thenReturn(contract(ContractApprovalStatus.NONE));
        Subcontractor subcontractor = new Subcontractor();
        subcontractor.setId(SUBCONTRACTOR_ID);
        subcontractor.setName("某分包商");
        when(subcontractorMapper.selectById(SUBCONTRACTOR_ID)).thenReturn(subcontractor);
        Project project = new Project();
        project.setId(PROJECT_ID);
        project.setProjectName("某项目");
        when(projectMapper.selectById(PROJECT_ID)).thenReturn(project);
        when(bpmInstanceService.start(any(InstanceStartRequest.class))).thenReturn("880");
        when(subcontractorContractMapper.update(
                        any(SubcontractorContract.class), any(Wrapper.class)))
                .thenReturn(1);

        String instanceId = service.submitApproval(CONTRACT_ID);

        assertThat(instanceId).isEqualTo("880");
        ArgumentCaptor<InstanceStartRequest> requestCaptor =
                ArgumentCaptor.forClass(InstanceStartRequest.class);
        verify(bpmInstanceService).start(requestCaptor.capture());
        InstanceStartRequest request = requestCaptor.getValue();
        assertThat(request.getTemplateCode()).isEqualTo("subcontractor-contract-approval");
        assertThat(request.getBizType()).isEqualTo("SUBCONTRACT_CONTRACT");
        assertThat(request.getVariables())
                .containsEntry("contractNumber", "FB-2026-001")
                .containsEntry("subcontractorName", "某分包商")
                .containsEntry("projectName", "某项目")
                .containsEntry("contractAmount", "100000.00");

        ArgumentCaptor<SubcontractorContract> patchCaptor =
                ArgumentCaptor.forClass(SubcontractorContract.class);
        verify(subcontractorContractMapper).update(patchCaptor.capture(), any(Wrapper.class));
        assertThat(patchCaptor.getValue().getApprovalStatus())
                .isEqualTo(ContractApprovalStatus.PENDING);
        assertThat(patchCaptor.getValue().getBpmInstanceId()).isEqualTo(880L);
    }

    @Test
    void pendingContractShouldRejectResubmit() {
        when(subcontractorContractMapper.selectById(CONTRACT_ID))
                .thenReturn(contract(ContractApprovalStatus.PENDING));

        assertThatThrownBy(() -> service.submitApproval(CONTRACT_ID))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        ex -> {
                            assertThat(ex.getErrorCode())
                                    .isEqualTo(ErrorCode.BIZ_STATE_CONFLICT);
                            assertThat(ex).hasMessageContaining("该合同已提交审批或已通过");
                        });
        verifyNoInteractions(bpmInstanceService);
    }

    @Test
    void missingContractShouldReject() {
        when(subcontractorContractMapper.selectById(CONTRACT_ID)).thenReturn(null);

        assertThatThrownBy(() -> service.submitApproval(CONTRACT_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.BIZ_NOT_FOUND);
        verifyNoInteractions(bpmInstanceService);
    }

    private SubcontractorContract contract(ContractApprovalStatus approvalStatus) {
        SubcontractorContract contract = new SubcontractorContract();
        contract.setId(CONTRACT_ID);
        contract.setSubcontractorId(SUBCONTRACTOR_ID);
        contract.setProjectId(PROJECT_ID);
        contract.setContractNumber("FB-2026-001");
        contract.setContractName("某分包合同");
        contract.setContractAmount(new BigDecimal("100000.00"));
        contract.setStartDate(LocalDateTime.of(2026, 1, 1, 0, 0));
        contract.setEndDate(LocalDateTime.of(2027, 1, 1, 0, 0));
        contract.setApprovalStatus(approvalStatus);
        return contract;
    }
}
