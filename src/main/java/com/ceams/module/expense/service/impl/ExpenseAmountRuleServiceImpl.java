package com.ceams.module.expense.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ceams.common.exception.BusinessException;
import com.ceams.common.exception.ErrorCode;
import com.ceams.common.response.PageResult;
import com.ceams.module.bpm.entity.BpmTemplate;
import com.ceams.module.bpm.entity.BpmTemplateVersion;
import com.ceams.module.bpm.enums.BpmBizType;
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
import com.ceams.module.expense.service.ExpenseAmountRuleService;
import com.ceams.module.expense.support.ExpenseAmountRuleMatcher;
import com.ceams.module.expense.util.PageUtil;
import com.ceams.module.expense.vo.AmountRuleVO;
import com.ceams.module.iam.entity.SysDept;
import com.ceams.module.iam.entity.SysUser;
import com.ceams.module.iam.mapper.DeptMapper;
import com.ceams.module.iam.mapper.UserMapper;
import com.ceams.module.project.entity.Project;
import com.ceams.module.project.mapper.ProjectMapper;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 报销额度分级规则服务实现。 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExpenseAmountRuleServiceImpl implements ExpenseAmountRuleService {

    /** 规则维护权限点。 */
    private static final String PERMISSION_MANAGE = "expense:amount-rule:manage";

    /** 报销业务类型。 */
    private static final String BIZ_TYPE_EXPENSE = BpmBizType.EXPENSE.name();

    /** 无规则命中的统一提示。 */
    private static final String NO_RULE_MESSAGE =
            "当前报销金额未匹配到审批流程规则，请联系管理员配置额度分级规则";

    private final ExpenseAmountRuleMapper ruleMapper;
    private final BpmTemplateMapper templateMapper;
    private final BpmTemplateVersionMapper versionMapper;
    private final DeptMapper deptMapper;
    private final ProjectMapper projectMapper;
    private final UserMapper userMapper;
    private final BpmOrgResolver orgResolver;
    private final BpmSecurityGuard securityGuard;
    private final ExpenseAmountRuleMatcher matcher;

    @Override
    public PageResult<AmountRuleVO> page(long page, long size, String keyword) {
        securityGuard.requireAdminOrPermission(PERMISSION_MANAGE);
        Page<ExpenseAmountRule> p = PageUtil.resolve(page, size);
        LambdaQueryWrapper<ExpenseAmountRule> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.isNotBlank(keyword)) {
            wrapper.and(
                    w ->
                            w.like(ExpenseAmountRule::getName, keyword)
                                    .or()
                                    .like(ExpenseAmountRule::getTemplateCode, keyword));
        }
        wrapper.orderByDesc(ExpenseAmountRule::getPriority)
                .orderByAsc(ExpenseAmountRule::getMinAmount)
                .orderByDesc(ExpenseAmountRule::getCreateTime);
        IPage<ExpenseAmountRule> result = ruleMapper.selectPage(p, wrapper);
        return new PageResult<>(
                toVOList(result.getRecords()),
                result.getTotal(),
                result.getCurrent(),
                result.getSize());
    }

    @Override
    public List<AmountRuleVO> options() {
        securityGuard.requireAdminOrPermission(PERMISSION_MANAGE);
        LambdaQueryWrapper<ExpenseAmountRule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ExpenseAmountRule::getStatus, AmountRuleStatus.ENABLED)
                .orderByDesc(ExpenseAmountRule::getPriority)
                .orderByAsc(ExpenseAmountRule::getMinAmount)
                .orderByDesc(ExpenseAmountRule::getCreateTime);
        return toVOList(ruleMapper.selectList(wrapper));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String create(AmountRuleSaveRequest request) {
        securityGuard.requireAdminOrPermission(PERMISSION_MANAGE);
        ExpenseAmountRule rule = new ExpenseAmountRule();
        applyRequest(rule, request);
        validateRule(rule, null);
        ruleMapper.insert(rule);
        log.info("新增报销额度规则：id={}，name={}，templateCode={}", rule.getId(), rule.getName(), rule.getTemplateCode());
        return String.valueOf(rule.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, AmountRuleSaveRequest request) {
        securityGuard.requireAdminOrPermission(PERMISSION_MANAGE);
        ExpenseAmountRule rule = requireRule(id);
        applyRequest(rule, request);
        validateRule(rule, id);
        ruleMapper.updateById(rule);
        log.info("修改报销额度规则：id={}，name={}，templateCode={}", id, rule.getName(), rule.getTemplateCode());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        securityGuard.requireAdminOrPermission(PERMISSION_MANAGE);
        requireRule(id);
        // 历史流程实例已绑定 template_version_id，删除规则不影响在途/历史审批
        ruleMapper.deleteById(id);
        log.info("删除报销额度规则：id={}", id);
    }

    @Override
    public String resolveTemplateCode(BigDecimal amount, Long projectId, Long userId) {
        if (amount == null) {
            throw BusinessException.of(ErrorCode.VALID_ILLEGAL_ARGUMENT, "报销金额不能为空");
        }
        Long deptId = null;
        if (userId != null) {
            SysUser user = userMapper.selectById(userId);
            if (user != null) {
                deptId = user.getDeptId();
            }
        }
        Set<Long> deptSubtreeIds = orgResolver.deptSubtreeIds(deptId);
        LambdaQueryWrapper<ExpenseAmountRule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ExpenseAmountRule::getStatus, AmountRuleStatus.ENABLED);
        List<ExpenseAmountRule> candidates = ruleMapper.selectList(wrapper);
        return matcher.match(candidates, amount, projectId, deptSubtreeIds)
                .map(ExpenseAmountRule::getTemplateCode)
                .orElseThrow(
                        () -> BusinessException.of(ErrorCode.BIZ_UNPROCESSABLE, NO_RULE_MESSAGE));
    }

    /** 保存请求 → 实体（PUT 全量覆盖语义，未传优先级/状态时取默认值）。 */
    private void applyRequest(ExpenseAmountRule rule, AmountRuleSaveRequest request) {
        rule.setName(request.getName().trim());
        rule.setMinAmount(request.getMinAmount());
        rule.setMaxAmount(request.getMaxAmount());
        rule.setDeptId(request.getDeptId());
        rule.setProjectId(request.getProjectId());
        rule.setTemplateCode(request.getTemplateCode().trim());
        rule.setPriority(request.getPriority() == null ? 0 : request.getPriority());
        rule.setStatus(request.getStatus() == null ? AmountRuleStatus.ENABLED : request.getStatus());
        rule.setRemark(request.getRemark());
    }

    /** 保存前校验：区间合法性、部门/项目存在性、模板可用性、同维度区间不重叠。 */
    private void validateRule(ExpenseAmountRule rule, Long excludeId) {
        if (rule.getMinAmount() == null || rule.getMinAmount().compareTo(BigDecimal.ZERO) < 0) {
            throw BusinessException.of(ErrorCode.VALID_ILLEGAL_ARGUMENT, "金额下限不能为空且不能为负");
        }
        if (rule.getMaxAmount() != null && rule.getMaxAmount().compareTo(rule.getMinAmount()) <= 0) {
            throw BusinessException.of(ErrorCode.VALID_ILLEGAL_ARGUMENT, "金额上限必须大于金额下限");
        }
        if (rule.getDeptId() != null && deptMapper.selectById(rule.getDeptId()) == null) {
            throw BusinessException.of(
                    ErrorCode.VALID_ILLEGAL_ARGUMENT, "部门不存在：" + rule.getDeptId());
        }
        if (rule.getProjectId() != null && projectMapper.selectById(rule.getProjectId()) == null) {
            throw BusinessException.of(
                    ErrorCode.VALID_ILLEGAL_ARGUMENT, "项目不存在：" + rule.getProjectId());
        }
        requireUsableExpenseTemplate(rule.getTemplateCode());
        requireNoOverlap(rule, excludeId);
    }

    /** 模板必须是存在、启用、EXPENSE 业务类型且当前版本已发布。 */
    private void requireUsableExpenseTemplate(String templateCode) {
        BpmTemplate template =
                templateMapper.selectOne(
                        new LambdaQueryWrapper<BpmTemplate>()
                                .eq(BpmTemplate::getCode, templateCode));
        if (template == null) {
            throw BusinessException.of(
                    ErrorCode.VALID_ILLEGAL_ARGUMENT, "流程模板不存在：" + templateCode);
        }
        if (template.getStatus() != TemplateStatus.ENABLED) {
            throw BusinessException.of(
                    ErrorCode.VALID_ILLEGAL_ARGUMENT, "流程模板未启用：" + templateCode);
        }
        if (!BIZ_TYPE_EXPENSE.equals(template.getBizType())) {
            throw BusinessException.of(
                    ErrorCode.VALID_ILLEGAL_ARGUMENT,
                    "仅支持报销（EXPENSE）类型的流程模板：" + templateCode);
        }
        if (template.getCurrentVersionId() == null) {
            throw BusinessException.of(
                    ErrorCode.VALID_ILLEGAL_ARGUMENT, "流程模板尚未发布可用版本：" + templateCode);
        }
        BpmTemplateVersion version = versionMapper.selectById(template.getCurrentVersionId());
        if (version == null || version.getStatus() != TemplateVersionStatus.PUBLISHED) {
            throw BusinessException.of(
                    ErrorCode.VALID_ILLEGAL_ARGUMENT, "流程模板当前版本未发布：" + templateCode);
        }
    }

    /** 同一维度（deptId + projectId，含都为 null）下金额区间不允许重叠。 */
    private void requireNoOverlap(ExpenseAmountRule rule, Long excludeId) {
        LambdaQueryWrapper<ExpenseAmountRule> wrapper = new LambdaQueryWrapper<>();
        if (rule.getDeptId() == null) {
            wrapper.isNull(ExpenseAmountRule::getDeptId);
        } else {
            wrapper.eq(ExpenseAmountRule::getDeptId, rule.getDeptId());
        }
        if (rule.getProjectId() == null) {
            wrapper.isNull(ExpenseAmountRule::getProjectId);
        } else {
            wrapper.eq(ExpenseAmountRule::getProjectId, rule.getProjectId());
        }
        wrapper.ne(excludeId != null, ExpenseAmountRule::getId, excludeId);
        List<ExpenseAmountRule> sameDimension = ruleMapper.selectList(wrapper);
        if (sameDimension == null) {
            return;
        }
        for (ExpenseAmountRule other : sameDimension) {
            if (other != null && overlaps(rule, other)) {
                throw BusinessException.of(
                        ErrorCode.BIZ_UNPROCESSABLE,
                        "同一维度（部门+项目）下金额区间不允许重叠，与规则「"
                                + other.getName()
                                + "」区间冲突");
            }
        }
    }

    /** 区间重叠判定：[min, max) 半开区间，max 为空视为 +∞。 */
    private static boolean overlaps(ExpenseAmountRule a, ExpenseAmountRule b) {
        boolean aEndsBeforeB =
                a.getMaxAmount() != null
                        && b.getMinAmount() != null
                        && b.getMinAmount().compareTo(a.getMaxAmount()) >= 0;
        boolean bEndsBeforeA =
                b.getMaxAmount() != null
                        && a.getMinAmount() != null
                        && a.getMinAmount().compareTo(b.getMaxAmount()) >= 0;
        return !aEndsBeforeB && !bEndsBeforeA;
    }

    private ExpenseAmountRule requireRule(Long id) {
        ExpenseAmountRule rule = ruleMapper.selectById(id);
        if (rule == null) {
            throw BusinessException.of(ErrorCode.BIZ_NOT_FOUND, "额度规则不存在");
        }
        return rule;
    }

    private List<AmountRuleVO> toVOList(List<ExpenseAmountRule> rules) {
        if (rules == null || rules.isEmpty()) {
            return List.of();
        }
        Map<Long, String> deptNames = loadDeptNames(rules);
        Map<Long, String> projectNames = loadProjectNames(rules);
        Map<String, String> templateNames = loadTemplateNames(rules);
        List<AmountRuleVO> result = new ArrayList<>(rules.size());
        for (ExpenseAmountRule rule : rules) {
            result.add(toVO(rule, deptNames, projectNames, templateNames));
        }
        return result;
    }

    private AmountRuleVO toVO(
            ExpenseAmountRule rule,
            Map<Long, String> deptNames,
            Map<Long, String> projectNames,
            Map<String, String> templateNames) {
        AmountRuleVO vo = new AmountRuleVO();
        vo.setId(rule.getId());
        vo.setName(rule.getName());
        vo.setMinAmount(rule.getMinAmount());
        vo.setMaxAmount(rule.getMaxAmount());
        vo.setDeptId(rule.getDeptId());
        vo.setDeptName(rule.getDeptId() == null ? null : deptNames.get(rule.getDeptId()));
        vo.setProjectId(rule.getProjectId());
        vo.setProjectName(rule.getProjectId() == null ? null : projectNames.get(rule.getProjectId()));
        vo.setTemplateCode(rule.getTemplateCode());
        vo.setTemplateName(
                rule.getTemplateCode() == null ? null : templateNames.get(rule.getTemplateCode()));
        vo.setPriority(rule.getPriority());
        vo.setStatus(rule.getStatus());
        vo.setRemark(rule.getRemark());
        vo.setCreateTime(rule.getCreateTime());
        vo.setUpdateTime(rule.getUpdateTime());
        return vo;
    }

    private Map<Long, String> loadDeptNames(List<ExpenseAmountRule> rules) {
        Set<Long> deptIds =
                rules.stream()
                        .map(ExpenseAmountRule::getDeptId)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());
        if (deptIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, String> names = new HashMap<>();
        for (SysDept dept : deptMapper.selectBatchIds(deptIds)) {
            if (dept != null) {
                names.put(dept.getId(), dept.getName());
            }
        }
        return names;
    }

    private Map<Long, String> loadProjectNames(List<ExpenseAmountRule> rules) {
        Set<Long> projectIds =
                rules.stream()
                        .map(ExpenseAmountRule::getProjectId)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());
        if (projectIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, String> names = new HashMap<>();
        for (Project project : projectMapper.selectBatchIds(projectIds)) {
            if (project != null) {
                names.put(project.getId(), project.getProjectName());
            }
        }
        return names;
    }

    private Map<String, String> loadTemplateNames(List<ExpenseAmountRule> rules) {
        Set<String> codes =
                rules.stream()
                        .map(ExpenseAmountRule::getTemplateCode)
                        .filter(StringUtils::isNotBlank)
                        .collect(Collectors.toSet());
        if (codes.isEmpty()) {
            return Map.of();
        }
        Map<String, String> names = new HashMap<>();
        for (BpmTemplate template :
                templateMapper.selectList(
                        new LambdaQueryWrapper<BpmTemplate>().in(BpmTemplate::getCode, codes))) {
            if (template != null) {
                names.put(template.getCode(), template.getName());
            }
        }
        return names;
    }
}
