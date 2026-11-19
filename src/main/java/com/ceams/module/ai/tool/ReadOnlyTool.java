package com.ceams.module.ai.tool;

import java.util.Map;

/**
 * 只读工具统一契约。
 *
 * <p>实现类只能调用业务模块 Service 的只读方法，禁止在本模块内编写 SQL、注入 Mapper 或执行任何 写操作。工具名必须与 {@code
 * docs/AI_IMPROVEMENT_PLAN.md} §6 的清单完全一致。
 */
public interface ReadOnlyTool {

    /** 工具名（与方案 §6 表格完全一致）。 */
    String name();

    /** 面向模型的中文说明：何时使用、参数如何填写。 */
    String description();

    /** JSON Schema（type/object/properties/required），不得包含 SQL。 */
    Map<String, Object> parameterSchema();

    /** 执行只读查询；参数非法时抛 {@link com.ceams.common.exception.BusinessException}。 */
    ToolResult execute(Map<String, Object> args);
}
