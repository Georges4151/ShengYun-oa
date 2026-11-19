package com.ceams.module.ai.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ceams.common.base.BaseEntity;
import com.ceams.module.ai.enums.AiMessageRole;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** AI 单条消息（ai_message）。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_message")
public class AiMessage extends BaseEntity {

    /** 所属会话ID（逻辑引用 ai_conversation.id）。 */
    private Long conversationId;

    /** 角色：USER/ASSISTANT/TOOL。 */
    private AiMessageRole role;

    /** 消息内容。 */
    private String content;

    /** 工具调用记录（JSON 文本：工具名、参数、返回行数等，不存全量业务数据）。 */
    private String toolCalls;

    /** 模型名。 */
    private String model;

    /** 提示词 token 数。 */
    private Integer promptTokens;

    /** 生成 token 数。 */
    private Integer completionTokens;

    /** 耗时（毫秒）。 */
    private Integer latencyMs;
}
