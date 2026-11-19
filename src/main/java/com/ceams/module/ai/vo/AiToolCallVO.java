package com.ceams.module.ai.vo;

import java.util.Map;
import lombok.Data;

/** 非流式对话中的工具调用视图：工具名、入参、返回行数。 */
@Data
public class AiToolCallVO {

    /** 只读工具名，如 {@code queryExpenseSummary}。 */
    private String name;

    /** 模型给出的工具入参（可能为空 Map，不返回 null）。 */
    private Map<String, Object> args;

    /** 工具返回的业务数据行数（查询失败时为 0）。 */
    private int rows;
}
