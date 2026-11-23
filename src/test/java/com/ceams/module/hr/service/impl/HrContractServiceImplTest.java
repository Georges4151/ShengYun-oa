package com.ceams.module.hr.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.ceams.common.exception.BusinessException;
import com.ceams.common.exception.ErrorCode;
import com.ceams.module.bpm.service.BpmInstanceService;
import com.ceams.module.hr.converter.HrContractConverter;
import com.ceams.module.hr.entity.HrContract;
import com.ceams.module.hr.entity.HrEmployee;
import com.ceams.module.hr.enums.ContractStatus;
import com.ceams.module.hr.enums.ContractType;
import com.ceams.module.hr.mapper.HrContractMapper;
import com.ceams.module.hr.mapper.HrEmployeeMapper;
import com.ceams.module.hr.dto.ContractCreateRequest;
import com.ceams.module.hr.vo.ContractVO;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** 合同服务单元测试：到期查询过滤条件、编号唯一与员工存在性校验。 */
@ExtendWith(MockitoExtension.class)
class HrContractServiceImplTest {

    @Mock private HrContractMapper contractMapper;
    @Mock private HrEmployeeMapper employeeMapper;
    @Mock private BpmInstanceService bpmInstanceService;

    private HrContractServiceImpl contractService;

    @BeforeEach
    void setUp() {
        contractService =
                new HrContractServiceImpl(
                        contractMapper,
                        employeeMapper,
                        Mappers.getMapper(HrContractConverter.class),
                        bpmInstanceService);
    }

    @Test
    void expiringShouldQueryActiveContractsWithinWindow() {
        LocalDate today = LocalDate.now();
        HrContract contract = new HrContract();
        contract.setId(1L);
        contract.setEmployeeId(11L);
        contract.setContractNo("HT-2026-001");
        contract.setContractType(ContractType.FIXED);
        contract.setStatus(ContractStatus.ACTIVE);
        contract.setEndDate(today.plusDays(10));
        when(contractMapper.selectList(any(Wrapper.class))).thenReturn(List.of(contract));

        List<ContractVO> result = contractService.expiring(30);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getContractNo()).isEqualTo("HT-2026-001");
        assertThat(result.get(0).getStatus()).isEqualTo(ContractStatus.ACTIVE);
        assertThat(result.get(0).getEndDate()).isEqualTo(today.plusDays(10));
        verify(contractMapper).selectList(any(Wrapper.class));
    }

    @Test
    void createShouldRejectDuplicateContractNo() {
        HrEmployee employee = new HrEmployee();
        employee.setId(11L);
        when(employeeMapper.selectById(11L)).thenReturn(employee);
        when(contractMapper.selectCount(any(Wrapper.class))).thenReturn(1L);

        ContractCreateRequest request = new ContractCreateRequest();
        request.setEmployeeId(11L);
        request.setContractNo("HT-2026-002");
        request.setContractType(ContractType.FIXED);

        assertThatThrownBy(() -> contractService.create(request))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.BIZ_DUPLICATE_CODE);
        verify(contractMapper, never()).insert(any(HrContract.class));
    }

    @Test
    void createShouldRejectUnknownEmployee() {
        ContractCreateRequest request = new ContractCreateRequest();
        request.setEmployeeId(99L);
        request.setContractNo("HT-2026-003");
        request.setContractType(ContractType.FIXED);

        assertThatThrownBy(() -> contractService.create(request))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.BIZ_NOT_FOUND);
    }
}
