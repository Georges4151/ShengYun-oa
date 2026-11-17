package com.ceams.module.hr.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ceams.module.bpm.service.BpmInstanceService;
import com.ceams.module.hr.converter.HrEmployeeChangeConverter;
import com.ceams.module.hr.converter.HrEmployeeConverter;
import com.ceams.module.hr.entity.HrEmployee;
import com.ceams.module.hr.enums.EmployeeStatus;
import com.ceams.module.hr.mapper.HrEmployeeChangeMapper;
import com.ceams.module.hr.mapper.HrEmployeeMapper;
import com.ceams.module.hr.vo.HeadcountDeptVO;
import com.ceams.module.iam.entity.SysDept;
import com.ceams.module.iam.mapper.DeptMapper;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** 部门人数统计单元测试：部门过滤、状态计数、空结果。 */
@ExtendWith(MockitoExtension.class)
class HrEmployeeHeadcountTest {

    @Mock private HrEmployeeMapper employeeMapper;
    @Mock private HrEmployeeChangeMapper changeMapper;
    @Mock private BpmInstanceService bpmInstanceService;
    @Mock private DeptMapper deptMapper;

    private HrEmployeeServiceImpl employeeService;

    @BeforeEach
    void setUp() {
        employeeService =
                new HrEmployeeServiceImpl(
                        employeeMapper,
                        changeMapper,
                        Mappers.getMapper(HrEmployeeConverter.class),
                        Mappers.getMapper(HrEmployeeChangeConverter.class),
                        bpmInstanceService,
                        deptMapper);
    }

    @Test
    void shouldGroupByDeptAndCountEachStatus() {
        when(employeeMapper.selectList(any()))
                .thenReturn(
                        List.of(
                                employee(11L, 2L, EmployeeStatus.PROBATION),
                                employee(12L, 2L, EmployeeStatus.ACTIVE),
                                employee(13L, 2L, EmployeeStatus.LEAVE),
                                employee(14L, 2L, EmployeeStatus.TERMINATED),
                                employee(15L, 1L, EmployeeStatus.ACTIVE),
                                // 未归属部门应被忽略
                                employee(16L, null, EmployeeStatus.ACTIVE)));
        when(deptMapper.selectBatchIds(any()))
                .thenReturn(List.of(dept(1L, "行政部"), dept(2L, "研发部")));

        List<HeadcountDeptVO> result = employeeService.headcountByDept(null);

        assertThat(result).hasSize(2);
        // 结果按 deptId 升序
        assertThat(result.get(0).getDeptId()).isEqualTo(1L);
        assertThat(result.get(0).getDeptName()).isEqualTo("行政部");
        assertThat(result.get(0).getTotalCount()).isEqualTo(1L);
        assertThat(result.get(0).getActiveCount()).isEqualTo(1L);

        HeadcountDeptVO dept2 = result.get(1);
        assertThat(dept2.getDeptId()).isEqualTo(2L);
        assertThat(dept2.getDeptName()).isEqualTo("研发部");
        assertThat(dept2.getTotalCount()).isEqualTo(4L);
        assertThat(dept2.getProbationCount()).isEqualTo(1L);
        assertThat(dept2.getActiveCount()).isEqualTo(1L);
        assertThat(dept2.getTerminatedCount()).isEqualTo(1L);
    }

    @Test
    void shouldFallbackToEmptyNameWhenDeptMissing() {
        when(employeeMapper.selectList(any()))
                .thenReturn(List.of(employee(21L, 9L, EmployeeStatus.ACTIVE)));
        when(deptMapper.selectBatchIds(any())).thenReturn(List.of());

        List<HeadcountDeptVO> result = employeeService.headcountByDept(null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getDeptName()).isEmpty();
    }

    @Test
    void shouldFilterByDeptId() {
        when(employeeMapper.selectList(any()))
                .thenReturn(List.of(employee(31L, 5L, EmployeeStatus.PROBATION)));
        when(deptMapper.selectBatchIds(any())).thenReturn(List.of(dept(5L, "财务部")));

        List<HeadcountDeptVO> result = employeeService.headcountByDept(5L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getDeptId()).isEqualTo(5L);
        assertThat(result.get(0).getProbationCount()).isEqualTo(1L);
    }

    @Test
    void shouldReturnEmptyWhenNoEmployees() {
        when(employeeMapper.selectList(any())).thenReturn(List.of());

        List<HeadcountDeptVO> result = employeeService.headcountByDept(null);

        assertThat(result).isEmpty();
        verify(deptMapper, never()).selectBatchIds(any());
    }

    private HrEmployee employee(Long id, Long deptId, EmployeeStatus status) {
        HrEmployee employee = new HrEmployee();
        employee.setId(id);
        employee.setDeptId(deptId);
        employee.setStatus(status);
        return employee;
    }

    private SysDept dept(Long id, String name) {
        SysDept dept = new SysDept();
        dept.setId(id);
        dept.setName(name);
        return dept;
    }
}
