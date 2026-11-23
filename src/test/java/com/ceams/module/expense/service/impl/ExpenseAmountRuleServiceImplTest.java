package com.ceams.module.expense.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ceams.common.exception.BusinessException;
import com.ceams.common.exception.ErrorCode;
import com.ceams.module.bpm.entity.BpmTemplate;
import com.ceams.module.bpm.entity.BpmTemplateVersion;
import com.ceams.module.bpm.enums.TemplateStatus;
import com.ceams.module.bpm.enums.TemplateVersionStatus;
import com.ceams.module.bpm.mapper.BpmTemplateMapper;
import com.ceams.module.bpm.mapper.BpmTemplateVersionMapper;
import com.ceams.module.bpm.support.BpmOrgResolver;
import com.ceams.module.bpm.support.BpmSecurityGuard;
import com.ceams.module.expense.dto.AmountRuleSaveRequest;
import com.ceams.module.expense.entity.ExpenseAmountRule;
import com.ceams.module.expense.enums.AmountRuleStatus;
import com.ceams.module.expense.mapper.ExpenseAmountRuleMapper;
import com.ceams.module.expense.support.ExpenseAmountRuleMatcher;
import com.ceams.module.iam.entity.SysUser;
import com.ceams.module.iam.mapper.DeptMapper;
import com.ceams.module.iam.mapper.UserMapper;
import com.ceams.module.project.mapper.ProjectMapper;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/** 报销额度规则服务（CRUD 校验与提交匹配）单元测试。 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ExpenseAmountRuleServiceImplTest {

    private static final String TEMPLATE_CODE = "EXPENSE";

    private static final Long USER_ID = 15L;

    private static final Long DEPT_ID = 7L;

    @Mock private ExpenseAmountRuleMapper ruleMapper;
    @Mock private BpmTemplateMapper templateMapper;
    @Mock private BpmTemplateVersionMapper versionMapper;
    @Mock private DeptMapper deptMapper;
    @Mock private ProjectMapper projectMapper;
    @Mock private UserMapper userMapper;
    @Mock private BpmOrgResolver orgResolver;
    @Mock private BpmSecurityGuard securityGuard;

    private ExpenseAmountRuleServiceImpl service;

    @BeforeEach
    void setUp() {
        service =
                new ExpenseAmountRuleServiceImpl(
                        ruleMapper,
                        templateMapper,
                        versionMapper,
                        deptMapper,
                        projectMapper,
                        userMapper,
                        orgResolver,
                        securityGuard,
                        new ExpenseAmountRuleMatcher());
    }

    /** 金额上限必须大于金额下限。 */
    @Test
    void createShouldRejectMaxAmountNotGreaterThanMinAmount() {
        AmountRuleSaveRequest request = request("规则A", "100", "100", null, null, TEMPLATE_CODE);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("金额上限必须大于金额下限")
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.VALID_ILLEGAL_ARGUMENT);
        verify(ruleMapper, never()).insert(any(ExpenseAmountRule.class));
    }

    /** 同一维度（都为 null）区间重叠时拒绝保存。 */
    @Test
    void createShouldRejectOverlappingIntervalInSameDimension() {
        mockUsableTemplate();
        when(ruleMapper.selectList(any()))
                .thenReturn(List.of(rule(9L, "既有规则", "0", "1000", null, null)));

        AmountRuleSaveRequest request = request("规则B", "500", "2000", null, null, TEMPLATE_CODE);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("区间不允许重叠")
                .hasMessageContaining("既有规则");
        verify(ruleMapper, never()).insert(any(ExpenseAmountRule.class));
    }

    /** 不同维度允许重叠，且未传 priority/status 时取默认值。 */
    @Test
    void createShouldAllowSameIntervalInDifferentDimension() {
        mockUsableTemplate();
        when(deptMapper.selectById(DEPT_ID)).thenReturn(new com.ceams.module.iam.entity.SysDept());
        when(ruleMapper.selectList(any())).thenReturn(List.of());

        AmountRuleSaveRequest request = request("规则C", "0", "1000", DEPT_ID, null, TEMPLATE_CODE);
        service.create(request);

        ArgumentCaptor<ExpenseAmountRule> captor = ArgumentCaptor.forClass(ExpenseAmountRule.class);
        verify(ruleMapper).insert(captor.capture());
        ExpenseAmountRule saved = captor.getValue();
        assertThat(saved.getDeptId()).isEqualTo(DEPT_ID);
        assertThat(saved.getPriority()).isZero();
        assertThat(saved.getStatus()).isEqualTo(AmountRuleStatus.ENABLED);
        assertThat(saved.getTemplateCode()).isEqualTo(TEMPLATE_CODE);
    }

    /** 模板未启用时拒绝绑定。 */
    @Test
    void createShouldRejectDisabledTemplate() {
        BpmTemplate template =
                template(TEMPLATE_CODE, TemplateStatus.DISABLED, "EXPENSE", 9001L);
        when(templateMapper.selectOne(any())).thenReturn(template);

        AmountRuleSaveRequest request = request("规则D", "0", "500", null, null, TEMPLATE_CODE);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("流程模板未启用");
    }

    /** 非报销业务类型模板（如项目变更）拒绝绑定。 */
    @Test
    void createShouldRejectNonExpenseTemplate() {
        BpmTemplate template =
                template("PROJECT_CHANGE", TemplateStatus.ENABLED, "CHANGE_REQUEST", 9002L);
        when(templateMapper.selectOne(any())).thenReturn(template);

        AmountRuleSaveRequest request = request("规则E", "0", "500", null, null, "PROJECT_CHANGE");

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("仅支持报销（EXPENSE）类型的流程模板");
    }

    /** 模板当前版本未发布时拒绝绑定。 */
    @Test
    void createShouldRejectTemplateWithoutPublishedVersion() {
        BpmTemplate template = template(TEMPLATE_CODE, TemplateStatus.ENABLED, "EXPENSE", 9001L);
        when(templateMapper.selectOne(any())).thenReturn(template);
        BpmTemplateVersion version = new BpmTemplateVersion();
        version.setId(9001L);
        version.setStatus(TemplateVersionStatus.DRAFT);
        when(versionMapper.selectById(9001L)).thenReturn(version);

        AmountRuleSaveRequest request = request("规则F", "0", "500", null, null, TEMPLATE_CODE);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("流程模板当前版本未发布");
    }

    /** 修改时排除自身，但与同维度其他规则重叠仍需拒绝。 */
    @Test
    void updateShouldRejectOverlapWithOtherRule() {
        ExpenseAmountRule existing = rule(10L, "规则G", "0", "1000", null, null);
        existing.setTemplateCode(TEMPLATE_CODE);
        when(ruleMapper.selectById(10L)).thenReturn(existing);
        mockUsableTemplate();
        when(ruleMapper.selectList(any()))
                .thenReturn(List.of(rule(11L, "其他规则", "800", "2000", null, null)));

        AmountRuleSaveRequest request = request("规则G", "0", "1500", null, null, TEMPLATE_CODE);

        assertThatThrownBy(() -> service.update(10L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("区间不允许重叠")
                .hasMessageContaining("其他规则");
        verify(ruleMapper, never()).updateById(any(ExpenseAmountRule.class));
    }

    /** 提交匹配：命中规则时返回模板编码。 */
    @Test
    void resolveTemplateCodeShouldReturnMatchedTemplateCode() {
        SysUser user = new SysUser();
        user.setId(USER_ID);
        user.setDeptId(DEPT_ID);
        when(userMapper.selectById(USER_ID)).thenReturn(user);
        when(orgResolver.deptSubtreeIds(DEPT_ID)).thenReturn(Set.of(DEPT_ID));
        when(ruleMapper.selectList(any()))
                .thenReturn(List.of(rule(1L, "兜底", "0", null, null, null)));

        String templateCode =
                service.resolveTemplateCode(new BigDecimal("120.50"), null, USER_ID);

        assertThat(templateCode).isEqualTo(TEMPLATE_CODE);
    }

    /** 提交匹配：无规则命中时抛出明确中文提示。 */
    @Test
    void resolveTemplateCodeShouldThrowWhenNoRuleMatched() {
        SysUser user = new SysUser();
        user.setId(USER_ID);
        user.setDeptId(DEPT_ID);
        when(userMapper.selectById(USER_ID)).thenReturn(user);
        when(orgResolver.deptSubtreeIds(DEPT_ID)).thenReturn(Set.of(DEPT_ID));
        when(ruleMapper.selectList(any()))
                .thenReturn(List.of(rule(1L, "小额", "0", "100", null, null)));

        assertThatThrownBy(
                        () ->
                                service.resolveTemplateCode(
                                        new BigDecimal("500"), null, USER_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessage("当前报销金额未匹配到审批流程规则，请联系管理员配置额度分级规则")
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.BIZ_UNPROCESSABLE);
    }

    private void mockUsableTemplate() {
        BpmTemplate template = template(TEMPLATE_CODE, TemplateStatus.ENABLED, "EXPENSE", 9001L);
        when(templateMapper.selectOne(any())).thenReturn(template);
        BpmTemplateVersion version = new BpmTemplateVersion();
        version.setId(9001L);
        version.setStatus(TemplateVersionStatus.PUBLISHED);
        when(versionMapper.selectById(9001L)).thenReturn(version);
    }

    private static BpmTemplate template(
            String code, TemplateStatus status, String bizType, Long currentVersionId) {
        BpmTemplate template = new BpmTemplate();
        template.setCode(code);
        template.setName(code + "-名称");
        template.setStatus(status);
        template.setBizType(bizType);
        template.setCurrentVersionId(currentVersionId);
        return template;
    }

    private static ExpenseAmountRule rule(
            Long id,
            String name,
            String minAmount,
            String maxAmount,
            Long deptId,
            Long projectId) {
        ExpenseAmountRule rule = new ExpenseAmountRule();
        rule.setId(id);
        rule.setName(name);
        rule.setMinAmount(new BigDecimal(minAmount));
        rule.setMaxAmount(maxAmount == null ? null : new BigDecimal(maxAmount));
        rule.setDeptId(deptId);
        rule.setProjectId(projectId);
        rule.setTemplateCode(TEMPLATE_CODE);
        rule.setPriority(0);
        rule.setStatus(AmountRuleStatus.ENABLED);
        return rule;
    }

    private static AmountRuleSaveRequest request(
            String name,
            String minAmount,
            String maxAmount,
            Long deptId,
            Long projectId,
            String templateCode) {
        AmountRuleSaveRequest request = new AmountRuleSaveRequest();
        request.setName(name);
        request.setMinAmount(new BigDecimal(minAmount));
        request.setMaxAmount(maxAmount == null ? null : new BigDecimal(maxAmount));
        request.setDeptId(deptId);
        request.setProjectId(projectId);
        request.setTemplateCode(templateCode);
        return request;
    }
}
