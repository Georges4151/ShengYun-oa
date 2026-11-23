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
import com.ceams.module.hr.entity.HrPosition;
import com.ceams.module.hr.enums.EmployeeStatus;
import com.ceams.module.hr.mapper.HrEmployeeChangeMapper;
import com.ceams.module.hr.mapper.HrEmployeeMapper;
import com.ceams.module.hr.mapper.HrJobLevelMapper;
import com.ceams.module.hr.mapper.HrPositionMapper;
import com.ceams.module.hr.vo.EmployeeVO;
import com.ceams.module.hr.vo.OrgTreeNodeVO;
import com.ceams.module.iam.entity.SysDept;
import com.ceams.module.iam.entity.SysUser;
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

/** 人事组织合并树单元测试：混合组装、排序、过滤、未分配部门、环截断与批量回填。 */
@ExtendWith(MockitoExtension.class)
class HrEmployeeOrgTreeTest {

    @Mock private HrEmployeeMapper employeeMapper;
    @Mock private HrEmployeeChangeMapper changeMapper;
    @Mock private HrJobLevelMapper jobLevelMapper;
    @Mock private HrPositionMapper positionMapper;
    @Mock private BpmInstanceService bpmInstanceService;
    @Mock private DeptMapper deptMapper;
    @Mock private UserMapper userMapper;

    private HrEmployeeServiceImpl employeeService;

    /** 分页改造使用 Lambda 列条件，纯单元测试需手工初始化员工实体元数据。 */
    @BeforeAll
    static void initTableInfoCache() {
        MybatisConfiguration configuration = new MybatisConfiguration();
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
    // 基础组装
    // ------------------------------------------------------------------

    @Test
    void shouldAssembleDepartmentsAndEmployeesTogether() {
        SysDept head = dept(1L, null, "总公司", 0);
        head.setLeaderId(900L);
        stubData(
                List.of(head, dept(2L, 1L, "研发部", 0)),
                List.of(
                        employeeWithUser(
                                10L, 1L, null, null, EmployeeStatus.ACTIVE, "E10", "张三", 900L),
                        employee(11L, 2L, 10L, null, EmployeeStatus.ACTIVE, "E11", "李四"),
                        employee(12L, 2L, 11L, null, EmployeeStatus.ACTIVE, "E12", "王五")));

        List<OrgTreeNodeVO> roots = employeeService.orgTree(null, null, null, false);

        assertThat(ids(roots)).containsExactly(1L);
        OrgTreeNodeVO headNode = roots.get(0);
        assertThat(headNode.getNodeType()).isEqualTo(OrgTreeNodeVO.NodeType.DEPT);
        assertThat(headNode.getLeaderName()).isEqualTo("张三");
        // 子部门优先，员工根节点排在其后
        assertThat(ids(headNode.getChildren())).containsExactly(2L, 10L);
        OrgTreeNodeVO devNode = headNode.getChildren().get(0);
        assertThat(devNode.getName()).isEqualTo("研发部");
        // 上级不在本部门的李四提升为部门内员工根，王五挂在李四下
        assertThat(ids(devNode.getChildren())).containsExactly(11L);
        assertThat(ids(devNode.getChildren().get(0).getChildren())).containsExactly(12L);
        assertThat(devNode.getChildren().get(0).getChildren().get(0).getChildren()).isEmpty();
    }

    @Test
    void shouldPutSubDepartmentsFirstAndSortEmployeesByLevelRank() {
        stubData(
                List.of(dept(2L, 1L, "B组", 2), dept(1L, null, "总部", 0), dept(3L, 1L, "A组", 1)),
                List.of(
                        employee(10L, 1L, null, 11L, EmployeeStatus.ACTIVE, "E10", "张三"),
                        employee(11L, 1L, null, 12L, EmployeeStatus.ACTIVE, "E11", "李四"),
                        employee(12L, 1L, null, null, EmployeeStatus.ACTIVE, "E12", "王五")));
        when(jobLevelMapper.selectBatchIds(any()))
                .thenReturn(List.of(level(11L, "P5", "P5", 5), level(12L, "P6", "P6", 9)));

        List<OrgTreeNodeVO> roots = employeeService.orgTree(null, null, null, false);

        // 子部门按 sort_order 升序；员工按职级 rank_no 降序，无职级排最后
        assertThat(ids(roots.get(0).getChildren())).containsExactly(3L, 2L, 11L, 10L, 12L);
    }

    @Test
    void shouldHideTerminatedEmployeesByDefaultAndShowWhenIncludeResigned() {
        stubData(
                List.of(dept(1L, null, "总部", 0), dept(2L, 1L, "空部门", 1)),
                List.of(employee(10L, 1L, null, null, EmployeeStatus.TERMINATED, "E10", "张三")));

        // 默认隐藏已离职员工：空部门仍保留，离职员工节点被过滤
        List<OrgTreeNodeVO> roots = employeeService.orgTree(null, null, null, false);

        assertThat(ids(roots)).containsExactly(1L);
        OrgTreeNodeVO head = roots.get(0);
        assertThat(ids(head.getChildren())).containsExactly(2L);
        OrgTreeNodeVO emptyDept = head.getChildren().get(0);
        assertThat(emptyDept.getChildren()).isEmpty();
        assertThat(emptyDept.getHeadcount()).isZero();

        // “全部员工”筛选时返回离职员工，由前端置灰
        List<OrgTreeNodeVO> all = employeeService.orgTree(null, null, null, true);
        OrgTreeNodeVO allHead = all.get(0);
        assertThat(ids(allHead.getChildren())).containsExactly(2L, 10L);
        OrgTreeNodeVO terminated = allHead.getChildren().get(1);
        assertThat(terminated.getStatus()).isEqualTo(EmployeeStatus.TERMINATED);
    }

    @Test
    void shouldAppendUnassignedDepartmentNodeLast() {
        stubData(
                List.of(dept(1L, null, "总部", 0)),
                List.of(
                        employee(10L, 1L, null, null, EmployeeStatus.ACTIVE, "E10", "张三"),
                        employee(20L, null, null, null, EmployeeStatus.ACTIVE, "E20", "李四")));

        List<OrgTreeNodeVO> roots = employeeService.orgTree(null, null, null, false);

        assertThat(ids(roots)).containsExactly(1L, "UNASSIGNED");
        OrgTreeNodeVO unassigned = roots.get(1);
        assertThat(unassigned.getNodeType()).isEqualTo(OrgTreeNodeVO.NodeType.UNASSIGNED);
        assertThat(unassigned.getName()).isEqualTo("未分配部门");
        assertThat(ids(unassigned.getChildren())).containsExactly(20L);
    }

    @Test
    void shouldMarkCrossDepartmentLeader() {
        stubData(
                List.of(dept(1L, null, "总公司", 0), dept(2L, 1L, "研发部", 0)),
                List.of(
                        employee(10L, 1L, null, null, EmployeeStatus.ACTIVE, "E10", "张三"),
                        employee(11L, 2L, 10L, null, EmployeeStatus.ACTIVE, "E11", "李四")));

        List<OrgTreeNodeVO> roots = employeeService.orgTree(null, null, null, false);

        OrgTreeNodeVO headEmployee = roots.get(0).getChildren().get(1);
        assertThat(headEmployee.getCrossDeptLeader()).isFalse();
        OrgTreeNodeVO devEmployee = roots.get(0).getChildren().get(0).getChildren().get(0);
        assertThat(devEmployee.getLeaderName()).isEqualTo("张三");
        assertThat(devEmployee.getCrossDeptLeader()).isTrue();
        assertThat(devEmployee.getLeaderDeptName()).isEqualTo("总公司");
    }

    @Test
    void shouldTruncateLeaderCycleWithoutInfiniteRecursion() {
        stubData(
                List.of(dept(1L, null, "总部", 0)),
                List.of(
                        employee(1L, 1L, 2L, null, EmployeeStatus.ACTIVE, "E01", "员工1"),
                        employee(2L, 1L, 1L, null, EmployeeStatus.ACTIVE, "E02", "员工2")));

        List<OrgTreeNodeVO> roots = employeeService.orgTree(null, null, null, false);

        assertThat(ids(roots)).containsExactly(1L);
        OrgTreeNodeVO first = roots.get(0).getChildren().get(0);
        assertThat(first.getId()).isEqualTo(1L);
        assertThat(ids(first.getChildren())).containsExactly(2L);
        // 回边被截断，不会继续嵌套
        assertThat(first.getChildren().get(0).getChildren()).isEmpty();
    }

    // ------------------------------------------------------------------
    // 过滤
    // ------------------------------------------------------------------

    @Test
    void shouldFilterByStatusAndKeepAncestorDepartmentsOnly() {
        stubData(
                List.of(
                        dept(1L, null, "总部", 0),
                        dept(2L, 1L, "研发部", 0),
                        dept(3L, null, "财务部", 1)),
                List.of(
                        employee(10L, 1L, null, null, EmployeeStatus.ACTIVE, "E10", "张三"),
                        employee(20L, 2L, null, null, EmployeeStatus.TERMINATED, "E20", "李四"),
                        employee(30L, 3L, null, null, EmployeeStatus.TERMINATED, "E30", "王五")));

        List<OrgTreeNodeVO> roots = employeeService.orgTree(EmployeeStatus.ACTIVE, null, null, false);

        // 无命中员工的部门（含其子树）不返回
        assertThat(ids(roots)).containsExactly(1L);
        assertThat(ids(roots.get(0).getChildren())).containsExactly(10L);
    }

    @Test
    void shouldKeepMatchedEmployeeWithLeaderChainAndDepartmentChain() {
        stubData(
                List.of(dept(1L, null, "总部", 0), dept(2L, 1L, "研发部", 0)),
                List.of(
                        employee(10L, 1L, null, null, EmployeeStatus.ACTIVE, "E10", "张三"),
                        employee(11L, 2L, 10L, null, EmployeeStatus.ACTIVE, "E11", "李四")));

        List<OrgTreeNodeVO> roots = employeeService.orgTree(null, "李四", null, false);

        assertThat(ids(roots)).containsExactly(1L);
        assertThat(ids(roots.get(0).getChildren())).containsExactly(2L, 10L);
        assertThat(ids(roots.get(0).getChildren().get(0).getChildren())).containsExactly(11L);
    }

    @Test
    void shouldKeepWholeSubtreeWhenKeywordMatchesDepartmentName() {
        stubData(
                List.of(
                        dept(1L, null, "技术中心", 0),
                        dept(2L, 1L, "研发部", 0),
                        dept(3L, null, "财务部", 1)),
                List.of(
                        employee(10L, 1L, null, null, EmployeeStatus.ACTIVE, "E10", "张三"),
                        employee(20L, 2L, null, null, EmployeeStatus.ACTIVE, "E20", "李四"),
                        employee(30L, 3L, null, null, EmployeeStatus.ACTIVE, "E30", "王五")));

        List<OrgTreeNodeVO> roots = employeeService.orgTree(null, "研发", null, false);

        assertThat(ids(roots)).containsExactly(1L);
        assertThat(ids(roots.get(0).getChildren())).containsExactly(2L);
        OrgTreeNodeVO devDept = roots.get(0).getChildren().get(0);
        assertThat(ids(devDept.getChildren())).containsExactly(20L);
    }

    @Test
    void shouldLimitToDepartmentSubtreeWithAncestorChain() {
        stubData(
                List.of(
                        dept(1L, null, "总部", 0),
                        dept(2L, 1L, "研发中心", 0),
                        dept(3L, 2L, "研发一组", 0),
                        dept(4L, null, "财务部", 1)),
                List.of(
                        employee(10L, 3L, null, null, EmployeeStatus.ACTIVE, "E10", "张三"),
                        employee(20L, 4L, null, null, EmployeeStatus.ACTIVE, "E20", "李四")));

        List<OrgTreeNodeVO> roots = employeeService.orgTree(null, null, 3L, false);

        assertThat(ids(roots)).containsExactly(1L);
        OrgTreeNodeVO middle = roots.get(0).getChildren().get(0);
        assertThat(middle.getId()).isEqualTo(2L);
        OrgTreeNodeVO target = middle.getChildren().get(0);
        assertThat(target.getId()).isEqualTo(3L);
        assertThat(ids(target.getChildren())).containsExactly(10L);
        // 兄弟部门与目标子树外的员工不返回
        assertThat(roots.get(0).getChildren()).hasSize(1);
    }

    @Test
    void shouldReturnEmptyTreeWhenFilteredDepartmentMissing() {
        stubData(List.of(dept(1L, null, "总部", 0)), List.of());

        assertThat(employeeService.orgTree(null, null, 99L, false)).isEmpty();
    }

    // ------------------------------------------------------------------
    // 人数与批量回填
    // ------------------------------------------------------------------

    @Test
    void shouldCountHeadcountIncludingSubDepartments() {
        stubData(
                List.of(dept(1L, null, "总部", 0), dept(2L, 1L, "研发部", 0)),
                List.of(
                        employee(10L, 1L, null, null, EmployeeStatus.ACTIVE, "E10", "张三"),
                        employee(11L, 2L, null, null, EmployeeStatus.ACTIVE, "E11", "李四"),
                        employee(12L, 2L, null, null, EmployeeStatus.PROBATION, "E12", "王五"),
                        employee(13L, 2L, null, null, EmployeeStatus.TERMINATED, "E13", "赵六")));

        List<OrgTreeNodeVO> roots = employeeService.orgTree(null, null, null, false);

        assertThat(roots.get(0).getHeadcount()).isEqualTo(3);
        assertThat(roots.get(0).getChildren().get(0).getHeadcount()).isEqualTo(2);
    }

    @Test
    void shouldFillNamesWithBatchQueriesOnly() {
        stubData(
                List.of(dept(1L, null, "总部", 0)),
                List.of(
                        employee(10L, 1L, null, 11L, EmployeeStatus.ACTIVE, "E10", "张三"),
                        employee(11L, 1L, 10L, 11L, EmployeeStatus.ACTIVE, "E11", "李四")));
        when(jobLevelMapper.selectBatchIds(any()))
                .thenReturn(List.of(level(11L, "P6", "资深工程师", 9)));

        List<OrgTreeNodeVO> roots = employeeService.orgTree(null, null, null, false);

        assertThat(roots.get(0).getChildren().get(0).getLevelName()).isEqualTo("资深工程师");
        assertThat(roots.get(0).getChildren().get(0).getLevelCode()).isEqualTo("P6");
        // 员工 / 部门 / 职级各一次批量查询，禁止逐行查询（N+1）
        verify(employeeMapper, times(1)).selectList(any());
        verify(deptMapper, times(1)).selectList(any());
        verify(jobLevelMapper, times(1)).selectBatchIds(any());
        verify(employeeMapper, never()).selectById(anyLong());
    }

    @Test
    void shouldFallbackToUserNameWhenDeptLeaderHasNoEmployeeProfile() {
        SysDept head = dept(1L, null, "总公司", 0);
        head.setLeaderId(1L);
        stubData(List.of(head), List.of());
        when(userMapper.selectBatchIds(any())).thenReturn(List.of(user(1L, "系统管理员")));

        List<OrgTreeNodeVO> roots = employeeService.orgTree(null, null, null, false);

        assertThat(roots).hasSize(1);
        assertThat(roots.get(0).getLeaderName()).isEqualTo("系统管理员");
        verify(userMapper, times(1)).selectBatchIds(any());
    }

    @Test
    void shouldPreferEmployeeProfileNameWhenDeptLeaderHasEmployee() {
        SysDept head = dept(1L, null, "总公司", 0);
        head.setLeaderId(900L);
        stubData(
                List.of(head),
                List.of(
                        employeeWithUser(
                                10L, 1L, null, null, EmployeeStatus.ACTIVE, "E10", "档案姓名", 900L)));
        when(userMapper.selectBatchIds(any())).thenReturn(List.of(user(900L, "账号姓名")));

        List<OrgTreeNodeVO> roots = employeeService.orgTree(null, null, null, false);

        assertThat(roots.get(0).getLeaderName()).isEqualTo("档案姓名");
    }

    @Test
    void shouldResolveAllDeptLeaderNamesWithSingleBatchQuery() {
        SysDept head = dept(1L, null, "总公司", 0);
        head.setLeaderId(1L);
        SysDept dev = dept(2L, 1L, "研发部", 0);
        dev.setLeaderId(1L);
        SysDept finance = dept(3L, null, "财务部", 1);
        finance.setLeaderId(2L);
        stubData(List.of(head, dev, finance), List.of());
        when(userMapper.selectBatchIds(any()))
                .thenReturn(List.of(user(1L, "系统管理员"), user(2L, "财务负责人")));

        List<OrgTreeNodeVO> roots = employeeService.orgTree(null, null, null, false);

        // 负责人用户 ID 去重后只发一次 IN 查询
        verify(userMapper, times(1)).selectBatchIds(any());
        assertThat(roots).hasSize(2);
        assertThat(roots.get(0).getLeaderName()).isEqualTo("系统管理员");
        assertThat(roots.get(0).getChildren().get(0).getLeaderName()).isEqualTo("系统管理员");
        assertThat(roots.get(1).getLeaderName()).isEqualTo("财务负责人");
    }

    // ------------------------------------------------------------------
    // 懒加载：roots / children
    // ------------------------------------------------------------------

    @Test
    void rootsShouldTrimChildrenAndKeepHasChildren() {
        stubData(
                List.of(dept(1L, null, "总部", 0), dept(2L, 1L, "研发部", 0)),
                List.of(
                        employee(10L, 1L, null, null, EmployeeStatus.ACTIVE, "E10", "张三"),
                        employee(20L, null, null, null, EmployeeStatus.ACTIVE, "E20", "李四")));

        List<OrgTreeNodeVO> roots = employeeService.orgTreeRoots(null, null, null, false);

        // 未分配部门虚拟节点仍在最后
        assertThat(ids(roots)).containsExactly(1L, "UNASSIGNED");
        // children 裁剪为空，展开能力由 hasChildren 表达
        assertThat(roots.get(0).getChildren()).isEmpty();
        assertThat(roots.get(0).getHasChildren()).isTrue();
        assertThat(roots.get(1).getChildren()).isEmpty();
        assertThat(roots.get(1).getHasChildren()).isTrue();
    }

    @Test
    void rootsShouldMarkLeafNodeWithoutChildren() {
        SysDept leaf = dept(1L, null, "空部门", 0);
        stubData(List.of(leaf), List.of());

        List<OrgTreeNodeVO> roots = employeeService.orgTreeRoots(null, null, null, false);

        assertThat(ids(roots)).containsExactly(1L);
        assertThat(roots.get(0).getHasChildren()).isFalse();
        assertThat(roots.get(0).getChildren()).isEmpty();
    }

    @Test
    void childrenOfDeptShouldPutSubDepartmentsFirstAndTrimChildren() {
        stubData(
                List.of(
                        dept(1L, null, "总部", 0),
                        dept(2L, 1L, "研发部", 1),
                        dept(3L, 1L, "财务部", 0)),
                List.of(
                        employee(10L, 1L, null, null, EmployeeStatus.ACTIVE, "E10", "张三"),
                        employee(20L, 2L, null, null, EmployeeStatus.ACTIVE, "E20", "李四")));

        List<OrgTreeNodeVO> children =
                employeeService.orgTreeChildren(
                        "DEPT", 1L, null, null, null, false);

        // 子部门按 sort_order 升序在前，员工根节点追加其后
        assertThat(ids(children)).containsExactly(3L, 2L, 10L);
        assertThat(children.get(0).getNodeType()).isEqualTo(OrgTreeNodeVO.NodeType.DEPT);
        assertThat(children.get(0).getHasChildren()).isFalse();
        assertThat(children.get(1).getHasChildren()).isTrue();
        assertThat(children.get(2).getNodeType()).isEqualTo(OrgTreeNodeVO.NodeType.EMPLOYEE);
        assertThat(children.get(2).getHasChildren()).isFalse();
        children.forEach(node -> assertThat(node.getChildren()).isEmpty());
    }

    @Test
    void childrenOfEmployeeShouldReturnDirectSubordinatesWithExpandFlag() {
        stubData(
                List.of(dept(1L, null, "总部", 0)),
                List.of(
                        employee(10L, 1L, null, null, EmployeeStatus.ACTIVE, "E10", "张三"),
                        employee(11L, 1L, 10L, null, EmployeeStatus.ACTIVE, "E11", "李四"),
                        employee(12L, 1L, 11L, null, EmployeeStatus.ACTIVE, "E12", "王五")));

        List<OrgTreeNodeVO> firstLevel =
                employeeService.orgTreeChildren("EMPLOYEE", 10L, null, null, null, false);
        assertThat(ids(firstLevel)).containsExactly(11L);
        assertThat(firstLevel.get(0).getChildren()).isEmpty();
        assertThat(firstLevel.get(0).getHasChildren()).isTrue();

        List<OrgTreeNodeVO> secondLevel =
                employeeService.orgTreeChildren("EMPLOYEE", 11L, null, null, null, false);
        assertThat(ids(secondLevel)).containsExactly(12L);
        assertThat(secondLevel.get(0).getHasChildren()).isFalse();
    }

    @Test
    void childrenShouldReturnEmptyForInvalidTypeOrMissingNode() {
        stubData(
                List.of(dept(1L, null, "总部", 0)),
                List.of(employee(10L, 1L, null, null, EmployeeStatus.ACTIVE, "E10", "张三")));

        assertThat(employeeService.orgTreeChildren("BOSS", 1L, null, null, null, false)).isEmpty();
        assertThat(employeeService.orgTreeChildren(null, 1L, null, null, null, false)).isEmpty();
        assertThat(employeeService.orgTreeChildren("DEPT", null, null, null, null, false)).isEmpty();
        assertThat(employeeService.orgTreeChildren("DEPT", 999L, null, null, null, false)).isEmpty();
        assertThat(employeeService.orgTreeChildren("EMPLOYEE", 999L, null, null, null, false)).isEmpty();
    }

    @Test
    void shouldCountTotalHeadcountIncludingTerminatedAndSubDepartments() {
        stubData(
                List.of(dept(1L, null, "总部", 0), dept(2L, 1L, "研发部", 0)),
                List.of(
                        employee(10L, 1L, null, null, EmployeeStatus.ACTIVE, "E10", "张三"),
                        employee(11L, 2L, null, null, EmployeeStatus.ACTIVE, "E11", "李四"),
                        employee(12L, 2L, null, null, EmployeeStatus.PROBATION, "E12", "王五"),
                        employee(13L, 2L, null, null, EmployeeStatus.TERMINATED, "E13", "赵六")));

        List<OrgTreeNodeVO> roots = employeeService.orgTree(null, null, null, false);

        OrgTreeNodeVO head = roots.get(0);
        OrgTreeNodeVO dev = head.getChildren().get(0);
        // headcount 仍为在职口径（排除离职）
        assertThat(head.getHeadcount()).isEqualTo(3);
        assertThat(dev.getHeadcount()).isEqualTo(2);
        // totalHeadcount 含离职且含子部门
        assertThat(head.getTotalHeadcount()).isEqualTo(4);
        assertThat(dev.getTotalHeadcount()).isEqualTo(3);
    }

    @Test
    void shouldFillPositionNameWithSingleBatchQuery() {
        HrEmployee first = employee(10L, 1L, null, null, EmployeeStatus.ACTIVE, "E10", "张三");
        first.setPositionId(7L);
        HrEmployee second = employee(11L, 1L, null, null, EmployeeStatus.ACTIVE, "E11", "李四");
        second.setPositionId(8L);
        stubData(List.of(dept(1L, null, "总部", 0)), List.of(first, second));
        when(positionMapper.selectBatchIds(any()))
                .thenReturn(List.of(position(7L, "后端工程师"), position(8L, "产品经理")));

        List<OrgTreeNodeVO> roots = employeeService.orgTree(null, null, null, false);

        OrgTreeNodeVO firstNode = roots.get(0).getChildren().get(0);
        OrgTreeNodeVO secondNode = roots.get(0).getChildren().get(1);
        assertThat(firstNode.getPositionName()).isEqualTo("后端工程师");
        assertThat(secondNode.getPositionName()).isEqualTo("产品经理");
        // 岗位名称一次 IN 批量回填，禁止逐行查询
        verify(positionMapper, times(1)).selectBatchIds(any());
    }

    // ------------------------------------------------------------------
    // 员工分页：deptId 含子部门
    // ------------------------------------------------------------------

    @Test
    void pageShouldIncludeAllSubDepartments() {
        when(deptMapper.selectList(any()))
                .thenReturn(List.of(dept(1L, null, "总部", 0), dept(2L, 1L, "研发部", 0)));
        Page<HrEmployee> page = new Page<>(1, 10);
        page.setRecords(List.of());
        page.setTotal(0);
        when(employeeMapper.selectPage(any(), any())).thenReturn(page);

        employeeService.page(1, 10, 1L, null, null);

        ArgumentCaptor<Wrapper> captor = ArgumentCaptor.forClass(Wrapper.class);
        verify(employeeMapper).selectPage(any(), captor.capture());
        LambdaQueryWrapper<HrEmployee> wrapper = (LambdaQueryWrapper<HrEmployee>) captor.getValue();
        assertThat(wrapper.getSqlSegment()).contains("dept_id IN");
        assertThat(wrapper.getParamNameValuePairs().values()).contains(1L, 2L);
    }

    @Test
    void pageShouldReturnEmptyWhenDepartmentMissing() {
        when(deptMapper.selectList(any())).thenReturn(List.of(dept(1L, null, "总部", 0)));

        PageResult<EmployeeVO> result = employeeService.page(1, 10, 99L, null, null);

        assertThat(result.getRecords()).isEmpty();
        assertThat(result.getTotal()).isZero();
        verify(employeeMapper, never()).selectPage(any(), any());
    }

    // ------------------------------------------------------------------
    // 夹具
    // ------------------------------------------------------------------

    private void stubData(List<SysDept> depts, List<HrEmployee> employees) {
        when(deptMapper.selectList(any())).thenReturn(depts);
        when(employeeMapper.selectList(any())).thenReturn(employees);
    }

    private List<Object> ids(List<OrgTreeNodeVO> nodes) {
        return nodes.stream().map(OrgTreeNodeVO::getId).toList();
    }

    private SysDept dept(Long id, Long parentId, String name, Integer sortOrder) {
        SysDept dept = new SysDept();
        dept.setId(id);
        dept.setParentId(parentId);
        dept.setName(name);
        dept.setSortOrder(sortOrder);
        return dept;
    }

    private HrEmployee employee(
            Long id,
            Long deptId,
            Long leaderId,
            Long levelId,
            EmployeeStatus status,
            String employeeNo,
            String name) {
        return employeeWithUser(id, deptId, leaderId, levelId, status, employeeNo, name, null);
    }

    private HrEmployee employeeWithUser(
            Long id,
            Long deptId,
            Long leaderId,
            Long levelId,
            EmployeeStatus status,
            String employeeNo,
            String name,
            Long userId) {
        HrEmployee employee = new HrEmployee();
        employee.setId(id);
        employee.setDeptId(deptId);
        employee.setLeaderId(leaderId);
        employee.setLevelId(levelId);
        employee.setStatus(status);
        employee.setEmployeeNo(employeeNo);
        employee.setName(name);
        employee.setUserId(userId);
        return employee;
    }

    private HrJobLevel level(Long id, String code, String name, Integer rankNo) {
        HrJobLevel level = new HrJobLevel();
        level.setId(id);
        level.setCode(code);
        level.setName(name);
        level.setRankNo(rankNo);
        return level;
    }

    private HrPosition position(Long id, String name) {
        HrPosition position = new HrPosition();
        position.setId(id);
        position.setName(name);
        return position;
    }

    private SysUser user(Long id, String name) {
        SysUser user = new SysUser();
        user.setId(id);
        user.setName(name);
        return user;
    }
}
