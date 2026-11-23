package com.ceams.module.hr.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ceams.common.crypto.SensitiveTypeHandler;
import com.ceams.common.response.PageResult;
import com.ceams.module.bpm.service.BpmInstanceService;
import com.ceams.module.hr.converter.HrEmployeeChangeConverter;
import com.ceams.module.hr.converter.HrEmployeeConverter;
import com.ceams.module.hr.entity.HrEmployee;
import com.ceams.module.hr.entity.HrJobLevel;
import com.ceams.module.hr.enums.EmployeeStatus;
import com.ceams.module.hr.mapper.HrEmployeeChangeMapper;
import com.ceams.module.hr.mapper.HrEmployeeMapper;
import com.ceams.module.hr.mapper.HrJobLevelMapper;
import com.ceams.module.hr.mapper.HrPositionMapper;
import com.ceams.module.hr.vo.EmployeeTreeVO;
import com.ceams.module.hr.vo.EmployeeVO;
import com.ceams.module.iam.mapper.DeptMapper;
import com.ceams.module.iam.mapper.UserMapper;
import java.util.List;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** 员工组织树与分页回填单元测试：父子层级、孤儿提升、成环保护、批量回填无 N+1。 */
@ExtendWith(MockitoExtension.class)
class HrEmployeeTreeTest {

    @Mock private HrEmployeeMapper employeeMapper;
    @Mock private HrEmployeeChangeMapper changeMapper;
    @Mock private HrJobLevelMapper jobLevelMapper;
    @Mock private HrPositionMapper positionMapper;
    @Mock private BpmInstanceService bpmInstanceService;
    @Mock private DeptMapper deptMapper;
    @Mock private UserMapper userMapper;

    private HrEmployeeServiceImpl employeeService;

    /** MyBatis-Plus Lambda 条件依赖 TableInfo 缓存，纯单元测试需手工初始化员工实体元数据。 */
    @BeforeAll
    static void initTableInfoCache() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        // 敏感字段使用构造注入的 TypeHandler，单元测试无 Spring 容器，注册占位实例即可
        configuration.getTypeHandlerRegistry().register(new SensitiveTypeHandler(null));
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
        TableInfoHelper.initTableInfo(assistant, HrEmployee.class);
    }

    @BeforeEach
    void setUp() {
        employeeService =
                new HrEmployeeServiceImpl(
                        employeeMapper,
                        changeMapper,
                        jobLevelMapper,
                        positionMapper,
                        Mappers.getMapper(HrEmployeeConverter.class),
                        Mappers.getMapper(HrEmployeeChangeConverter.class),
                        bpmInstanceService,
                        deptMapper,
                        userMapper);
    }

    // ------------------------------------------------------------------
    // tree
    // ------------------------------------------------------------------

    @Test
    void treeShouldBuildHierarchyByLeaderId() {
        when(employeeMapper.selectList(any(Wrapper.class)))
                .thenReturn(
                        List.of(
                                employee(1L, null, 10L, EmployeeStatus.ACTIVE, "E01"),
                                employee(2L, 1L, 10L, EmployeeStatus.ACTIVE, "E02"),
                                employee(3L, 2L, 10L, EmployeeStatus.ACTIVE, "E03")));

        List<EmployeeTreeVO> roots = employeeService.tree(null, null);

        // 根节点为无上级的 1 号员工
        assertThat(roots).extracting(EmployeeTreeVO::getId).containsExactly(1L);
        EmployeeTreeVO root = roots.get(0);
        assertThat(root.getChildren()).extracting(EmployeeTreeVO::getId).containsExactly(2L);
        assertThat(root.getChildren().get(0).getChildren())
                .extracting(EmployeeTreeVO::getId)
                .containsExactly(3L);
        assertThat(root.getEmployeeNo()).isEqualTo("E01");
        assertThat(root.getDeptId()).isEqualTo(10L);
    }

    @Test
    void treeShouldPromoteOrphanWhoseLeaderNotInResult() {
        when(employeeMapper.selectList(any(Wrapper.class)))
                .thenReturn(
                        List.of(
                                employee(1L, null, 10L, EmployeeStatus.ACTIVE, "E01"),
                                // 上级 99 不在结果集（例如被部门/状态过滤掉），应提升为根
                                employee(2L, 99L, 10L, EmployeeStatus.ACTIVE, "E02"),
                                employee(3L, 1L, 10L, EmployeeStatus.ACTIVE, "E03")));

        List<EmployeeTreeVO> roots = employeeService.tree(null, null);

        assertThat(roots).extracting(EmployeeTreeVO::getId).containsExactly(1L, 2L);
        assertThat(roots.get(0).getChildren()).extracting(EmployeeTreeVO::getId).containsExactly(3L);
        assertThat(roots.get(1).getChildren()).isEmpty();
    }

    @Test
    void treeShouldApplyDeptAndStatusFilters() {
        when(employeeMapper.selectList(any(Wrapper.class)))
                .thenReturn(List.of(employee(1L, null, 3L, EmployeeStatus.ACTIVE, "E01")));

        List<EmployeeTreeVO> roots = employeeService.tree(3L, EmployeeStatus.ACTIVE);

        assertThat(roots).extracting(EmployeeTreeVO::getId).containsExactly(1L);
        ArgumentCaptor<Wrapper> captor = ArgumentCaptor.forClass(Wrapper.class);
        verify(employeeMapper).selectList(captor.capture());
        LambdaQueryWrapper<HrEmployee> wrapper = (LambdaQueryWrapper<HrEmployee>) captor.getValue();
        // deptId/status 条件下推到 SQL
        assertThat(wrapper.getSqlSegment()).contains("dept_id =").contains("status =");
        assertThat(wrapper.getParamNameValuePairs()).containsValue(3L);
    }

    @Test
    void treeShouldReturnEmptyWhenNoEmployees() {
        when(employeeMapper.selectList(any(Wrapper.class))).thenReturn(List.of());

        assertThat(employeeService.tree(null, null)).isEmpty();
    }

    @Test
    void treeShouldBreakCycleWithoutInfiniteLoop() {
        // 1 <-> 2 互为上级：按查询顺序首个节点作为根，避免无限递归
        when(employeeMapper.selectList(any(Wrapper.class)))
                .thenReturn(
                        List.of(
                                employee(1L, 2L, 10L, EmployeeStatus.ACTIVE, "E01"),
                                employee(2L, 1L, 10L, EmployeeStatus.ACTIVE, "E02")));

        List<EmployeeTreeVO> roots = employeeService.tree(null, null);

        assertThat(roots).extracting(EmployeeTreeVO::getId).containsExactly(1L);
        assertThat(roots.get(0).getChildren()).extracting(EmployeeTreeVO::getId).containsExactly(2L);
    }

    // ------------------------------------------------------------------
    // page：批量回填
    // ------------------------------------------------------------------

    @Test
    void pageShouldFillLeaderAndLevelNamesInBatchQueries() {
        HrEmployee first = employee(1L, 5L, 10L, EmployeeStatus.ACTIVE, "E01");
        first.setLevelId(9L);
        HrEmployee second = employee(2L, 5L, 10L, EmployeeStatus.ACTIVE, "E02");
        second.setLevelId(9L);
        Page<HrEmployee> page = new Page<>(1, 10);
        page.setRecords(List.of(first, second));
        page.setTotal(2);
        when(employeeMapper.selectPage(any(), any())).thenReturn(page);
        when(employeeMapper.selectBatchIds(any()))
                .thenReturn(List.of(employee(5L, null, 10L, EmployeeStatus.ACTIVE, "E05")));
        when(jobLevelMapper.selectBatchIds(any())).thenReturn(List.of(jobLevel(9L, "P6", "资深工程师")));

        PageResult<EmployeeVO> result = employeeService.page(1, 10, null, null, null);

        assertThat(result.getTotal()).isEqualTo(2);
        EmployeeVO vo = result.getRecords().get(0);
        assertThat(vo.getLeaderName()).isEqualTo("员工5");
        assertThat(vo.getLeaderCode()).isEqualTo("E05");
        assertThat(vo.getLevelName()).isEqualTo("资深工程师");
        assertThat(vo.getLevelCode()).isEqualTo("P6");

        // 上级与职级各只做一次批量查询，禁止逐行查询（N+1）
        verify(employeeMapper, times(1)).selectBatchIds(any());
        verify(jobLevelMapper, times(1)).selectBatchIds(any());
        verify(employeeMapper, never()).selectById(anyLong());
    }

    @Test
    void pageShouldSkipBatchQueriesWhenNoLeaderOrLevel() {
        Page<HrEmployee> page = new Page<>(1, 10);
        page.setRecords(List.of(employee(1L, null, 10L, EmployeeStatus.ACTIVE, "E01")));
        page.setTotal(1);
        when(employeeMapper.selectPage(any(), any())).thenReturn(page);

        PageResult<EmployeeVO> result = employeeService.page(1, 10, null, null, null);

        assertThat(result.getRecords()).hasSize(1);
        assertThat(result.getRecords().get(0).getLeaderName()).isNull();
        assertThat(result.getRecords().get(0).getLevelName()).isNull();
        verify(employeeMapper, never()).selectBatchIds(any());
        verify(jobLevelMapper, never()).selectBatchIds(any());
    }

    // ------------------------------------------------------------------
    // 夹具
    // ------------------------------------------------------------------

    private HrEmployee employee(
            Long id, Long leaderId, Long deptId, EmployeeStatus status, String employeeNo) {
        HrEmployee employee = new HrEmployee();
        employee.setId(id);
        employee.setLeaderId(leaderId);
        employee.setDeptId(deptId);
        employee.setStatus(status);
        employee.setEmployeeNo(employeeNo);
        employee.setName("员工" + id);
        return employee;
    }

    private HrJobLevel jobLevel(Long id, String code, String name) {
        HrJobLevel jobLevel = new HrJobLevel();
        jobLevel.setId(id);
        jobLevel.setCode(code);
        jobLevel.setName(name);
        jobLevel.setStatus("ENABLED");
        return jobLevel;
    }
}
