package com.ceams.module.bpm.support;

import lombok.Data;

/**
 * 简单条件定义（服务端求值，不支持脚本）。
 *
 * <p>支持三类条件：表单字段数值比较、发起人部门、发起人角色；条件不命中走默认出口。
 */
@Data
public class BpmSchemaCondition {

    /** 字段名（表单字段或 initiatorDept/initiatorRole 等保留字段）。 */
    private String field;

    /** 比较运算符：EQ/NE/GT/GE/LT/LE。 */
    private String operator;

    /** 比较值（统一字符串）。 */
    private String value;

    /** 命中后的目标节点编码。 */
    private String targetNodeKey;
}
