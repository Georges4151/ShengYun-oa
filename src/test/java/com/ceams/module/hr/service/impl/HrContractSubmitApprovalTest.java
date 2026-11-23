package com.ceams.module.hr.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.ceams.common.enums.ContractApprovalStatus;
import com.ceams.common.exception.BusinessException;
import com.ceams.common.exception.ErrorCode;
import com.ceams.module.bpm.dto.InstanceStartRequest;
import com.ceams.module.bpm.service.BpmInstanceService;
import com.ceams.module.hr.converter.HrContractConverter;
import com.ceams.module.hr.entity.HrContract;
import com.ceams.module.hr.entity.HrEmployee;
import com.ceams.module.hr.enums.ContractType;
import com.ceams.module.hr.mapper.HrContractMapper;
import com.ceams.module.hr.mapper.HrEmployeeMapper;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** 劳动合同发起审批单元测试：模板参数、状态写回、重复提交拦截。 */
@ExtendWith(MockitoExtension.class)
class HrContractSubmitApprovalTest {

    private static final Long CONTRACT_ID = 7201L;
    private static final Long EMPLOYEE_ID = 11L;

    @Mock private HrContractMapper contractMapper;
    @Mock private HrEmployeeMapper employeeMapper;
    @Mock private HrContractConverter contractConverter;
    @Mock private BpmInstanceService bpmInstanceService;

    private HrContractServiceImpl service;

    @BeforeEach
    void setUp() {
        service =
                new HrContractServiceImpl(
                        contractMapper, employeeMapper, contractConverter, bpmInstanceService);
    }

    @Test
    void submitShouldStartFlowAndWritePending() {
        when(contractMapper.selectById(CONTRACT_ID))
                .thenReturn(contract(ContractApprovalStatus.NONE));
        HrEmployee employee = new HrEmployee();
        employee.setId(EMPLOYEE_ID);
        employee.setName("张三");
        when(employeeMapper.selectById(EMPLOYEE_ID)).thenReturn(employee);
        when(bpmInstanceService.start(any(InstanceStartRequest.class))).thenReturn("777");
        when(contractMapper.update(any(HrContract.class), any(Wrapper.class))).thenReturn(1);

        String instanceId = service.submitApproval(CONTRACT_ID);

        assertThat(instanceId).isEqualTo("777");
        ArgumentCaptor<InstanceStartRequest> requestCaptor =
                ArgumentCaptor.forClass(InstanceStartRequest.class);
        verify(bpmInstanceService).start(requestCaptor.capture());
        InstanceStartRequest request = requestCaptor.getValue();
        assertThat(request.getTemplateCode()).isEqualTo("hr-contract-approval");
        assertThat(request.getBizType()).isEqualTo("HR_CONTRACT");
        assertThat(request.getBizId()).isEqualTo(CONTRACT_ID);
        assertThat(request.getVariables())
                .containsEntry("contractNo", "HT-2026-001")
                .containsEntry("employeeName", "张三");

        ArgumentCaptor<HrContract> patchCaptor = ArgumentCaptor.forClass(HrContract.class);
        verify(contractMapper).update(patchCaptor.capture(), any(Wrapper.class));
        assertThat(patchCaptor.getValue().getApprovalStatus())
                .isEqualTo(ContractApprovalStatus.PENDING);
        assertThat(patchCaptor.getValue().getBpmInstanceId()).isEqualTo(777L);
    }

    @Test
    void rejectedContractCanSubmitAgain() {
        when(contractMapper.selectById(CONTRACT_ID))
                .thenReturn(contract(ContractApprovalStatus.REJECTED));
        when(employeeMapper.selectById(EMPLOYEE_ID)).thenReturn(new HrEmployee());
        when(bpmInstanceService.start(any(InstanceStartRequest.class))).thenReturn("778");
        when(contractMapper.update(any(HrContract.class), any(Wrapper.class))).thenReturn(1);

        assertThat(service.submitApproval(CONTRACT_ID)).isEqualTo("778");
    }

    @Test
    void pendingContractShouldRejectResubmit() {
        when(contractMapper.selectById(CONTRACT_ID))
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
    void approvedContractShouldRejectResubmit() {
        when(contractMapper.selectById(CONTRACT_ID))
                .thenReturn(contract(ContractApprovalStatus.APPROVED));

        assertThatThrownBy(() -> service.submitApproval(CONTRACT_ID))
                .isInstanceOf(BusinessException.class);
        verifyNoInteractions(bpmInstanceService);
    }

    @Test
    void concurrentStatusChangeShouldRejectAndNotKeepFlow() {
        when(contractMapper.selectById(CONTRACT_ID))
                .thenReturn(contract(ContractApprovalStatus.NONE));
        when(employeeMapper.selectById(EMPLOYEE_ID)).thenReturn(new HrEmployee());
        when(bpmInstanceService.start(any(InstanceStartRequest.class))).thenReturn("779");
        when(contractMapper.update(any(HrContract.class), any(Wrapper.class))).thenReturn(0);

        assertThatThrownBy(() -> service.submitApproval(CONTRACT_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.BIZ_STATE_CONFLICT);
    }

    @Test
    void missingContractShouldReject() {
        when(contractMapper.selectById(CONTRACT_ID)).thenReturn(null);

        assertThatThrownBy(() -> service.submitApproval(CONTRACT_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.BIZ_NOT_FOUND);
        verify(bpmInstanceService, never()).start(any());
    }

    private HrContract contract(ContractApprovalStatus approvalStatus) {
        HrContract contract = new HrContract();
        contract.setId(CONTRACT_ID);
        contract.setEmployeeId(EMPLOYEE_ID);
        contract.setContractNo("HT-2026-001");
        contract.setContractType(ContractType.FIXED);
        contract.setStartDate(LocalDate.of(2026, 1, 1));
        contract.setEndDate(LocalDate.of(2028, 12, 31));
        contract.setApprovalStatus(approvalStatus);
        return contract;
    }
}
