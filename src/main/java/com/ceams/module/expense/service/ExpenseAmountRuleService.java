package com.ceams.module.expense.service;

import com.ceams.common.response.PageResult;
import com.ceams.module.expense.dto.AmountRuleSaveRequest;
import com.ceams.module.expense.vo.AmountRuleVO;
import java.math.BigDecimal;
import java.util.List;

/** 报销额度分级规则服务。 */
public interface ExpenseAmountRuleService {

    /** 分页查询规则（keyword 匹配规则名称或流程模板编码）。 */
    PageResult<AmountRuleVO> page(long page, long size, String keyword);

    /** 全部启用规则（供下拉选项使用）。 */
    List<AmountRuleVO> options();

    String create(AmountRuleSaveRequest request);

    void update(Long id, AmountRuleSaveRequest request);

    void delete(Long id);

    /**
     * 按报销金额 / 归属项目 / 提交人解析应使用的 BPM 流程模板编码。
     *
     * <p>匹配优先级：项目规则 &gt; 部门规则（含子部门）&gt; 通用规则；无命中时抛业务异常，
     * 由调用方提示用户联系管理员配置额度分级规则（不做静默回退）。
     *
     * @param amount 报销金额
     * @param projectId 报销归属项目 ID（可空）
     * @param userId 提交人用户 ID（可空，用于解析所属部门）
     * @return 命中的 BPM 流程模板编码
     */
    String resolveTemplateCode(BigDecimal amount, Long projectId, Long userId);
}
