package com.ceams.module.bpm.support;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import lombok.Data;

/**
 * 条件求值上下文：表单变量 + 发起人角色 + 发起人部门（含子部门）。
 *
 * <p>变量来自 bpm_variable；角色与部门来自 IAM（sys_user_role / sys_dept）。
 * 组织数据缺失时对应集合为空，求值器降级为“不命中并告警”，不抛系统异常。
 */
@Data
public class BpmConditionContext {

    /** 流程变量（var_key -> var_value）。 */
    private Map<String, String> variables = new HashMap<>();

    /** 发起人角色编码集合（统一大写，不含 ROLE_ 前缀）。 */
    private Set<String> roles = new HashSet<>();

    /** 发起人所属部门 ID。 */
    private Long initiatorDeptId;

    /** 发起人所属部门子树 ID 集合（含本部门）。 */
    private Set<Long> initiatorDeptIds = new HashSet<>();

    /** 发起人所属部门子树名称集合（用于按部门名匹配）。 */
    private Set<String> initiatorDeptNames = new HashSet<>();
}
